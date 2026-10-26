package com.sunpawtechnologies.scrollless.ui.onboarding

import app.cash.turbine.test
import com.sunpawtechnologies.scrollless.domain.model.AppSettings
import com.sunpawtechnologies.scrollless.domain.model.LimitMetric
import com.sunpawtechnologies.scrollless.testing.FakeLimitRepository
import com.sunpawtechnologies.scrollless.testing.FakeSettingsRepository
import com.sunpawtechnologies.scrollless.testing.FixedTimeProvider
import com.sunpawtechnologies.scrollless.testing.MainDispatcherRule
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

class OnboardingViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val settingsRepository = FakeSettingsRepository().apply { emit(AppSettings()) }
    private val limits = FakeLimitRepository()
    private val time = FixedTimeProvider()

    private fun viewModel() = OnboardingViewModel(settingsRepository, limits, time)

    @Test
    fun `get started moves to the daily limit step without finishing`() = runTest {
        val viewModel = viewModel()
        viewModel.effects.test {
            viewModel.onGetStartedClicked()
            assertEquals(OnboardingStep.DAILY_LIMIT, viewModel.uiState.value.step)
            expectNoEvents()
        }
        assertFalse(settingsRepository.settings.first().onboardingCompleted)
    }

    @Test
    fun `continue saves the chosen instagram limit from today, then finishes`() = runTest {
        val viewModel = viewModel()
        viewModel.effects.test {
            viewModel.onGetStartedClicked()
            viewModel.onLimitSelected(45)
            viewModel.onContinueClicked()

            assertEquals(OnboardingEffect.Finished, awaitItem())
            assertTrue(settingsRepository.settings.first().onboardingCompleted)
            assertTrue(viewModel.uiState.value.isCompleting)
        }
        val saved = limits.history(LimitMetric.INSTAGRAM_APP_TIME).single()
        assertEquals(45, saved.value)
        assertEquals(LocalDate.parse("2026-10-24"), saved.effectiveFrom)
    }

    @Test
    fun `decide later finishes without saving a limit`() = runTest {
        val viewModel = viewModel()
        viewModel.effects.test {
            viewModel.onGetStartedClicked()
            viewModel.onSkipClicked()
            assertEquals(OnboardingEffect.Finished, awaitItem())
        }
        assertTrue(limits.history(LimitMetric.INSTAGRAM_APP_TIME).isEmpty())
    }

    @Test
    fun `out of range limits are ignored`() {
        val viewModel = viewModel()
        viewModel.onLimitSelected(1)
        assertEquals(LimitMetric.INSTAGRAM_APP_TIME.defaultDailyLimit, viewModel.uiState.value.dailyLimitMinutes)
    }

    @Test
    fun `repeated taps finish only once`() = runTest {
        val viewModel = viewModel()
        viewModel.effects.test {
            viewModel.onGetStartedClicked()
            viewModel.onContinueClicked()
            viewModel.onContinueClicked()
            viewModel.onSkipClicked()

            assertEquals(OnboardingEffect.Finished, awaitItem())
            expectNoEvents()
        }
    }

    @Test
    fun `back returns to welcome`() {
        val viewModel = viewModel()
        viewModel.onGetStartedClicked()
        viewModel.onBack()
        assertEquals(OnboardingStep.WELCOME, viewModel.uiState.value.step)
    }
}
