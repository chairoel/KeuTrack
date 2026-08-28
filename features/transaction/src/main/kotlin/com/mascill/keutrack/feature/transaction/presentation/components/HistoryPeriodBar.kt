package com.mascill.keutrack.feature.transaction.presentation.components

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mascill.keutrack.core.designsystem.theme.KeuTrackTheme
import com.mascill.keutrack.feature.transaction.presentation.model.HistoryPeriodPreset

private const val BAR_CHIP_SPACING = 8
private const val BAR_CHIP_PH = 12
private const val BAR_CHIP_PV = 8
private const val BAR_CAPTION_PT = 8
private const val CHIP_ALL = "Semua"
private const val CHIP_LAST_7 = "7 hari"
private const val CHIP_MONTH = "Periode ini"
private const val CHIP_CUSTOM = "Custom"

@Composable
fun HistoryPeriodBar(
    selectedPreset: HistoryPeriodPreset,
    customRangeLabel: String?,
    rangeError: String?,
    onPresetSelected: (HistoryPeriodPreset) -> Unit,
    modifier: Modifier = Modifier,
) {
    val typography = KeuTrackTheme.typography
    val textColors = KeuTrackTheme.textColors
    val semantic = KeuTrackTheme.semanticColors

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(BAR_CHIP_SPACING.dp),
        ) {
            PeriodChip(
                label = CHIP_ALL,
                selected = selectedPreset == HistoryPeriodPreset.All,
                onClick = { onPresetSelected(HistoryPeriodPreset.All) },
            )
            PeriodChip(
                label = CHIP_LAST_7,
                selected = selectedPreset == HistoryPeriodPreset.Last7Days,
                onClick = { onPresetSelected(HistoryPeriodPreset.Last7Days) },
            )
            PeriodChip(
                label = CHIP_MONTH,
                selected = selectedPreset == HistoryPeriodPreset.CurrentMonth,
                onClick = { onPresetSelected(HistoryPeriodPreset.CurrentMonth) },
            )
            PeriodChip(
                label = CHIP_CUSTOM,
                selected = selectedPreset == HistoryPeriodPreset.Custom,
                onClick = { onPresetSelected(HistoryPeriodPreset.Custom) },
            )
        }
        if (selectedPreset == HistoryPeriodPreset.Custom && !customRangeLabel.isNullOrBlank()) {
            Text(
                text = customRangeLabel,
                style = typography.bodyRegular12,
                color = textColors.body,
                modifier = Modifier.padding(top = BAR_CAPTION_PT.dp),
            )
        }
        if (!rangeError.isNullOrBlank()) {
            Text(
                text = rangeError,
                style = typography.bodyRegular12,
                color = semantic.error,
                modifier = Modifier.padding(top = BAR_CAPTION_PT.dp),
            )
        }
    }
}

@Composable
private fun PeriodChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val semantic = KeuTrackTheme.semanticColors
    val effects = KeuTrackTheme.effectTokens
    val typography = KeuTrackTheme.typography
    val shape = RoundedCornerShape(percent = 50)
    val background = if (selected) semantic.primary else semantic.surfaceContainerHigh
    val contentColor = if (selected) Color.White else semantic.onSurface

    Text(
        text = label,
        style = typography.bodyBold12,
        color = contentColor,
        modifier =
            Modifier
                .clip(shape)
                .background(background)
                .then(
                    if (selected) {
                        Modifier
                    } else {
                        Modifier.border(
                            width = effects.ghostBorderWidth,
                            color = effects.ghostBorderColor,
                            shape = shape,
                        )
                    },
                )
                .clickable(onClick = onClick)
                .padding(horizontal = BAR_CHIP_PH.dp, vertical = BAR_CHIP_PV.dp),
    )
}

@Preview(showBackground = true, name = "Period bar — Semua")
@Composable
private fun HistoryPeriodBarPreview() {
    KeuTrackTheme(darkTheme = false) {
        HistoryPeriodBar(
            selectedPreset = HistoryPeriodPreset.All,
            customRangeLabel = null,
            rangeError = null,
            onPresetSelected = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(
    showBackground = true,
    name = "Period bar — Custom dark",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun HistoryPeriodBarCustomDarkPreview() {
    KeuTrackTheme(darkTheme = true) {
        HistoryPeriodBar(
            selectedPreset = HistoryPeriodPreset.Custom,
            customRangeLabel = "12–20 Agu 2026",
            rangeError = null,
            onPresetSelected = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}
