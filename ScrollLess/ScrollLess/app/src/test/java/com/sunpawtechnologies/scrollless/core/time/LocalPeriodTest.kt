package com.sunpawtechnologies.scrollless.core.time

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.WeekFields

class LocalPeriodTest {

    private val iso = WeekFields.ISO // Monday start
    private val sundayStart = WeekFields.SUNDAY_START
    private fun date(text: String) = LocalDate.parse(text)

    @Test
    fun `day period covers exactly one date`() {
        val period = LocalPeriod.containing(PeriodType.DAY, date("2026-10-24"), iso)
        assertEquals(date("2026-10-24"), period.start)
        assertEquals(date("2026-10-25"), period.endExclusive)
        assertEquals(1, period.lengthInDays)
    }

    @Test
    fun `week start follows the locale's first day of week`() {
        val saturday = date("2026-10-24")
        assertEquals(date("2026-10-19"), LocalPeriod.containing(PeriodType.WEEK, saturday, iso).start)
        assertEquals(date("2026-10-18"), LocalPeriod.containing(PeriodType.WEEK, saturday, sundayStart).start)
    }

    @Test
    fun `week on its own first day starts that day`() {
        val monday = date("2026-10-19")
        val period = LocalPeriod.containing(PeriodType.WEEK, monday, iso)
        assertEquals(monday, period.start)
        assertEquals(7, period.lengthInDays)
    }

    @Test
    fun `february length handles leap years including century rules`() {
        fun febLength(year: Int) =
            LocalPeriod.containing(PeriodType.MONTH, LocalDate.of(year, 2, 10), iso).lengthInDays
        assertEquals(28, febLength(2026))
        assertEquals(29, febLength(2028))
        assertEquals(28, febLength(2100))
        assertEquals(29, febLength(2000))
    }

    @Test
    fun `month lengths come from the calendar`() {
        assertEquals(31, LocalPeriod.containing(PeriodType.MONTH, date("2026-10-24"), iso).lengthInDays)
        assertEquals(30, LocalPeriod.containing(PeriodType.MONTH, date("2026-11-30"), iso).lengthInDays)
    }

    @Test
    fun `year length is 366 in leap years`() {
        assertEquals(365, LocalPeriod.containing(PeriodType.YEAR, date("2026-06-01"), iso).lengthInDays)
        assertEquals(366, LocalPeriod.containing(PeriodType.YEAR, date("2028-06-01"), iso).lengthInDays)
    }

    @Test
    fun `previous day before March 1 in a leap year is February 29`() {
        val previous = LocalPeriod.containing(PeriodType.DAY, date("2028-03-01"), iso).previous(iso)
        assertEquals(date("2028-02-29"), previous.start)
    }

    @Test
    fun `previous month of January is December of the prior year`() {
        val previous = LocalPeriod.containing(PeriodType.MONTH, date("2027-01-15"), iso).previous(iso)
        assertEquals(date("2026-12-01"), previous.start)
        assertEquals(date("2027-01-01"), previous.endExclusive)
    }

    @Test
    fun `weeks crossing a year boundary stay seven days long`() {
        val week = LocalPeriod.containing(PeriodType.WEEK, date("2027-01-01"), iso)
        assertEquals(date("2026-12-28"), week.start)
        assertEquals(date("2027-01-04"), week.endExclusive)
        assertEquals(date("2026-12-21"), week.previous(iso).start)
    }

    @Test
    fun `previous year of a leap year is a normal year`() {
        val previous = LocalPeriod.containing(PeriodType.YEAR, date("2029-03-01"), iso).previous(iso)
        assertEquals(date("2028-01-01"), previous.start)
        assertEquals(366, previous.lengthInDays)
    }

    @Test
    fun `contains is inclusive of start and exclusive of end`() {
        val week = LocalPeriod.containing(PeriodType.WEEK, date("2026-10-24"), iso)
        assertTrue(date("2026-10-19") in week)
        assertTrue(date("2026-10-25") in week)
        assertFalse(date("2026-10-26") in week)
        assertEquals(7, week.days().size)
        assertEquals(date("2026-10-25"), week.days().last())
    }

    @Test
    fun `DST days have 23 and 25 hours`() {
        val newYork = ZoneId.of("America/New_York")
        fun hours(day: String): Long {
            val period = LocalPeriod.containing(PeriodType.DAY, date(day), iso)
            return Duration.between(period.startInstant(newYork), period.endInstant(newYork)).toHours()
        }
        assertEquals(23, hours("2026-03-08"))
        assertEquals(25, hours("2026-11-01"))
        assertEquals(24, hours("2026-10-24"))
    }
}
