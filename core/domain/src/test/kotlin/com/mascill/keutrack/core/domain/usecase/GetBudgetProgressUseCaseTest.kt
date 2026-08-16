package com.mascill.keutrack.core.domain.usecase

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.mascill.keutrack.core.domain.model.Budget
import com.mascill.keutrack.core.domain.repository.BudgetRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.time.Instant

class GetBudgetProgressUseCaseTest {

    private val repo = mockk<BudgetRepository>()
    private val useCase = GetBudgetProgressUseCase(repo)

    @Test
    fun `delegates month to repository`() = runTest {
        val budget = Budget(
            id = "b-1",
            userId = "user-1",
            categoryId = "cat-food",
            limit = 100_000L,
            spent = 25_000L,
            month = "2026-08",
            createdAt = Instant.parse("2026-08-01T00:00:00Z"),
        )
        every { repo.observeBudgets("2026-08") } returns flowOf(listOf(budget))

        useCase("2026-08").test {
            val budgets = awaitItem()
            assertThat(budgets).hasSize(1)
            assertThat(budgets.first().remaining).isEqualTo(75_000L)
            assertThat(budgets.first().isOverBudget).isFalse()
            awaitComplete()
        }
        verify(exactly = 1) { repo.observeBudgets("2026-08") }
    }

    @Test
    fun `over-budget remaining is negative`() = runTest {
        val budget = Budget(
            id = "b-2",
            userId = "user-1",
            categoryId = "cat-food",
            limit = 10_000L,
            spent = 12_000L,
            month = "2026-08",
            createdAt = Instant.parse("2026-08-01T00:00:00Z"),
        )
        every { repo.observeBudgets("2026-08") } returns flowOf(listOf(budget))

        useCase("2026-08").test {
            val item = awaitItem().first()
            assertThat(item.remaining).isEqualTo(-2_000L)
            assertThat(item.isOverBudget).isTrue()
            awaitComplete()
        }
    }
}
