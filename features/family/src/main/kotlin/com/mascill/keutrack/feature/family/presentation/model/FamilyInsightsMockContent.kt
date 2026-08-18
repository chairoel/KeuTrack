package com.mascill.keutrack.feature.family.presentation.model

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

enum class FamilyHistoryCategoryIcon {
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

fun FamilyHistoryCategoryIcon.toImageVector(): ImageVector =
    when (this) {
        FamilyHistoryCategoryIcon.Restaurant -> Icons.Filled.Restaurant
        FamilyHistoryCategoryIcon.Transport -> Icons.Filled.DirectionsCar
        FamilyHistoryCategoryIcon.Payout -> Icons.Filled.Payments
        FamilyHistoryCategoryIcon.Utilities -> Icons.Filled.ReceiptLong
        FamilyHistoryCategoryIcon.School -> Icons.Filled.School
        FamilyHistoryCategoryIcon.Entertainment -> Icons.Filled.Movie
        FamilyHistoryCategoryIcon.Health -> Icons.Filled.LocalHospital
        FamilyHistoryCategoryIcon.Shopping -> Icons.Filled.ShoppingCart
        FamilyHistoryCategoryIcon.Investment -> Icons.AutoMirrored.Filled.TrendingUp
        FamilyHistoryCategoryIcon.Other -> Icons.Filled.MoreHoriz
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
    val addedByLabel: String,
)

/** Preview-only mock content — not used as a runtime data source. */
data class FamilyInsightsMockContent(
    val monthlyTotalExpense: Long,
    val spendSegments: List<FamilySpendSegment>,
    val budgetRows: List<FamilyBudgetRowUi>,
    val historyRows: List<FamilyHistoryRowUi>,
    val insightTitle: String,
    val insightBody: String,
    val insightCtaLabel: String,
    val showJoinBanner: Boolean = false,
    val hasFamilyWallet: Boolean = true,
)

val DefaultFamilyInsightsMockContent =
    FamilyInsightsMockContent(
        monthlyTotalExpense = 4_850_000L,
        spendSegments =
            listOf(
                FamilySpendSegment(
                    label = "Siti",
                    detail = "65% • Rp 3.152.000",
                    fraction = 0.65f,
                ),
                FamilySpendSegment(
                    label = "Budi",
                    detail = "35% • Rp 1.698.000",
                    fraction = 0.35f,
                ),
            ),
        budgetRows =
            listOf(
                FamilyBudgetRowUi(
                    title = "Household",
                    spentLabel = "Rp 1.200.000",
                    capLabel = "Rp 2.000.000",
                    progress = 0.6f,
                    footnote = "On track — sisa Rp 800.000",
                    tone = FamilyBudgetBarTone.Success,
                    muted = false,
                ),
                FamilyBudgetRowUi(
                    title = "Education",
                    spentLabel = "Rp 950.000",
                    capLabel = "Rp 1.000.000",
                    progress = 0.95f,
                    footnote = "Mendekati limit (5% tersisa)",
                    tone = FamilyBudgetBarTone.Error,
                    muted = false,
                ),
                FamilyBudgetRowUi(
                    title = "Shared Leisure",
                    spentLabel = "Rp 450.000",
                    capLabel = "Rp 800.000",
                    progress = 0.56f,
                    footnote = null,
                    tone = FamilyBudgetBarTone.Primary,
                    muted = true,
                ),
            ),
        historyRows =
            listOf(
                FamilyHistoryRowUi(
                    title = "Monthly Groceries",
                    subtitle = "Belanja",
                    amountLabel = "Rp 245.800",
                    categoryIcon = FamilyHistoryCategoryIcon.Shopping,
                    addedByLabel = "Siti",
                ),
                FamilyHistoryRowUi(
                    title = "Electricity Bill",
                    subtitle = "Tagihan",
                    amountLabel = "Rp 112.000",
                    categoryIcon = FamilyHistoryCategoryIcon.Utilities,
                    addedByLabel = "Budi",
                ),
                FamilyHistoryRowUi(
                    title = "Tuition Fees",
                    subtitle = "Pendidikan",
                    amountLabel = "Rp 850.000",
                    categoryIcon = FamilyHistoryCategoryIcon.School,
                    addedByLabel = "Siti",
                ),
            ),
        insightTitle = "Saving Together",
        insightBody =
            "Pengeluaran keluarga turun 12% dibanding bulan lalu. " +
                "Pertahankan kebiasaan baik ini bersama!",
        insightCtaLabel = "Adjust Targets",
        showJoinBanner = false,
        hasFamilyWallet = true,
    )

/** Preview helper: mock content → production [FamilyUIState]. */
fun FamilyInsightsMockContent.toPreviewUiState(): FamilyUIState =
    FamilyUIState(
        isLoading = false,
        showJoinBanner = showJoinBanner,
        hasFamilyWallet = hasFamilyWallet,
        familyWalletId = if (hasFamilyWallet) "preview-family-wallet" else null,
        monthlyTotalExpense = monthlyTotalExpense,
        spendSegments = spendSegments,
        budgetRows = budgetRows,
        historyRows = historyRows,
        insightTitle = insightTitle,
        insightBody = insightBody,
        insightCtaLabel = insightCtaLabel,
        showInsightCard = insightBody.isNotBlank(),
    )
