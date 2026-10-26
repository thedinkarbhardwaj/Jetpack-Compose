package com.sunpawtechnologies.scrollless.ui.settings

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import com.sunpawtechnologies.scrollless.domain.model.ACCESSIBILITY_DISCLOSURE_VERSION
import com.sunpawtechnologies.scrollless.domain.model.AccessibilityConsent
import com.sunpawtechnologies.scrollless.domain.model.AppSettings
import com.sunpawtechnologies.scrollless.domain.model.OverlayPosition
import com.sunpawtechnologies.scrollless.domain.model.PermissionSnapshot
import com.sunpawtechnologies.scrollless.domain.model.ThemeMode
import com.sunpawtechnologies.scrollless.permissions.SettingsTarget
import com.sunpawtechnologies.scrollless.testing.FakePermissionMonitor
import com.sunpawtechnologies.scrollless.testing.FakeSettingsRepository
import com.sunpawtechnologies.scrollless.testing.MainDispatcherRule
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Instant

class SettingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val settingsRepository = FakeSettingsRepository()
    private val permissions = FakePermissionMonitor(PermissionSnapshot(instagramInstalled = true))

    private fun viewModel() = SettingsViewModel(settingsRepository, permissions)

    @Test
    fun `stays loading until settings arrive`() = runTest {
        viewModel().uiState.test {
            assertTrue(awaitItem().isLoading)
            settingsRepository.emit(AppSettings(themeMode = ThemeMode.LIGHT))
            val state = awaitLoaded()
            assertEquals(ThemeMode.LIGHT, state.themeMode)
            assertEquals(0, state.setupStepsDone)
        }
    }

    @Test
    fun `selecting a theme persists it and updates state`() = runTest {
        settingsRepository.emit(AppSettings())
        val viewModel = viewModel()
        viewModel.uiState.test {
            assertEquals(ThemeMode.SYSTEM, awaitLoaded().themeMode)
            viewModel.onThemeModeSelected(ThemeMode.DARK)
            assertEquals(ThemeMode.DARK, awaitItem().themeMode)
        }
        assertEquals(ThemeMode.DARK, settingsRepository.settings.first().themeMode)
    }

    @Test
    fun `counter can be switched off and its position reset`() = runTest {
        settingsRepository.emit(AppSettings(overlayEnabled = true, overlayPosition = OverlayPosition(0.5f, 0.5f)))
        val viewModel = viewModel()
        viewModel.uiState.test {
            val initial = awaitLoaded()
            assertTrue(initial.overlayEnabled)
            assertTrue(initial.overlayMoved)

            viewModel.onOverlayEnabledChanged(false)
            assertFalse(awaitItem().overlayEnabled)

            viewModel.onResetOverlayPosition()
            assertFalse(awaitItem().overlayMoved)
        }
        assertNull(settingsRepository.settings.first().overlayPosition)
    }

    @Test
    fun `counter is explained as needing reels tracking until tracking is active`() = runTest {
        settingsRepository.emit(AppSettings())
        val viewModel = viewModel()
        viewModel.uiState.test {
            assertFalse(awaitLoaded().reelsTrackingActive)

            settingsRepository.emit(
                AppSettings(accessibilityConsent = AccessibilityConsent(ACCESSIBILITY_DISCLOSURE_VERSION, Instant.EPOCH)),
            )
            permissions.state.value = PermissionSnapshot(
                usageAccessGranted = true,
                accessibilityServiceEnabled = true,
                accessibilityServiceRunning = true,
                instagramInstalled = true,
            )
            var state = awaitItem()
            while (!state.reelsTrackingActive) state = awaitItem()
            assertEquals(2, state.setupStepsDone)
        }
    }

    @Test
    fun `payment app exclusions can be added and removed, invalid names are rejected`() = runTest {
        settingsRepository.emit(AppSettings())
        val viewModel = SettingsViewModel(settingsRepository, permissions, isInstalled = { it == "net.one97.paytm" })
        viewModel.uiState.test {
            assertEquals(listOf("Paytm"), awaitLoaded().detectedPaymentApps)

            viewModel.onExclusionDraftChanged("not a package")
            assertTrue(awaitItem().exclusionDraftInvalid)
            viewModel.onAddExclusion()
            expectNoEvents()

            viewModel.onExclusionDraftChanged("com.example.bank")
            assertFalse(awaitItem().exclusionDraftInvalid)
            viewModel.onAddExclusion()
            var state = awaitItem()
            while (state.customExcludedPackages.isEmpty() || state.exclusionDraft.isNotEmpty()) state = awaitItem()
            assertEquals(listOf("com.example.bank"), state.customExcludedPackages)

            viewModel.onRemoveExclusion("com.example.bank")
            assertEquals(emptyList<String>(), awaitItem().customExcludedPackages)
        }
    }
}

class SettingsPaymentSafetyTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val settingsRepository = FakeSettingsRepository().apply { emit(AppSettings()) }
    private val permissions = FakePermissionMonitor()

    @Test
    fun `developer mode and the reels service switch are reported as android reports them`() = runTest {
        val viewModel = SettingsViewModel(settingsRepository, permissions)
        viewModel.uiState.test {
            val off = awaitLoaded()
            assertFalse(off.developerModeEnabled)
            assertFalse(off.reelsTrackingSwitchedOn)
            permissions.state.value = PermissionSnapshot(developerModeEnabled = true, accessibilityServiceEnabled = true)
            val on = awaitItem()
            assertTrue(on.developerModeEnabled)
            assertTrue(on.reelsTrackingSwitchedOn)
        }
    }

    @Test
    fun `turning reels tracking off reports the outcome`() = runTest {
        var turnedOff = true
        val viewModel = SettingsViewModel(settingsRepository, permissions, turnOffReelsTracking = { turnedOff })
        viewModel.effects.test {
            viewModel.onTurnOffReelsTracking()
            assertEquals(SettingsEffect.ReelsTrackingTurnedOff, awaitItem())
            turnedOff = false
            viewModel.onTurnOffReelsTracking()
            assertEquals(SettingsEffect.OpenSettings(SettingsTarget.ACCESSIBILITY), awaitItem())
        }
    }

    @Test
    fun `developer options are only opened, never changed`() = runTest {
        val viewModel = SettingsViewModel(settingsRepository, permissions)
        viewModel.effects.test {
            viewModel.onOpenDeveloperOptions()
            assertEquals(SettingsEffect.OpenSettings(SettingsTarget.DEVELOPER_OPTIONS), awaitItem())
        }
    }
}

private suspend fun ReceiveTurbine<SettingsUiState>.awaitLoaded(): SettingsUiState {
    var state = awaitItem()
    while (state.isLoading) state = awaitItem()
    return state
}
