package com.mascill.keutrack.core.domain.usecase

import com.mascill.keutrack.core.domain.model.CategorySummary
import com.mascill.keutrack.core.domain.repository.BudgetRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

data class MonthlySummaryResult(
    val currentMonth: CategorySummary?,
    val trend: List<CategorySummary>,
)

class GetMonthlySummaryUseCase @Inject constructor(
    private val budgetRepository: BudgetRepository,
) {
    operator fun invoke(
        currentMonth: String,
        trendMonths: List<String> = emptyList(),
    ): Flow<MonthlySummaryResult> =
        budgetRepository.observeMonthlySummaries(
            listOf(currentMonth) + trendMonths
        ).map { summaries ->
            MonthlySummaryResult(
                currentMonth = summaries.firstOrNull { it.period == currentMonth },
                trend = summaries.sortedBy { it.period },
            )
        }
}
