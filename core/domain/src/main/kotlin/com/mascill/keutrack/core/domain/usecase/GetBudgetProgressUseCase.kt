package com.mascill.keutrack.core.domain.usecase

import com.mascill.keutrack.core.domain.model.Budget
import com.mascill.keutrack.core.domain.repository.BudgetRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetBudgetProgressUseCase @Inject constructor(
    private val budgetRepository: BudgetRepository,
) {
    operator fun invoke(month: String): Flow<List<Budget>> =
        budgetRepository.observeBudgets(month)
}
