package com.mascill.keutrack.feature.transaction.presentation.components

import android.app.DatePickerDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import java.time.LocalDate

/**
 * Shows the platform [DatePickerDialog] while [visible] is true.
 * Selected date is reported as [LocalDate]; convert to Instant via
 * [com.mascill.keutrack.feature.transaction.presentation.model.TransactionUiMapper.localDateToInstant]
 * (start of day, system default zone).
 */
@Composable
fun DatePickerDialogHost(
    visible: Boolean,
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val dialog = remember(context) { DatePickerDialog(context) }

    DisposableEffect(visible, selectedDate) {
        if (visible) {
            dialog.setOnDateSetListener { _, year, month, dayOfMonth ->
                onDateSelected(LocalDate.of(year, month + 1, dayOfMonth))
                onDismiss()
            }
            dialog.setOnCancelListener { onDismiss() }
            dialog.updateDate(
                selectedDate.year,
                selectedDate.monthValue - 1,
                selectedDate.dayOfMonth,
            )
            if (!dialog.isShowing) {
                dialog.show()
            }
        }
        onDispose {
            dialog.setOnDateSetListener(null)
            dialog.setOnCancelListener(null)
            if (dialog.isShowing) {
                dialog.dismiss()
            }
        }
    }
}
