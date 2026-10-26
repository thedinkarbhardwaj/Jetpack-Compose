package com.sunpawtechnologies.scrollless.ui.limits

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.sunpawtechnologies.scrollless.core.time.TimeProvider
import com.sunpawtechnologies.scrollless.di.appContainer
import com.sunpawtechnologies.scrollless.domain.model.LimitMetric
import com.sunpawtechnologies.scrollless.domain.model.LimitUnit
import com.sunpawtechnologies.scrollless.domain.model.PermissionSnapshot
import com.sunpawtechnologies.scrollless.domain.model.WARNING_THRESHOLD_RANGE
import com.sunpawtechnologies.scrollless.domain.model.hasCurrentAccessibilityConsent
import com.sunpawtechnologies.scrollless.domain.model.isLimitEnabled
import com.sunpawtechnologies.scrollless.domain.repository.LimitRepository
import com.sunpawtechnologies.scrollless.domain.repository.PermissionMonitor
import com.sunpawtechnologies.scrollless.domain.repository.SettingsRepository
import com.sunpawtechnologies.scrollless.domain.usecase.LimitAlertRules
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class LimitError { NOT_A_NUMBER, OUT_OF_RANGE }

data class LimitField(
    val metric: LimitMetric,
    val saved: Int,
    /** What the user typed. Equals [saved] when there's nothing to save. */
    val draft: String,
    val error: LimitError?,
    val justSaved: Boolean,
    /** The user wants this limit. Off: still measured and shown, but no ring, status or reminder. */
    val enabled: Boolean = true,
    /** Something measures this metric right now (permission granted, app installed). */
    val measuredNow: Boolean = true,
) {
    val canSave: Boolean get() = error == null && draft.toIntOrNull() != saved
    val presets: List<Int> get() = if (metric.unit == LimitUnit.MINUTES) listOf(30, 45, 60, 90) else listOf(20, 50, 100)
}

data class LimitsUiState(
    val isLoading: Boolean = true,
    val fields: List<LimitField> = emptyList(),
    val warningThresholdPercent: Int = 0,
    /** ScrollLess may post notifications (runtime permission and app switch). */
    val notificationsAllowed: Boolean = false,
)

/** Pure validation, shared with tests. */
fun validateLimit(metric: LimitMetric, text: String): LimitError? {
    val value = text.trim().toIntOrNull() ?: return LimitError.NOT_A_NUMBER
    return if (value in metric.validRange) null else LimitError.OUT_OF_RANGE
}

class LimitsViewModel(
    private val limitRepository: LimitRepository,
    private val settingsRepository: SettingsRepository,
    private val timeProvider: TimeProvider,
    permissionMonitor: PermissionMonitor? = null,
    private val notificationsAllowed: () -> Boolean = { false },
    /** Re-checks whether the periodic limit check should be scheduled. */
    private val onLimitSetupChanged: () -> Unit = {},
) : ViewModel() {

    private val drafts = MutableStateFlow<Map<LimitMetric, String>>(emptyMap())
    private val justSaved = MutableStateFlow<LimitMetric?>(null)
    /** Slider position while dragging; persisted when the drag ends. */
    private val thresholdDraft = MutableStateFlow<Int?>(null)
    private val notifications = MutableStateFlow(notificationsAllowed())

    private val editing = combine(drafts, justSaved, thresholdDraft) { drafts, saved, threshold -> Triple(drafts, saved, threshold) }

    val uiState: StateFlow<LimitsUiState> = combine(
        limitRepository.observeLimitsOn(timeProvider.today()),
        settingsRepository.settings,
        permissionMonitor?.state ?: flowOf(PermissionSnapshot()),
        editing,
        notifications,
    ) { limits, settings, permissions, (drafts, saved, thresholdDraft), notificationsOn ->
        val measured = LimitAlertRules.measurableMetrics(permissions, settings.hasCurrentAccessibilityConsent())
        LimitsUiState(
            isLoading = false,
            fields = LimitMetric.configurable.map { metric ->
                val current = limits.getValue(metric)
                val draft = drafts[metric] ?: current.toString()
                LimitField(
                    metric = metric,
                    saved = current,
                    draft = draft,
                    error = validateLimit(metric, draft),
                    justSaved = saved == metric && drafts[metric] == null,
                    enabled = settings.isLimitEnabled(metric),
                    measuredNow = metric in measured,
                )
            },
            warningThresholdPercent = thresholdDraft ?: settings.warningThresholdPercent,
            notificationsAllowed = notificationsOn,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LimitsUiState())

    /** Notification permission can change in system settings or the permission dialog. */
    fun onResume() {
        notifications.value = notificationsAllowed()
        onLimitSetupChanged()
    }

    fun onEnabledChanged(metric: LimitMetric, enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setLimitEnabled(metric, enabled)
            onLimitSetupChanged()
        }
    }

    fun onDraftChanged(metric: LimitMetric, text: String) {
        val digits = text.filter(Char::isDigit).take(4)
        drafts.update { it + (metric to digits) }
        justSaved.value = null
    }

    fun onPresetSelected(metric: LimitMetric, value: Int) {
        drafts.update { it + (metric to value.toString()) }
        onSave(metric)
    }

    fun onSave(metric: LimitMetric) {
        val text = drafts.value[metric] ?: return
        if (validateLimit(metric, text) != null) return
        viewModelScope.launch {
            // Applies from today; earlier days keep the limit they had.
            limitRepository.setLimit(metric, text.trim().toInt(), timeProvider.today())
            drafts.update { it - metric }
            justSaved.value = metric
        }
    }

    fun onThresholdChanged(percent: Int) {
        thresholdDraft.value = percent.coerceIn(WARNING_THRESHOLD_RANGE)
    }

    fun onThresholdChangeFinished() {
        val percent = thresholdDraft.value ?: return
        viewModelScope.launch {
            settingsRepository.setWarningThresholdPercent(percent)
            thresholdDraft.value = null
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val container = appContainer()
                LimitsViewModel(
                    container.limitRepository,
                    container.settingsRepository,
                    container.timeProvider,
                    container.permissionMonitor,
                    container.limitNotifier::canNotify,
                    container::syncLimitCheckSchedule,
                )
            }
        }
    }
}
