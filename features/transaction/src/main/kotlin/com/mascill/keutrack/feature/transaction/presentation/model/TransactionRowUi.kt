package com.mascill.keutrack.feature.transaction.presentation.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.ui.graphics.vector.ImageVector
import com.mascill.keutrack.core.domain.model.SyncStatus

enum class TransactionCategoryIcon {
    Restaurant,
    Transport,
    Payout,
    Utilities,
    School,
    Entertainment,
    Health,
    Shopping,
    Investment,
    Other,
}

data class TransactionRowUi(
    val id: String,
    val title: String,
    val categoryLabel: String,
    val timeLabel: String,
    val amountLabel: String,
    val isExpense: Boolean,
    val walletLabel: String,
    val categoryIcon: TransactionCategoryIcon,
    val syncStatus: SyncStatus = SyncStatus.SYNCED,
)

fun TransactionCategoryIcon.toImageVector(): ImageVector =
    when (this) {
        TransactionCategoryIcon.Restaurant -> Icons.Filled.Restaurant
        TransactionCategoryIcon.Transport -> Icons.Filled.DirectionsCar
        TransactionCategoryIcon.Payout -> Icons.Filled.Payments
        TransactionCategoryIcon.Utilities -> Icons.Filled.ReceiptLong
        TransactionCategoryIcon.School -> Icons.Filled.School
        TransactionCategoryIcon.Entertainment -> Icons.Filled.Movie
        TransactionCategoryIcon.Health -> Icons.Filled.LocalHospital
        TransactionCategoryIcon.Shopping -> Icons.Filled.ShoppingCart
        TransactionCategoryIcon.Investment -> Icons.AutoMirrored.Filled.TrendingUp
        TransactionCategoryIcon.Other -> Icons.Filled.MoreHoriz
    }
