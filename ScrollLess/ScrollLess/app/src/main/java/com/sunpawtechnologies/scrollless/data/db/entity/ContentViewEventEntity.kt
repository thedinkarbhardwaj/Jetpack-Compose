package com.sunpawtechnologies.scrollless.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.sunpawtechnologies.scrollless.domain.model.ContentKind

/**
 * One estimated Reel/Short view. The local day and hour are fixed at write time in the zone
 * the user was in, so history doesn't move when they later change time zones.
 */
@Entity(
    tableName = "content_view_event",
    indices = [
        Index(value = ["dedup_key"], unique = true),
        Index(value = ["kind", "local_epoch_day"]),
    ],
)
data class ContentViewEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val kind: ContentKind,
    @ColumnInfo(name = "occurred_at_utc_ms") val occurredAtUtcMs: Long,
    @ColumnInfo(name = "zone_id") val zoneId: String,
    @ColumnInfo(name = "local_epoch_day") val localEpochDay: Long,
    @ColumnInfo(name = "local_hour") val localHour: Int,
    @ColumnInfo(name = "dedup_key") val dedupKey: String,
    @ColumnInfo(name = "detector_version") val detectorVersion: Int,
)
