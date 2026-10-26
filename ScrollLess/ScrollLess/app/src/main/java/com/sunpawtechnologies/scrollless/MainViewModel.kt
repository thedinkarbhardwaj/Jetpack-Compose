package com.sunpawtechnologies.scrollless

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.sunpawtechnologies.scrollless.di.appContainer
import com.sunpawtechnologies.scrollless.domain.model.ThemeMode
import com.sunpawtechnologies.scrollless.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

sealed interface MainUiState {
    data object Loading : MainUiState

    data class Ready(
        val themeMode: ThemeMode,
        val onboardingCompleted: Boolean,
    ) : MainUiState
}

class MainViewModel(settingsRepository: SettingsRepository) : ViewModel() {

    // Eager, so settings load while the splash screen is up, before any UI collects.
    val uiState: StateFlow<MainUiState> = settingsRepository.settings
        .map { MainUiState.Ready(themeMode = it.themeMode, onboardingCompleted = it.onboardingCompleted) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, MainUiState.Loading)

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { MainViewModel(appContainer().settingsRepository) }
        }
    }
}
