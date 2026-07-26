package com.mascill.keutrack.feature.transaction.presentation.model

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

enum class EntryTransactionKind {
    Expense,
    Income,
}

data class NewEntryCategoryUI(
    val id: String,
    val label: String,
    val icon: ImageVector,
    val accent: Color,
    /** Pinned at trailing edge of the horizontal chip row; still last in see-all sheet. */
    val isOther: Boolean = false,
)

data class WalletOptionUi(
    val id: String,
    val name: String,
    val typeLabel: String,
    val familyId: String?,
)
