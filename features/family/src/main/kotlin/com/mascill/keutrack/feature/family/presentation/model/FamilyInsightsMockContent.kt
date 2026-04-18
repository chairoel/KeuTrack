package com.mascill.keutrack.feature.family.presentation.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.ui.graphics.vector.ImageVector

enum class FamilyHistoryCategoryIcon {
    Groceries,
    Utilities,
    School,
}

fun FamilyHistoryCategoryIcon.toImageVector(): ImageVector =
    when (this) {
        FamilyHistoryCategoryIcon.Groceries -> Icons.Filled.ShoppingCart
        FamilyHistoryCategoryIcon.Utilities -> Icons.Filled.ElectricBolt
        FamilyHistoryCategoryIcon.School -> Icons.Filled.School
    }

enum class FamilyMemberAttribution {
    Husband,
    Wife,
}

enum class FamilyBudgetBarTone {
    Success,
    Error,
    Primary,
}

data class FamilySpendSegment(
    val label: String,
    val detail: String,
    val fraction: Float,
)

data class FamilyBudgetRowUi(
    val title: String,
    val spentLabel: String,
    val capLabel: String,
    val progress: Float,
    val footnote: String?,
    val tone: FamilyBudgetBarTone,
    val muted: Boolean,
)

data class FamilyHistoryRowUi(
    val title: String,
    val subtitle: String,
    val amountLabel: String,
    val categoryIcon: FamilyHistoryCategoryIcon,
    val addedBy: FamilyMemberAttribution,
)

data class FamilyInsightsMockContent(
    val pulseReportLabel: String,
    val breakdownTitle: String,
    val monthlyTotalLabel: String,
    val monthlyTotalAmount: String,
    val spendSegments: List<FamilySpendSegment>,
    val sharedBudgetsTitle: String,
    val budgetRows: List<FamilyBudgetRowUi>,
    val historySectionTitle: String,
    val historyRows: List<FamilyHistoryRowUi>,
    val insightTitle: String,
    val insightBody: String,
    val insightCtaLabel: String,
)

val DefaultFamilyInsightsMockContent =
    FamilyInsightsMockContent(
        pulseReportLabel = "Pulse Report",
        breakdownTitle = "Family Breakdown",
        monthlyTotalLabel = "Monthly Total",
        monthlyTotalAmount = "$4,850",
        spendSegments =
            listOf(
                FamilySpendSegment(
                    label = "Suami (Husband)",
                    detail = "65% • $3,152",
                    fraction = 0.65f,
                ),
                FamilySpendSegment(
                    label = "Istri (Wife)",
                    detail = "35% • $1,698",
                    fraction = 0.35f,
                ),
            ),
        sharedBudgetsTitle = "Shared Budgets",
        budgetRows =
            listOf(
                FamilyBudgetRowUi(
                    title = "Household",
                    spentLabel = "$1,200",
                    capLabel = "$2,000",
                    progress = 0.6f,
                    footnote = "On track to save $200 this month",
                    tone = FamilyBudgetBarTone.Success,
                    muted = false,
                ),
                FamilyBudgetRowUi(
                    title = "Education",
                    spentLabel = "$950",
                    capLabel = "$1,000",
                    progress = 0.95f,
                    footnote = "Approaching limit (5% left)",
                    tone = FamilyBudgetBarTone.Error,
                    muted = false,
                ),
                FamilyBudgetRowUi(
                    title = "Shared Leisure",
                    spentLabel = "$450",
                    capLabel = "$800",
                    progress = 0.56f,
                    footnote = null,
                    tone = FamilyBudgetBarTone.Primary,
                    muted = true,
                ),
            ),
        historySectionTitle = "Family History Log",
        historyRows =
            listOf(
                FamilyHistoryRowUi(
                    title = "Monthly Groceries",
                    subtitle = "Superindo Supermarket",
                    amountLabel = "$245.80",
                    categoryIcon = FamilyHistoryCategoryIcon.Groceries,
                    addedBy = FamilyMemberAttribution.Wife,
                ),
                FamilyHistoryRowUi(
                    title = "Electricity Bill",
                    subtitle = "PLN Monthly Payment",
                    amountLabel = "$112.00",
                    categoryIcon = FamilyHistoryCategoryIcon.Utilities,
                    addedBy = FamilyMemberAttribution.Husband,
                ),
                FamilyHistoryRowUi(
                    title = "Tuition Fees",
                    subtitle = "Kindergarten Q3",
                    amountLabel = "$850.00",
                    categoryIcon = FamilyHistoryCategoryIcon.School,
                    addedBy = FamilyMemberAttribution.Wife,
                ),
            ),
        insightTitle = "Saving Together",
        insightBody =
            "Your family is spending 12% less on Household items compared to last month. " +
                "This covers 45% of your joint vacation goal!",
        insightCtaLabel = "Adjust Targets",
    )
