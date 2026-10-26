package com.sunpawtechnologies.scrollless.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.sunpawtechnologies.scrollless.data.db.entity.TrackingCoverageEntity
import com.sunpawtechnologies.scrollless.domain.model.CoverageEndReason
import com.sunpawtechnologies.scrollless.domain.model.TrackingSource

@Dao
interface TrackingCoverageDao {
    @Insert
    suspend fun insert(coverage: TrackingCoverageEntity): Long

    /** Closes an open interval. Intervals that are already closed are left unchanged. */
    @Query(
        """
        UPDATE tracking_coverage SET end_utc_ms = :endUtcMs, end_reason = :reason
        WHERE id = :id AND end_utc_ms IS NULL
        """,
    )
    suspend fun close(id: Long, endUtcMs: Long, reason: CoverageEndReason)

    /**
     * Closes intervals a killed process left open. The last Reel recorded inside the interval
     * is the latest moment the service was provably alive, so the interval ends there (or at
     * its start if nothing was recorded).
     */
    @Query(
        """
        UPDATE tracking_coverage SET
            end_utc_ms = MAX(
                start_utc_ms,
                COALESCE(
                    (SELECT MAX(occurred_at_utc_ms) FROM content_view_event
                     WHERE occurred_at_utc_ms >= tracking_coverage.start_utc_ms),
                    start_utc_ms
                )
            ),
            end_reason = :reason
        WHERE source = :source AND end_utc_ms IS NULL
        """,
    )
    suspend fun closeAllOpenAtLastActivity(source: TrackingSource, reason: CoverageEndReason): Int

    /** How often tracking was cut off by the system (process killed or service unbound) since [sinceUtcMs]. */
    @Query("SELECT COUNT(*) FROM tracking_coverage WHERE source = :source AND end_reason = :reason AND end_utc_ms >= :sinceUtcMs")
    suspend fun countEndedSince(source: TrackingSource, reason: CoverageEndReason, sinceUtcMs: Long): Int

    @Query("SELECT * FROM tracking_coverage WHERE source = :source ORDER BY start_utc_ms DESC LIMIT 1")
    suspend fun latest(source: TrackingSource): TrackingCoverageEntity?

    @Query("UPDATE tracking_coverage SET end_utc_ms = :endUtcMs WHERE id = :id")
    suspend fun updateEnd(id: Long, endUtcMs: Long)

    @Query(
        """
        SELECT * FROM tracking_coverage
        WHERE source = :source AND start_utc_ms < :toUtcMs
          AND (end_utc_ms IS NULL OR end_utc_ms > :fromUtcMs)
        ORDER BY start_utc_ms
        """,
    )
    suspend fun overlapping(source: TrackingSource, fromUtcMs: Long, toUtcMs: Long): List<TrackingCoverageEntity>
}
