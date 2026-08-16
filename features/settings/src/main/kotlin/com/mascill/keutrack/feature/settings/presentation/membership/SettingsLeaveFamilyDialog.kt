package com.mascill.keutrack.feature.settings.presentation.membership

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.AlertDialog
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mascill.keutrack.core.designsystem.component.KeuTrackButton
import com.mascill.keutrack.core.designsystem.model.KeuTrackButtonStyle
import com.mascill.keutrack.core.designsystem.theme.KeuTrackTheme

@Composable
fun SettingsLeaveFamilyDialog(
    familyName: String,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val typography = KeuTrackTheme.typography
    val textColors = KeuTrackTheme.textColors
    val semantic = KeuTrackTheme.semanticColors
    val displayName = familyName.ifBlank { "keluarga ini" }

    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        title = {
            Text(
                text = "Keluar dari keluarga?",
                style = typography.headingBold20,
                color = textColors.title,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text =
                        "Anda akan keluar dari $displayName. " +
                            "Wallet keluarga di perangkat ini akan dihapus; " +
                            "data bersama tetap ada untuk anggota lain.",
                    style = typography.bodyRegular14,
                    color = textColors.body,
                )
                if (isLoading) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        CircularProgressIndicator(color = semantic.primary)
                        Text(
                            text = "Memproses…",
                            style = typography.bodyRegular14,
                            color = semantic.onSurfaceVariant,
                            modifier = Modifier.padding(start = 4.dp),
                        )
                    }
                }
            }
        },
        confirmButton = {
            KeuTrackButton(
                text = "Keluar",
                onClick = onConfirm,
                enabled = !isLoading,
                style = KeuTrackButtonStyle.Tertiary,
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isLoading) {
                Text("Batal")
            }
        },
        backgroundColor = KeuTrackTheme.semanticColors.surfaceContainerLowest,
    )
}
