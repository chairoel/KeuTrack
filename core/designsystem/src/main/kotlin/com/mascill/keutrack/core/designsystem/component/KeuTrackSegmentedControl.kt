package com.mascill.keutrack.core.designsystem.component

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mascill.keutrack.core.designsystem.theme.KeuTrackShapeTokens
import com.mascill.keutrack.core.designsystem.theme.KeuTrackTheme

private const val TOGGLE_PADDING = 4
private const val TOGGLE_INNER_SPACING = 4
private const val TOGGLE_SEGMENT_PV = 12
private const val TOGGLE_LEFT_PRIMARY_CONTAINER_ALPHA = 0.85f
private const val TOGGLE_RIGHT_PRIMARY_LIGHT_ALPHA = 0.1f
private const val TOGGLE_RIGHT_PRIMARY_DARK_ALPHA = 0.35f

/**
 * Two-tab segmented control (expense/income style).
 * Left selected uses tertiary/expense treatment; right uses primary/income treatment.
 */
@Composable
fun KeuTrackSegmentedControl(
    leftLabel: String,
    rightLabel: String,
    leftSelected: Boolean,
    onLeftClick: () -> Unit,
    onRightClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isDark = isSystemInDarkTheme()
    val semantic = KeuTrackTheme.semanticColors
    val shapes = KeuTrackTheme.shapeTokens
    val typography = KeuTrackTheme.typography

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(shapes.radiusLg))
                .background(semantic.surfaceContainerHigh)
                .padding(TOGGLE_PADDING.dp),
        horizontalArrangement = Arrangement.spacedBy(TOGGLE_INNER_SPACING.dp),
    ) {
        Segment(
            label = leftLabel,
            background =
                when {
                    !leftSelected -> Color.Transparent
                    !isDark -> semantic.surfaceContainerLowest
                    else -> semantic.primaryContainer.copy(alpha = TOGGLE_LEFT_PRIMARY_CONTAINER_ALPHA)
                },
            labelColor =
                when {
                    !leftSelected -> semantic.onSurfaceVariant
                    !isDark -> semantic.tertiary
                    else -> Color.White
                },
            textStyle = typography.bodyBold14,
            shapes = shapes,
            onClick = onLeftClick,
            modifier = Modifier.weight(1f),
        )
        Segment(
            label = rightLabel,
            background =
                when {
                    leftSelected -> Color.Transparent
                    !isDark -> semantic.primary.copy(alpha = TOGGLE_RIGHT_PRIMARY_LIGHT_ALPHA)
                    else -> semantic.primary.copy(alpha = TOGGLE_RIGHT_PRIMARY_DARK_ALPHA)
                },
            labelColor =
                when {
                    leftSelected -> semantic.onSurfaceVariant
                    !isDark -> semantic.primary
                    else -> Color.White
                },
            textStyle = typography.bodyBold14,
            shapes = shapes,
            onClick = onRightClick,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun Segment(
    label: String,
    background: Color,
    labelColor: Color,
    textStyle: TextStyle,
    shapes: KeuTrackShapeTokens,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .clip(RoundedCornerShape(shapes.radiusMd))
                .background(background)
                .clickable(onClick = onClick)
                .padding(vertical = TOGGLE_SEGMENT_PV.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = textStyle,
            color = labelColor,
        )
    }
}

@Preview(name = "Light", showBackground = true)
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
private fun KeuTrackSegmentedControlPreview() {
    KeuTrackTheme {
        KeuTrackSegmentedControl(
            leftLabel = "Expense",
            rightLabel = "Income",
            leftSelected = true,
            onLeftClick = {},
            onRightClick = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}
