package com.sunpawtechnologies.scrollless.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.sunpawtechnologies.scrollless.core.time.TimeProvider
import com.sunpawtechnologies.scrollless.di.appContainer
import com.sunpawtechnologies.scrollless.domain.model.LimitMetric
import com.sunpawtechnologies.scrollless.domain.repository.LimitRepository
import com.sunpawtechnologies.scrollless.domain.repository.SettingsRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class OnboardingStep { WELCOME, DAILY_LIMIT }

data class OnboardingUiState(
    val step: OnboardingStep = OnboardingStep.WELCOME,
    /** The daily Instagram open-time limit being chosen, in minutes. */
    val dailyLimitMinutes: Int = LimitMetric.INSTAGRAM_APP_TIME.defaultDailyLimit,
    val isCompleting: Boolean = false,
) {
    companion object {
        val LIMIT_CHOICES = listOf(30, 45, 60, 90, 120)
    }
}

sealed interface OnboardingEffect {
    /** Onboarding is done. Permission setup follows; the dashboard covers it if the user leaves early. */
    data object Finished : OnboardingEffect
}

class OnboardingViewModel(
    private val settingsRepository: SettingsRepository,
    private val limitRepository: LimitRepository? = null,
    private val timeProvider: TimeProvider? = null,
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    private val _effects = Channel<OnboardingEffect>(Channel.BUFFERED)
    val effects: Flow<OnboardingEffect> = _effects.receiveAsFlow()

    fun onGetStartedClicked() {
        _uiState.update { it.copy(step = OnboardingStep.DAILY_LIMIT) }
    }

    fun onBack() {
        _uiState.update { it.copy(step = OnboardingStep.WELCOME) }
    }

    fun onLimitSelected(minutes: Int) {
        if (minutes in LimitMetric.INSTAGRAM_APP_TIME.validRange) _uiState.update { it.copy(dailyLimitMinutes = minutes) }
    }

    /** Saves the chosen limit, then finishes. */
    fun onContinueClicked() = finish(saveLimit = true)

    /** Keeps the default limit; it can be changed any time in Limits. */
    fun onSkipClicked() = finish(saveLimit = false)

    private fun finish(saveLimit: Boolean) {
        if (_uiState.value.isCompleting) return
        _uiState.update { it.copy(isCompleting = true) }
        val minutes = _uiState.value.dailyLimitMinutes
        viewModelScope.launch {
            val limits = limitRepository
            val time = timeProvider
            if (saveLimit && limits != null && time != null) {
                limits.setLimit(LimitMetric.INSTAGRAM_APP_TIME, minutes, time.today())
            }
            // Persist before navigating: leaving this screen clears the ViewModel and cancels its scope.
            settingsRepository.setOnboardingCompleted(true)
            _effects.send(OnboardingEffect.Finished)
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val container = appContainer()
                OnboardingViewModel(container.settingsRepository, container.limitRepository, container.timeProvider)
            }
        }
    }
}
