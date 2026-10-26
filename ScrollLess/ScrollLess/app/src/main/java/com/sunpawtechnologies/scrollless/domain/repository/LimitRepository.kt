package com.sunpawtechnologies.scrollless.domain.repository

import com.sunpawtechnologies.scrollless.domain.model.LimitMetric
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/** A limit value that applies from [effectiveFrom] until the next change. */
data class LimitChange(val effectiveFrom: LocalDate, val value: Int)

/**
 * Daily limits with history, so past days are judged against the limit that applied then
 * rather than today's value.
 */
interface LimitRepository {
    /** The limit for every metric on [date]. Metrics never configured use their default. */
    fun observeLimitsOn(date: LocalDate): Flow<Map<LimitMetric, Int>>

    /** Sets [metric] to [value] from [effectiveFrom] onward. Setting the same day twice keeps the latest value. */
    suspend fun setLimit(metric: LimitMetric, value: Int, effectiveFrom: LocalDate)

    /** All changes for [metric], oldest first. */
    suspend fun history(metric: LimitMetric): List<LimitChange>
}

/** The limit in effect on [date], given changes sorted oldest first. */
fun List<LimitChange>.limitOn(date: LocalDate, default: Int): Int =
    lastOrNull { !it.effectiveFrom.isAfter(date) }?.value ?: default
