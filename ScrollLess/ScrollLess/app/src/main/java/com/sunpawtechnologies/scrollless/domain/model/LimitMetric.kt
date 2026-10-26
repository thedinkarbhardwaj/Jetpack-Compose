package com.sunpawtechnologies.scrollless.domain.model

enum class LimitUnit { COUNT, MINUTES }

/**
 * A metric with a user-configurable daily limit. Stored by name: never rename or remove a constant.
 *
 * @param measurable false while nothing measures the metric. Its saved value is kept, but it's
 *   hidden from the UI and never evaluated, so the user can't set a limit that silently never fires.
 */
enum class LimitMetric(
    val unit: LimitUnit,
    val defaultDailyLimit: Int,
    val validRange: IntRange,
    val measurable: Boolean = true,
) {
    /** Estimated Reels swiped to (Accessibility, optional). */
    INSTAGRAM_REELS(LimitUnit.COUNT, 50, 1..1000),
    YOUTUBE_SHORTS(LimitUnit.COUNT, 50, 1..1000, measurable = false),
    YOUTUBE_REGULAR_VIDEO(LimitUnit.MINUTES, 60, 5..720, measurable = false),

    /** Time Instagram was in the foreground: all of Instagram, not only Reels (Usage Access). */
    INSTAGRAM_APP_TIME(LimitUnit.MINUTES, 60, 5..720),

    /** Time YouTube was in the foreground: Shorts and videos together (Usage Access). */
    YOUTUBE_APP_TIME(LimitUnit.MINUTES, 60, 5..720),
    ;

    companion object {
        /** The limits the user can set, in display order. App time first: it works without Accessibility. */
        val configurable: List<LimitMetric> = listOf(INSTAGRAM_APP_TIME, YOUTUBE_APP_TIME, INSTAGRAM_REELS)
    }
}

enum class LimitStatus { WITHIN, APPROACHING, REACHED }

/**
 * Shared by the dashboard, the Instagram counter and limit alerts, so all three always agree.
 * Reaching the limit exactly counts as REACHED. Counting continues afterwards.
 */
fun limitStatus(value: Long, limit: Long, warningThresholdPercent: Int): LimitStatus = when {
    limit <= 0 -> LimitStatus.WITHIN
    value >= limit -> LimitStatus.REACHED
    value * 100 >= limit * warningThresholdPercent -> LimitStatus.APPROACHING
    else -> LimitStatus.WITHIN
}
