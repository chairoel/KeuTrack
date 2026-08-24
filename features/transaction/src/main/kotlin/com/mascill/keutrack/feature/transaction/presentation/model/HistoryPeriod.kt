package com.mascill.keutrack.feature.transaction.presentation.model

import com.mascill.keutrack.core.common.utils.PeriodBounds
import com.mascill.keutrack.core.common.utils.PeriodLabels
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

enum class HistoryPeriodPreset {
    All,
    Last7Days,
    CurrentMonth,
    Custom,
}

data class HistoryPeriod(
    val preset: HistoryPeriodPreset = HistoryPeriodPreset.All,
    val customFrom: LocalDate? = null,
    val customTo: LocalDate? = null,
) {
    val hasActiveFilter: Boolean get() = preset != HistoryPeriodPreset.All
}

object HistoryPeriodLabels {

    private val locale: Locale = Locale.forLanguageTag("id-ID")
    private val monthYearFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("MMMM yyyy", locale)
    private val dayMonthFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("d MMM", locale)
    private val dayMonthYearFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("d MMM yyyy", locale)

    fun summary(
        preset: HistoryPeriodPreset,
        customFrom: LocalDate? = null,
        customTo: LocalDate? = null,
        cycleStartDay: Int = PeriodBounds.MIN_CYCLE_START_DAY,
    ): String =
        when (preset) {
            HistoryPeriodPreset.All -> "Semua"
            HistoryPeriodPreset.Last7Days -> "7 hari"
            HistoryPeriodPreset.CurrentMonth ->
                PeriodLabels.format(
                    PeriodBounds.containing(LocalDate.now(), cycleStartDay),
                    cycleStartDay,
                )
            HistoryPeriodPreset.Custom ->
                if (customFrom != null && customTo != null) {
                    formatCustomRange(customFrom, customTo)
                } else {
                    "Custom"
                }
        }

    fun formatCustomRange(from: LocalDate, to: LocalDate): String {
        val toLabel = to.format(dayMonthYearFormatter)
        return when {
            from == to -> toLabel
            from.year == to.year && from.month == to.month ->
                "${from.dayOfMonth}–$toLabel"
            from.year == to.year ->
                "${from.format(dayMonthFormatter)} – $toLabel"
            else ->
                "${from.format(dayMonthYearFormatter)} – $toLabel"
        }
    }
}
