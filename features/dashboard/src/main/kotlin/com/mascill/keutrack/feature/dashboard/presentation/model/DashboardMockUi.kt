package com.mascill.keutrack.feature.dashboard.presentation.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.ui.graphics.vector.ImageVector

enum class TransactionCategoryIcon {
    Restaurant,
    Transport,
    Payout,
    Utilities,
}

data class TransactionRowUi(
    val title: String,
    val categoryLabel: String,
    val timeLabel: String,
    val amountLabel: String,
    val isExpense: Boolean,
    val walletLabel: String,
    val categoryIcon: TransactionCategoryIcon,
)

data class DashboardMockContent(
    val userFirstName: String,
    val pageTitle: String,
    val personalBalanceLabel: String,
    val personalBalanceAmount: String,
    val personalMonthChangeLabel: String,
    val familyBalanceLabel: String,
    val familyBalanceAmount: String,
    val familySharedSummary: String,
    val incomeLabel: String,
    val incomeAmount: String,
    val expenseLabel: String,
    val expenseAmount: String,
    val transactions: List<TransactionRowUi>,
)

fun TransactionCategoryIcon.toImageVector(): ImageVector =
    when (this) {
        TransactionCategoryIcon.Restaurant -> Icons.Filled.Restaurant
        TransactionCategoryIcon.Transport -> Icons.Filled.DirectionsCar
        TransactionCategoryIcon.Payout -> Icons.Filled.AttachMoney
        TransactionCategoryIcon.Utilities -> Icons.Filled.ReceiptLong
    }

val DefaultDashboardMockContent =
    DashboardMockContent(
        userFirstName = "Adhi",
        pageTitle = "Financial Journal",
        personalBalanceLabel = "Current Balance",
        personalBalanceAmount = "IDR 12.450.000",
        personalMonthChangeLabel = "+2.4% this month",
        familyBalanceLabel = "Available Shared",
        familyBalanceAmount = "IDR 45.820.500",
        familySharedSummary = "Shared with 4 people",
        incomeLabel = "INCOME",
        incomeAmount = "IDR 8.2M",
        expenseLabel = "EXPENSE",
        expenseAmount = "IDR 3.5M",
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
                ),
            ),
    )
