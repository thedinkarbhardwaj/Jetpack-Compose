package com.sunpawtechnologies.scrollless.data.repository

import com.sunpawtechnologies.scrollless.data.db.dao.TrackingCoverageDao
import com.sunpawtechnologies.scrollless.data.db.entity.TrackingCoverageEntity
import com.sunpawtechnologies.scrollless.domain.model.CoverageEndReason
import com.sunpawtechnologies.scrollless.domain.model.TrackingSource
import com.sunpawtechnologies.scrollless.domain.repository.TrackingCoverageRepository
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class RoomTrackingCoverageRepository(
    private val dao: TrackingCoverageDao,
) : TrackingCoverageRepository {

    override suspend fun start(source: TrackingSource, at: Instant): Long =
        dao.insert(TrackingCoverageEntity(source = source, startUtcMs = at.toEpochMilli()))

    override suspend fun end(id: Long, at: Instant, reason: CoverageEndReason) =
        dao.close(id, at.toEpochMilli(), reason)

    override suspend fun closeAbandoned(source: TrackingSource) {
        dao.closeAllOpenAtLastActivity(source, CoverageEndReason.SERVICE_INTERRUPTED)
    }

    override suspend fun recordCovered(source: TrackingSource, from: Instant, to: Instant) {
        if (!to.isAfter(from)) return
        val latest = dao.latest(source)
        val latestEnd = latest?.endUtcMs
        if (latest != null && latestEnd != null && latestEnd >= from.toEpochMilli() && latest.startUtcMs <= from.toEpochMilli()) {
            if (to.toEpochMilli() > latestEnd) dao.updateEnd(latest.id, to.toEpochMilli())
        } else {
            dao.insert(TrackingCoverageEntity(source = source, startUtcMs = from.toEpochMilli(), endUtcMs = to.toEpochMilli()))
        }
    }

    override suspend fun lastCoveredEnd(source: TrackingSource): Instant? =
        dao.latest(source)?.endUtcMs?.let(Instant::ofEpochMilli)

    override suspend fun interruptionsSince(source: TrackingSource, since: Instant): Int =
        dao.countEndedSince(source, CoverageEndReason.SERVICE_INTERRUPTED, since.toEpochMilli())

    override suspend fun trackedDays(
        source: TrackingSource,
        from: LocalDate,
        toInclusive: LocalDate,
        zone: ZoneId,
        now: Instant,
    ): Set<LocalDate> {
        val rangeStart = from.atStartOfDay(zone).toInstant()
        val rangeEnd = toInclusive.plusDays(1).atStartOfDay(zone).toInstant()
        val days = mutableSetOf<LocalDate>()
        dao.overlapping(source, rangeStart.toEpochMilli(), rangeEnd.toEpochMilli()).forEach { interval ->
            val start = maxOf(Instant.ofEpochMilli(interval.startUtcMs), rangeStart)
            val end = minOf(interval.endUtcMs?.let(Instant::ofEpochMilli) ?: now, rangeEnd)
            var day = start.atZone(zone).toLocalDate()
            // Inclusive of the end's day, so zero-length intervals still mark their day.
            val lastDay = (if (end.isAfter(start)) end.minusMillis(1) else start).atZone(zone).toLocalDate()
            while (!day.isAfter(lastDay)) {
                days += day
                day = day.plusDays(1)
            }
        }
        return days
    }
}
