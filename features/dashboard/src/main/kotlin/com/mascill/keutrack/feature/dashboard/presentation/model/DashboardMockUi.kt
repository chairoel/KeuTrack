package com.mascill.keutrack.feature.dashboard.presentation.model

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
    val title: String,
    val categoryLabel: String,
    val timeLabel: String,
    val amountLabel: String,
    val isExpense: Boolean,
    val walletLabel: String,
    val categoryIcon: TransactionCategoryIcon,
    val syncStatus: SyncStatus = SyncStatus.SYNCED,
) {
    val isLocalOnly: Boolean
        get() = syncStatus != SyncStatus.SYNCED
}

/** Preview-only mock content — not used as a runtime data source. */
data class DashboardMockContent(
    val userFirstName: String,
    val avatar: String?,
    val pageTitle: String,
    val personalBalanceLabel: String,
    val personalBalanceAmount: String,
    val personalMonthChangeLabel: String,
    val familyBalanceLabel: String,
    val familyBalanceAmount: String,
    val familySharedSummary: String,
    val transactions: List<TransactionRowUi>,
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

val DefaultDashboardMockContent =
    DashboardMockContent(
        avatar = "",
        userFirstName = "Adhi",
        pageTitle = "Financial Journal",
        personalBalanceLabel = "Current Balance",
        personalBalanceAmount = "IDR 12.450.000",
        personalMonthChangeLabel = "+2.4% this month",
        familyBalanceLabel = "Available Shared",
        familyBalanceAmount = "IDR 45.820.500",
        familySharedSummary = "Shared with 4 people",
        transactions =
            listOf(
                TransactionRowUi(
                    title = "Bakmi GM Restaurant",
                    categoryLabel = "Food & Drinks",
                    timeLabel = "12:45 PM",
                    amountLabel = "IDR 125.000",
                    isExpense = true,
                    walletLabel = "Personal",
                    categoryIcon = TransactionCategoryIcon.Restaurant,
                    syncStatus = SyncStatus.PENDING,
                ),
                TransactionRowUi(
                    title = "GoRide — Office",
                    categoryLabel = "Transport",
                    timeLabel = "Yesterday",
                    amountLabel = "IDR 34.000",
                    isExpense = true,
                    walletLabel = "Personal",
                    categoryIcon = TransactionCategoryIcon.Transport,
                ),
                TransactionRowUi(
                    title = "Salary — March",
                    categoryLabel = "Payout",
                    timeLabel = "Mar 1",
                    amountLabel = "IDR 5.500.000",
                    isExpense = false,
                    walletLabel = "Personal",
                    categoryIcon = TransactionCategoryIcon.Payout,
                ),
                TransactionRowUi(
                    title = "PLN Token",
                    categoryLabel = "Utilities",
                    timeLabel = "Mar 2",
                    amountLabel = "IDR 200.000",
                    isExpense = true,
                    walletLabel = "Family",
                    categoryIcon = TransactionCategoryIcon.Utilities,
                    syncStatus = SyncStatus.FAILED,
                ),
            ),
    )

/** Preview helper: mock content → production [DashboardUIState]. */
fun DashboardMockContent.toPreviewUiState(): DashboardUIState =
    DashboardUIState(
        isLoading = false,
        userFirstName = userFirstName,
        avatarUrl = avatar,
        pageTitle = pageTitle,
        personalBalance = 12_450_000L,
        familyBalance = 45_820_500L,
        familyMemberInitials = listOf("A", "B", "C", "D"),
        familySharedSummary = familySharedSummary,
        monthChangeLabel = personalMonthChangeLabel,
        recentTransactions = transactions,
    )
