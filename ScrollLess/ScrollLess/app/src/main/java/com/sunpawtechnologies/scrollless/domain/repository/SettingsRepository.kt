package com.sunpawtechnologies.scrollless.domain.repository

import com.sunpawtechnologies.scrollless.domain.model.AccessibilityConsent
import com.sunpawtechnologies.scrollless.domain.model.AppSettings
import com.sunpawtechnologies.scrollless.domain.model.LimitMetric
import com.sunpawtechnologies.scrollless.domain.model.OverlayPosition
import com.sunpawtechnologies.scrollless.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val settings: Flow<AppSettings>

    suspend fun setThemeMode(mode: ThemeMode)

    suspend fun setOnboardingCompleted(completed: Boolean)

    suspend fun recordAccessibilityConsent(consent: AccessibilityConsent)

    suspend fun clearAccessibilityConsent()

    suspend fun setOverlayEnabled(enabled: Boolean)

    /** Null resets the counter to its default position. */
    suspend fun setOverlayPosition(position: OverlayPosition?)

    /** Must be within [com.sunpawtechnologies.scrollless.domain.model.WARNING_THRESHOLD_RANGE]. */
    suspend fun setWarningThresholdPercent(percent: Int)

    /** Adds or removes a package the counter must never appear over. */
    suspend fun setPackageExcluded(packageName: String, excluded: Boolean)

    suspend fun setLimitEnabled(metric: LimitMetric, enabled: Boolean)

    suspend fun setReelsTrackingTurnedOffByUser(turnedOff: Boolean)

    /** Records that the user dismissed [key] (a [com.sunpawtechnologies.scrollless.domain.model.limitAlertKey]); drops other days' keys. */
    suspend fun acknowledgeLimitAlert(key: String, epochDay: Long)
}
