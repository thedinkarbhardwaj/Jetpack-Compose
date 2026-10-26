package com.sunpawtechnologies.scrollless.domain.usecase

import com.sunpawtechnologies.scrollless.core.time.LocalPeriod
import com.sunpawtechnologies.scrollless.core.time.PeriodType
import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.roundToInt

sealed interface BucketKey {
    data class Hour(val hour: Int) : BucketKey
    data class Day(val date: LocalDate) : BucketKey
    data class Month(val month: YearMonth) : BucketKey
}

enum class BucketState {
    TRACKED,

    /** Tracking wasn't running. Shown as a gap, never as zero. */
    NOT_TRACKED,

    /** Hasn't happened yet. */
    FUTURE,
}

/** [value] is non-null only for [BucketState.TRACKED]. A tracked bucket with no usage is a real 0. */
data class ChartBucket(val key: BucketKey, val state: BucketState, val value: Long?)

data class PeriodComparison(
    val previousTotal: Long,
    val difference: Long,
    /** Null when the previous total is 0: a percentage from a zero baseline is meaningless. */
    val percentChange: Int?,
)

data class MetricReport(
    val buckets: List<ChartBucket>,
    /** Null when nothing in the period was tracked. */
    val total: Long?,
    /** Per tracked day (week/month) or per tracked month (year). Null for a single day. */
    val average: Long?,
    val peak: ChartBucket?,
    val trackedBuckets: Int,
    /** Tracked days at or over the limit that applied that day. Null without a limit or for a single day. */
    val daysAtOrOverLimit: Int?,
    /** Null when the previous period has no tracked data. */
    val comparison: PeriodComparison?,
)

/**
 * Builds chart buckets and summary figures for one metric over one period.
 *
 * Pure function of its inputs, so every boundary case (leap years, month lengths, untracked
 * days, partial current periods) is unit-tested.
 */
object AnalyticsCalculator {

    /**
     * @param dailyValues totals for days that have records. Must cover [period] and [previous].
     * @param trackedDays days on which tracking ran at any point.
     * @param hourlyValues per-hour totals for [period]'s single day, used when the period is a day.
     * @param currentHour local hour now, so later hours of today count as future.
     * @param limitOn the limit that applied on a given day, or null if the metric has no limit.
     */
    fun report(
        period: LocalPeriod,
        previous: LocalPeriod,
        dailyValues: Map<LocalDate, Long>,
        trackedDays: Set<LocalDate>,
        today: LocalDate,
        currentHour: Int,
        hourlyValues: Map<Int, Long> = emptyMap(),
        limitOn: ((LocalDate) -> Int)? = null,
    ): MetricReport {
        fun dayBucketState(date: LocalDate) = when {
            date.isAfter(today) -> BucketState.FUTURE
            date in trackedDays -> BucketState.TRACKED
            else -> BucketState.NOT_TRACKED
        }

        fun dayValue(date: LocalDate): Long? =
            if (dayBucketState(date) == BucketState.TRACKED) dailyValues[date] ?: 0L else null

        val buckets = when (period.type) {
            PeriodType.DAY -> {
                val day = period.start
                val dayState = dayBucketState(day)
                (0..23).map { hour ->
                    val state = when {
                        dayState != BucketState.TRACKED -> dayState
                        day == today && hour > currentHour -> BucketState.FUTURE
                        else -> BucketState.TRACKED
                    }
                    ChartBucket(BucketKey.Hour(hour), state, if (state == BucketState.TRACKED) hourlyValues[hour] ?: 0L else null)
                }
            }
            PeriodType.WEEK, PeriodType.MONTH -> period.days().map { date ->
                ChartBucket(BucketKey.Day(date), dayBucketState(date), dayValue(date))
            }
            PeriodType.YEAR -> (1..12).map { month ->
                val yearMonth = YearMonth.of(period.start.year, month)
                val days = (1..yearMonth.lengthOfMonth()).map { yearMonth.atDay(it) }
                val trackedValues = days.mapNotNull(::dayValue)
                val state = when {
                    yearMonth.atDay(1).isAfter(today) -> BucketState.FUTURE
                    trackedValues.isNotEmpty() -> BucketState.TRACKED
                    else -> BucketState.NOT_TRACKED
                }
                ChartBucket(BucketKey.Month(yearMonth), state, if (state == BucketState.TRACKED) trackedValues.sum() else null)
            }
        }

        val tracked = buckets.filter { it.state == BucketState.TRACKED }
        val total = if (tracked.isEmpty()) null else tracked.sumOf { it.value ?: 0L }
        val average = if (period.type == PeriodType.DAY || total == null) null else total / tracked.size
        val peak = tracked.filter { (it.value ?: 0L) > 0 }.maxByOrNull { it.value ?: 0L }

        val daysAtOrOverLimit = if (limitOn == null || period.type == PeriodType.DAY) {
            null
        } else {
            period.days().count { date ->
                val value = dayValue(date)
                value != null && value >= limitOn(date)
            }
        }

        val previousTracked = previous.days().filter { dayBucketState(it) == BucketState.TRACKED }
        val comparison = if (total == null || previousTracked.isEmpty()) {
            null
        } else {
            val previousTotal = previousTracked.sumOf { dailyValues[it] ?: 0L }
            val difference = total - previousTotal
            PeriodComparison(
                previousTotal = previousTotal,
                difference = difference,
                percentChange = if (previousTotal > 0) (difference * 100.0 / previousTotal).roundToInt() else null,
            )
        }

        return MetricReport(buckets, total, average, peak, tracked.size, daysAtOrOverLimit, comparison)
    }
}
