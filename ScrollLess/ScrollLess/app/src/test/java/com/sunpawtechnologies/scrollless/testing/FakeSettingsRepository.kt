package com.sunpawtechnologies.scrollless.testing

import com.sunpawtechnologies.scrollless.domain.model.AccessibilityConsent
import com.sunpawtechnologies.scrollless.domain.model.AppSettings
import com.sunpawtechnologies.scrollless.domain.model.LimitMetric
import com.sunpawtechnologies.scrollless.domain.model.OverlayPosition
import com.sunpawtechnologies.scrollless.domain.model.ThemeMode
import com.sunpawtechnologies.scrollless.domain.model.WARNING_THRESHOLD_RANGE
import com.sunpawtechnologies.scrollless.domain.repository.SettingsRepository
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.distinctUntilChanged

/** Emits nothing until [emit] is called, mimicking DataStore's asynchronous first read. */
class FakeSettingsRepository : SettingsRepository {
    // DROP_OLDEST: like DataStore, the latest value always wins. With the default SUSPEND policy,
    // tryEmit drops a write while a subscriber is still busy, so back-to-back edits could be lost.
    private val state = MutableSharedFlow<AppSettings>(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    override val settings: Flow<AppSettings> = state.distinctUntilChanged()

    fun emit(settings: AppSettings) {
        state.tryEmit(settings)
    }

    private fun edit(transform: (AppSettings) -> AppSettings) {
        emit(transform(state.replayCache.firstOrNull() ?: AppSettings()))
    }

    override suspend fun setThemeMode(mode: ThemeMode) = edit { it.copy(themeMode = mode) }

    override suspend fun setOnboardingCompleted(completed: Boolean) = edit { it.copy(onboardingCompleted = completed) }

    override suspend fun recordAccessibilityConsent(consent: AccessibilityConsent) =
        edit { it.copy(accessibilityConsent = consent) }

    override suspend fun clearAccessibilityConsent() = edit { it.copy(accessibilityConsent = null) }

    override suspend fun setOverlayEnabled(enabled: Boolean) = edit { it.copy(overlayEnabled = enabled) }

    override suspend fun setOverlayPosition(position: OverlayPosition?) = edit { it.copy(overlayPosition = position) }

    override suspend fun setWarningThresholdPercent(percent: Int) {
        require(percent in WARNING_THRESHOLD_RANGE)
        edit { it.copy(warningThresholdPercent = percent) }
    }

    override suspend fun setPackageExcluded(packageName: String, excluded: Boolean) = edit {
        it.copy(customExcludedPackages = if (excluded) it.customExcludedPackages + packageName else it.customExcludedPackages - packageName)
    }

    override suspend fun setLimitEnabled(metric: LimitMetric, enabled: Boolean) = edit {
        it.copy(disabledLimits = if (enabled) it.disabledLimits - metric else it.disabledLimits + metric)
    }

    override suspend fun setReelsTrackingTurnedOffByUser(turnedOff: Boolean) = edit { it.copy(reelsTrackingTurnedOffByUser = turnedOff) }

    override suspend fun acknowledgeLimitAlert(key: String, epochDay: Long) = edit {
        it.copy(acknowledgedLimitAlerts = it.acknowledgedLimitAlerts.filterTo(mutableSetOf()) { old -> old.startsWith("$epochDay:") } + key)
    }
}
