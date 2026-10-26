package com.sunpawtechnologies.scrollless.data.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.sunpawtechnologies.scrollless.domain.model.AccessibilityConsent
import com.sunpawtechnologies.scrollless.domain.model.AppSettings
import com.sunpawtechnologies.scrollless.domain.model.DEFAULT_WARNING_THRESHOLD_PERCENT
import com.sunpawtechnologies.scrollless.domain.model.LimitMetric
import com.sunpawtechnologies.scrollless.domain.model.OverlayPosition
import com.sunpawtechnologies.scrollless.domain.model.ThemeMode
import com.sunpawtechnologies.scrollless.domain.model.WARNING_THRESHOLD_RANGE
import com.sunpawtechnologies.scrollless.domain.model.isValidPackageName
import com.sunpawtechnologies.scrollless.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import java.time.Instant

class DataStoreSettingsRepository(
    private val dataStore: DataStore<Preferences>,
) : SettingsRepository {

    override val settings: Flow<AppSettings> = dataStore.data
        .catch { error -> if (error is IOException) emit(emptyPreferences()) else throw error }
        .map { it.toAppSettings() }

    override suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    override suspend fun setOnboardingCompleted(completed: Boolean) {
        dataStore.edit { it[Keys.ONBOARDING_COMPLETED] = completed }
    }

    override suspend fun recordAccessibilityConsent(consent: AccessibilityConsent) {
        dataStore.edit {
            it[Keys.CONSENT_VERSION] = consent.disclosureVersion
            it[Keys.CONSENT_AT_MS] = consent.acceptedAt.toEpochMilli()
        }
    }

    override suspend fun clearAccessibilityConsent() {
        dataStore.edit {
            it.remove(Keys.CONSENT_VERSION)
            it.remove(Keys.CONSENT_AT_MS)
        }
    }

    override suspend fun setOverlayEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.OVERLAY_ENABLED] = enabled }
    }

    override suspend fun setOverlayPosition(position: OverlayPosition?) {
        dataStore.edit {
            if (position == null) {
                it.remove(Keys.OVERLAY_X)
                it.remove(Keys.OVERLAY_Y)
            } else {
                it[Keys.OVERLAY_X] = position.xFraction
                it[Keys.OVERLAY_Y] = position.yFraction
            }
        }
    }

    override suspend fun setWarningThresholdPercent(percent: Int) {
        require(percent in WARNING_THRESHOLD_RANGE) { "Warning threshold $percent% outside $WARNING_THRESHOLD_RANGE" }
        dataStore.edit { it[Keys.WARNING_THRESHOLD] = percent }
    }

    override suspend fun setPackageExcluded(packageName: String, excluded: Boolean) {
        val name = packageName.trim()
        require(isValidPackageName(name)) { "Not a package name: $name" }
        dataStore.edit {
            val current = it[Keys.EXCLUDED_PACKAGES].orEmpty()
            it[Keys.EXCLUDED_PACKAGES] = if (excluded) current + name else current - name
        }
    }

    override suspend fun setLimitEnabled(metric: LimitMetric, enabled: Boolean) {
        dataStore.edit {
            val current = it[Keys.DISABLED_LIMITS].orEmpty()
            it[Keys.DISABLED_LIMITS] = if (enabled) current - metric.name else current + metric.name
        }
    }

    override suspend fun setReelsTrackingTurnedOffByUser(turnedOff: Boolean) {
        dataStore.edit { it[Keys.REELS_TURNED_OFF_BY_USER] = turnedOff }
    }

    override suspend fun acknowledgeLimitAlert(key: String, epochDay: Long) {
        val todayPrefix = "$epochDay:"
        dataStore.edit {
            val kept = it[Keys.ACKNOWLEDGED_ALERTS].orEmpty().filterTo(mutableSetOf()) { old -> old.startsWith(todayPrefix) }
            it[Keys.ACKNOWLEDGED_ALERTS] = kept + key
        }
    }

    private fun Preferences.toAppSettings(): AppSettings {
        val consentVersion = this[Keys.CONSENT_VERSION]
        val consentAtMs = this[Keys.CONSENT_AT_MS]
        val overlayX = this[Keys.OVERLAY_X]
        val overlayY = this[Keys.OVERLAY_Y]
        return AppSettings(
            themeMode = ThemeMode.entries.firstOrNull { it.name == this[Keys.THEME_MODE] } ?: ThemeMode.SYSTEM,
            onboardingCompleted = this[Keys.ONBOARDING_COMPLETED] ?: false,
            accessibilityConsent = if (consentVersion != null && consentAtMs != null) {
                AccessibilityConsent(consentVersion, Instant.ofEpochMilli(consentAtMs))
            } else {
                null
            },
            overlayEnabled = this[Keys.OVERLAY_ENABLED] ?: false,
            overlayPosition = if (overlayX != null && overlayY != null) {
                OverlayPosition(overlayX.coerceIn(0f, 1f), overlayY.coerceIn(0f, 1f))
            } else {
                null
            },
            warningThresholdPercent = (this[Keys.WARNING_THRESHOLD] ?: DEFAULT_WARNING_THRESHOLD_PERCENT)
                .coerceIn(WARNING_THRESHOLD_RANGE),
            customExcludedPackages = this[Keys.EXCLUDED_PACKAGES].orEmpty(),
            // Unknown names (e.g. from a newer version) are ignored rather than crashing.
            disabledLimits = this[Keys.DISABLED_LIMITS].orEmpty()
                .mapNotNullTo(mutableSetOf()) { name -> LimitMetric.entries.firstOrNull { it.name == name } },
            reelsTrackingTurnedOffByUser = this[Keys.REELS_TURNED_OFF_BY_USER] ?: false,
            acknowledgedLimitAlerts = this[Keys.ACKNOWLEDGED_ALERTS].orEmpty(),
        )
    }

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val CONSENT_VERSION = intPreferencesKey("accessibility_consent_version")
        val CONSENT_AT_MS = longPreferencesKey("accessibility_consent_at_ms")
        val OVERLAY_ENABLED = booleanPreferencesKey("overlay_enabled")
        val OVERLAY_X = floatPreferencesKey("overlay_x_fraction")
        val OVERLAY_Y = floatPreferencesKey("overlay_y_fraction")
        val WARNING_THRESHOLD = intPreferencesKey("warning_threshold_percent")
        val EXCLUDED_PACKAGES = stringSetPreferencesKey("excluded_packages")
        val DISABLED_LIMITS = stringSetPreferencesKey("disabled_limits")
        val REELS_TURNED_OFF_BY_USER = booleanPreferencesKey("reels_tracking_turned_off_by_user")
        val ACKNOWLEDGED_ALERTS = stringSetPreferencesKey("acknowledged_limit_alerts")
    }
}
