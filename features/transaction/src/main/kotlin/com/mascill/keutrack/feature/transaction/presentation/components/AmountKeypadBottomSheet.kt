package com.mascill.keutrack.feature.transaction.presentation.components

import android.content.res.Configuration
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mascill.keutrack.core.designsystem.component.KeuTrackAmountKeypad
import com.mascill.keutrack.core.designsystem.component.KeuTrackButton
import com.mascill.keutrack.core.designsystem.component.KeuTrackModalBottomSheet
import com.mascill.keutrack.core.designsystem.theme.KeuTrackTheme

private const val KEYPAD_SHEET_DONE = "Done"
private const val KEYPAD_SHEET_PH = 20
private const val KEYPAD_SHEET_PB = 16
private const val KEYPAD_SHEET_AFTER_KEYPAD = 16

@Composable
fun AmountKeypadBottomSheet(
    onDigit: (Long) -> Unit,
    onTripleZero: () -> Unit,
    onBackspace: () -> Unit,
    onDismiss: () -> Unit,
) {
    KeuTrackModalBottomSheet(
        onDismissRequest = onDismiss,
        scrimColor = Color.Transparent,
    ) {
        AmountKeypadSheetContent(
            onDigit = onDigit,
            onTripleZero = onTripleZero,
            onBackspace = onBackspace,
            onDismiss = onDismiss,
        )
    }
}

@Composable
internal fun AmountKeypadSheetContent(
    onDigit: (Long) -> Unit,
    onTripleZero: () -> Unit,
    onBackspace: () -> Unit,
    onDismiss: () -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        withFrameNanos { }
        focusRequester.requestFocus()
    }

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester)
                .focusable()
                .onPreviewKeyEvent { event ->
                    when (val action = amountHardwareKeyAction(event.key, event.type)) {
                        is AmountHardwareKeyAction.Digit -> {
                            onDigit(action.value)
                            true
                        }
                        AmountHardwareKeyAction.Backspace -> {
                            onBackspace()
                            true
                        }
                        AmountHardwareKeyAction.Dismiss -> {
                            onDismiss()
                            true
                        }
                        AmountHardwareKeyAction.Consume -> true
                    }
                }
                .padding(horizontal = KEYPAD_SHEET_PH.dp)
                .padding(bottom = KEYPAD_SHEET_PB.dp)
                .navigationBarsPadding(),
    ) {
        KeuTrackAmountKeypad(
            onDigit = onDigit,
            onTripleZero = onTripleZero,
            onBackspace = onBackspace,
        )
        Spacer(modifier = Modifier.height(KEYPAD_SHEET_AFTER_KEYPAD.dp))
        KeuTrackButton(
            text = KEYPAD_SHEET_DONE,
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Preview(name = "Light", showBackground = true)
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
private fun AmountKeypadSheetContentPreview() {
    KeuTrackTheme {
        AmountKeypadSheetContent(
            onDigit = {},
            onTripleZero = {},
            onBackspace = {},
            onDismiss = {},
        )
    }
}
