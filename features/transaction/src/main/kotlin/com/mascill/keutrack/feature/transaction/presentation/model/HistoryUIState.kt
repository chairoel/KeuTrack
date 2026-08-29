package com.mascill.keutrack.feature.transaction.presentation.model

import java.time.LocalDate

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
    val periodPreset: HistoryPeriodPreset = HistoryPeriodPreset.All,
    val customFrom: LocalDate? = null,
    val customTo: LocalDate? = null,
    val periodSummaryLabel: String = HistoryPeriodLabels.summary(HistoryPeriodPreset.All),
    val hasActivePeriodFilter: Boolean = false,
    val periodRangeError: String? = null,
    val incomeTotal: Long = 0L,
    val expenseTotal: Long = 0L,
)
