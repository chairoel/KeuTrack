package com.mascill.keutrack.core.domain.model

enum class TransactionType(val value: String) {
    INCOME("income"),
    EXPENSE("expense");

    companion object {
        fun fromValue(value: String): TransactionType =
            entries.firstOrNull { it.value == value } ?: EXPENSE
    }
}
