package com.mascill.keutrack.core.common.utils

import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

/**
 * Inclusive Instant ranges for calendar periods and payday cycles.
 *
 * Room filters with `dateEpochMs >= startMs AND dateEpochMs <= endMs`, so [ClosedRange.endInclusive]
 * must be the last instant of the period — not the start of the next day/month.
 */
object PeriodBounds {

    const val MIN_CYCLE_START_DAY = 1
    const val MAX_CYCLE_START_DAY = 28

    fun ofYearMonth(
        month: YearMonth,
        zone: ZoneId = ZoneId.systemDefault(),
    ): ClosedRange<Instant> {
        val start = month.atDay(1).atStartOfDay(zone).toInstant()
        val endExclusive = month.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant()
        return start..endExclusive.minusNanos(1)
    }

    fun ofLocalDates(
        from: LocalDate,
        to: LocalDate,
        zone: ZoneId = ZoneId.systemDefault(),
    ): ClosedRange<Instant> {
        val start = from.atStartOfDay(zone).toInstant()
        val endExclusive = to.plusDays(1).atStartOfDay(zone).toInstant()
        return start..endExclusive.minusNanos(1)
    }

    fun clampCycleStartDay(startDay: Int): Int =
        startDay.coerceIn(MIN_CYCLE_START_DAY, MAX_CYCLE_START_DAY)

    fun containing(
        date: LocalDate,
        startDay: Int,
    ): FinancePeriod {
        val anchor = clampCycleStartDay(startDay)
        val startThis = date.withDayOfMonth(minOf(anchor, date.lengthOfMonth()))
        val start =
            if (!date.isBefore(startThis)) {
                startThis
            } else {
                val previousMonth = startThis.minusMonths(1)
                previousMonth.withDayOfMonth(minOf(anchor, previousMonth.lengthOfMonth()))
            }
        return FinancePeriod(start = start, end = inclusiveEnd(start))
    }

    fun plusPeriods(period: FinancePeriod, periods: Long): FinancePeriod {
        val start = period.start.plusMonths(periods)
        return FinancePeriod(start = start, end = inclusiveEnd(start))
    }

    fun periodKey(date: LocalDate, startDay: Int): String =
        containing(date, startDay).periodKey

    fun toInstantRange(
        period: FinancePeriod,
        zone: ZoneId = ZoneId.systemDefault(),
    ): ClosedRange<Instant> = ofLocalDates(period.start, period.end, zone)

    private fun inclusiveEnd(start: LocalDate): LocalDate =
        start.plusMonths(1).minusDays(1)
}
