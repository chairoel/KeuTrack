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
                    memberNamesByUserId = familyGroup?.memberNames.orEmpty(),
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
            // Prefer oldest match (canonical owner wallet) if orphans remain locally.
            walletSummary.familyWallets
                .filter { it.familyId == familyId }
                .minByOrNull { it.createdAt }
                ?.let { return it }
        }
        return walletSummary.familyWallets.minByOrNull { it.createdAt }
    }

    /** Donut legend: monthly family expense grouped by member, not category. */
    fun toSpendSegmentsFromTransactions(
        transactions: List<Transaction>,
        memberNamesByUserId: Map<String, String> = emptyMap(),
    ): List<FamilySpendSegment> {
        val expenses = transactions.filter { it.type == TransactionType.EXPENSE }
        val total = expenses.sumOf { it.amount }
        if (total <= 0L) return emptyList()

        return expenses
            .groupBy { memberKey(it) }
            .map { (key, txs) ->
                val amount = txs.sumOf { it.amount }
                amount to resolveMemberLabel(key, txs, memberNamesByUserId)
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

    /** Shared Budgets: one row per category (merged if several member budgets share it). */
    fun toBudgetRows(
        budgets: List<Budget>,
        categoriesById: Map<String, Category>,
    ): List<FamilyBudgetRowUi> =
        budgets
            .groupBy { it.categoryId }
            .map { (categoryId, categoryBudgets) ->
                val spent = categoryBudgets.sumOf { it.spent }
                val limit = categoryBudgets.sumOf { it.limit }
                val progress = if (limit > 0L) (spent.toFloat() / limit.toFloat()) else 0f
                val isOverBudget = spent > limit
                val tone = budgetTone(progress = progress, isOverBudget = isOverBudget)
                FamilyBudgetRowUi(
                    title = categoriesById[categoryId]?.name ?: "Budget",
                    spentLabel = CurrencyFormat.formatIdr(spent),
                    capLabel = CurrencyFormat.formatIdr(limit),
                    progress = progress.coerceIn(0f, 1f),
                    footnote = budgetFootnote(spent = spent, limit = limit, progress = progress),
                    tone = tone,
                    muted = tone == FamilyBudgetBarTone.Primary && !isOverBudget,
                )
            }
            .sortedByDescending { it.progress }

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

    private fun memberKey(tx: Transaction): String =
        tx.userId.ifBlank { tx.addedByName.ifBlank { DEFAULT_ADDED_BY } }

    private fun resolveMemberLabel(
        key: String,
        txs: List<Transaction>,
        memberNamesByUserId: Map<String, String>,
    ): String {
        val fromTx = txs.maxByOrNull { it.date }?.addedByName?.takeIf { it.isNotBlank() }
        if (!fromTx.isNullOrBlank()) return fromTx
        return memberNamesByUserId[key]?.takeIf { it.isNotBlank() } ?: DEFAULT_ADDED_BY
    }

    private fun budgetTone(progress: Float, isOverBudget: Boolean): FamilyBudgetBarTone =
        when {
            isOverBudget || progress >= BUDGET_WARN_THRESHOLD -> FamilyBudgetBarTone.Error
            progress <= 0.6f -> FamilyBudgetBarTone.Success
            else -> FamilyBudgetBarTone.Primary
        }

    private fun budgetFootnote(spent: Long, limit: Long, progress: Float): String? {
        if (limit <= 0L) return null
        return when {
            spent > limit -> {
                val over = spent - limit
                "Melebihi limit ${CurrencyFormat.formatIdr(over)}"
            }
            progress >= BUDGET_WARN_THRESHOLD -> {
                val leftPct = ((1f - progress) * 100f).roundToInt().coerceAtLeast(0)
                "Mendekati limit ($leftPct% tersisa)"
            }
            progress <= 0.6f -> {
                val remaining = (limit - spent).coerceAtLeast(0L)
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
