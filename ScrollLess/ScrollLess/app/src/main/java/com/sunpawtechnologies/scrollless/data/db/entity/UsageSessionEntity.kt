package com.sunpawtechnologies.scrollless.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.sunpawtechnologies.scrollless.domain.model.Platform
import com.sunpawtechnologies.scrollless.domain.model.TrackingSource
import com.sunpawtechnologies.scrollless.domain.model.UsageMode

/**
 * A timed session that lies within a single local day (longer sessions are split at midnight
 * before insert). The unique key lets a re-imported or extended session replace its old row.
 */
@Entity(
    tableName = "usage_session",
    indices = [
        Index(value = ["source", "platform", "mode", "start_utc_ms"], unique = true),
        Index(value = ["platform", "mode", "local_epoch_day"]),
    ],
)
data class UsageSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val platform: Platform,
    val mode: UsageMode,
    val source: TrackingSource,
    @ColumnInfo(name = "start_utc_ms") val startUtcMs: Long,
    @ColumnInfo(name = "end_utc_ms") val endUtcMs: Long,
    @ColumnInfo(name = "zone_id") val zoneId: String,
    @ColumnInfo(name = "local_epoch_day") val localEpochDay: Long,
)
