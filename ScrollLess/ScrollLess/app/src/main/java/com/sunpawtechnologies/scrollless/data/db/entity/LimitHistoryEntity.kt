package com.sunpawtechnologies.scrollless.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.sunpawtechnologies.scrollless.domain.model.LimitMetric

/** A limit value that applies from [effectiveFromEpochDay] until the next row for the same metric. */
@Entity(
    tableName = "limit_history",
    indices = [Index(value = ["metric", "effective_from_epoch_day"], unique = true)],
)
data class LimitHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val metric: LimitMetric,
    @ColumnInfo(name = "limit_value") val limitValue: Int,
    @ColumnInfo(name = "effective_from_epoch_day") val effectiveFromEpochDay: Long,
)
