package com.mascill.keutrack.core.data.repository

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.mascill.keutrack.core.data.datasource.AuthNetworkDataSource
import com.mascill.keutrack.core.data.datasource.local.BudgetLocalDataSource
import com.mascill.keutrack.core.data.datasource.local.CategorySummaryLocalDataSource
import com.mascill.keutrack.core.data.db.entity.BudgetEntity
import com.mascill.keutrack.core.data.db.entity.CategorySummaryEntity
import com.mascill.keutrack.core.data.mapper.BudgetMapper
import com.mascill.keutrack.core.data.mapper.CategorySummaryMapper
import com.mascill.keutrack.core.data.model.AuthUserResponse
import com.mascill.keutrack.core.data.sync.SyncScheduler
import com.mascill.keutrack.core.domain.model.Budget
import com.mascill.keutrack.core.domain.model.SyncStatus
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.time.Instant

class BudgetRepositoryImplTest {

    private val local = mockk<BudgetLocalDataSource>(relaxed = true)
    private val summaryLocal = mockk<CategorySummaryLocalDataSource>(relaxed = true)
    private val authDS = mockk<AuthNetworkDataSource>(relaxed = true)
    private val syncScheduler = mockk<SyncScheduler>(relaxed = true)
    private val repo = BudgetRepositoryImpl(
        local = local,
        summaryLocal = summaryLocal,
        budgetMapper = BudgetMapper(),
        summaryMapper = CategorySummaryMapper(),
        authNetworkDataSource = authDS,
        syncScheduler = syncScheduler,
    )

    @Test
    fun `observeBudgets maps entities for the month`() = runTest {
        every { local.observeByMonth("2026-08") } returns flowOf(listOf(budgetEntity()))

        repo.observeBudgets("2026-08").test {
            val budgets = awaitItem()
            assertThat(budgets).hasSize(1)
            assertThat(budgets.first().remaining).isEqualTo(75_000L)
            awaitComplete()
        }
    }

    @Test
    fun `createBudget writes pending and keeps caller spent`() = runTest {
        val budget = Budget(
            id = "b-1",
            userId = "user-1",
            categoryId = "cat-food",
            limit = 100_000L,
            spent = 40_000L,
            month = "2026-08",
            createdAt = Instant.parse("2026-08-01T00:00:00Z"),
        )
        coEvery { local.upsert(any()) } just runs

        repo.createBudget(budget)

        coVerify {
            local.upsert(match { it.spent == 40_000L && it.syncStatus == SyncStatus.PENDING.name })
        }
        verify { syncScheduler.enqueueSync() }
    }

    @Test
    fun `findFamilyBudget maps matching local row`() = runTest {
        coEvery {
            local.getByMonthCategoryAndFamily("2026-08", "cat-food", "fam-1")
        } returns budgetEntity().copy(familyId = "fam-1")

        val found = repo.findFamilyBudget("fam-1", "cat-food", "2026-08")

        assertThat(found?.id).isEqualTo("b-1")
        assertThat(found?.familyId).isEqualTo("fam-1")
    }

    @Test
    fun `observeMonthlySummaries returns empty when user is missing`() = runTest {
        every { authDS.getCurrentUser() } returns null

        repo.observeMonthlySummaries(listOf("2026-08")).test {
            assertThat(awaitItem()).isEmpty()
            awaitComplete()
        }
    }

    @Test
    fun `observeMonthlySummaries maps remote-backed local rows`() = runTest {
        every { authDS.getCurrentUser() } returns AuthUserResponse("user-1", "Irul", "a@b.c", null)
        every { summaryLocal.observeByPeriods("user-1", listOf("2026-08")) } returns flowOf(
            listOf(
                CategorySummaryEntity(
                    period = "2026-08",
                    userId = "user-1",
                    familyId = null,
                    totalIncome = 100_000L,
                    totalExpense = 40_000L,
                    byCategoryJson = "{}",
                    topExpenseCategoryId = null,
                ),
            ),
        )

        repo.observeMonthlySummaries(listOf("2026-08")).test {
            val summaries = awaitItem()
            assertThat(summaries.first().netBalance).isEqualTo(60_000L)
            awaitComplete()
        }
    }

    private fun budgetEntity() = BudgetEntity(
        id = "b-1",
        userId = "user-1",
        familyId = null,
        categoryId = "cat-food",
        limit = 100_000L,
        spent = 25_000L,
        period = "monthly",
        month = "2026-08",
        walletId = null,
        syncStatus = "SYNCED",
        createdAtEpochMs = Instant.parse("2026-08-01T00:00:00Z").toEpochMilli(),
    )
}
