package com.mascill.keutrack.feature.settings.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.mascill.keutrack.core.designsystem.component.KeuTrackStatusChip
import com.mascill.keutrack.core.designsystem.model.KeuTrackStatusTone

@Composable
fun SettingsStatusChip(
    label: String,
    modifier: Modifier = Modifier,
) {
    KeuTrackStatusChip(
        text = label,
        modifier = modifier,
        tone = KeuTrackStatusTone.Success,
    )
}
