package com.mascill.keutrack.feature.settings.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mascill.keutrack.core.designsystem.component.KeuTrackCard
import com.mascill.keutrack.core.designsystem.theme.KeuTrackTheme

private const val SETTINGS_CURRENCY_ICON = 28
private const val SETTINGS_CURRENCY_TITLE_PT = 2
private const val SETTINGS_CURRENCY_SPACER = 16
private const val SETTINGS_CURRENCY_CONTENT_WEIGHT = 1f

@Composable
fun SettingsPrimaryCurrencyRow(
    title: String,
    subtitle: String,
    options: List<String>,
    selectedCode: String,
    onCurrencySelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val semantic = KeuTrackTheme.semanticColors
    val typography = KeuTrackTheme.typography
    val textColors = KeuTrackTheme.textColors

    var expanded by remember { mutableStateOf(false) }

    KeuTrackCard(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.AccountBalanceWallet,
                contentDescription = null,
                tint = semantic.success,
                modifier = Modifier.size(SETTINGS_CURRENCY_ICON.dp),
            )
            Spacer(modifier = Modifier.width(SETTINGS_CURRENCY_SPACER.dp))
            Column(modifier = Modifier.weight(SETTINGS_CURRENCY_CONTENT_WEIGHT)) {
                Text(
                    text = title,
                    style = typography.bodyBold16,
                    color = textColors.title,
                )
                Text(
                    text = subtitle,
                    style = typography.bodyRegular14,
                    color = textColors.body,
                    modifier = Modifier.padding(top = SETTINGS_CURRENCY_TITLE_PT.dp),
                )
            }
            Box {
                TextButton(onClick = { expanded = true }) {
                    Text(
                        text = selectedCode,
                        style = typography.bodyBold14,
                        color = semantic.primary,
                    )
                    Icon(
                        imageVector = Icons.Filled.ArrowDropDown,
                        contentDescription = null,
                        tint = semantic.primary,
                    )
                }
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                ) {
                    options.forEach { code ->
                        DropdownMenuItem(
                            onClick = {
                                onCurrencySelected(code)
                                expanded = false
                            },
                        ) {
                            Text(
                                text = code,
                                style = typography.bodyRegular16,
                                color = textColors.title,
                            )
                        }
                    }
                }
            }
        }
    }
}

//@Preview
//@Composable
//private fun SettingsPrimaryCurrencyRowPreview() {
//    KeuTrackTheme {
//        SettingsPrimaryCurrencyRow(
//            title = "Primary Currency",
//            subtitle = "Used for all overview balances",
//            options = listOf("IDR", "USD", "EUR"),
//            selectedCode = "IDR",
//            onCurrencySelected = {}
//        )
//    }
//}
