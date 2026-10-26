package com.sunpawtechnologies.scrollless.data.repository

import com.sunpawtechnologies.scrollless.core.time.splitByLocalDay
import com.sunpawtechnologies.scrollless.data.db.dao.ContentViewDao
import com.sunpawtechnologies.scrollless.data.db.dao.UsageSessionDao
import com.sunpawtechnologies.scrollless.data.db.entity.ContentViewEventEntity
import com.sunpawtechnologies.scrollless.data.db.entity.UsageSessionEntity
import com.sunpawtechnologies.scrollless.domain.model.ContentKind
import com.sunpawtechnologies.scrollless.domain.model.Platform
import com.sunpawtechnologies.scrollless.domain.model.TrackingSource
import com.sunpawtechnologies.scrollless.domain.model.UsageMode
import com.sunpawtechnologies.scrollless.domain.repository.UsageRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

class RoomUsageRepository(
    private val contentViewDao: ContentViewDao,
    private val usageSessionDao: UsageSessionDao,
) : UsageRepository {

    override suspend fun recordContentView(
        kind: ContentKind,
        occurredAt: Instant,
        zone: ZoneId,
        dedupKey: String,
        detectorVersion: Int,
    ): Boolean {
        val local = occurredAt.atZone(zone)
        val rowId = contentViewDao.insert(
            ContentViewEventEntity(
                kind = kind,
                occurredAtUtcMs = occurredAt.toEpochMilli(),
                zoneId = zone.id,
                localEpochDay = local.toLocalDate().toEpochDay(),
                localHour = local.hour,
                dedupKey = dedupKey,
                detectorVersion = detectorVersion,
            ),
        )
        return rowId != -1L
    }

    override suspend fun recordSession(
        platform: Platform,
        mode: UsageMode,
        source: TrackingSource,
        start: Instant,
        endExclusive: Instant,
        zone: ZoneId,
    ) {
        val rows = splitByLocalDay(start, endExclusive, zone).map { slice ->
            UsageSessionEntity(
                platform = platform,
                mode = mode,
                source = source,
                startUtcMs = slice.start.toEpochMilli(),
                endUtcMs = slice.endExclusive.toEpochMilli(),
                zoneId = zone.id,
                localEpochDay = slice.date.toEpochDay(),
            )
        }
        if (rows.isNotEmpty()) usageSessionDao.upsert(rows)
    }

    override fun observeContentViewCount(kind: ContentKind, date: LocalDate): Flow<Int> =
        contentViewDao.observeCountOnDay(kind, date.toEpochDay())

    override fun observeUsageDuration(platform: Platform, mode: UsageMode, date: LocalDate): Flow<Duration> =
        usageSessionDao.observeTotalMillisOnDay(platform, mode, date.toEpochDay()).map(Duration::ofMillis)

    override suspend fun contentViewCountsByDay(kind: ContentKind, from: LocalDate, toInclusive: LocalDate): Map<LocalDate, Long> =
        contentViewDao.countsByDay(kind, from.toEpochDay(), toInclusive.toEpochDay())
            .associate { LocalDate.ofEpochDay(it.key) to it.total }

    override suspend fun contentViewCountsByHour(kind: ContentKind, date: LocalDate): Map<Int, Long> =
        contentViewDao.countsByHour(kind, date.toEpochDay()).associate { it.key.toInt() to it.total }

    override suspend fun usageMillisByDay(
        platform: Platform,
        mode: UsageMode,
        from: LocalDate,
        toInclusive: LocalDate,
    ): Map<LocalDate, Long> =
        usageSessionDao.millisByDay(platform, mode, from.toEpochDay(), toInclusive.toEpochDay())
            .associate { LocalDate.ofEpochDay(it.key) to it.total }

    override suspend fun usageMillisByHour(platform: Platform, mode: UsageMode, date: LocalDate): Map<Int, Long> {
        val byHour = mutableMapOf<Int, Long>()
        usageSessionDao.sessionsOnDay(platform, mode, date.toEpochDay()).forEach { session ->
            val zone = ZoneId.of(session.zoneId)
            var cursor = Instant.ofEpochMilli(session.startUtcMs)
            val end = Instant.ofEpochMilli(session.endUtcMs)
            while (cursor < end) {
                val local = cursor.atZone(zone)
                val nextHour = local.truncatedTo(ChronoUnit.HOURS).plusHours(1).toInstant()
                val sliceEnd = minOf(nextHour, end)
                byHour.merge(local.hour, sliceEnd.toEpochMilli() - cursor.toEpochMilli(), Long::plus)
                cursor = sliceEnd
            }
        }
        return byHour
    }
}
