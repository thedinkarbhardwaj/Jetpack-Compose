package com.sunpawtechnologies.scrollless.tracking.usagestats

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Android keeps detailed usage events for only a few days; asking for more just returns less. */
const val MAX_CATCH_UP_DAYS = 7L

/**
 * Where an import starts reading Android's usage history.
 *
 * - Normally yesterday's midnight, so an ongoing session that crossed midnight is complete.
 * - After a gap (ScrollLess not opened for days), from the start of the day the last import
 *   ended, to recover the missed days.
 * - On first run, as far back as Android may still have events.
 *
 * Always a local midnight and never more than [MAX_CATCH_UP_DAYS] back. Session starts are
 * real event timestamps, so a different window start never duplicates stored sessions.
 */
fun importWindowStart(today: LocalDate, zone: ZoneId, lastCoveredEnd: Instant?): Instant {
    val yesterday = today.minusDays(1).atStartOfDay(zone).toInstant()
    val earliest = today.minusDays(MAX_CATCH_UP_DAYS).atStartOfDay(zone).toInstant()
    if (lastCoveredEnd == null) return earliest
    val lastDayStart = lastCoveredEnd.atZone(zone).toLocalDate().atStartOfDay(zone).toInstant()
    return minOf(yesterday, maxOf(lastDayStart, earliest))
}

/**
 * Where tracked coverage may honestly begin for an import of [windowStart, now).
 *
 * If Android's history reaches back to the window start, the whole window is covered. If it
 * begins later (older events already discarded), coverage starts at the next local midnight
 * after the first event, so a partially remembered day is shown as "not tracked" rather
 * than as a low total. Returns null when nothing can be claimed.
 */
fun coverageStart(windowStart: Instant, firstEvent: Instant?, now: Instant, zone: ZoneId): Instant? {
    if (firstEvent == null) return null
    val start = if (!firstEvent.isAfter(windowStart.plus(HISTORY_SLACK))) {
        windowStart
    } else {
        firstEvent.atZone(zone).toLocalDate().plusDays(1).atStartOfDay(zone).toInstant()
    }
    return start.takeIf { it.isBefore(now) }
}

/** Events rarely land exactly on midnight; history reaching this close to the window start counts as complete. */
private val HISTORY_SLACK: Duration = Duration.ofHours(2)
