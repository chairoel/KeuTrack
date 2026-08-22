package com.mascill.keutrack.core.data.datasource.local

import com.mascill.keutrack.core.data.db.entity.BudgetEntity

internal suspend fun BudgetLocalDataSource.findBudgetForExpense(
    month: String,
    categoryId: String,
    familyId: String?,
): BudgetEntity? {
    val familyKey = familyId?.takeIf { it.isNotBlank() }
    return if (familyKey != null) {
        getByMonthCategoryAndFamily(month, categoryId, familyKey)
    } else {
        getByMonthCategoryPersonal(month, categoryId)
    }
}
