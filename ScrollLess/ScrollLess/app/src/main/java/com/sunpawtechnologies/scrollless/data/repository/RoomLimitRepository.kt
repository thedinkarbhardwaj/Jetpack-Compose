package com.sunpawtechnologies.scrollless.data.repository

import com.sunpawtechnologies.scrollless.data.db.dao.LimitDao
import com.sunpawtechnologies.scrollless.data.db.entity.LimitHistoryEntity
import com.sunpawtechnologies.scrollless.domain.model.LimitMetric
import com.sunpawtechnologies.scrollless.domain.repository.LimitChange
import com.sunpawtechnologies.scrollless.domain.repository.LimitRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.LocalDate

class RoomLimitRepository(private val limitDao: LimitDao) : LimitRepository {

    override fun observeLimitsOn(date: LocalDate): Flow<Map<LimitMetric, Int>> {
        val perMetric = LimitMetric.entries.map { metric ->
            limitDao.observeLimitOn(metric, date.toEpochDay()).map { metric to (it ?: metric.defaultDailyLimit) }
        }
        return combine(perMetric) { it.toMap() }
    }

    override suspend fun setLimit(metric: LimitMetric, value: Int, effectiveFrom: LocalDate) {
        require(value in metric.validRange) { "Limit $value outside ${metric.validRange} for $metric" }
        limitDao.upsert(
            LimitHistoryEntity(
                metric = metric,
                limitValue = value,
                effectiveFromEpochDay = effectiveFrom.toEpochDay(),
            ),
        )
    }

    override suspend fun history(metric: LimitMetric): List<LimitChange> =
        limitDao.history(metric).map { LimitChange(LocalDate.ofEpochDay(it.effectiveFromEpochDay), it.limitValue) }
}
