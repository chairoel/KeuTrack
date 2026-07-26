package com.mascill.keutrack.feature.family.presentation.model

import com.mascill.keutrack.core.designsystem.format.CurrencyFormat
import com.mascill.keutrack.core.domain.model.Budget
import com.mascill.keutrack.core.domain.model.Category
import com.mascill.keutrack.core.domain.model.CategorySummary
import com.mascill.keutrack.core.domain.model.Transaction
import com.mascill.keutrack.core.domain.model.User
import com.mascill.keutrack.core.domain.usecase.MonthlySummaryResult
import com.mascill.keutrack.core.domain.usecase.WalletSummary
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

internal object FamilyUiMapper {

    private const val TOP_SPEND_SEGMENTS = 5
    private const val BUDGET_WARN_THRESHOLD = 0.85f
    private const val DEFAULT_ADDED_BY = "Anggota"
    private const val INSIGHT_TITLE = "Saving Together"
    private const val INSIGHT_CTA = "Adjust Targets"

    fun toUiState(
        user: User?,
        walletSummary: WalletSummary,
        transactions: List<Transaction>,
        monthlySummary: MonthlySummaryResult,
        budgets: List<Budget>,
        categoriesById: Map<String, Category>,
        priorPeriod: String,
    ): FamilyUIState {
        val familyWallet = walletSummary.familyWallets.firstOrNull()
        val familyWalletIds = walletSummary.familyWallets.map { it.id }.toSet()
        val current = monthlySummary.currentMonth
        val prior = priorFromTrend(monthlySummary, priorPeriod)
        val insight = savingTogetherInsight(current = current, prior = prior)

        return FamilyUIState(
            isLoading = false,
            errorMessage = null,
            showJoinBanner = user?.familyId.isNullOrBlank(),
            hasFamilyWallet = familyWallet != null,
            familyWalletId = familyWallet?.id,
            monthlyTotalExpense = current?.totalExpense ?: 0L,
            spendSegments = toSpendSegments(current),
            budgetRows =
                toBudgetRows(
                    budgets = filterSharedBudgets(budgets, familyWalletIds),
                    categoriesById = categoriesById,
                ),
            historyRows =
                toHistoryRows(
                    transactions = transactions,
                    categoriesById = categoriesById,
                ),
            insightTitle = insight?.title.orEmpty(),
            insightBody = insight?.body.orEmpty(),
            insightCtaLabel = insight?.ctaLabel.orEmpty(),
            showInsightCard = insight != null,
        )
    }

    fun priorFromTrend(
        result: MonthlySummaryResult,
        priorPeriod: String,
    ): CategorySummary? = result.trend.firstOrNull { it.period == priorPeriod }

    fun toSpendSegments(summary: CategorySummary?): List<FamilySpendSegment> {
        if (summary == null || summary.totalExpense <= 0L) return emptyList()
        val total = summary.totalExpense
        return summary.byCategory.values
            .filter { it.totalExpense > 0L }
            .sortedByDescending { it.totalExpense }
            .take(TOP_SPEND_SEGMENTS)
            .map { breakdown ->
                val pct = breakdown.percentOfTotal(total)
                val fraction = (breakdown.totalExpense.toDouble() / total.toDouble()).toFloat()
                FamilySpendSegment(
                    label = breakdown.name,
                    detail =
                        String.format(
                            Locale.forLanguageTag("id-ID"),
                            "%.0f%% • %s",
                            pct,
                            CurrencyFormat.formatIdr(breakdown.totalExpense),
                        ),
                    fraction = fraction,
                )
            }
    }

    fun toBudgetRows(
        budgets: List<Budget>,
        categoriesById: Map<String, Category>,
    ): List<FamilyBudgetRowUi> =
        budgets.map { budget ->
            val title = categoriesById[budget.categoryId]?.name ?: "Budget"
            val progress = budget.progressPercent.coerceIn(0f, 1f)
            val tone = budgetTone(budget)
            FamilyBudgetRowUi(
                title = title,
                spentLabel = CurrencyFormat.formatIdr(budget.spent),
                capLabel = CurrencyFormat.formatIdr(budget.limit),
                progress = progress,
                footnote = budgetFootnote(budget),
                tone = tone,
                muted = tone == FamilyBudgetBarTone.Primary && !budget.isOverBudget,
            )
        }

    fun toHistoryRows(
        transactions: List<Transaction>,
        categoriesById: Map<String, Category>,
    ): List<FamilyHistoryRowUi> =
        transactions.map { tx ->
            val category = categoriesById[tx.categoryId]
            val categoryName = category?.name ?: "Lainnya"
            val title = tx.note?.takeIf { it.isNotBlank() } ?: categoryName
            FamilyHistoryRowUi(
                title = title,
                subtitle = categoryName,
                amountLabel = CurrencyFormat.formatIdr(tx.amount),
                categoryIcon = iconKeyToHistoryIcon(category?.icon),
                addedByLabel = tx.addedByName.ifBlank { DEFAULT_ADDED_BY },
            )
        }

    fun filterSharedBudgets(
        budgets: List<Budget>,
        familyWalletIds: Set<String>,
    ): List<Budget> =
        budgets.filter { budget ->
            !budget.familyId.isNullOrBlank() ||
                (budget.walletId != null && budget.walletId in familyWalletIds)
        }

    fun savingTogetherInsight(
        current: CategorySummary?,
        prior: CategorySummary?,
    ): FamilyInsightCopy? {
        if (current == null || prior == null) return null
        val priorExpense = prior.totalExpense
        if (priorExpense <= 0L) return null
        val delta = current.totalExpense - priorExpense
        val pct = (delta.toDouble() / abs(priorExpense).toDouble()) * 100.0
        val pctRounded = pct.roundToInt()
        val body =
            when {
                pctRounded < 0 ->
                    "Pengeluaran keluarga turun ${abs(pctRounded)}% dibanding bulan lalu. " +
                        "Pertahankan kebiasaan baik ini bersama!"
                pctRounded > 0 ->
                    "Pengeluaran keluarga naik $pctRounded% dibanding bulan lalu. " +
                        "Cek shared budgets untuk menyesuaikan target."
                else ->
                    "Pengeluaran keluarga relatif sama dengan bulan lalu. " +
                        "Tetap pantau shared budgets agar on track."
            }
        return FamilyInsightCopy(
            title = INSIGHT_TITLE,
            body = body,
            ctaLabel = INSIGHT_CTA,
        )
    }

    fun iconKeyToHistoryIcon(iconKey: String?): FamilyHistoryCategoryIcon =
        when (iconKey) {
            "Restaurant" -> FamilyHistoryCategoryIcon.Restaurant
            "DirectionsCar" -> FamilyHistoryCategoryIcon.Transport
            "Receipt", "ReceiptLong", "ElectricBolt" -> FamilyHistoryCategoryIcon.Utilities
            "Payments" -> FamilyHistoryCategoryIcon.Payout
            "School" -> FamilyHistoryCategoryIcon.School
            "Movie" -> FamilyHistoryCategoryIcon.Entertainment
            "LocalHospital" -> FamilyHistoryCategoryIcon.Health
            "ShoppingCart" -> FamilyHistoryCategoryIcon.Shopping
            "TrendingUp" -> FamilyHistoryCategoryIcon.Investment
            else -> FamilyHistoryCategoryIcon.Other
        }

    private fun budgetTone(budget: Budget): FamilyBudgetBarTone =
        when {
            budget.isOverBudget || budget.progressPercent >= BUDGET_WARN_THRESHOLD ->
                FamilyBudgetBarTone.Error
            budget.progressPercent <= 0.6f -> FamilyBudgetBarTone.Success
            else -> FamilyBudgetBarTone.Primary
        }

    private fun budgetFootnote(budget: Budget): String? {
        if (budget.limit <= 0L) return null
        return when {
            budget.isOverBudget -> {
                val over = budget.spent - budget.limit
                "Melebihi limit ${CurrencyFormat.formatIdr(over)}"
            }
            budget.progressPercent >= BUDGET_WARN_THRESHOLD -> {
                val leftPct = ((1f - budget.progressPercent) * 100f).roundToInt().coerceAtLeast(0)
                "Mendekati limit ($leftPct% tersisa)"
            }
            budget.progressPercent <= 0.6f -> {
                val remaining = budget.remaining.coerceAtLeast(0L)
                "On track — sisa ${CurrencyFormat.formatIdr(remaining)}"
            }
            else -> null
        }
    }
}

internal data class FamilyInsightCopy(
    val title: String,
    val body: String,
    val ctaLabel: String,
)
