package com.mascill.keutrack.core.domain.model

enum class BudgetPeriod(val value: String) {
    MONTHLY("monthly"),
    WEEKLY("weekly");

    companion object {
        fun fromValue(value: String): BudgetPeriod =
            entries.firstOrNull { it.value == value } ?: MONTHLY
    }
}
