package com.mascill.keutrack.feature.transaction.presentation.components

import android.content.res.Configuration
import androidx.compose.material.AlertDialog
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.mascill.keutrack.core.designsystem.theme.KeuTrackTheme

private const val DELETE_DIALOG_TITLE = "Hapus transaksi?"
private const val DELETE_DIALOG_BODY =
    "Transaksi ini akan dihapus dari riwayat. Saldo dan anggaran akan disesuaikan."
private const val DELETE_DIALOG_DISMISS = "Batal"
private const val DELETE_DIALOG_CONFIRM = "Hapus"

@Composable
fun DeleteTransactionDialog(
    isBusy: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val typography = KeuTrackTheme.typography
    val textColors = KeuTrackTheme.textColors
    val semantic = KeuTrackTheme.semanticColors

    AlertDialog(
        onDismissRequest = { if (!isBusy) onDismiss() },
        title = {
            Text(
                text = DELETE_DIALOG_TITLE,
                style = typography.headingBold20,
                color = textColors.title,
            )
        },
        text = {
            Text(
                text = DELETE_DIALOG_BODY,
                style = typography.bodyRegular14,
                color = textColors.body,
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = !isBusy) {
                Text(
                    text = DELETE_DIALOG_CONFIRM,
                    style = typography.bodyBold16,
                    color = semantic.error,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isBusy) {
                Text(text = DELETE_DIALOG_DISMISS)
            }
        },
        backgroundColor = semantic.surfaceContainerLowest,
    )
}

@Preview(showBackground = true, name = "Delete dialog")
@Composable
private fun DeleteTransactionDialogPreview() {
    KeuTrackTheme(darkTheme = false) {
        DeleteTransactionDialog(
            isBusy = false,
            onDismiss = {},
            onConfirm = {},
        )
    }
}

@Preview(
    name = "Delete dialog — Dark",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun DeleteTransactionDialogDarkPreview() {
    KeuTrackTheme(darkTheme = true) {
        DeleteTransactionDialog(
            isBusy = false,
            onDismiss = {},
            onConfirm = {},
        )
    }
}
