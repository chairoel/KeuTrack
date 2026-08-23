package com.mascill.keutrack.feature.transaction.presentation.components

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType

/**
 * Hardware / emulator keyboard mapping for the amount keypad sheet.
 * Only [KeyEventType.KeyDown] produces a digit, backspace, or dismiss.
 * All other keys and KeyUp are [Consume] so letters never enter amount or Note.
 */
internal sealed class AmountHardwareKeyAction {
    data class Digit(val value: Long) : AmountHardwareKeyAction()

    data object Backspace : AmountHardwareKeyAction()

    data object Dismiss : AmountHardwareKeyAction()

    data object Consume : AmountHardwareKeyAction()
}

internal fun amountHardwareKeyAction(
    key: Key,
    type: KeyEventType,
): AmountHardwareKeyAction {
    if (type != KeyEventType.KeyDown) return AmountHardwareKeyAction.Consume
    return when (key) {
        Key.Zero, Key.NumPad0 -> AmountHardwareKeyAction.Digit(0L)
        Key.One, Key.NumPad1 -> AmountHardwareKeyAction.Digit(1L)
        Key.Two, Key.NumPad2 -> AmountHardwareKeyAction.Digit(2L)
        Key.Three, Key.NumPad3 -> AmountHardwareKeyAction.Digit(3L)
        Key.Four, Key.NumPad4 -> AmountHardwareKeyAction.Digit(4L)
        Key.Five, Key.NumPad5 -> AmountHardwareKeyAction.Digit(5L)
        Key.Six, Key.NumPad6 -> AmountHardwareKeyAction.Digit(6L)
        Key.Seven, Key.NumPad7 -> AmountHardwareKeyAction.Digit(7L)
        Key.Eight, Key.NumPad8 -> AmountHardwareKeyAction.Digit(8L)
        Key.Nine, Key.NumPad9 -> AmountHardwareKeyAction.Digit(9L)
        Key.Backspace, Key.Delete -> AmountHardwareKeyAction.Backspace
        Key.Enter, Key.NumPadEnter, Key.Escape -> AmountHardwareKeyAction.Dismiss
        else -> AmountHardwareKeyAction.Consume
    }
}
