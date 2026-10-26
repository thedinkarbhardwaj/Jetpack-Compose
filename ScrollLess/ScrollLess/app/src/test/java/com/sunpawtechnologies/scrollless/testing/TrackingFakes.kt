package com.sunpawtechnologies.scrollless.testing

import com.sunpawtechnologies.scrollless.core.time.TimeProvider
import com.sunpawtechnologies.scrollless.domain.model.ContentKind
import com.sunpawtechnologies.scrollless.domain.model.CoverageEndReason
import com.sunpawtechnologies.scrollless.domain.model.LimitMetric
import com.sunpawtechnologies.scrollless.domain.model.PermissionSnapshot
import com.sunpawtechnologies.scrollless.domain.model.Platform
import com.sunpawtechnologies.scrollless.domain.model.TrackingSource
import com.sunpawtechnologies.scrollless.domain.model.UsageMode
import com.sunpawtechnologies.scrollless.domain.repository.LimitChange
import com.sunpawtechnologies.scrollless.domain.repository.LimitRepository
import com.sunpawtechnologies.scrollless.domain.repository.PermissionMonitor
import com.sunpawtechnologies.scrollless.domain.repository.TrackingCoverageRepository
import com.sunpawtechnologies.scrollless.domain.repository.UsageRepository
import com.sunpawtechnologies.scrollless.tracking.usagestats.UsageImporter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.WeekFields

class FakePermissionMonitor(initial: PermissionSnapshot = PermissionSnapshot()) : PermissionMonitor {
    override val state = MutableStateFlow(initial)
    var refreshCount = 0
        private set

    override fun refresh() {
        refreshCount++
    }
}

class FixedTimeProvider(
    var instant: Instant = Instant.parse("2026-10-24T06:30:00Z"),
    private val zoneId: ZoneId = ZoneId.of("Asia/Kolkata"),
) : TimeProvider {
    override fun now(): Instant = instant
    override fun zone(): ZoneId = zoneId
    override fun weekFields(): WeekFields = WeekFields.ISO
}

/** In-memory usage store keyed by app and day. Tests set values directly. */
class FakeUsageRepository : UsageRepository {
    private val reels = mutableMapOf<LocalDate, MutableStateFlow<Int>>()
    private val durations = mutableMapOf<Pair<Platform, LocalDate>, MutableStateFlow<Duration>>()

    fun setReels(date: LocalDate, count: Int) {
        reels.getOrPut(date) { MutableStateFlow(0) }.value = count
    }

    fun setInstagramTime(date: LocalDate, duration: Duration) = setTime(Platform.INSTAGRAM, date, duration)

    fun setYouTubeTime(date: LocalDate, duration: Duration) = setTime(Platform.YOUTUBE, date, duration)

    private fun setTime(platform: Platform, date: LocalDate, duration: Duration) {
        durationFlow(platform, date).value = duration
    }

    private fun durationFlow(platform: Platform, date: LocalDate) =
        durations.getOrPut(platform to date) { MutableStateFlow(Duration.ZERO) }

    override suspend fun recordContentView(
        kind: ContentKind,
        occurredAt: Instant,
        zone: ZoneId,
        dedupKey: String,
        detectorVersion: Int,
    ): Boolean = error("Not used by these tests")

    override suspend fun recordSession(
        platform: Platform,
        mode: UsageMode,
        source: TrackingSource,
        start: Instant,
        endExclusive: Instant,
        zone: ZoneId,
    ) = error("Not used by these tests")

    override fun observeContentViewCount(kind: ContentKind, date: LocalDate): Flow<Int> =
        reels.getOrPut(date) { MutableStateFlow(0) }

    override fun observeUsageDuration(platform: Platform, mode: UsageMode, date: LocalDate): Flow<Duration> = durationFlow(platform, date)

    override suspend fun contentViewCountsByDay(kind: ContentKind, from: LocalDate, toInclusive: LocalDate): Map<LocalDate, Long> =
        reels.filterKeys { !it.isBefore(from) && !it.isAfter(toInclusive) }.mapValues { it.value.value.toLong() }

    override suspend fun contentViewCountsByHour(kind: ContentKind, date: LocalDate): Map<Int, Long> = emptyMap()

    override suspend fun usageMillisByDay(platform: Platform, mode: UsageMode, from: LocalDate, toInclusive: LocalDate): Map<LocalDate, Long> =
        durations.filterKeys { (p, day) -> p == platform && !day.isBefore(from) && !day.isAfter(toInclusive) }
            .entries.associate { (key, flow) -> key.second to flow.value.toMillis() }

    override suspend fun usageMillisByHour(platform: Platform, mode: UsageMode, date: LocalDate): Map<Int, Long> = emptyMap()
}

class FakeLimitRepository : LimitRepository {
    private val changes = MutableStateFlow<Map<LimitMetric, List<LimitChange>>>(emptyMap())

    override fun observeLimitsOn(date: LocalDate): Flow<Map<LimitMetric, Int>> = changes.map { all ->
        LimitMetric.entries.associateWith { metric ->
            all[metric].orEmpty().lastOrNull { !it.effectiveFrom.isAfter(date) }?.value ?: metric.defaultDailyLimit
        }
    }

    override suspend fun setLimit(metric: LimitMetric, value: Int, effectiveFrom: LocalDate) {
        require(value in metric.validRange)
        changes.value = changes.value + (metric to (changes.value[metric].orEmpty().filter { it.effectiveFrom != effectiveFrom } +
            LimitChange(effectiveFrom, value)).sortedBy { it.effectiveFrom })
    }

    override suspend fun history(metric: LimitMetric): List<LimitChange> = changes.value[metric].orEmpty()
}

/** Tests set the tracked days per source directly. */
class FakeTrackingCoverageRepository : TrackingCoverageRepository {
    val tracked = mutableMapOf<TrackingSource, Set<LocalDate>>()

    override suspend fun start(source: TrackingSource, at: Instant): Long = 1
    override suspend fun end(id: Long, at: Instant, reason: CoverageEndReason) = Unit
    override suspend fun closeAbandoned(source: TrackingSource) = Unit
    override suspend fun recordCovered(source: TrackingSource, from: Instant, to: Instant) = Unit
    var interruptions = 0
    var lastEnd: Instant? = null
    override suspend fun lastCoveredEnd(source: TrackingSource): Instant? = lastEnd
    override suspend fun interruptionsSince(source: TrackingSource, since: Instant): Int = interruptions

    override suspend fun trackedDays(source: TrackingSource, from: LocalDate, toInclusive: LocalDate, zone: ZoneId, now: Instant): Set<LocalDate> =
        tracked[source].orEmpty().filterTo(mutableSetOf()) { !it.isBefore(from) && !it.isAfter(toInclusive) }
}

class FakeUsageImporter : UsageImporter {
    var importCount = 0
        private set

    override suspend fun importRecentUsage() {
        importCount++
    }
}
