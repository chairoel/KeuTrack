package com.mascill.keutrack.core.designsystem.component

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mascill.keutrack.core.designsystem.theme.KeuTrackTheme

private const val KEYPAD_ROW_SPACING = 8
private const val KEYPAD_CELL_HEIGHT = 52
private const val KEYPAD_TRIPLE_ZERO = "000"
private const val KEYPAD_BACKSPACE = "⌫"

private val KEYPAD_ROWS =
    listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf(KEYPAD_TRIPLE_ZERO, "0", KEYPAD_BACKSPACE),
    )

/**
 * Stateless amount keypad. Parent owns amount state and applies digit / backspace / 000 rules.
 */
@Composable
fun KeuTrackAmountKeypad(
    onDigit: (Long) -> Unit,
    onBackspace: () -> Unit,
    onTripleZero: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(KEYPAD_ROW_SPACING.dp),
    ) {
        KEYPAD_ROWS.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(KEYPAD_ROW_SPACING.dp),
            ) {
                row.forEach { key ->
                    KeuTrackKeypadCell(
                        label = key,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            when (key) {
                                KEYPAD_BACKSPACE -> onBackspace()
                                KEYPAD_TRIPLE_ZERO -> onTripleZero()
                                else -> onDigit(key.first().digitToInt().toLong())
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun KeuTrackKeypadCell(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val semantic = KeuTrackTheme.semanticColors
    val textColors = KeuTrackTheme.textColors
    val typography = KeuTrackTheme.typography
    val shapes = KeuTrackTheme.shapeTokens

    Box(
        modifier =
            modifier
                .height(KEYPAD_CELL_HEIGHT.dp)
                .clip(RoundedCornerShape(shapes.radiusMd))
                .background(semantic.surfaceContainerHigh)
                .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = typography.headingBold20,
            color = textColors.title,
        )
    }
}

@Preview(name = "Light", showBackground = true)
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
private fun KeuTrackAmountKeypadPreview() {
    KeuTrackTheme {
        KeuTrackAmountKeypad(
            onDigit = {},
            onBackspace = {},
            onTripleZero = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}
