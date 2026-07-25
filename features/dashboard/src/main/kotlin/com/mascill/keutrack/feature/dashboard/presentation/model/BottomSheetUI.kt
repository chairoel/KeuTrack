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
