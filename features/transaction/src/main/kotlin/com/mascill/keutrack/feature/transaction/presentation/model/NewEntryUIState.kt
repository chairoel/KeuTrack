package com.mascill.keutrack.feature.transaction.presentation.model

import java.time.Instant

data class NewEntryUIState(
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val navigateBack: Boolean = false,
    val kind: EntryTransactionKind = EntryTransactionKind.Expense,
    val amount: Long = 0L,
    val categories: List<NewEntryCategoryUI> = emptyList(),
    val selectedCategoryId: String? = null,
    val wallets: List<WalletOptionUi> = emptyList(),
    val selectedWalletId: String? = null,
    val selectedDate: Instant = Instant.now(),
    val note: String = "",
    val userId: String? = null,
    val addedByName: String = "",
) {
    val selectedWallet: WalletOptionUi?
        get() = wallets.firstOrNull { it.id == selectedWalletId }

    val hasWallet: Boolean
        get() = !selectedWalletId.isNullOrBlank()
}
