package com.sunpawtechnologies.scrollless.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.sunpawtechnologies.scrollless.di.appContainer
import com.sunpawtechnologies.scrollless.domain.model.DEFAULT_SENSITIVE_APPS
import com.sunpawtechnologies.scrollless.domain.model.ThemeMode
import com.sunpawtechnologies.scrollless.domain.model.hasCurrentAccessibilityConsent
import com.sunpawtechnologies.scrollless.domain.model.isValidPackageName
import com.sunpawtechnologies.scrollless.domain.repository.PermissionMonitor
import com.sunpawtechnologies.scrollless.domain.repository.SettingsRepository
import com.sunpawtechnologies.scrollless.permissions.SettingsTarget
import com.sunpawtechnologies.scrollless.ui.permissions.AccessibilityStatus
import com.sunpawtechnologies.scrollless.ui.permissions.PermissionsViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val isLoading: Boolean = true,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val overlayEnabled: Boolean = true,
    val overlayMoved: Boolean = false,
    /** The counter needs Reels tracking; without it the toggle is explained instead of silently doing nothing. */
    val reelsTrackingActive: Boolean = false,
    /** ScrollLess's Accessibility service is switched on in system settings (running or not). */
    val reelsTrackingSwitchedOn: Boolean = false,
    val setupStepsDone: Int = 0,
    val usageAccessGranted: Boolean = false,
    val setupStepsTotal: Int = 2,
    /** Developer options or USB debugging are on: some payment apps refuse to open, with or without ScrollLess. */
    val developerModeEnabled: Boolean = false,
    /** Built-in payment/banking apps actually installed on this phone (display names). */
    val detectedPaymentApps: List<String> = emptyList(),
    val customExcludedPackages: List<String> = emptyList(),
    val exclusionDraft: String = "",
    val exclusionDraftInvalid: Boolean = false,
)

sealed interface SettingsEffect {
    data class OpenSettings(val target: SettingsTarget) : SettingsEffect

    /** Reels tracking was switched off from inside ScrollLess. */
    data object ReelsTrackingTurnedOff : SettingsEffect
}

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    permissionMonitor: PermissionMonitor,
    private val isInstalled: (String) -> Boolean = { false },
    private val turnOffReelsTracking: (suspend () -> Boolean)? = null,
) : ViewModel() {

    private val exclusionDraft = MutableStateFlow("")

    private val _effects = Channel<SettingsEffect>(Channel.BUFFERED)
    val effects: Flow<SettingsEffect> = _effects.receiveAsFlow()

    val uiState: StateFlow<SettingsUiState> =
        combine(settingsRepository.settings, permissionMonitor.state, exclusionDraft) { settings, permissions, draft ->
            // Same rule as the setup screen, so both always show the same "n of 2" count.
            // Whether Instagram is installed is a separate question shown on the dashboard.
            val reelsActive = PermissionsViewModel.accessibilityStatus(permissions, settings.hasCurrentAccessibilityConsent()) ==
                AccessibilityStatus.ACTIVE
            SettingsUiState(
                isLoading = false,
                themeMode = settings.themeMode,
                overlayEnabled = settings.overlayEnabled,
                overlayMoved = settings.overlayPosition != null,
                reelsTrackingActive = reelsActive,
                reelsTrackingSwitchedOn = permissions.accessibilityServiceEnabled,
                setupStepsDone = listOf(permissions.usageAccessGranted, reelsActive).count { it },
                usageAccessGranted = permissions.usageAccessGranted,
                developerModeEnabled = permissions.developerModeEnabled,
                detectedPaymentApps = DEFAULT_SENSITIVE_APPS.filterKeys(isInstalled).values.toList(),
                customExcludedPackages = settings.customExcludedPackages.sorted(),
                exclusionDraft = draft,
                exclusionDraftInvalid = draft.isNotBlank() && !isValidPackageName(draft),
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    fun onThemeModeSelected(mode: ThemeMode) {
        viewModelScope.launch { settingsRepository.setThemeMode(mode) }
    }

    fun onOverlayEnabledChanged(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setOverlayEnabled(enabled) }
    }

    fun onResetOverlayPosition() {
        viewModelScope.launch { settingsRepository.setOverlayPosition(null) }
    }

    /** Only ScrollLess's own service is switched off. If it isn't running here, the user does it in settings. */
    fun onTurnOffReelsTracking() {
        val turnOff = turnOffReelsTracking ?: return
        viewModelScope.launch {
            _effects.send(if (turnOff()) SettingsEffect.ReelsTrackingTurnedOff else SettingsEffect.OpenSettings(SettingsTarget.ACCESSIBILITY))
        }
    }

    fun onOpenAccessibilitySettings() = open(SettingsTarget.ACCESSIBILITY)

    /** Opens Developer options for the user to review. ScrollLess never changes them. */
    fun onOpenDeveloperOptions() = open(SettingsTarget.DEVELOPER_OPTIONS)

    fun onExclusionDraftChanged(text: String) {
        exclusionDraft.value = text.filterNot(Char::isWhitespace).take(MAX_PACKAGE_LENGTH)
    }

    fun onAddExclusion() {
        val name = exclusionDraft.value
        if (!isValidPackageName(name)) return
        viewModelScope.launch {
            settingsRepository.setPackageExcluded(name, excluded = true)
            exclusionDraft.value = ""
        }
    }

    fun onRemoveExclusion(packageName: String) {
        viewModelScope.launch { settingsRepository.setPackageExcluded(packageName, excluded = false) }
    }

    private fun open(target: SettingsTarget) {
        viewModelScope.launch { _effects.send(SettingsEffect.OpenSettings(target)) }
    }

    companion object {
        private const val MAX_PACKAGE_LENGTH = 150

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val container = appContainer()
                SettingsViewModel(
                    container.settingsRepository,
                    container.permissionMonitor,
                    container.permissionMonitor::isInstalled,
                    container.turnOffReelsTracking::invoke,
                )
            }
        }
    }
}
