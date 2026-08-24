package com.mascill.keutrack.core.common.utils

import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Inclusive payday / calendar finance window.
 *
 * [periodKey] is `yyyy-MM` of [end], so `startDay = 1` stays compatible with calendar months.
 */
data class FinancePeriod(
    val start: LocalDate,
    val end: LocalDate,
) {
    val periodKey: String get() = YearMonth.from(end).toString()

    fun contains(date: LocalDate): Boolean =
        !date.isBefore(start) && !date.isAfter(end)

    fun minusPeriods(periods: Long): FinancePeriod =
        PeriodBounds.plusPeriods(this, -periods)

    fun plusPeriods(periods: Long): FinancePeriod =
        PeriodBounds.plusPeriods(this, periods)

    fun toInstantRange(zone: ZoneId = ZoneId.systemDefault()) =
        PeriodBounds.toInstantRange(this, zone)
}

object PeriodLabels {

    private val locale: Locale = Locale.forLanguageTag("id-ID")
    private val monthYearFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("MMMM yyyy", locale)
    private val dayMonthFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("d MMM", locale)
    private val dayMonthYearFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("d MMM yyyy", locale)

    fun format(period: FinancePeriod, startDay: Int): String {
        val day = PeriodBounds.clampCycleStartDay(startDay)
        return if (day == PeriodBounds.MIN_CYCLE_START_DAY) {
            YearMonth.from(period.end).format(monthYearFormatter)
        } else {
            formatRange(period.start, period.end)
        }
    }

    fun formatRange(start: LocalDate, end: LocalDate): String =
        if (start.year == end.year) {
            "${start.format(dayMonthFormatter)} – ${end.format(dayMonthYearFormatter)}"
        } else {
            "${start.format(dayMonthYearFormatter)} – ${end.format(dayMonthYearFormatter)}"
        }

    fun formatPreview(period: FinancePeriod): String =
        "${period.start.format(dayMonthYearFormatter)} – ${period.end.format(dayMonthYearFormatter)}"

    fun cycleSubtitle(startDay: Int): String {
        val day = PeriodBounds.clampCycleStartDay(startDay)
        return if (day == PeriodBounds.MIN_CYCLE_START_DAY) {
            "Tanggal 1 – akhir bulan (kalender)"
        } else {
            "Tanggal $day – sehari sebelum tanggal $day berikutnya"
        }
    }
}
