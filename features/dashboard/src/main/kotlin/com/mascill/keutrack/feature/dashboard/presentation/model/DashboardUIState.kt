package com.mascill.keutrack.feature.dashboard.presentation.model

/**
 * Production UI state for the Dashboard screen.
 * Amounts are stored as [Long] (rupiah); format for display in the UI/mapper layer.
 */
data class DashboardUIState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val userFirstName: String = "",
    val avatarUrl: String? = null,
    val pageTitle: String = "Financial Journal",
    val personalBalance: Long = 0L,
    val familyBalance: Long = 0L,
    val familyMemberInitials: List<String> = emptyList(),
    val familySharedSummary: String = "",
    val monthChangeLabel: String? = null,
    val incomeTotal: Long = 0L,
    val expenseTotal: Long = 0L,
    val recentTransactions: List<TransactionRowUi> = emptyList(),
    val isPersonalBalanceVisible: Boolean = true,
    val isFamilyBalanceVisible: Boolean = true,
    val isPersonalWalletSyncing: Boolean = false,
)
