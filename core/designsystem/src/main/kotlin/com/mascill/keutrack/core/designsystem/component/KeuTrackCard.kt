package com.mascill.keutrack.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.mascill.keutrack.core.designsystem.theme.KeuTrackTheme

@Composable
fun KeuTrackCard(
    modifier: Modifier = Modifier,
    highlighted: Boolean = false,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable () -> Unit,
) {
    val semantic = KeuTrackTheme.semanticColors
    val shapes = KeuTrackTheme.shapeTokens

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(shapes.radiusLg))
            .background(
                if (highlighted) semantic.surfaceContainerHigh else semantic.surfaceContainerLowest
            )
            .border(
                width = KeuTrackTheme.effectTokens.ghostBorderWidth,
                color = KeuTrackTheme.effectTokens.ghostBorderColor,
                shape = RoundedCornerShape(shapes.radiusLg)
            )
            .padding(contentPadding)
    ) {
        content()
    }
}
