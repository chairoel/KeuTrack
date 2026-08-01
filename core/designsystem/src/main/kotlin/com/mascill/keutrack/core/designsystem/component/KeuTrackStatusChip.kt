package com.mascill.keutrack.core.designsystem.component

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mascill.keutrack.core.designsystem.model.KeuTrackStatusTone
import com.mascill.keutrack.core.designsystem.theme.KeuTrackTheme

private const val CHIP_PH = 10
private const val CHIP_PV = 4
private const val CHIP_CORNER = 50
private const val CHIP_ALPHA = 0.22f

@Composable
fun KeuTrackStatusChip(
    text: String,
    modifier: Modifier = Modifier,
    tone: KeuTrackStatusTone = KeuTrackStatusTone.Neutral,
) {
    val semantic = KeuTrackTheme.semanticColors
    val typography = KeuTrackTheme.typography
    val warning = KeuTrackTheme.warningColors

    val toneColor =
        when (tone) {
            KeuTrackStatusTone.Success -> semantic.success
            KeuTrackStatusTone.Warning -> warning.w500
            KeuTrackStatusTone.Danger -> semantic.error
            KeuTrackStatusTone.Neutral -> semantic.onSurfaceVariant
        }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(percent = CHIP_CORNER),
        color = toneColor.copy(alpha = CHIP_ALPHA),
    ) {
        Text(
            text = text,
            style = typography.bodyBold10,
            color = toneColor,
            modifier =
                Modifier.padding(
                    horizontal = CHIP_PH.dp,
                    vertical = CHIP_PV.dp,
                ),
        )
    }
}

@Preview(name = "Light", showBackground = true)
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
private fun KeuTrackStatusChipPreview() {
    KeuTrackTheme {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            KeuTrackStatusChip(text = "ACTIVE", tone = KeuTrackStatusTone.Success)
            KeuTrackStatusChip(text = "PENDING", tone = KeuTrackStatusTone.Warning)
            KeuTrackStatusChip(text = "FAILED", tone = KeuTrackStatusTone.Danger)
            KeuTrackStatusChip(text = "IDLE", tone = KeuTrackStatusTone.Neutral)
        }
    }
}
