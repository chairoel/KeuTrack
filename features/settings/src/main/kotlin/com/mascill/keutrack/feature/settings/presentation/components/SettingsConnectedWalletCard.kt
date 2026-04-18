package com.mascill.keutrack.feature.settings.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mascill.keutrack.core.designsystem.component.KeuTrackCard
import com.mascill.keutrack.core.designsystem.theme.KeuTrackTheme
import com.mascill.keutrack.feature.settings.presentation.model.ConnectedWalletUi
import com.mascill.keutrack.feature.settings.presentation.model.DefaultSettingsMockContent

private const val SETTINGS_WALLET_ICON = 28
private const val SETTINGS_WALLET_CHIP_PT = 6
private const val SETTINGS_WALLET_LEADING_ACCENT = 4
private const val SETTINGS_WALLET_CONTENT_PADDING = 16
private const val SETTINGS_WALLET_TITLE_PS = 16
private const val SETTINGS_WALLET_TITLE_WEIGHT = 1f
private const val SETTINGS_WALLET_MAIN_WEIGHT = 1f
private const val SETTINGS_WALLET_DESC_PT = 2

@Composable
fun SettingsConnectedWalletCard(
    wallet: ConnectedWalletUi,
    modifier: Modifier = Modifier,
) {
    val semantic = KeuTrackTheme.semanticColors
    val typography = KeuTrackTheme.typography
    val textColors = KeuTrackTheme.textColors

    KeuTrackCard(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(0.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (wallet.leadingAccent) {
                Spacer(
                    modifier =
                        Modifier
                            .width(SETTINGS_WALLET_LEADING_ACCENT.dp)
                            .fillMaxHeight()
                            .background(color = semantic.success),
                )
            }
            Row(
                modifier = Modifier
                    .weight(SETTINGS_WALLET_MAIN_WEIGHT)
                    .fillMaxWidth()
                    .padding(SETTINGS_WALLET_CONTENT_PADDING.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = wallet.icon,
                    contentDescription = null,
                    tint = semantic.primary,
                    modifier = Modifier.size(SETTINGS_WALLET_ICON.dp),
                )
                Spacer(modifier = Modifier.width(SETTINGS_WALLET_TITLE_PS.dp))
                Column(modifier = Modifier.weight(SETTINGS_WALLET_TITLE_WEIGHT)) {
                    Text(
                        text = wallet.title,
                        style = typography.bodyBold16,
                        color = textColors.title,
                    )
                    Text(
                        text = wallet.subtitle,
                        style = typography.bodyRegular14,
                        color = textColors.body,
                        modifier = Modifier.padding(top = SETTINGS_WALLET_DESC_PT.dp),
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = wallet.amountLabel,
                        style = typography.bodyBold14,
                        color = textColors.title,
                    )
                    SettingsStatusChip(
                        label = wallet.statusLabel,
                        modifier = Modifier.padding(top = SETTINGS_WALLET_CHIP_PT.dp),
                    )
                }
            }
        }
    }
}

//@Preview
//@Composable
//private fun SettingsConnectedWalletCardPreview() {
//    KeuTrackTheme {
//        SettingsConnectedWalletCard(
//            wallet = DefaultSettingsMockContent.connectedWallets[1]
//        )
//    }
//}
