package com.mascill.keutrack.feature.settings.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.mascill.keutrack.core.designsystem.theme.KeuTrackTheme

@Composable
fun SettingsSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
) {
    val typography = KeuTrackTheme.typography
    val textColors = KeuTrackTheme.textColors

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = title,
            style = typography.bodyBold16,
            color = textColors.title,
        )
        trailing?.invoke()
    }
}

//@Preview(showBackground = true)
//@Composable
//private fun SettingsSectionHeaderPreview() {
//    KeuTrackTheme {
//        SettingsSectionHeader(
//            title = "Connected Wallets"
//        )
//    }
//}
