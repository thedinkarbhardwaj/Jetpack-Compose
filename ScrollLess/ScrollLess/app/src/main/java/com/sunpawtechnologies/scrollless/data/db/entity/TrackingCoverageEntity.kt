package com.sunpawtechnologies.scrollless.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.sunpawtechnologies.scrollless.domain.model.CoverageEndReason
import com.sunpawtechnologies.scrollless.domain.model.TrackingSource

/**
 * An interval during which [source] was actually tracking. Time outside these intervals is
 * "not tracked", which keeps missing data from looking like zero usage.
 */
@Entity(
    tableName = "tracking_coverage",
    indices = [Index(value = ["source", "start_utc_ms"])],
)
data class TrackingCoverageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val source: TrackingSource,
    @ColumnInfo(name = "start_utc_ms") val startUtcMs: Long,
    /** Null while the interval is still open. */
    @ColumnInfo(name = "end_utc_ms") val endUtcMs: Long? = null,
    @ColumnInfo(name = "end_reason") val endReason: CoverageEndReason? = null,
)
