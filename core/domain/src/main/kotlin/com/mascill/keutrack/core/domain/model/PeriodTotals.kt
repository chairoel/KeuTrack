package com.mascill.keutrack.core.domain.model

data class PeriodTotals(
    val incomeTotal: Long = 0L,
    val expenseTotal: Long = 0L,
) {
    val netBalance: Long get() = incomeTotal - expenseTotal
}
