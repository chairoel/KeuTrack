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
    val warning = KeuTrackTheme.warningColors

    val resolvedTone =
        if (isOverLimit) {
            KeuTrackProgressTone.Danger
        } else {
            tone
        }

    // Semantic tones win over fillColor so awareness bars never pick up category hex.
    val fillBrush =
        when (resolvedTone) {
            KeuTrackProgressTone.Success -> SolidColor(success.s500)
            KeuTrackProgressTone.Warning -> SolidColor(warning.w300)
            KeuTrackProgressTone.Caution -> SolidColor(warning.w500)
            KeuTrackProgressTone.Danger -> SolidColor(semantic.error)
            KeuTrackProgressTone.Primary ->
                if (fillColor != null) {
                    SolidColor(fillColor)
                } else {
                    Brush.horizontalGradient(
                        colors = listOf(semantic.primary, semantic.primaryContainer),
                    )
                }
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
            KeuTrackProgressBar(progress = 0.55f, tone = KeuTrackProgressTone.Success)
            KeuTrackProgressBar(progress = 0.70f, tone = KeuTrackProgressTone.Warning)
            KeuTrackProgressBar(progress = 0.85f, tone = KeuTrackProgressTone.Caution)
            KeuTrackProgressBar(progress = 0.95f, tone = KeuTrackProgressTone.Danger)
            KeuTrackProgressBar(progress = 1f, isOverLimit = true)
            KeuTrackProgressBar(progress = 0.35f, tone = KeuTrackProgressTone.Primary)
        }
    }
}
