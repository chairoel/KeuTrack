package com.mascill.keutrack.feature.dashboard.presentation.model

import com.mascill.keutrack.core.common.utils.PeriodBounds
import com.mascill.keutrack.core.designsystem.format.CurrencyFormat
import com.mascill.keutrack.core.domain.model.Category
import com.mascill.keutrack.core.domain.model.CategorySummary
import com.mascill.keutrack.core.domain.model.FamilyGroup
import com.mascill.keutrack.core.domain.model.Transaction
import com.mascill.keutrack.core.domain.model.TransactionType
import com.mascill.keutrack.core.domain.model.User
import com.mascill.keutrack.core.domain.model.WalletType
import com.mascill.keutrack.core.domain.usecase.MonthlySummaryResult
import com.mascill.keutrack.core.domain.usecase.WalletSummary
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

internal object DashboardUiMapper {

    private val timeFormatter =
        DateTimeFormatter.ofPattern("h:mm a", Locale.forLanguageTag("id-ID"))
    private val dayFormatter =
        DateTimeFormatter.ofPattern("d MMM", Locale.forLanguageTag("id-ID"))

    fun greetingFirstName(user: User?, fallback: String = "there"): String {
        val u = user ?: return fallback
        val fromDisplay = u.displayName.trim().split(" ").firstOrNull().orEmpty()
        if (fromDisplay.isNotEmpty()) return fromDisplay
        val fromEmail = u.email.substringBefore('@').trim()
        if (fromEmail.isNotEmpty()) {
            return fromEmail.replaceFirstChar { c -> c.titlecaseChar() }
        }
        return fallback
    }

    fun familySharedSummary(familyWalletCount: Int): String =
        when {
            familyWalletCount <= 0 -> "No shared wallets yet"
            familyWalletCount == 1 -> "Shared with 1 wallet"
            else -> "Shared with $familyWalletCount wallets"
        }

    fun familyMemberInitials(user: User?, family: FamilyGroup?): List<String> {
        if (family == null || family.memberIds.isEmpty()) return emptyList()
        return family.memberIds
            .mapNotNull { memberId ->
                initialFrom(resolveMemberName(user, memberId, family.memberNames[memberId]))
            }
            .take(MAX_FAMILY_AVATARS)
    }

    private fun resolveMemberName(
        user: User?,
        memberId: String,
        storedName: String?,
    ): String? {
        val fromStore = storedName?.trim().orEmpty()
        if (fromStore.isNotEmpty()) return fromStore
        if (memberId.isNotEmpty() && memberId == user?.uid) {
            return greetingFirstName(user, fallback = "")
                .takeIf { it.isNotBlank() }
        }
        return null
    }

    private fun initialFrom(name: String?): String? {
        val first = name?.trim()?.firstOrNull() ?: return null
        return first.uppercaseChar().toString()
    }

    fun monthChangeLabel(
        current: CategorySummary?,
        prior: CategorySummary?,
    ): String? {
        if (current == null || prior == null) return null
        return monthChangeLabel(
            currentNet = current.netBalance,
            priorNet = prior.netBalance,
            cycleStartDay = PeriodBounds.MIN_CYCLE_START_DAY,
        )
    }

    fun monthChangeLabel(
        currentNet: Long,
        priorNet: Long,
        cycleStartDay: Int,
    ): String? {
        if (priorNet == 0L) return null
        val delta = currentNet - priorNet
        val pct = (delta.toDouble() / abs(priorNet).toDouble()) * 100.0
        val sign = if (pct >= 0) "+" else ""
        val suffix =
            if (cycleStartDay == PeriodBounds.MIN_CYCLE_START_DAY) {
                "this month"
            } else {
                "periode ini"
            }
        return String.format(Locale.forLanguageTag("id-ID"), "%s%.1f%% %s", sign, pct, suffix)
    }

    fun priorFromTrend(
        result: MonthlySummaryResult,
        priorPeriod: String,
    ): CategorySummary? = result.trend.firstOrNull { it.period == priorPeriod }

    fun toTransactionRows(
        transactions: List<Transaction>,
        categoriesById: Map<String, Category>,
        walletsById: Map<String, WalletType>,
    ): List<TransactionRowUi> =
        transactions.map { tx ->
            val category = categoriesById[tx.categoryId]
            val categoryName = category?.name ?: "Lainnya"
            val title = tx.note?.takeIf { it.isNotBlank() } ?: categoryName
            val walletType = walletsById[tx.walletId]
            TransactionRowUi(
                title = title,
                categoryLabel = categoryName,
                timeLabel = formatTimeLabel(tx.date),
                amountLabel = CurrencyFormat.formatIdr(tx.amount),
                isExpense = tx.type == TransactionType.EXPENSE,
                walletLabel =
                    when (walletType) {
                        WalletType.FAMILY -> "Family"
                        else -> "Personal"
                    },
                categoryIcon = iconKeyToCategoryIcon(category?.icon),
                syncStatus = tx.syncStatus,
            )
        }

    fun mapWalletTypes(summary: WalletSummary): Map<String, WalletType> {
        val map = mutableMapOf<String, WalletType>()
        summary.personalWallet?.let { map[it.id] = WalletType.PERSONAL }
        summary.familyWallets.forEach { map[it.id] = WalletType.FAMILY }
        return map
    }

    private fun formatTimeLabel(instant: Instant): String {
        val zoned = instant.atZone(ZoneId.systemDefault())
        val today = java.time.LocalDate.now(ZoneId.systemDefault())
        return when (zoned.toLocalDate()) {
            today -> timeFormatter.format(zoned)
            today.minusDays(1) -> "Yesterday"
            else -> dayFormatter.format(zoned)
        }
    }

    fun iconKeyToCategoryIcon(iconKey: String?): TransactionCategoryIcon =
        when (iconKey) {
            "Restaurant" -> TransactionCategoryIcon.Restaurant
            "DirectionsCar" -> TransactionCategoryIcon.Transport
            "Receipt" -> TransactionCategoryIcon.Utilities
            "Payments" -> TransactionCategoryIcon.Payout
            "School" -> TransactionCategoryIcon.School
            "Movie" -> TransactionCategoryIcon.Entertainment
            "LocalHospital" -> TransactionCategoryIcon.Health
            "ShoppingCart" -> TransactionCategoryIcon.Shopping
            "TrendingUp" -> TransactionCategoryIcon.Investment
            else -> TransactionCategoryIcon.Other
        }

    private const val MAX_FAMILY_AVATARS = 4
}
