package com.mascill.keutrack.feature.settings.presentation.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mascill.keutrack.core.designsystem.theme.KeuTrackTheme

private const val SETTINGS_CHIP_PH = 10
private const val SETTINGS_CHIP_PV = 4
private const val SETTINGS_CHIP_CORNER = 50
private const val SETTINGS_CHIP_ALPHA = 0.22f

@Composable
fun SettingsStatusChip(
    label: String,
    modifier: Modifier = Modifier,
) {
    val semantic = KeuTrackTheme.semanticColors
    val typography = KeuTrackTheme.typography

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(percent = SETTINGS_CHIP_CORNER),
        color = semantic.success.copy(alpha = SETTINGS_CHIP_ALPHA),
    ) {
        Text(
            text = label,
            style = typography.bodyBold10,
            color = semantic.success,
            modifier =
                Modifier.padding(
                    horizontal = SETTINGS_CHIP_PH.dp,
                    vertical = SETTINGS_CHIP_PV.dp,
                ),
        )
    }
}

//@Preview
//@Composable
//private fun SettingsStatusChipPreview() {
//    KeuTrackTheme {
//        SettingsStatusChip(
//            label = "Active"
//        )
//    }
//}
