package com.mascill.keutrack.core.designsystem.component

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mascill.keutrack.core.designsystem.model.KeuTrackProgressTone
import com.mascill.keutrack.core.designsystem.theme.KeuTrackTheme

@Composable
fun KeuTrackProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    isOverLimit: Boolean = false,
    tone: KeuTrackProgressTone = KeuTrackProgressTone.Primary,
    fillColor: Color? = null,
) {
    val semantic = KeuTrackTheme.semanticColors
    val shapes = KeuTrackTheme.shapeTokens
    val success = KeuTrackTheme.successColors

    val resolvedTone =
        if (isOverLimit) {
            KeuTrackProgressTone.Danger
        } else {
            tone
        }

    val fillBrush =
        when {
            fillColor != null && resolvedTone != KeuTrackProgressTone.Danger ->
                SolidColor(fillColor)
            resolvedTone == KeuTrackProgressTone.Success ->
                Brush.horizontalGradient(
                    colors = listOf(semantic.secondary, success.s700),
                )
            resolvedTone == KeuTrackProgressTone.Danger ->
                Brush.horizontalGradient(
                    colors = listOf(semantic.error, semantic.tertiary),
                )
            else ->
                Brush.horizontalGradient(
                    colors = listOf(semantic.primary, semantic.primaryContainer),
                )
        }

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .height(shapes.progressThickness)
                .clip(RoundedCornerShape(shapes.radiusXl))
                .background(semantic.surfaceContainerHighest),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(progress.coerceIn(0f, 1f))
                    .clip(RoundedCornerShape(shapes.radiusXl))
                    .background(fillBrush),
        )
    }
}

@Preview(name = "Light", showBackground = true)
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
private fun KeuTrackProgressBarPreview() {
    KeuTrackTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            KeuTrackProgressBar(progress = 0.72f, tone = KeuTrackProgressTone.Success)
            KeuTrackProgressBar(progress = 1f, isOverLimit = true)
            KeuTrackProgressBar(progress = 0.35f, tone = KeuTrackProgressTone.Primary)
        }
    }
}
