package com.mascill.keutrack.core.designsystem.component

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mascill.keutrack.core.designsystem.theme.KeuTrackTheme

private const val CATEGORY_CHIP_BOX = 56
private const val CATEGORY_CHIP_ICON = 26
private const val CATEGORY_CHIP_LABEL_PT = 6
private const val CATEGORY_CHIP_SELECTED_ALPHA = 0.85f

@Composable
fun KeuTrackCategoryChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    containerColor: Color = Color.Unspecified,
) {
    val semantic = KeuTrackTheme.semanticColors
    val textColors = KeuTrackTheme.textColors
    val typography = KeuTrackTheme.typography
    val shapes = KeuTrackTheme.shapeTokens
    val chipShape = RoundedCornerShape(shapes.radiusMd)
    val chipInteractionSource = remember { MutableInteractionSource() }
    val accent =
        if (containerColor != Color.Unspecified) {
            containerColor
        } else {
            semantic.primary
        }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        BoxIcon(
            selected = selected,
            accent = accent,
            surface = semantic.surfaceContainerHigh,
            chipShape = chipShape,
            interactionSource = chipInteractionSource,
            onClick = onClick,
            icon = icon,
            label = label,
        )
        Spacer(modifier = Modifier.height(CATEGORY_CHIP_LABEL_PT.dp))
        Text(
            text = label,
            style = typography.bodyRegular12,
            color = textColors.body,
            maxLines = 1,
        )
    }
}

@Composable
private fun BoxIcon(
    selected: Boolean,
    accent: Color,
    surface: Color,
    chipShape: RoundedCornerShape,
    interactionSource: MutableInteractionSource,
    onClick: () -> Unit,
    icon: ImageVector?,
    label: String,
) {
    Box(
        modifier =
            Modifier
                .size(CATEGORY_CHIP_BOX.dp)
                .clip(chipShape)
                .background(
                    if (selected) {
                        accent.copy(alpha = CATEGORY_CHIP_SELECTED_ALPHA)
                    } else {
                        surface
                    },
                )
                .clickable(
                    interactionSource = interactionSource,
                    indication = ripple(bounded = true),
                    onClick = onClick,
                ),
        contentAlignment = Alignment.Center,
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (selected) Color.White else accent,
                modifier = Modifier.size(CATEGORY_CHIP_ICON.dp),
            )
        }
    }
}

@Preview(name = "Light", showBackground = true)
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
private fun KeuTrackCategoryChipPreview() {
    KeuTrackTheme {
        Row(modifier = Modifier.padding(16.dp)) {
            KeuTrackCategoryChip(
                label = "Food",
                selected = true,
                onClick = {},
                icon = Icons.Filled.Star,
                containerColor = KeuTrackTheme.warningColors.w500,
            )
            Spacer(modifier = Modifier.size(12.dp))
            KeuTrackCategoryChip(
                label = "Food",
                selected = false,
                onClick = {},
                icon = Icons.Filled.Star,
                containerColor = KeuTrackTheme.warningColors.w500,
            )
        }
    }
}
