package com.mascill.keutrack.feature.dashboard.presentation.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.LocalHospital
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.mascill.keutrack.core.designsystem.format.CurrencyFormat
import com.mascill.keutrack.core.domain.model.Category
import com.mascill.keutrack.core.domain.model.CategorySummary
import com.mascill.keutrack.core.domain.model.CategoryType
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

    fun monthChangeLabel(
        current: CategorySummary?,
        prior: CategorySummary?,
    ): String? {
        if (current == null || prior == null) return null
        val priorNet = prior.netBalance
        if (priorNet == 0L) return null
        val delta = current.netBalance - priorNet
        val pct = (delta.toDouble() / abs(priorNet).toDouble()) * 100.0
        val sign = if (pct >= 0) "+" else ""
        return String.format(Locale.forLanguageTag("id-ID"), "%s%.1f%% this month", sign, pct)
    }

    fun priorFromTrend(
        result: MonthlySummaryResult,
        priorPeriod: String,
    ): CategorySummary? = result.trend.firstOrNull { it.period == priorPeriod }

    fun filterCategoriesForKind(
        categories: List<Category>,
        kind: EntryTransactionKind,
    ): List<Category> {
        val target =
            when (kind) {
                EntryTransactionKind.Expense -> CategoryType.EXPENSE
                EntryTransactionKind.Income -> CategoryType.INCOME
            }
        return categories.filter { it.type == target || it.type == CategoryType.BOTH }
    }

    fun toNewEntryCategories(categories: List<Category>): List<NewEntryCategoryUI> =
        categories.map { category ->
            NewEntryCategoryUI(
                id = category.id,
                label = category.name,
                icon = iconKeyToImageVector(category.icon),
                accent = parseHexColor(category.color),
            )
        }

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

    fun iconKeyToImageVector(iconKey: String): ImageVector =
        when (iconKey) {
            "Restaurant" -> Icons.Outlined.Restaurant
            "DirectionsCar" -> Icons.Outlined.DirectionsCar
            "Receipt" -> Icons.Outlined.Receipt
            "Payments" -> Icons.Outlined.Payments
            "School" -> Icons.Outlined.School
            "Movie" -> Icons.Outlined.Movie
            "LocalHospital" -> Icons.Outlined.LocalHospital
            "ShoppingCart" -> Icons.Outlined.ShoppingCart
            "TrendingUp" -> Icons.AutoMirrored.Filled.TrendingUp
            else -> Icons.Outlined.MoreHoriz
        }

    fun parseHexColor(hex: String): Color =
        try {
            val normalized = if (hex.startsWith("#")) hex else "#$hex"
            Color(android.graphics.Color.parseColor(normalized))
        } catch (_: IllegalArgumentException) {
            Color(0xFF78909C)
        }
}
