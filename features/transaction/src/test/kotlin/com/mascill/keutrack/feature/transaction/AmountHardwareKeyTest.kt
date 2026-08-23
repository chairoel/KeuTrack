package com.mascill.keutrack.feature.transaction

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import com.google.common.truth.Truth.assertThat
import com.mascill.keutrack.feature.transaction.presentation.components.AmountHardwareKeyAction
import com.mascill.keutrack.feature.transaction.presentation.components.amountHardwareKeyAction
import org.junit.Test

class AmountHardwareKeyTest {

    @Test
    fun mapsRowDigitsAndNumPadToTheSameAction() {
        assertThat(amountHardwareKeyAction(Key.Three, KeyEventType.KeyDown))
            .isEqualTo(AmountHardwareKeyAction.Digit(3L))
        assertThat(amountHardwareKeyAction(Key.NumPad3, KeyEventType.KeyDown))
            .isEqualTo(AmountHardwareKeyAction.Digit(3L))
        assertThat(amountHardwareKeyAction(Key.Zero, KeyEventType.KeyDown))
            .isEqualTo(AmountHardwareKeyAction.Digit(0L))
        assertThat(amountHardwareKeyAction(Key.NumPad0, KeyEventType.KeyDown))
            .isEqualTo(AmountHardwareKeyAction.Digit(0L))
    }

    @Test
    fun mapsBackspaceAndDelete() {
        assertThat(amountHardwareKeyAction(Key.Backspace, KeyEventType.KeyDown))
            .isEqualTo(AmountHardwareKeyAction.Backspace)
        assertThat(amountHardwareKeyAction(Key.Delete, KeyEventType.KeyDown))
            .isEqualTo(AmountHardwareKeyAction.Backspace)
    }

    @Test
    fun mapsEnterAndEscapeToDismiss() {
        assertThat(amountHardwareKeyAction(Key.Enter, KeyEventType.KeyDown))
            .isEqualTo(AmountHardwareKeyAction.Dismiss)
        assertThat(amountHardwareKeyAction(Key.Escape, KeyEventType.KeyDown))
            .isEqualTo(AmountHardwareKeyAction.Dismiss)
    }

    @Test
    fun consumesLettersAndSymbolsWithoutADigit() {
        assertThat(amountHardwareKeyAction(Key.A, KeyEventType.KeyDown))
            .isEqualTo(AmountHardwareKeyAction.Consume)
        assertThat(amountHardwareKeyAction(Key.Comma, KeyEventType.KeyDown))
            .isEqualTo(AmountHardwareKeyAction.Consume)
        assertThat(amountHardwareKeyAction(Key.Period, KeyEventType.KeyDown))
            .isEqualTo(AmountHardwareKeyAction.Consume)
        assertThat(amountHardwareKeyAction(Key.Minus, KeyEventType.KeyDown))
            .isEqualTo(AmountHardwareKeyAction.Consume)
        assertThat(amountHardwareKeyAction(Key.Spacebar, KeyEventType.KeyDown))
            .isEqualTo(AmountHardwareKeyAction.Consume)
    }

    @Test
    fun keyUpDoesNotAppendADigit() {
        assertThat(amountHardwareKeyAction(Key.Seven, KeyEventType.KeyUp))
            .isEqualTo(AmountHardwareKeyAction.Consume)
    }
}
