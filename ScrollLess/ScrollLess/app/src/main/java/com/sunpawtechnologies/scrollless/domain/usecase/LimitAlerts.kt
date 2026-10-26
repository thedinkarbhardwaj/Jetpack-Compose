package com.sunpawtechnologies.scrollless.domain.usecase

import com.sunpawtechnologies.scrollless.core.time.TimeProvider
import com.sunpawtechnologies.scrollless.domain.model.ContentKind
import com.sunpawtechnologies.scrollless.domain.model.LimitMetric
import com.sunpawtechnologies.scrollless.domain.model.LimitStatus
import com.sunpawtechnologies.scrollless.domain.model.LimitUnit
import com.sunpawtechnologies.scrollless.domain.model.PermissionSnapshot
import com.sunpawtechnologies.scrollless.domain.model.Platform
import com.sunpawtechnologies.scrollless.domain.model.TrackingAvailability
import com.sunpawtechnologies.scrollless.domain.model.UsageMode
import com.sunpawtechnologies.scrollless.domain.model.hasCurrentAccessibilityConsent
import com.sunpawtechnologies.scrollless.domain.model.isLimitEnabled
import com.sunpawtechnologies.scrollless.domain.model.limitStatus
import com.sunpawtechnologies.scrollless.domain.repository.LimitRepository
import com.sunpawtechnologies.scrollless.domain.repository.PermissionMonitor
import com.sunpawtechnologies.scrollless.domain.repository.SettingsRepository
import com.sunpawtechnologies.scrollless.domain.repository.UsageRepository
import com.sunpawtechnologies.scrollless.tracking.TrackingLog
import com.sunpawtechnologies.scrollless.tracking.usagestats.UsageImporter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant
import java.time.LocalDate

enum class AlertLevel { WARNING, REACHED }

/**
 * One limit crossing to tell the user about.
 *
 * @param value today's value in base units: milliseconds for [LimitUnit.MINUTES], items for [LimitUnit.COUNT].
 * @param limit the daily limit in the metric's own unit (minutes or items).
 */
data class LimitAlert(
    val metric: LimitMetric,
    val level: AlertLevel,
    val value: Long,
    val limit: Int,
    val epochDay: Long,
)

/** Pure rules, shared by the scheduled check and the dashboard. */
object LimitAlertRules {
    fun limitInBaseUnits(metric: LimitMetric, limit: Int): Long =
        if (metric.unit == LimitUnit.MINUTES) limit * 60_000L else limit.toLong()

    fun status(metric: LimitMetric, value: Long, limit: Int, warningThresholdPercent: Int): LimitStatus =
        limitStatus(value, limitInBaseUnits(metric, limit), warningThresholdPercent)

    /** The highest level crossed, or null. Reaching the limit skips the warning. */
    fun levelFor(metric: LimitMetric, value: Long, limit: Int, warningThresholdPercent: Int): AlertLevel? =
        when (status(metric, value, limit, warningThresholdPercent)) {
            LimitStatus.REACHED -> AlertLevel.REACHED
            LimitStatus.APPROACHING -> AlertLevel.WARNING
            LimitStatus.WITHIN -> null
        }

    /** Dedup key per day: the warning percentage, or 100 for "reached". */
    fun thresholdKey(level: AlertLevel, warningThresholdPercent: Int): Int =
        if (level == AlertLevel.REACHED) 100 else warningThresholdPercent

    /**
     * Today's values that are actually measured. A metric whose source isn't available is left
     * out entirely, so missing data never looks like "0, within limit".
     */
    fun measurableMetrics(permissions: PermissionSnapshot, reelsConsent: Boolean): Set<LimitMetric> = buildSet {
        if (TrackingReadiness.instagramTime(permissions) == TrackingAvailability.Active) add(LimitMetric.INSTAGRAM_APP_TIME)
        if (TrackingReadiness.youtubeTime(permissions) == TrackingAvailability.Active) add(LimitMetric.YOUTUBE_APP_TIME)
        if (TrackingReadiness.instagramReels(permissions, reelsConsent) == TrackingAvailability.Active) add(LimitMetric.INSTAGRAM_REELS)
    }
}

/** Remembers which alerts were sent, so each fires at most once per metric, level and day. */
fun interface AlertLog {
    /** Returns true if this alert hadn't fired yet and is now recorded; false if it already fired. */
    suspend fun markFired(epochDay: Long, metric: LimitMetric, thresholdPercent: Int, at: Instant): Boolean
}

interface LimitNotifier {
    /** Notifications are allowed for ScrollLess (runtime permission and app/channel switches). */
    fun canNotify(): Boolean

    fun notify(alert: LimitAlert)
}

/**
 * Compares today's measured usage with the user's limits and sends each new crossing once.
 *
 * Runs from a periodic job (every ~15 minutes) and, in Advanced mode, after new Reels are
 * counted. It reads only what's already recorded plus a short Usage Access import; it never
 * polls continuously.
 */
class LimitAlertChecker(
    private val permissionMonitor: PermissionMonitor,
    private val settingsRepository: SettingsRepository,
    private val usageRepository: UsageRepository,
    private val limitRepository: LimitRepository,
    private val usageImporter: UsageImporter,
    private val alertLog: AlertLog,
    private val notifier: LimitNotifier,
    private val timeProvider: TimeProvider,
) {
    /** The job and the service may both trigger a check; they run one at a time. */
    private val mutex = Mutex()

    /** @param importUsage read fresh Usage Access data first (skip when only Reels changed). */
    suspend fun check(importUsage: Boolean = true): List<LimitAlert> = mutex.withLock {
        if (!notifier.canNotify()) {
            // Not recorded as fired: if notifications are allowed later today, the alert still comes.
            TrackingLog.d("limit_check_skipped", "reason" to "notifications_off")
            return emptyList()
        }
        if (importUsage) {
            permissionMonitor.refresh()
            usageImporter.importRecentUsage()
        }
        val permissions = permissionMonitor.state.value
        val settings = settingsRepository.settings.first()
        val today = timeProvider.today()
        val limits = limitRepository.observeLimitsOn(today).first()
        val measured = LimitAlertRules.measurableMetrics(permissions, settings.hasCurrentAccessibilityConsent())

        val sent = mutableListOf<LimitAlert>()
        for (metric in measured) {
            if (!settings.isLimitEnabled(metric)) continue
            val value = todayValue(metric, today)
            val limit = limits.getValue(metric)
            val level = LimitAlertRules.levelFor(metric, value, limit, settings.warningThresholdPercent) ?: continue
            val key = LimitAlertRules.thresholdKey(level, settings.warningThresholdPercent)
            if (!alertLog.markFired(today.toEpochDay(), metric, key, timeProvider.now())) continue
            val alert = LimitAlert(metric, level, value, limit, today.toEpochDay())
            notifier.notify(alert)
            sent += alert
            TrackingLog.i("limit_alert_sent", "metric" to metric, "level" to level, "value" to value, "limit" to limit)
        }
        sent
    }

    private suspend fun todayValue(metric: LimitMetric, today: LocalDate): Long = when (metric) {
        LimitMetric.INSTAGRAM_APP_TIME ->
            usageRepository.observeUsageDuration(Platform.INSTAGRAM, UsageMode.APP_FOREGROUND, today).first().toMillis()
        LimitMetric.YOUTUBE_APP_TIME ->
            usageRepository.observeUsageDuration(Platform.YOUTUBE, UsageMode.APP_FOREGROUND, today).first().toMillis()
        LimitMetric.INSTAGRAM_REELS ->
            usageRepository.observeContentViewCount(ContentKind.INSTAGRAM_REEL, today).first().toLong()
        LimitMetric.YOUTUBE_SHORTS, LimitMetric.YOUTUBE_REGULAR_VIDEO -> 0L // Not measurable; never reached.
    }
}
