package com.mascill.keutrack.feature.dashboard.presentation.model

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
)

object NewEntryKeypad {
    const val TRIPLE_ZERO = "000"
    const val BACKSPACE = "⌫"
    val ROWS =
        listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9"),
            listOf(TRIPLE_ZERO, "0", BACKSPACE),
        )
}
