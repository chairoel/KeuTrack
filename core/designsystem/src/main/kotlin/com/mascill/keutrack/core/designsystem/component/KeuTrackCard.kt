package com.mascill.keutrack.core.designsystem.component

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.material.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mascill.keutrack.core.designsystem.theme.KeuTrackTheme

private const val CARD_FOCUSED_BORDER_WIDTH = 2

@Composable
fun KeuTrackCard(
    modifier: Modifier = Modifier,
    highlighted: Boolean = false,
    focused: Boolean = false,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val semantic = KeuTrackTheme.semanticColors
    val shapes = KeuTrackTheme.shapeTokens
    val effects = KeuTrackTheme.effectTokens
    val cardShape = RoundedCornerShape(shapes.radiusLg)

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(cardShape)
                .background(
                    if (highlighted) {
                        semantic.surfaceContainerHigh
                    } else {
                        semantic.surfaceContainerLowest
                    },
                )
                .border(
                    width = if (focused) CARD_FOCUSED_BORDER_WIDTH.dp else effects.ghostBorderWidth,
                    color = if (focused) semantic.primary else effects.ghostBorderColor,
                    shape = cardShape,
                )
                .then(
                    if (onClick != null) {
                        Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(bounded = true),
                            onClick = onClick,
                        )
                    } else {
                        Modifier
                    },
                )
                .padding(contentPadding),
    ) {
        content()
    }
}

@Preview(name = "Light", showBackground = true)
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
private fun KeuTrackCardPreview() {
    KeuTrackTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            KeuTrackCard {
                Text(
                    text = "Default card",
                    style = KeuTrackTheme.typography.bodyBold16,
                    color = KeuTrackTheme.textColors.title,
                )
            }
        }
    }
}

@Preview(name = "Focused — Light", showBackground = true)
@Preview(name = "Focused — Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
private fun KeuTrackCardFocusedPreview() {
    KeuTrackTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            KeuTrackCard(highlighted = true, focused = true) {
                Text(
                    text = "Focused card",
                    style = KeuTrackTheme.typography.bodyBold16,
                    color = KeuTrackTheme.semanticColors.primary,
                )
            }
        }
    }
}
