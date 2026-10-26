package com.sunpawtechnologies.scrollless.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import com.sunpawtechnologies.scrollless.domain.model.LimitMetric

/** Marks a warning threshold as already shown for a day, so it fires once even after process death. */
@Entity(
    tableName = "warning_fired",
    primaryKeys = ["local_epoch_day", "metric", "threshold_percent"],
)
data class WarningFiredEntity(
    @ColumnInfo(name = "local_epoch_day") val localEpochDay: Long,
    val metric: LimitMetric,
    @ColumnInfo(name = "threshold_percent") val thresholdPercent: Int,
    @ColumnInfo(name = "fired_at_utc_ms") val firedAtUtcMs: Long,
)
