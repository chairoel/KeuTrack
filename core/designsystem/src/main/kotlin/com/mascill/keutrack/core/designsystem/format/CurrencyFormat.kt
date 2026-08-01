package com.mascill.keutrack.core.designsystem.format

import java.text.NumberFormat
import java.util.Locale
import kotlin.math.min

/** Max whole rupiah digits the keypad can represent (≈100 trillion). */
const val MAX_AMOUNT_RUPIAH = 99_999_999_999_999L

private const val IDR_PREFIX = "Rp "
private const val SIGNED_EXPENSE_PREFIX = "- "
private const val SIGNED_INCOME_PREFIX = "+ "

private val idNumberFormat: NumberFormat =
    NumberFormat.getNumberInstance(Locale.forLanguageTag("id-ID")).apply {
        maximumFractionDigits = 0
        minimumFractionDigits = 0
        isGroupingUsed = true
    }

object CurrencyFormat {
    /** Format Long rupiah → "Rp 12.500" (locale id-ID, without decimals). */
    fun formatIdr(amount: Long): String {
        val safe = min(amount.coerceAtLeast(0L), MAX_AMOUNT_RUPIAH)
        return IDR_PREFIX + idNumberFormat.format(safe)
    }

    /** Signed display for expense/income rows, e.g. "- Rp 12.500" / "+ Rp 12.500". */
    fun formatIdrSigned(amount: Long, isExpense: Boolean): String {
        val prefix = if (isExpense) SIGNED_EXPENSE_PREFIX else SIGNED_INCOME_PREFIX
        return prefix + formatIdr(amount)
    }
}
