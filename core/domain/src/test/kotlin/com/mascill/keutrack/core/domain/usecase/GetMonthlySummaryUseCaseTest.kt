package com.mascill.keutrack.core.domain.usecase

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.mascill.keutrack.core.domain.model.CategorySummary
import com.mascill.keutrack.core.domain.repository.BudgetRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test

class GetMonthlySummaryUseCaseTest {

    private val repo = mockk<BudgetRepository>()
    private val useCase = GetMonthlySummaryUseCase(repo)

    @Test
    fun `returns current month and sorted trend`() = runTest {
        val current = summary("2026-08", income = 100_000L, expense = 40_000L)
        val july = summary("2026-07", income = 80_000L, expense = 50_000L)
        val june = summary("2026-06", income = 70_000L, expense = 20_000L)
        every {
            repo.observeMonthlySummaries(listOf("2026-08", "2026-07", "2026-06"))
        } returns flowOf(listOf(current, june, july))

        useCase(currentMonth = "2026-08", trendMonths = listOf("2026-07", "2026-06")).test {
            val result = awaitItem()
            assertThat(result.currentMonth).isEqualTo(current)
            assertThat(result.trend.map { it.period }).containsExactly("2026-06", "2026-07", "2026-08").inOrder()
            awaitComplete()
        }
    }

    @Test
    fun `missing current month yields null currentMonth`() = runTest {
        every { repo.observeMonthlySummaries(listOf("2026-08")) } returns flowOf(emptyList())

        useCase("2026-08").test {
            val result = awaitItem()
            assertThat(result.currentMonth).isNull()
            assertThat(result.trend).isEmpty()
            awaitComplete()
        }
        verify(exactly = 1) { repo.observeMonthlySummaries(listOf("2026-08")) }
    }

    private fun summary(period: String, income: Long, expense: Long) = CategorySummary(
        period = period,
        userId = "user-1",
        totalIncome = income,
        totalExpense = expense,
        byCategory = emptyMap(),
    )
}
