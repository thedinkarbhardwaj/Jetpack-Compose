package com.sunpawtechnologies.scrollless.core.time

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import java.time.temporal.WeekFields

enum class PeriodType { DAY, WEEK, MONTH, YEAR }

/**
 * A period in the user's local calendar, from [start] (inclusive) to [endExclusive].
 *
 * Lengths come from the calendar, never from fixed constants, so months, leap years and
 * locale-specific week starts are handled by java.time.
 */
data class LocalPeriod(
    val type: PeriodType,
    val start: LocalDate,
    val endExclusive: LocalDate,
) {
    init {
        require(start < endExclusive) { "Period must not be empty: $start..$endExclusive" }
    }

    val lengthInDays: Int get() = ChronoUnit.DAYS.between(start, endExclusive).toInt()

    operator fun contains(date: LocalDate): Boolean = date >= start && date < endExclusive

    fun days(): List<LocalDate> = List(lengthInDays) { start.plusDays(it.toLong()) }

    /** Midnight can be skipped by a DST change in some zones; atStartOfDay picks the first valid instant. */
    fun startInstant(zone: ZoneId): Instant = start.atStartOfDay(zone).toInstant()

    fun endInstant(zone: ZoneId): Instant = endExclusive.atStartOfDay(zone).toInstant()

    fun previous(weekFields: WeekFields): LocalPeriod = containing(type, start.minusDays(1), weekFields)

    companion object {
        fun containing(type: PeriodType, date: LocalDate, weekFields: WeekFields): LocalPeriod =
            when (type) {
                PeriodType.DAY -> LocalPeriod(type, date, date.plusDays(1))
                PeriodType.WEEK -> {
                    val start = date.with(TemporalAdjusters.previousOrSame(weekFields.firstDayOfWeek))
                    LocalPeriod(type, start, start.plusWeeks(1))
                }
                PeriodType.MONTH -> {
                    val start = date.withDayOfMonth(1)
                    LocalPeriod(type, start, start.plusMonths(1))
                }
                PeriodType.YEAR -> {
                    val start = date.withDayOfYear(1)
                    LocalPeriod(type, start, start.plusYears(1))
                }
            }
    }
}

/** The part of a time interval that falls on one local calendar day. */
data class LocalDaySlice(
    val date: LocalDate,
    val start: Instant,
    val endExclusive: Instant,
) {
    val duration: Duration get() = Duration.between(start, endExclusive)
}

/**
 * Splits [start, endExclusive) at each local midnight in [zone], so usage that crosses midnight
 * is credited to the correct days.
 */
fun splitByLocalDay(start: Instant, endExclusive: Instant, zone: ZoneId): List<LocalDaySlice> {
    require(!endExclusive.isBefore(start)) { "End $endExclusive is before start $start" }
    val slices = mutableListOf<LocalDaySlice>()
    var cursor = start
    var date = start.atZone(zone).toLocalDate()
    while (cursor < endExclusive) {
        val nextMidnight = date.plusDays(1).atStartOfDay(zone).toInstant()
        val sliceEnd = minOf(nextMidnight, endExclusive)
        slices += LocalDaySlice(date, cursor, sliceEnd)
        cursor = sliceEnd
        date = date.plusDays(1)
    }
    return slices
}
