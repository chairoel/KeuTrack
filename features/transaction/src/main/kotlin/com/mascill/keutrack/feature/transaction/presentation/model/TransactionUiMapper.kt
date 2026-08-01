package com.mascill.keutrack.feature.transaction.presentation.model

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
import com.mascill.keutrack.core.domain.model.CategoryType
import com.mascill.keutrack.core.domain.model.Transaction
import com.mascill.keutrack.core.domain.model.TransactionType
import com.mascill.keutrack.core.domain.model.Wallet
import com.mascill.keutrack.core.domain.model.WalletType
import com.mascill.keutrack.core.domain.usecase.WalletSummary
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import androidx.core.graphics.toColorInt

internal object TransactionUiMapper {

    private val zoneId: ZoneId = ZoneId.systemDefault()
    private val timeFormatter =
        DateTimeFormatter.ofPattern("h:mm a", Locale.forLanguageTag("id-ID"))
    private val dayFormatter =
        DateTimeFormatter.ofPattern("d MMM", Locale.forLanguageTag("id-ID"))
    private val dateChipFormatter =
        DateTimeFormatter.ofPattern("d MMM yyyy", Locale.forLanguageTag("id-ID"))

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
        categories
            .sortedWith(
                compareBy<Category> { it.isOtherCategory() }
                    .thenBy { it.name },
            )
            .map { category ->
                NewEntryCategoryUI(
                    id = category.id,
                    label = category.name,
                    icon = iconKeyToImageVector(category.icon),
                    accent = parseHexColor(category.color),
                    isOther = category.isOtherCategory(),
                )
            }

    /** Identifies the catch-all "Lainnya" category. */
    private fun Category.isOtherCategory(): Boolean =
        id == OTHER_CATEGORY_ID ||
            name.equals(LABEL_OTHER_CATEGORY, ignoreCase = true) ||
            icon == "MoreHoriz"

    fun toWalletOptions(summary: WalletSummary): List<WalletOptionUi> {
        val options = mutableListOf<WalletOptionUi>()
        summary.personalWallet?.let { wallet ->
            options += wallet.toOption(typeLabel = LABEL_PERSONAL)
        }
        summary.familyWallets.forEach { wallet ->
            options += wallet.toOption(typeLabel = LABEL_FAMILY)
        }
        return options
    }

    fun defaultWalletId(summary: WalletSummary): String? =
        summary.personalWallet?.id ?: summary.familyWallets.firstOrNull()?.id

    fun toTransactionRows(
        transactions: List<Transaction>,
        categoriesById: Map<String, Category>,
        walletsById: Map<String, Wallet>,
    ): List<TransactionRowUi> =
        transactions.map { tx ->
            val category = categoriesById[tx.categoryId]
            val categoryName = category?.name ?: LABEL_OTHER_CATEGORY
            val title = tx.note?.takeIf { it.isNotBlank() } ?: categoryName
            val wallet = walletsById[tx.walletId]
            TransactionRowUi(
                id = tx.id,
                title = title,
                categoryLabel = categoryName,
                timeLabel = formatTimeLabel(tx.date),
                amountLabel = CurrencyFormat.formatIdr(tx.amount),
                isExpense = tx.type == TransactionType.EXPENSE,
                walletLabel =
                    when (wallet?.type) {
                        WalletType.FAMILY -> LABEL_FAMILY
                        else -> LABEL_PERSONAL
                    },
                categoryIcon = iconKeyToCategoryIcon(category?.icon),
                syncStatus = tx.syncStatus,
            )
        }

    fun mapWallets(summary: WalletSummary): Map<String, Wallet> {
        val map = mutableMapOf<String, Wallet>()
        summary.personalWallet?.let { map[it.id] = it }
        summary.familyWallets.forEach { map[it.id] = it }
        return map
    }

    /**
     * Converts [LocalDate] → [Instant] at start of day in the system default zone.
     * Start-of-day keeps the calendar day stable when formatting back for display.
     */
    fun localDateToInstant(date: LocalDate): Instant =
        date.atStartOfDay(zoneId).toInstant()

    fun instantToLocalDate(instant: Instant): LocalDate =
        instant.atZone(zoneId).toLocalDate()

    fun formatDateChip(instant: Instant): String {
        val local = instantToLocalDate(instant)
        val today = LocalDate.now(zoneId)
        return when (local) {
            today -> LABEL_TODAY
            today.minusDays(1) -> LABEL_YESTERDAY
            else -> dateChipFormatter.format(local)
        }
    }

    private fun formatTimeLabel(instant: Instant): String {
        val zoned = instant.atZone(zoneId)
        val today = LocalDate.now(zoneId)
        return when (zoned.toLocalDate()) {
            today -> timeFormatter.format(zoned)
            today.minusDays(1) -> LABEL_YESTERDAY
            else -> dayFormatter.format(zoned)
        }
    }

    private fun Wallet.toOption(typeLabel: String): WalletOptionUi =
        WalletOptionUi(
            id = id,
            name = name.ifBlank { typeLabel },
            typeLabel = typeLabel,
            familyId = familyId,
        )

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
            Color(normalized.toColorInt())
        } catch (_: IllegalArgumentException) {
            Color(0xFF78909C)
        }

    private const val LABEL_PERSONAL = "Personal"
    private const val LABEL_FAMILY = "Family"
    private const val LABEL_OTHER_CATEGORY = "Lainnya"
    private const val OTHER_CATEGORY_ID = "cat_lainnya"
    private const val LABEL_TODAY = "Today"
    private const val LABEL_YESTERDAY = "Yesterday"
}
