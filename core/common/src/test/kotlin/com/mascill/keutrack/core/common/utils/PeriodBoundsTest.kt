package com.mascill.keutrack.core.common.utils

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZonedDateTime

class PeriodBoundsTest {

    private val wib: ZoneId = ZoneId.of("Asia/Jakarta")

    @Test
    fun `August 2026 WIB starts at first instant and ends before September`() {
        val range = PeriodBounds.ofYearMonth(YearMonth.of(2026, 8), wib)
        val augustStart = zoned(2026, 8, 1)
        val septemberStart = zoned(2026, 9, 1)

        assertThat(range.start).isEqualTo(augustStart)
        assertThat(range.endInclusive).isEqualTo(septemberStart.minusNanos(1))
        assertThat(range.endInclusive).isLessThan(septemberStart)
    }

    @Test
    fun `31-day month includes the last day`() {
        val range = PeriodBounds.ofYearMonth(YearMonth.of(2026, 1), wib)
        val januaryStart = zoned(2026, 1, 1)
        val februaryStart = zoned(2026, 2, 1)

        assertThat(range.start).isEqualTo(januaryStart)
        assertThat(range.endInclusive).isEqualTo(februaryStart.minusNanos(1))
        assertThat(range.endInclusive).isGreaterThan(zoned(2026, 1, 31))
    }

    @Test
    fun `February non-leap year ends before March`() {
        val range = PeriodBounds.ofYearMonth(YearMonth.of(2026, 2), wib)
        val februaryStart = zoned(2026, 2, 1)
        val marchStart = zoned(2026, 3, 1)

        assertThat(range.start).isEqualTo(februaryStart)
        assertThat(range.endInclusive).isEqualTo(marchStart.minusNanos(1))
        assertThat(range.endInclusive).isGreaterThan(zoned(2026, 2, 28))
        assertThat(range.endInclusive).isLessThan(marchStart)
    }

    @Test
    fun `single local date is inclusive start of day to end of day`() {
        val day = LocalDate.of(2026, 8, 15)
        val range = PeriodBounds.ofLocalDates(from = day, to = day, zone = wib)
        val dayStart = zoned(2026, 8, 15)
        val nextDayStart = zoned(2026, 8, 16)

        assertThat(range.start).isEqualTo(dayStart)
        assertThat(range.endInclusive).isEqualTo(nextDayStart.minusNanos(1))
        assertThat(range.endInclusive).isLessThan(nextDayStart)
    }

    @Test
    fun `from equal to is a one-day inclusive range`() {
        val day = LocalDate.of(2026, 2, 28)
        val range = PeriodBounds.ofLocalDates(from = day, to = day, zone = wib)

        assertThat(range.start).isEqualTo(zoned(2026, 2, 28))
        assertThat(range.endInclusive).isEqualTo(zoned(2026, 3, 1).minusNanos(1))
    }

    private fun zoned(year: Int, month: Int, day: Int) =
        ZonedDateTime.of(year, month, day, 0, 0, 0, 0, wib).toInstant()
}
