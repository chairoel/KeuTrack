package com.mascill.keutrack.feature.transaction.presentation.model

enum class HistoryScope {
    All,
    Personal,
    Family,
}

data class HistoryUIState(
    val isLoading: Boolean = true,
    val items: List<TransactionRowUi> = emptyList(),
    val errorMessage: String? = null,
    val scope: HistoryScope = HistoryScope.All,
)
