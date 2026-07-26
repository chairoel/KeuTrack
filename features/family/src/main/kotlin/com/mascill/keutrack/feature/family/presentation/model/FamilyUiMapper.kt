package com.mascill.keutrack.feature.family.presentation.model

import com.mascill.keutrack.core.designsystem.format.CurrencyFormat
import com.mascill.keutrack.core.domain.model.Budget
import com.mascill.keutrack.core.domain.model.Category
import com.mascill.keutrack.core.domain.model.FamilyGroup
import com.mascill.keutrack.core.domain.model.Transaction
import com.mascill.keutrack.core.domain.model.TransactionType
import com.mascill.keutrack.core.domain.model.User
import com.mascill.keutrack.core.domain.model.Wallet
import com.mascill.keutrack.core.domain.usecase.WalletSummary
import java.time.YearMonth
import java.time.ZoneId
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

internal object FamilyUiMapper {

    private const val TOP_SPEND_SEGMENTS = 5
    private const val HISTORY_LIMIT = 5
    private const val BUDGET_WARN_THRESHOLD = 0.85f
    private const val DEFAULT_ADDED_BY = "Anggota"
    private const val INSIGHT_TITLE = "Saving Together"
    private const val INSIGHT_CTA = "Adjust Targets"

    fun toUiState(
        user: User?,
        familyGroup: FamilyGroup?,
        walletSummary: WalletSummary,
        familyTransactions: List<Transaction>,
        budgets: List<Budget>,
        categoriesById: Map<String, Category>,
        currentMonth: YearMonth,
        priorMonth: YearMonth,
    ): FamilyUIState {
        val familyWallet = resolveFamilyWallet(user, walletSummary)
        val familyWalletIds =
            walletSummary.familyWallets
                .filter { wallet ->
                    user?.familyId.isNullOrBlank() || wallet.familyId == user?.familyId
                }
                .map { it.id }
                .toSet()
                .ifEmpty { familyWallet?.id?.let { setOf(it) }.orEmpty() }

        val currentMonthTxs = familyTransactions.filter { it.date.toYearMonth() == currentMonth }
        val priorMonthTxs = familyTransactions.filter { it.date.toYearMonth() == priorMonth }
        val currentExpense = currentMonthTxs.expenseTotal()
        val priorExpense = priorMonthTxs.expenseTotal()
        val insight = savingTogetherInsight(currentExpense, priorExpense)

        return FamilyUIState(
            isLoading = false,
            errorMessage = null,
            showJoinBanner = user?.familyId.isNullOrBlank(),
            hasFamilyWallet = familyWallet != null,
            familyWalletId = familyWallet?.id,
            familyName = familyGroup?.name,
            inviteCode = familyGroup?.inviteCode,
            monthlyTotalExpense = currentExpense,
            spendSegments =
                toSpendSegmentsFromTransactions(
                    transactions = currentMonthTxs,
                    categoriesById = categoriesById,
                ),
            budgetRows =
                toBudgetRows(
                    budgets =
                        filterSharedBudgets(
                            budgets = budgets,
                            familyWalletIds = familyWalletIds,
                            familyId = user?.familyId,
                        ),
                    categoriesById = categoriesById,
                ),
            historyRows =
                toHistoryRows(
                    transactions = familyTransactions.take(HISTORY_LIMIT),
                    categoriesById = categoriesById,
                ),
            insightTitle = insight?.title.orEmpty(),
            insightBody = insight?.body.orEmpty(),
            insightCtaLabel = insight?.ctaLabel.orEmpty(),
            showInsightCard = insight != null,
        )
    }

    fun resolveFamilyWallet(user: User?, walletSummary: WalletSummary): Wallet? {
        val familyId = user?.familyId
        if (!familyId.isNullOrBlank()) {
            walletSummary.familyWallets.firstOrNull { it.familyId == familyId }?.let { return it }
        }
        return walletSummary.familyWallets.firstOrNull()
    }

    fun toSpendSegmentsFromTransactions(
        transactions: List<Transaction>,
        categoriesById: Map<String, Category>,
    ): List<FamilySpendSegment> {
        val expenses = transactions.filter { it.type == TransactionType.EXPENSE }
        val total = expenses.sumOf { it.amount }
        if (total <= 0L) return emptyList()

        return expenses
            .groupBy { it.categoryId }
            .map { (categoryId, txs) ->
                val amount = txs.sumOf { it.amount }
                val name = categoriesById[categoryId]?.name ?: "Lainnya"
                amount to name
            }
            .sortedByDescending { it.first }
            .take(TOP_SPEND_SEGMENTS)
            .map { (amount, name) ->
                val pct = (amount.toFloat() / total.toFloat()) * 100f
                FamilySpendSegment(
                    label = name,
                    detail =
                        String.format(
                            Locale.forLanguageTag("id-ID"),
                            "%.0f%% • %s",
                            pct,
                            CurrencyFormat.formatIdr(amount),
                        ),
                    fraction = amount.toFloat() / total.toFloat(),
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
        familyId: String?,
    ): List<Budget> =
        budgets.filter { budget ->
            (!familyId.isNullOrBlank() && budget.familyId == familyId) ||
                (!budget.familyId.isNullOrBlank()) ||
                (budget.walletId != null && budget.walletId in familyWalletIds)
        }

    fun savingTogetherInsight(
        currentExpense: Long,
        priorExpense: Long,
    ): FamilyInsightCopy? {
        if (priorExpense <= 0L) return null
        val delta = currentExpense - priorExpense
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

    private fun List<Transaction>.expenseTotal(): Long =
        filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }

    private fun java.time.Instant.toYearMonth(): YearMonth =
        YearMonth.from(atZone(ZoneId.systemDefault()).toLocalDate())

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
