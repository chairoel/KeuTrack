package com.mascill.keutrack.feature.transaction.presentation.components

import android.app.DatePickerDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import java.time.LocalDate
import java.time.ZoneId

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
    minDate: LocalDate? = null,
    maxDate: LocalDate? = null,
) {
    val context = LocalContext.current
    val dialog = remember(context) { DatePickerDialog(context) }
    val zone = ZoneId.systemDefault()
    val constrainedDate =
        selectedDate
            .let { date -> minDate?.let { date.coerceAtLeast(it) } ?: date }
            .let { date -> maxDate?.let { date.coerceAtMost(it) } ?: date }

    DisposableEffect(visible, constrainedDate, minDate, maxDate) {
        if (visible) {
            val picker = dialog.datePicker
            val minMillis =
                (minDate ?: LocalDate.of(1900, 1, 1))
                    .atStartOfDay(zone)
                    .toInstant()
                    .toEpochMilli()
            val maxMillis =
                (maxDate ?: LocalDate.of(2100, 12, 31))
                    .plusDays(1)
                    .atStartOfDay(zone)
                    .toInstant()
                    .minusMillis(1)
                    .toEpochMilli()
            picker.minDate = minOf(minMillis, maxMillis)
            picker.maxDate = maxOf(minMillis, maxMillis)
            dialog.setOnDateSetListener { _, year, month, dayOfMonth ->
                onDateSelected(LocalDate.of(year, month + 1, dayOfMonth))
                onDismiss()
            }
            dialog.setOnCancelListener { onDismiss() }
            dialog.updateDate(
                constrainedDate.year,
                constrainedDate.monthValue - 1,
                constrainedDate.dayOfMonth,
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
