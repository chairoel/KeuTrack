package com.mascill.keutrack.feature.transaction.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Check
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mascill.keutrack.core.designsystem.component.KeuTrackModalBottomSheet
import com.mascill.keutrack.core.designsystem.theme.KeuTrackTheme
import com.mascill.keutrack.feature.transaction.presentation.model.WalletOptionUi

private const val WALLET_PICKER_TITLE = "Pilih Dompet"
private const val WALLET_PICKER_EMPTY = "Belum ada dompet"
private const val WALLET_PICKER_PH = 20
private const val WALLET_PICKER_PB = 16
private const val WALLET_PICKER_ROW_PV = 14
private const val WALLET_PICKER_TITLE_PB = 12

@Composable
fun WalletPickerBottomSheet(
    wallets: List<WalletOptionUi>,
    selectedWalletId: String?,
    onDismiss: () -> Unit,
    onWalletSelected: (String) -> Unit,
) {
    val typography = KeuTrackTheme.typography
    val textColors = KeuTrackTheme.textColors
    val semantic = KeuTrackTheme.semanticColors

    KeuTrackModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = WALLET_PICKER_PH.dp)
                    .padding(bottom = WALLET_PICKER_PB.dp)
                    .navigationBarsPadding(),
        ) {
            Text(
                text = WALLET_PICKER_TITLE,
                style = typography.headingBold20,
                color = textColors.title,
                modifier = Modifier.padding(bottom = WALLET_PICKER_TITLE_PB.dp),
            )

            if (wallets.isEmpty()) {
                Text(
                    text = WALLET_PICKER_EMPTY,
                    style = typography.bodyRegular14,
                    color = textColors.body,
                )
                Spacer(modifier = Modifier.height(8.dp))
            } else {
                LazyColumn {
                    items(wallets, key = { it.id }) { wallet ->
                        val selected = wallet.id == selectedWalletId
                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onWalletSelected(wallet.id)
                                        onDismiss()
                                    }
                                    .padding(vertical = WALLET_PICKER_ROW_PV.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.AccountBalanceWallet,
                                contentDescription = null,
                                tint = semantic.primary,
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = wallet.name,
                                    style = typography.bodyBold16,
                                    color = textColors.title,
                                )
                                Text(
                                    text = wallet.typeLabel,
                                    style = typography.bodyRegular12,
                                    color = textColors.body,
                                )
                            }
                            if (selected) {
                                Icon(
                                    imageVector = Icons.Outlined.Check,
                                    contentDescription = null,
                                    tint = semantic.primary,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
