package com.mascill.keutrack.feature.family.presentation.membership

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.AlertDialog
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mascill.keutrack.core.designsystem.component.KeuTrackButton
import com.mascill.keutrack.core.designsystem.component.KeuTrackTextField
import com.mascill.keutrack.core.designsystem.model.KeuTrackButtonStyle
import com.mascill.keutrack.core.designsystem.theme.KeuTrackTheme

enum class FamilyMembershipDialogMode {
    Create,
    Join,
}

@Composable
fun FamilyMembershipDialog(
    mode: FamilyMembershipDialogMode,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (String) -> Unit,
) {
    var input by rememberSaveable { mutableStateOf("") }
    val typography = KeuTrackTheme.typography
    val textColors = KeuTrackTheme.textColors
    val semantic = KeuTrackTheme.semanticColors

    val title =
        when (mode) {
            FamilyMembershipDialogMode.Create -> "Buat Keluarga"
            FamilyMembershipDialogMode.Join -> "Gabung dengan Kode"
        }
    val label =
        when (mode) {
            FamilyMembershipDialogMode.Create -> "Nama keluarga"
            FamilyMembershipDialogMode.Join -> "Kode undangan"
        }
    val placeholder =
        when (mode) {
            FamilyMembershipDialogMode.Create -> "Contoh: Keluarga Amri"
            FamilyMembershipDialogMode.Join -> "KEU-ABC-123"
        }
    val confirmLabel =
        when (mode) {
            FamilyMembershipDialogMode.Create -> "Buat"
            FamilyMembershipDialogMode.Join -> "Gabung"
        }

    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        title = {
            Text(
                text = title,
                style = typography.headingBold20,
                color = textColors.title,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                KeuTrackTextField(
                    value = input,
                    onValueChange = { input = it },
                    label = label,
                    placeholder = placeholder,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (isLoading) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.padding(end = 4.dp),
                            color = semantic.primary,
                        )
                        Text(
                            text = "Memproses…",
                            style = typography.bodyRegular14,
                            color = semantic.onSurfaceVariant,
                        )
                    }
                }
            }
        },
        confirmButton = {
            KeuTrackButton(
                text = confirmLabel,
                onClick = { onSubmit(input) },
                enabled = !isLoading && input.isNotBlank(),
                style = KeuTrackButtonStyle.Primary,
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
