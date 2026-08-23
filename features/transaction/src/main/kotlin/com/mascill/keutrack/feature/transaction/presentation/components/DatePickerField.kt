package com.mascill.keutrack.feature.transaction.presentation.components

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.mascill.keutrack.core.designsystem.component.KeuTrackButton
import com.mascill.keutrack.core.designsystem.model.KeuTrackButtonStyle
import com.mascill.keutrack.core.designsystem.theme.KeuTrackTheme
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.Locale

private const val DATE_PICKER_TITLE = "Pilih tanggal"
private const val DATE_RANGE_TITLE = "Pilih rentang tanggal"
private const val DATE_RANGE_HINT = "Pilih tanggal mulai dan akhir"
private const val DATE_RANGE_END_PLACEHOLDER = "…"
private const val DATE_PICKER_CANCEL = "Batal"
private const val DATE_PICKER_CONFIRM = "Pilih"
private const val DATE_PICKER_PREV_MONTH = "Bulan sebelumnya"
private const val DATE_PICKER_NEXT_MONTH = "Bulan berikutnya"
private const val DATE_PICKER_CARD_PH = 20
private const val DATE_PICKER_CARD_PV = 20
private const val DATE_PICKER_TITLE_PB = 4
private const val DATE_PICKER_HEADLINE_PB = 12
private const val DATE_PICKER_WEEKDAY_PT = 8
private const val DATE_PICKER_ACTIONS_PT = 16
private const val DATE_PICKER_ACTION_SPACING = 8
private const val DATE_RANGE_FILL_ALPHA = 0.18f
private val DATE_PICKER_LOCALE: Locale = Locale.forLanguageTag("id-ID")
private val DATE_PICKER_MONTH_FORMATTER: DateTimeFormatter =
    DateTimeFormatter.ofPattern("MMMM yyyy", DATE_PICKER_LOCALE)
private val DATE_RANGE_DAY_FORMATTER: DateTimeFormatter =
    DateTimeFormatter.ofPattern("d MMM yyyy", DATE_PICKER_LOCALE)

private enum class CalendarDayRole {
    None,
    Single,
    RangeStart,
    RangeMiddle,
    RangeEnd,
}

/**
 * Themed single-date calendar dialog. Selected date is reported as [LocalDate]; convert to Instant
 * via
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
    title: String = DATE_PICKER_TITLE,
) {
    if (!visible) return

    val constrainedDate = constrainToBounds(selectedDate, minDate, maxDate)
    var displayedMonth by remember(visible, constrainedDate) {
        mutableStateOf(YearMonth.from(constrainedDate))
    }
    var pendingDate by remember(visible, constrainedDate) { mutableStateOf(constrainedDate) }

    CalendarDialog(
        title = title,
        headline = null,
        displayedMonth = displayedMonth,
        minDate = minDate,
        maxDate = maxDate,
        confirmEnabled = true,
        onPreviousMonth = { displayedMonth = displayedMonth.minusMonths(1) },
        onNextMonth = { displayedMonth = displayedMonth.plusMonths(1) },
        onDismiss = onDismiss,
        onConfirm = {
            onDateSelected(pendingDate)
            onDismiss()
        },
        roleForDate = { date ->
            if (date == pendingDate) CalendarDayRole.Single else CalendarDayRole.None
        },
        onDateClick = { pendingDate = it },
    )
}

/**
 * Themed date-range calendar. First tap sets the start, second tap sets the end; [onRangeSelected]
 * fires only when both ends are confirmed.
 */
@Composable
fun DateRangePickerDialogHost(
    visible: Boolean,
    startDate: LocalDate?,
    endDate: LocalDate?,
    onRangeSelected: (LocalDate, LocalDate) -> Unit,
    onDismiss: () -> Unit,
    minDate: LocalDate? = null,
    maxDate: LocalDate? = null,
    title: String = DATE_RANGE_TITLE,
) {
    if (!visible) return

    val initialFrom = startDate?.let { constrainToBounds(it, minDate, maxDate) }
    val initialTo = endDate?.let { constrainToBounds(it, minDate, maxDate) }
    var displayedMonth by remember(visible, initialFrom, initialTo) {
        mutableStateOf(YearMonth.from(initialFrom ?: LocalDate.now()))
    }
    var pendingFrom by remember(visible, initialFrom) { mutableStateOf(initialFrom) }
    var pendingTo by remember(visible, initialTo) { mutableStateOf(initialTo) }

    CalendarDialog(
        title = title,
        headline = rangeHeadline(pendingFrom, pendingTo),
        displayedMonth = displayedMonth,
        minDate = minDate,
        maxDate = maxDate,
        confirmEnabled = pendingFrom != null && pendingTo != null,
        onPreviousMonth = { displayedMonth = displayedMonth.minusMonths(1) },
        onNextMonth = { displayedMonth = displayedMonth.plusMonths(1) },
        onDismiss = onDismiss,
        onConfirm = {
            val from = pendingFrom ?: return@CalendarDialog
            val to = pendingTo ?: return@CalendarDialog
            onRangeSelected(from, to)
            onDismiss()
        },
        roleForDate = { date -> rangeRole(date, pendingFrom, pendingTo) },
        onDateClick = { date ->
            val from = pendingFrom
            val to = pendingTo
            when {
                from == null || to != null -> {
                    pendingFrom = date
                    pendingTo = null
                }
                date < from -> {
                    pendingFrom = date
                    pendingTo = from
                }
                else -> pendingTo = date
            }
        },
    )
}

@Composable
private fun CalendarDialog(
    title: String,
    headline: String?,
    displayedMonth: YearMonth,
    minDate: LocalDate?,
    maxDate: LocalDate?,
    confirmEnabled: Boolean,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    roleForDate: (LocalDate) -> CalendarDayRole,
    onDateClick: (LocalDate) -> Unit,
) {
    val minMonth = minDate?.let { YearMonth.from(it) }
    val maxMonth = maxDate?.let { YearMonth.from(it) }
    val canGoPrevious = minMonth == null || displayedMonth > minMonth
    val canGoNext = maxMonth == null || displayedMonth < maxMonth
    val weekFields = remember { WeekFields.of(DATE_PICKER_LOCALE) }
    val days = remember(displayedMonth) { monthCells(displayedMonth, weekFields) }
    val weekdays = remember(weekFields) { weekdayLabels(weekFields) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        val semantic = KeuTrackTheme.semanticColors
        val shapes = KeuTrackTheme.shapeTokens
        val typography = KeuTrackTheme.typography
        val textColors = KeuTrackTheme.textColors
        val cardShape = RoundedCornerShape(shapes.radiusXl)

        Surface(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = DATE_PICKER_CARD_PH.dp),
            shape = cardShape,
            color = semantic.surfaceContainerLow,
        ) {
            Column(
                modifier =
                    Modifier.padding(
                        horizontal = DATE_PICKER_CARD_PH.dp,
                        vertical = DATE_PICKER_CARD_PV.dp,
                    ),
            ) {
                if (headline != null) {
                    Text(
                        text = title,
                        style = typography.bodyRegular14,
                        color = textColors.body,
                        modifier = Modifier.padding(bottom = DATE_PICKER_TITLE_PB.dp),
                    )
                    Text(
                        text = headline,
                        style = typography.headingBold20,
                        color = textColors.title,
                        modifier = Modifier.padding(bottom = DATE_PICKER_HEADLINE_PB.dp),
                    )
                } else {
                    Text(
                        text = title,
                        style = typography.headingBold20,
                        color = textColors.title,
                        modifier = Modifier.padding(bottom = DATE_PICKER_HEADLINE_PB.dp),
                    )
                }
                MonthHeader(
                    monthLabel = displayedMonth.format(DATE_PICKER_MONTH_FORMATTER),
                    canGoPrevious = canGoPrevious,
                    canGoNext = canGoNext,
                    onPrevious = onPreviousMonth,
                    onNext = onNextMonth,
                )
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(top = DATE_PICKER_WEEKDAY_PT.dp),
                ) {
                    weekdays.forEach { label ->
                        Text(
                            text = label,
                            style = typography.bodyBold12,
                            color = textColors.body,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                days.chunked(7).forEach { week ->
                    Row(modifier = Modifier.fillMaxWidth()) {
                        week.forEach { date ->
                            DayCell(
                                date = date,
                                role = date?.let(roleForDate) ?: CalendarDayRole.None,
                                today = date == LocalDate.now(),
                                enabled =
                                    date != null &&
                                        (minDate == null || !date.isBefore(minDate)) &&
                                        (maxDate == null || !date.isAfter(maxDate)),
                                onClick = onDateClick,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(top = DATE_PICKER_ACTIONS_PT.dp),
                    horizontalArrangement = Arrangement.spacedBy(DATE_PICKER_ACTION_SPACING.dp),
                ) {
                    KeuTrackButton(
                        text = DATE_PICKER_CANCEL,
                        onClick = onDismiss,
                        style = KeuTrackButtonStyle.Secondary,
                        modifier = Modifier.weight(1f),
                    )
                    KeuTrackButton(
                        text = DATE_PICKER_CONFIRM,
                        onClick = onConfirm,
                        enabled = confirmEnabled,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun MonthHeader(
    monthLabel: String,
    canGoPrevious: Boolean,
    canGoNext: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    val semantic = KeuTrackTheme.semanticColors
    val typography = KeuTrackTheme.typography
    val textColors = KeuTrackTheme.textColors
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onPrevious, enabled = canGoPrevious) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = DATE_PICKER_PREV_MONTH,
                tint = if (canGoPrevious) semantic.onSurface else semantic.onSurfaceVariant,
            )
        }
        Text(
            text = monthLabel,
            style = typography.bodyBold16,
            color = textColors.title,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onNext, enabled = canGoNext) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = DATE_PICKER_NEXT_MONTH,
                tint = if (canGoNext) semantic.onSurface else semantic.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun DayCell(
    date: LocalDate?,
    role: CalendarDayRole,
    today: Boolean,
    enabled: Boolean,
    onClick: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val semantic = KeuTrackTheme.semanticColors
    val typography = KeuTrackTheme.typography
    val textColors = KeuTrackTheme.textColors
    val effects = KeuTrackTheme.effectTokens
    val rangeFill = semantic.primary.copy(alpha = DATE_RANGE_FILL_ALPHA)
    val isEndpoint =
        enabled &&
            (
                role == CalendarDayRole.Single ||
                    role == CalendarDayRole.RangeStart ||
                    role == CalendarDayRole.RangeEnd
            )
    val contentColor =
        when {
            date == null -> Color.Transparent
            !enabled -> textColors.disable
            isEndpoint -> Color.White
            else -> semantic.onSurface
        }
    Box(
        modifier =
            modifier
                .aspectRatio(1f)
                .padding(vertical = 2.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (date != null) {
            if (role == CalendarDayRole.RangeMiddle ||
                role == CalendarDayRole.RangeStart ||
                role == CalendarDayRole.RangeEnd
            ) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(
                                if (role == CalendarDayRole.RangeMiddle) 1f else 0.5f,
                            )
                            .align(
                                when (role) {
                                    CalendarDayRole.RangeStart -> Alignment.CenterEnd
                                    CalendarDayRole.RangeEnd -> Alignment.CenterStart
                                    else -> Alignment.Center
                                },
                            )
                            .background(rangeFill),
                )
            }
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(horizontal = 2.dp)
                        .clip(CircleShape)
                        .background(if (isEndpoint) semantic.primary else Color.Transparent)
                        .then(
                            if (today && enabled && !isEndpoint) {
                                Modifier.border(
                                    width = effects.ghostBorderWidth,
                                    color = semantic.primary,
                                    shape = CircleShape,
                                )
                            } else {
                                Modifier
                            },
                        )
                        .clickable(enabled = enabled) { onClick(date) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = date.dayOfMonth.toString(),
                    style = typography.bodyBold14,
                    color = contentColor,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

private fun constrainToBounds(
    date: LocalDate,
    minDate: LocalDate?,
    maxDate: LocalDate?,
): LocalDate =
    date
        .let { minDate?.let { bound -> it.coerceAtLeast(bound) } ?: it }
        .let { maxDate?.let { bound -> it.coerceAtMost(bound) } ?: it }

private fun rangeRole(
    date: LocalDate,
    from: LocalDate?,
    to: LocalDate?,
): CalendarDayRole {
    if (from == null) return CalendarDayRole.None
    if (to == null) {
        return if (date == from) CalendarDayRole.Single else CalendarDayRole.None
    }
    return when {
        date == from && date == to -> CalendarDayRole.Single
        date == from -> CalendarDayRole.RangeStart
        date == to -> CalendarDayRole.RangeEnd
        date > from && date < to -> CalendarDayRole.RangeMiddle
        else -> CalendarDayRole.None
    }
}

private fun rangeHeadline(from: LocalDate?, to: LocalDate?): String {
    if (from == null) return DATE_RANGE_HINT
    val startLabel = from.format(DATE_RANGE_DAY_FORMATTER)
    val endLabel = to?.format(DATE_RANGE_DAY_FORMATTER) ?: DATE_RANGE_END_PLACEHOLDER
    return "$startLabel – $endLabel"
}

private fun weekdayLabels(weekFields: WeekFields): List<String> {
    val start = weekFields.firstDayOfWeek
    return (0 until 7).map { offset ->
        start.plus(offset.toLong()).getDisplayName(TextStyle.SHORT, DATE_PICKER_LOCALE)
    }
}

private fun monthCells(month: YearMonth, weekFields: WeekFields): List<LocalDate?> {
    val first = month.atDay(1)
    val shift = (first.dayOfWeek.value - weekFields.firstDayOfWeek.value + 7) % 7
    val cells = MutableList<LocalDate?>(shift) { null }
    for (day in 1..month.lengthOfMonth()) {
        cells += month.atDay(day)
    }
    while (cells.size % 7 != 0) {
        cells += null
    }
    return cells
}

@Preview(showBackground = true, name = "Date picker — light")
@Composable
private fun DatePickerDialogHostPreview() {
    KeuTrackTheme(darkTheme = false) {
        DatePickerDialogHost(
            visible = true,
            selectedDate = LocalDate.of(2026, 8, 23),
            onDateSelected = {},
            onDismiss = {},
            maxDate = LocalDate.of(2026, 8, 23),
        )
    }
}

@Preview(showBackground = true, name = "Date range — light")
@Composable
private fun DateRangePickerDialogHostPreview() {
    KeuTrackTheme(darkTheme = false) {
        DateRangePickerDialogHost(
            visible = true,
            startDate = LocalDate.of(2026, 8, 1),
            endDate = LocalDate.of(2026, 8, 17),
            onRangeSelected = { _, _ -> },
            onDismiss = {},
            maxDate = LocalDate.of(2026, 8, 23),
        )
    }
}

@Preview(
    showBackground = true,
    name = "Date range — dark",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun DateRangePickerDialogHostDarkPreview() {
    KeuTrackTheme(darkTheme = true) {
        DateRangePickerDialogHost(
            visible = true,
            startDate = LocalDate.of(2026, 8, 12),
            endDate = null,
            onRangeSelected = { _, _ -> },
            onDismiss = {},
            minDate = LocalDate.of(2026, 7, 1),
            maxDate = LocalDate.of(2026, 8, 23),
        )
    }
}
