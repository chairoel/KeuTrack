package com.mascill.keutrack.core.domain.repository

import com.mascill.keutrack.core.domain.model.Budget
import com.mascill.keutrack.core.domain.model.CategorySummary
import kotlinx.coroutines.flow.Flow

interface BudgetRepository {

    fun observeBudgets(month: String): Flow<List<Budget>>

    suspend fun getBudgetById(id: String): Budget?

    suspend fun findFamilyBudget(
        familyId: String,
        categoryId: String,
        month: String,
    ): Budget?

    suspend fun createBudget(budget: Budget)

    suspend fun updateBudget(budget: Budget)

    suspend fun deleteBudget(budgetId: String)

    fun observeMonthlySummary(month: String): Flow<CategorySummary?>

    fun observeMonthlySummaries(months: List<String>): Flow<List<CategorySummary>>
}
