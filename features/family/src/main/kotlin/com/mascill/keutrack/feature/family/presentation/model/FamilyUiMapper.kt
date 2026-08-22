package com.mascill.keutrack.feature.family.presentation.model

import com.mascill.keutrack.core.designsystem.format.CurrencyFormat
import com.mascill.keutrack.core.domain.model.Budget
import com.mascill.keutrack.core.domain.model.Category
import com.mascill.keutrack.core.domain.model.CategoryType
import com.mascill.keutrack.core.domain.model.FamilyGroup
import com.mascill.keutrack.core.domain.model.FamilyRole
import com.mascill.keutrack.core.domain.model.Transaction
import com.mascill.keutrack.core.domain.model.TransactionType
import com.mascill.keutrack.core.domain.model.User
import com.mascill.keutrack.core.domain.model.Wallet
import com.mascill.keutrack.core.domain.usecase.WalletSummary
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

internal object FamilyUiMapper {

    private const val TOP_SPEND_SEGMENTS = 5
    private const val HISTORY_LIMIT = 5
    private const val BUDGET_SUCCESS_MAX = 0.60f
    private const val BUDGET_WATCH_MAX = 0.75f
    private const val BUDGET_RED_AFTER = 0.90f
    private const val DEFAULT_ADDED_BY = "Anggota"
    private const val INSIGHT_TITLE = "Saving Together"
    private const val INSIGHT_CTA = "Atur Target"
    private val BUDGET_MONTH_FORMATTER =
        DateTimeFormatter.ofPattern("MMMM yyyy", Locale.forLanguageTag("id-ID"))

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
                            familyId = user?.familyId,
                        ),
                    transactions = currentMonthTxs,
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
            canEditBudgets =
                canEditBudgets(
                    user = user,
                    hasFamilyWallet = familyWallet != null,
                ),
            budgetMonthLabel = formatBudgetMonth(currentMonth),
            expenseCategories = toExpenseCategoryOptions(categoriesById),
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

    /**
     * Shared Budgets: one row per category from this month's family expenses.
     * Budget limits overlay progress when a shared budget exists for that category.
     */
    fun toBudgetRows(
        budgets: List<Budget>,
        categoriesById: Map<String, Category>,
        transactions: List<Transaction> = emptyList(),
    ): List<FamilyBudgetRowUi> {
        val spentByCategory =
            transactions
                .filter { it.type == TransactionType.EXPENSE }
                .groupBy { it.categoryId }
                .mapValues { (_, txs) -> txs.sumOf { it.amount } }
        val limitByCategory =
            budgets
                .groupBy { it.categoryId }
                .mapValues { (_, categoryBudgets) -> categoryBudgets.sumOf { it.limit } }
        val spentFromBudget =
            budgets
                .groupBy { it.categoryId }
                .mapValues { (_, categoryBudgets) -> categoryBudgets.sumOf { it.spent } }
        val monthlyTotal = spentByCategory.values.sum()
        val categoryIds = spentByCategory.keys + limitByCategory.keys
        if (categoryIds.isEmpty()) return emptyList()

        return categoryIds
            .map { categoryId ->
                val spent = spentByCategory[categoryId] ?: spentFromBudget[categoryId] ?: 0L
                val limit = limitByCategory[categoryId] ?: 0L
                toCategoryBudgetRow(
                    categoryId = categoryId,
                    title = categoriesById[categoryId]?.name ?: "Lainnya",
                    spent = spent,
                    limit = limit,
                    monthlyTotal = monthlyTotal,
                    barColorHex = categoriesById[categoryId]?.color ?: DEFAULT_BUDGET_BAR_COLOR,
                )
            }
            .sortedByDescending { it.progress }
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

    fun canEditBudgets(
        user: User?,
        hasFamilyWallet: Boolean,
    ): Boolean {
        val familyId = user?.familyId
        return !familyId.isNullOrBlank() &&
            FamilyRole.fromValue(user?.familyRole.orEmpty()) == FamilyRole.OWNER &&
            hasFamilyWallet
    }

    fun formatBudgetMonth(month: YearMonth): String =
        month.format(BUDGET_MONTH_FORMATTER)

    private fun toExpenseCategoryOptions(
        categoriesById: Map<String, Category>,
    ): List<FamilyBudgetCategoryOption> =
        categoriesById.values
            .filter { it.type == CategoryType.EXPENSE }
            .sortedBy { it.name.lowercase(Locale.forLanguageTag("id-ID")) }
            .map { FamilyBudgetCategoryOption(id = it.id, name = it.name) }

    fun filterSharedBudgets(
        budgets: List<Budget>,
        familyId: String?,
    ): List<Budget> =
        budgets.filter { budget ->
            !familyId.isNullOrBlank() && budget.familyId == familyId
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

    private fun toCategoryBudgetRow(
        categoryId: String,
        title: String,
        spent: Long,
        limit: Long,
        monthlyTotal: Long,
        barColorHex: String,
    ): FamilyBudgetRowUi {
        val hasLimit = limit > 0L
        val rawProgress =
            when {
                hasLimit -> spent.toFloat() / limit.toFloat()
                monthlyTotal > 0L -> spent.toFloat() / monthlyTotal.toFloat()
                else -> 0f
            }
        val isOverBudget = hasLimit && spent > limit
        val tone =
            if (hasLimit) {
                budgetTone(
                    progress = rawProgress,
                    isOverBudget = isOverBudget || spent * 10L > limit * 9L,
                )
            } else {
                FamilyBudgetBarTone.Neutral
            }
        return FamilyBudgetRowUi(
            categoryId = categoryId,
            title = title,
            spentLabel = CurrencyFormat.formatIdr(spent),
            capLabel = CurrencyFormat.formatIdr(if (hasLimit) limit else monthlyTotal),
            progress = rawProgress.coerceIn(0f, 1f),
            footnote = budgetFootnote(spent = spent, limit = limit, monthlyTotal = monthlyTotal),
            tone = tone,
            muted = !hasLimit,
            hasLimit = hasLimit,
            barColorHex = barColorHex,
        )
    }

    internal fun budgetTone(progress: Float, isOverBudget: Boolean): FamilyBudgetBarTone =
        when {
            isOverBudget || progress > BUDGET_RED_AFTER -> FamilyBudgetBarTone.Error
            progress <= BUDGET_SUCCESS_MAX -> FamilyBudgetBarTone.Success
            progress <= BUDGET_WATCH_MAX -> FamilyBudgetBarTone.Watch
            else -> FamilyBudgetBarTone.Critical
        }

    private fun budgetFootnote(spent: Long, limit: Long, monthlyTotal: Long): String? {
        if (limit > 0L) {
            val progress = spent.toFloat() / limit.toFloat()
            val remaining = (limit - spent).coerceAtLeast(0L)
            val leftPct = ((1f - progress.coerceAtMost(1f)) * 100f).roundToInt().coerceAtLeast(0)
            return when {
                spent > limit -> "Melebihi limit ${CurrencyFormat.formatIdr(spent - limit)}"
                progress > BUDGET_RED_AFTER || spent * 10L > limit * 9L ->
                    "Limit hampir habis ($leftPct% tersisa)"
                progress <= BUDGET_SUCCESS_MAX ->
                    "On track — sisa ${CurrencyFormat.formatIdr(remaining)}"
                progress <= BUDGET_WATCH_MAX ->
                    "Perhatian — sisa ${CurrencyFormat.formatIdr(remaining)}"
                else -> "Mendekati limit ($leftPct% tersisa)"
            }
        }
        if (monthlyTotal <= 0L) return null
        val pct = ((spent.toFloat() / monthlyTotal.toFloat()) * 100f).roundToInt()
        return "$pct% dari pengeluaran keluarga"
    }
}

internal data class FamilyInsightCopy(
    val title: String,
    val body: String,
    val ctaLabel: String,
)
