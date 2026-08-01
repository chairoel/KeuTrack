package com.mascill.keutrack.core.domain.model

data class CategorySummary(
    val period: String,
    val userId: String,
    val familyId: String? = null,
    val totalIncome: Long,
    val totalExpense: Long,
    val byCategory: Map<String, CategoryBreakdown>,
    val topExpenseCategoryId: String? = null,
) {
    val netBalance: Long get() = totalIncome - totalExpense
}

data class CategoryBreakdown(
    val name: String,
    val totalExpense: Long,
    val totalIncome: Long,
    val transactionCount: Int,
) {
    fun percentOfTotal(totalExpense: Long): Float =
        if (totalExpense > 0) (this.totalExpense.toFloat() / totalExpense * 100) else 0f
}
