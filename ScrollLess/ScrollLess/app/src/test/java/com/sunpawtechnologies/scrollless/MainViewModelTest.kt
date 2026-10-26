package com.sunpawtechnologies.scrollless

import app.cash.turbine.test
import com.sunpawtechnologies.scrollless.domain.model.AppSettings
import com.sunpawtechnologies.scrollless.domain.model.ThemeMode
import com.sunpawtechnologies.scrollless.testing.FakeSettingsRepository
import com.sunpawtechnologies.scrollless.testing.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class MainViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val settingsRepository = FakeSettingsRepository()

    @Test
    fun `stays loading until settings are read, then becomes ready`() = runTest {
        val viewModel = MainViewModel(settingsRepository)
        viewModel.uiState.test {
            assertEquals(MainUiState.Loading, awaitItem())

            settingsRepository.emit(AppSettings(themeMode = ThemeMode.DARK, onboardingCompleted = true))
            assertEquals(MainUiState.Ready(ThemeMode.DARK, onboardingCompleted = true), awaitItem())
        }
    }

    @Test
    fun `theme change is reflected without restarting`() = runTest {
        settingsRepository.emit(AppSettings())
        val viewModel = MainViewModel(settingsRepository)
        viewModel.uiState.test {
            assertEquals(MainUiState.Ready(ThemeMode.SYSTEM, onboardingCompleted = false), awaitItem())

            settingsRepository.setThemeMode(ThemeMode.LIGHT)
            assertEquals(MainUiState.Ready(ThemeMode.LIGHT, onboardingCompleted = false), awaitItem())
        }
    }
}
