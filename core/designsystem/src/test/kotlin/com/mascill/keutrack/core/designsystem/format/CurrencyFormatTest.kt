package com.mascill.keutrack.core.designsystem.format

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class CurrencyFormatTest {

    @Test
    fun `formatIdr formats zero correctly`() {
        assertThat(CurrencyFormat.formatIdr(0L)).isEqualTo("Rp 0")
    }

    @Test
    fun `formatIdr formats thousands with dot separator`() {
        assertThat(CurrencyFormat.formatIdr(12_500L)).isEqualTo("Rp 12.500")
    }

    @Test
    fun `formatIdr formats millions correctly`() {
        assertThat(CurrencyFormat.formatIdr(1_250_000L)).isEqualTo("Rp 1.250.000")
    }

    @Test
    fun `formatIdr handles large numbers`() {
        assertThat(CurrencyFormat.formatIdr(99_999_999_999_999L)).isEqualTo("Rp 99.999.999.999.999")
    }

    @Test
    fun `formatIdr clamps negative amounts to zero`() {
        assertThat(CurrencyFormat.formatIdr(-5_000L)).isEqualTo("Rp 0")
    }

    @Test
    fun `formatIdrSigned prefixes expense and income`() {
        assertThat(CurrencyFormat.formatIdrSigned(12_500L, isExpense = true)).isEqualTo("- Rp 12.500")
        assertThat(CurrencyFormat.formatIdrSigned(12_500L, isExpense = false)).isEqualTo("+ Rp 12.500")
    }
}
