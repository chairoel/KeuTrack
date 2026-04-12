package com.mascill.keutrack.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.mascill.keutrack.core.designsystem.theme.KeuTrackTheme

@Composable
fun KeuTrackCard(
    modifier: Modifier = Modifier,
    highlighted: Boolean = false,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val semantic = KeuTrackTheme.semanticColors
    val shapes = KeuTrackTheme.shapeTokens
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
                    width = KeuTrackTheme.effectTokens.ghostBorderWidth,
                    color = KeuTrackTheme.effectTokens.ghostBorderColor,
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
