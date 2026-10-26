package com.sunpawtechnologies.scrollless.domain.repository

import com.sunpawtechnologies.scrollless.domain.model.ContentKind
import com.sunpawtechnologies.scrollless.domain.model.Platform
import com.sunpawtechnologies.scrollless.domain.model.TrackingSource
import com.sunpawtechnologies.scrollless.domain.model.UsageMode
import kotlinx.coroutines.flow.Flow
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

interface UsageRepository {
    /**
     * Records one viewed Reel or Short on the local day of [occurredAt] in [zone].
     *
     * @param dedupKey identifies the view; a second event with the same key is ignored.
     * @return true if the view was new, false if it was a duplicate.
     */
    suspend fun recordContentView(
        kind: ContentKind,
        occurredAt: Instant,
        zone: ZoneId,
        dedupKey: String,
        detectorVersion: Int,
    ): Boolean

    /**
     * Records a timed session, split at local midnights. Re-recording a session with the same
     * start (e.g. after it grows) replaces it instead of double counting.
     */
    suspend fun recordSession(
        platform: Platform,
        mode: UsageMode,
        source: TrackingSource,
        start: Instant,
        endExclusive: Instant,
        zone: ZoneId,
    )

    fun observeContentViewCount(kind: ContentKind, date: LocalDate): Flow<Int>

    fun observeUsageDuration(platform: Platform, mode: UsageMode, date: LocalDate): Flow<Duration>

    /** Days with no recorded views are absent. Whether they were tracked comes from coverage. */
    suspend fun contentViewCountsByDay(kind: ContentKind, from: LocalDate, toInclusive: LocalDate): Map<LocalDate, Long>

    /** Keyed by local hour of day (0..23), in the zone each view was recorded in. */
    suspend fun contentViewCountsByHour(kind: ContentKind, date: LocalDate): Map<Int, Long>

    suspend fun usageMillisByDay(platform: Platform, mode: UsageMode, from: LocalDate, toInclusive: LocalDate): Map<LocalDate, Long>

    /** Each session's time split across the local hours it spans. */
    suspend fun usageMillisByHour(platform: Platform, mode: UsageMode, date: LocalDate): Map<Int, Long>
}
