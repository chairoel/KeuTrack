package com.mascill.keutrack.core.common.utils

import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

/**
 * Inclusive Instant ranges for calendar periods.
 *
 * Room filters with `dateEpochMs >= startMs AND dateEpochMs <= endMs`, so [ClosedRange.endInclusive]
 * must be the last instant of the period — not the start of the next day/month.
 */
object PeriodBounds {

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
}
