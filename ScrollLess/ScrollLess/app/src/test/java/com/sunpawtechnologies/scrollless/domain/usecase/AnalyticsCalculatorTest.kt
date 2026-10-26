package com.sunpawtechnologies.scrollless.domain.usecase

import com.sunpawtechnologies.scrollless.core.time.LocalPeriod
import com.sunpawtechnologies.scrollless.core.time.PeriodType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.WeekFields

class AnalyticsCalculatorTest {

    private val iso = WeekFields.ISO
    private fun date(text: String) = LocalDate.parse(text)
    private fun period(type: PeriodType, day: String) = LocalPeriod.containing(type, date(day), iso)

    private fun report(
        period: LocalPeriod,
        values: Map<LocalDate, Long>,
        tracked: Set<LocalDate>,
        today: LocalDate = date("2026-10-31"),
        hour: Int = 23,
        hourly: Map<Int, Long> = emptyMap(),
        limitOn: ((LocalDate) -> Int)? = null,
    ) = AnalyticsCalculator.report(period, period.previous(iso), values, tracked, today, hour, hourly, limitOn)

    @Test
    fun `untracked days are gaps, not zeros, and are left out of averages`() {
        val week = period(PeriodType.WEEK, "2026-10-19") // Mon 19 .. Sun 25
        val tracked = setOf(date("2026-10-19"), date("2026-10-20"), date("2026-10-21"))
        val result = report(week, mapOf(date("2026-10-19") to 30L, date("2026-10-21") to 60L), tracked)

        assertEquals(listOf(30L, 0L, 60L, null, null, null, null), result.buckets.map { it.value })
        assertEquals(BucketState.NOT_TRACKED, result.buckets[3].state)
        assertEquals(90L, result.total)
        assertEquals(30L, result.average) // 90 over 3 tracked days, not 7
        assertEquals(3, result.trackedBuckets)
    }

    @Test
    fun `a tracked day with no records is a real zero`() {
        val week = period(PeriodType.WEEK, "2026-10-19")
        val result = report(week, emptyMap(), setOf(date("2026-10-20")))
        assertEquals(0L, result.total)
        assertEquals(BucketState.TRACKED, result.buckets[1].state)
        assertNull(result.peak)
    }

    @Test
    fun `nothing tracked means no totals at all`() {
        val result = report(period(PeriodType.MONTH, "2026-10-01"), emptyMap(), emptySet())
        assertNull(result.total)
        assertNull(result.average)
        assertNull(result.comparison)
        assertEquals(0, result.trackedBuckets)
    }

    @Test
    fun `future days are neither tracked nor gaps`() {
        val week = period(PeriodType.WEEK, "2026-10-19")
        val today = date("2026-10-22")
        val result = report(week, emptyMap(), week.days().toSet(), today = today)
        assertEquals(4, result.trackedBuckets)
        assertTrue(result.buckets.drop(4).all { it.state == BucketState.FUTURE })
    }

    @Test
    fun `month buckets follow the calendar including leap february`() {
        assertEquals(29, report(period(PeriodType.MONTH, "2028-02-10"), emptyMap(), emptySet(), today = date("2028-12-31")).buckets.size)
        assertEquals(28, report(period(PeriodType.MONTH, "2026-02-10"), emptyMap(), emptySet()).buckets.size)
    }

    @Test
    fun `year buckets sum tracked days per month and average over tracked months`() {
        val year = period(PeriodType.YEAR, "2026-06-01")
        val values = mapOf(date("2026-01-05") to 10L, date("2026-01-06") to 5L, date("2026-03-01") to 30L)
        val tracked = setOf(date("2026-01-05"), date("2026-01-06"), date("2026-03-01"))
        val result = report(year, values, tracked, today = date("2026-10-31"))

        assertEquals(12, result.buckets.size)
        assertEquals(15L, result.buckets[0].value)
        assertEquals(BucketState.NOT_TRACKED, result.buckets[1].state)
        assertEquals(30L, result.buckets[2].value)
        assertEquals(BucketState.FUTURE, result.buckets[11].state)
        assertEquals(45L, result.total)
        assertEquals(22L, result.average) // 45 over 2 tracked months
        assertEquals(BucketKey.Month(YearMonth.of(2026, 3)), result.peak?.key)
    }

    @Test
    fun `day view uses hourly values and later hours of today are future`() {
        val day = period(PeriodType.DAY, "2026-10-31")
        val result = report(day, mapOf(day.start to 12L), setOf(day.start), today = day.start, hour = 14, hourly = mapOf(9 to 5L, 14 to 7L))
        assertEquals(24, result.buckets.size)
        assertEquals(5L, result.buckets[9].value)
        assertEquals(BucketState.FUTURE, result.buckets[15].state)
        assertEquals(12L, result.total)
        assertNull(result.average)
        assertNull(result.daysAtOrOverLimit)
    }

    @Test
    fun `comparison uses only tracked days of the previous period`() {
        val week = period(PeriodType.WEEK, "2026-10-19")
        val previousDays = week.previous(iso).days()
        val values = mapOf(previousDays[0] to 40L, date("2026-10-19") to 60L)
        val result = report(week, values, setOf(previousDays[0], date("2026-10-19")))
        val comparison = result.comparison!!
        assertEquals(40L, comparison.previousTotal)
        assertEquals(20L, comparison.difference)
        assertEquals(50, comparison.percentChange)
    }

    @Test
    fun `no percentage from a zero baseline`() {
        val week = period(PeriodType.WEEK, "2026-10-19")
        val previousDay = week.previous(iso).start
        val result = report(week, mapOf(date("2026-10-19") to 25L), setOf(previousDay, date("2026-10-19")))
        assertEquals(0L, result.comparison?.previousTotal)
        assertEquals(25L, result.comparison?.difference)
        assertNull(result.comparison?.percentChange)
    }

    @Test
    fun `no comparison when the previous period was never tracked`() {
        val week = period(PeriodType.WEEK, "2026-10-19")
        assertNull(report(week, mapOf(date("2026-10-19") to 5L), setOf(date("2026-10-19"))).comparison)
    }

    @Test
    fun `days over limit use the limit that applied each day`() {
        val week = period(PeriodType.WEEK, "2026-10-19")
        val values = week.days().associateWith { 40L }
        // Limit 50 until Wednesday, then 30.
        val limitOn = { day: LocalDate -> if (day.isBefore(date("2026-10-22"))) 50 else 30 }
        val result = report(week, values, week.days().toSet(), limitOn = limitOn)
        assertEquals(4, result.daysAtOrOverLimit) // Thu..Sun
    }

    @Test
    fun `reaching the limit exactly counts as at the limit`() {
        val week = period(PeriodType.WEEK, "2026-10-19")
        val result = report(week, mapOf(date("2026-10-19") to 50L), setOf(date("2026-10-19")), limitOn = { 50 })
        assertEquals(1, result.daysAtOrOverLimit)
    }
}
