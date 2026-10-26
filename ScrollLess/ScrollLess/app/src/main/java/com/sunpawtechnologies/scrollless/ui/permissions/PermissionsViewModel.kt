package com.sunpawtechnologies.scrollless.ui.permissions

import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.sunpawtechnologies.scrollless.core.time.TimeProvider
import com.sunpawtechnologies.scrollless.di.appContainer
import com.sunpawtechnologies.scrollless.domain.model.ACCESSIBILITY_DISCLOSURE_VERSION
import com.sunpawtechnologies.scrollless.domain.model.AccessibilityConsent
import com.sunpawtechnologies.scrollless.domain.model.PermissionSnapshot
import com.sunpawtechnologies.scrollless.domain.model.hasCurrentAccessibilityConsent
import com.sunpawtechnologies.scrollless.domain.repository.PermissionMonitor
import com.sunpawtechnologies.scrollless.domain.repository.SettingsRepository
import com.sunpawtechnologies.scrollless.domain.repository.TrackingCoverageRepository
import com.sunpawtechnologies.scrollless.domain.model.TrackingSource
import com.sunpawtechnologies.scrollless.permissions.batteryGuideUrl
import java.time.Duration
import com.sunpawtechnologies.scrollless.permissions.SettingsTarget
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AccessibilityStatus { NOT_ENABLED, NEEDS_CONSENT, NOT_RUNNING, ACTIVE }

data class PermissionsUiState(
    val isLoading: Boolean = true,
    val usageAccessGranted: Boolean = false,
    val accessibilityStatus: AccessibilityStatus = AccessibilityStatus.NOT_ENABLED,
    val consentGiven: Boolean = false,
    val instagramInstalled: Boolean = true,
    val showDisclosure: Boolean = false,
    /** Battery optimization is off for ScrollLess (Settings › Battery › Unrestricted). */
    val batteryUnrestricted: Boolean = true,
    /** Times Android cut Reels tracking off in the last 7 days (killed process or unbound service). */
    val recentInterruptions: Int = 0,
    /** dontkillmyapp.com page for this phone's brand, if it's known to stop background apps. */
    val batteryGuideUrl: String? = null,
) {
    val completedSteps: Int
        get() = listOf(usageAccessGranted, accessibilityStatus == AccessibilityStatus.ACTIVE).count { it }
    val totalSteps: Int get() = 2
}

sealed interface PermissionsEffect {
    data class OpenSettings(val target: SettingsTarget) : PermissionsEffect
    data class OpenWebPage(val url: String) : PermissionsEffect
}

class PermissionsViewModel(
    permissionMonitor: PermissionMonitor,
    private val settingsRepository: SettingsRepository,
    private val timeProvider: TimeProvider,
    private val coverageRepository: TrackingCoverageRepository? = null,
    manufacturer: String = "",
    private val turnOffReelsTracking: (suspend () -> Boolean)? = null,
) : ViewModel() {

    private val guideUrl = batteryGuideUrl(manufacturer)

    /** Re-counted whenever permission state changes (e.g. on every resume). */
    private val interruptions = permissionMonitor.state.map {
        coverageRepository?.interruptionsSince(TrackingSource.ACCESSIBILITY, timeProvider.now().minus(Duration.ofDays(7))) ?: 0
    }

    private val showDisclosure = MutableStateFlow(false)
    private val consentGiven = settingsRepository.settings
        .map { it.hasCurrentAccessibilityConsent() }
        .distinctUntilChanged()

    val uiState: StateFlow<PermissionsUiState> =
        combine(permissionMonitor.state, consentGiven, showDisclosure, interruptions) { permissions, consent, disclosure, interrupted ->
            PermissionsUiState(
                isLoading = false,
                usageAccessGranted = permissions.usageAccessGranted,
                accessibilityStatus = accessibilityStatus(permissions, consent),
                consentGiven = consent,
                instagramInstalled = permissions.instagramInstalled,
                showDisclosure = disclosure,
                batteryUnrestricted = permissions.ignoringBatteryOptimizations,
                recentInterruptions = interrupted,
                batteryGuideUrl = guideUrl,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PermissionsUiState())

    private val _effects = Channel<PermissionsEffect>(Channel.BUFFERED)
    val effects: Flow<PermissionsEffect> = _effects.receiveAsFlow()

    fun onEnableUsageAccess() = open(SettingsTarget.USAGE_ACCESS)

    /** The disclosure always comes before the settings screen, as Play policy requires. */
    fun onAccessibilityAction() {
        val state = uiState.value
        when {
            !state.consentGiven -> showDisclosure.value = true
            else -> open(SettingsTarget.ACCESSIBILITY)
        }
    }

    fun onDisclosureAccepted() {
        val alreadyEnabled = uiState.value.accessibilityStatus != AccessibilityStatus.NOT_ENABLED
        viewModelScope.launch {
            settingsRepository.recordAccessibilityConsent(
                AccessibilityConsent(ACCESSIBILITY_DISCLOSURE_VERSION, timeProvider.now()),
            )
            settingsRepository.setReelsTrackingTurnedOffByUser(false)
            showDisclosure.value = false
            if (!alreadyEnabled) _effects.send(PermissionsEffect.OpenSettings(SettingsTarget.ACCESSIBILITY))
        }
    }

    fun onDisclosureDismissed() {
        showDisclosure.value = false
    }

    /** Switches ScrollLess's own service off; if it isn't running here, the user does it in settings. */
    fun onTurnOffReelsTracking() {
        val turnOff = turnOffReelsTracking ?: return
        viewModelScope.launch {
            if (!turnOff()) _effects.send(PermissionsEffect.OpenSettings(SettingsTarget.ACCESSIBILITY))
        }
    }

    fun onOpenAppInfo() = open(SettingsTarget.APP_DETAILS)

    fun onOpenBatterySettings() = open(SettingsTarget.BATTERY)

    fun onOpenBatteryGuide() {
        val url = guideUrl ?: return
        viewModelScope.launch { _effects.send(PermissionsEffect.OpenWebPage(url)) }
    }

    private fun open(target: SettingsTarget) {
        viewModelScope.launch { _effects.send(PermissionsEffect.OpenSettings(target)) }
    }

    companion object {
        fun accessibilityStatus(permissions: PermissionSnapshot, consentGiven: Boolean): AccessibilityStatus = when {
            !permissions.accessibilityServiceEnabled -> AccessibilityStatus.NOT_ENABLED
            !consentGiven -> AccessibilityStatus.NEEDS_CONSENT
            !permissions.accessibilityServiceRunning -> AccessibilityStatus.NOT_RUNNING
            else -> AccessibilityStatus.ACTIVE
        }

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val container = appContainer()
                PermissionsViewModel(
                    container.permissionMonitor,
                    container.settingsRepository,
                    container.timeProvider,
                    container.trackingCoverageRepository,
                    Build.MANUFACTURER.orEmpty(),
                    container.turnOffReelsTracking::invoke,
                )
            }
        }
    }
}
