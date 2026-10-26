package com.sunpawtechnologies.scrollless.data.repository

import com.sunpawtechnologies.scrollless.data.db.dao.WarningDao
import com.sunpawtechnologies.scrollless.data.db.entity.WarningFiredEntity
import com.sunpawtechnologies.scrollless.domain.model.LimitMetric
import com.sunpawtechnologies.scrollless.domain.usecase.AlertLog
import java.time.Instant

/** The primary key (day, metric, threshold) makes "already fired" atomic, even across processes and restarts. */
class RoomAlertLog(private val warningDao: WarningDao) : AlertLog {
    override suspend fun markFired(epochDay: Long, metric: LimitMetric, thresholdPercent: Int, at: Instant): Boolean =
        warningDao.insert(WarningFiredEntity(epochDay, metric, thresholdPercent, at.toEpochMilli())) != -1L
}
