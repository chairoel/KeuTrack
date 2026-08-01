package com.mascill.keutrack.feature.transaction.presentation.model

data class HistoryUIState(
    val isLoading: Boolean = true,
    val items: List<TransactionRowUi> = emptyList(),
    val errorMessage: String? = null,
)
