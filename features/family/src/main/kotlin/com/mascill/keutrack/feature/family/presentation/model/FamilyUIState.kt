package com.mascill.keutrack.feature.family.presentation.model

/**
 * Production UI state for the Family Insights screen.
 * Amounts are stored as [Long] (rupiah); format for display in the UI/mapper layer.
 */
data class FamilyUIState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val showJoinBanner: Boolean = false,
    val hasFamilyWallet: Boolean = false,
    val familyWalletId: String? = null,
    val familyName: String? = null,
    val inviteCode: String? = null,
    val monthlyTotalExpense: Long = 0L,
    val spendSegments: List<FamilySpendSegment> = emptyList(),
    val budgetRows: List<FamilyBudgetRowUi> = emptyList(),
    val historyRows: List<FamilyHistoryRowUi> = emptyList(),
    val insightTitle: String = "",
    val insightBody: String = "",
    val insightCtaLabel: String = "",
    val showInsightCard: Boolean = false,
    val isMembershipLoading: Boolean = false,
    val membershipMessage: String? = null,
    val canEditBudgets: Boolean = false,
    val budgetSheet: FamilyBudgetSheetState? = null,
    val isBudgetSaving: Boolean = false,
    val budgetMessage: String? = null,
    val budgetMonthLabel: String = "",
    val expenseCategories: List<FamilyBudgetCategoryOption> = emptyList(),
)

data class FamilyBudgetCategoryOption(
    val id: String,
    val name: String,
)

data class FamilyBudgetSheetState(
    val categoryId: String? = null,
    val categoryLocked: Boolean = false,
    val limitInput: String = "",
    val existingBudgetId: String? = null,
    val errorMessage: String? = null,
)
