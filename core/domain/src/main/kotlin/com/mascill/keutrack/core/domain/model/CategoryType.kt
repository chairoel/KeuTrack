package com.mascill.keutrack.core.domain.model

enum class CategoryType(val value: String) {
    INCOME("income"),
    EXPENSE("expense"),
    BOTH("both");

    companion object {
        fun fromValue(value: String): CategoryType =
            entries.firstOrNull { it.value == value } ?: BOTH
    }
}
