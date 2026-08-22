package com.mascill.keutrack.feature.transaction.presentation.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
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
