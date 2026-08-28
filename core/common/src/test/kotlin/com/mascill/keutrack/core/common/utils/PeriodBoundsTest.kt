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

    @Test
    fun `startDay 1 containing matches calendar month and ofYearMonth`() {
        val date = LocalDate.of(2026, 8, 23)
        val period = PeriodBounds.containing(date, startDay = 1)

        assertThat(period.start).isEqualTo(LocalDate.of(2026, 8, 1))
        assertThat(period.end).isEqualTo(LocalDate.of(2026, 8, 31))
        assertThat(period.periodKey).isEqualTo("2026-08")
        assertThat(period.toInstantRange(wib)).isEqualTo(
            PeriodBounds.ofYearMonth(YearMonth.of(2026, 8), wib),
        )
    }

    @Test
    fun `23 Aug 2026 startDay 25 is 25 Jul to 24 Aug`() {
        val period = PeriodBounds.containing(LocalDate.of(2026, 8, 23), startDay = 25)

        assertThat(period.start).isEqualTo(LocalDate.of(2026, 7, 25))
        assertThat(period.end).isEqualTo(LocalDate.of(2026, 8, 24))
        assertThat(period.periodKey).isEqualTo("2026-08")
        assertThat(period.contains(LocalDate.of(2026, 7, 26))).isTrue()
        assertThat(period.contains(LocalDate.of(2026, 8, 25))).isFalse()
    }

    @Test
    fun `25 Aug 2026 startDay 25 opens the next cycle`() {
        val period = PeriodBounds.containing(LocalDate.of(2026, 8, 25), startDay = 25)

        assertThat(period.start).isEqualTo(LocalDate.of(2026, 8, 25))
        assertThat(period.end).isEqualTo(LocalDate.of(2026, 9, 24))
        assertThat(period.periodKey).isEqualTo("2026-09")
    }

    @Test
    fun `February clamps startDay 28`() {
        val midFeb = PeriodBounds.containing(LocalDate.of(2026, 2, 10), startDay = 28)
        assertThat(midFeb.start).isEqualTo(LocalDate.of(2026, 1, 28))
        assertThat(midFeb.end).isEqualTo(LocalDate.of(2026, 2, 27))
        assertThat(midFeb.periodKey).isEqualTo("2026-02")

        val feb28 = PeriodBounds.containing(LocalDate.of(2026, 2, 28), startDay = 28)
        assertThat(feb28.start).isEqualTo(LocalDate.of(2026, 2, 28))
        assertThat(feb28.end).isEqualTo(LocalDate.of(2026, 3, 27))
        assertThat(feb28.periodKey).isEqualTo("2026-03")
    }

    @Test
    fun `plusPeriods shifts by one cycle`() {
        val current = PeriodBounds.containing(LocalDate.of(2026, 8, 23), startDay = 25)
        val previous = current.minusPeriods(1)
        val next = current.plusPeriods(1)

        assertThat(previous.start).isEqualTo(LocalDate.of(2026, 6, 25))
        assertThat(previous.end).isEqualTo(LocalDate.of(2026, 7, 24))
        assertThat(next.start).isEqualTo(LocalDate.of(2026, 8, 25))
        assertThat(next.end).isEqualTo(LocalDate.of(2026, 9, 24))
    }

    @Test
    fun `26 Jul with startDay 25 uses August periodKey`() {
        assertThat(PeriodBounds.periodKey(LocalDate.of(2026, 7, 26), 25)).isEqualTo("2026-08")
        assertThat(PeriodBounds.periodKey(LocalDate.of(2026, 7, 26), 1)).isEqualTo("2026-07")
    }

    private fun zoned(year: Int, month: Int, day: Int) =
        ZonedDateTime.of(year, month, day, 0, 0, 0, 0, wib).toInstant()
}
