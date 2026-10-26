package com.sunpawtechnologies.scrollless.domain.repository

import com.sunpawtechnologies.scrollless.domain.model.CoverageEndReason
import com.sunpawtechnologies.scrollless.domain.model.TrackingSource
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Records when tracking was actually running, so gaps can be shown as "not tracked". */
interface TrackingCoverageRepository {
    /** Opens an interval and returns its id. */
    suspend fun start(source: TrackingSource, at: Instant): Long

    suspend fun end(id: Long, at: Instant, reason: CoverageEndReason)

    /**
     * Closes intervals left open by a process that died without shutting down. Their real end
     * is unknown, so they end at the last recorded activity: an under-estimate ("not tracked")
     * is safer than claiming coverage that may not have existed.
     */
    suspend fun closeAbandoned(source: TrackingSource)

    /** Records that [from, to) was fully covered, merging with the previous interval if they touch. */
    suspend fun recordCovered(source: TrackingSource, from: Instant, to: Instant)

    /** End of the most recent coverage interval for [source], or null if it never ran. */
    suspend fun lastCoveredEnd(source: TrackingSource): Instant?

    /** How many times the system cut tracking off since [since] (killed process or unbound service). */
    suspend fun interruptionsSince(source: TrackingSource, since: Instant): Int

    /** Local days on which [source] was tracking at any point. Days outside this set are "not tracked". */
    suspend fun trackedDays(source: TrackingSource, from: LocalDate, toInclusive: LocalDate, zone: ZoneId, now: Instant): Set<LocalDate>
}
