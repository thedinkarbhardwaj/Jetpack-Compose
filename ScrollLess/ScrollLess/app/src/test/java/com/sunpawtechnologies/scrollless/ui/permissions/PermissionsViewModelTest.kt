package com.sunpawtechnologies.scrollless.ui.permissions

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import com.sunpawtechnologies.scrollless.domain.model.ACCESSIBILITY_DISCLOSURE_VERSION
import com.sunpawtechnologies.scrollless.domain.model.AccessibilityConsent
import com.sunpawtechnologies.scrollless.domain.model.AppSettings
import com.sunpawtechnologies.scrollless.domain.model.PermissionSnapshot
import com.sunpawtechnologies.scrollless.permissions.SettingsTarget
import com.sunpawtechnologies.scrollless.testing.FakePermissionMonitor
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

class PermissionsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val permissions = FakePermissionMonitor(PermissionSnapshot(instagramInstalled = true))
    private val settings = FakeSettingsRepository().apply { emit(AppSettings()) }
    private val time = FixedTimeProvider()

    private fun viewModel() = PermissionsViewModel(permissions, settings, time)

    private fun consented() = settings.emit(
        AppSettings(accessibilityConsent = AccessibilityConsent(ACCESSIBILITY_DISCLOSURE_VERSION, time.now())),
    )

    @Test
    fun `fresh install shows nothing set up`() = runTest {
        viewModel().uiState.test {
            val state = awaitLoaded()
            assertFalse(state.usageAccessGranted)
            assertEquals(AccessibilityStatus.NOT_ENABLED, state.accessibilityStatus)
            assertEquals(0, state.completedSteps)
        }
    }

    @Test
    fun `status follows what Android reports, including revocation`() = runTest {
        consented()
        viewModel().uiState.test {
            awaitLoaded()
            permissions.state.value = PermissionSnapshot(
                usageAccessGranted = true,
                accessibilityServiceEnabled = true,
                accessibilityServiceRunning = true,
                instagramInstalled = true,
            )
            val granted = awaitItem()
            assertEquals(AccessibilityStatus.ACTIVE, granted.accessibilityStatus)
            assertEquals(2, granted.completedSteps)

            permissions.state.value = permissions.state.value.copy(usageAccessGranted = false)
            assertFalse(awaitItem().usageAccessGranted)
        }
    }

    @Test
    fun `enabled but stopped service is reported as not running`() {
        val status = PermissionsViewModel.accessibilityStatus(
            PermissionSnapshot(accessibilityServiceEnabled = true, accessibilityServiceRunning = false),
            consentGiven = true,
        )
        assertEquals(AccessibilityStatus.NOT_RUNNING, status)
    }

    @Test
    fun `turning on reels tracking shows the disclosure before any settings screen`() = runTest {
        val viewModel = viewModel()
        viewModel.effects.test {
            viewModel.uiState.test {
                awaitLoaded()
                viewModel.onAccessibilityAction()
                assertTrue(awaitItem().showDisclosure)
            }
            expectNoEvents()
        }
    }

    @Test
    fun `accepting the disclosure records consent and then opens accessibility settings`() = runTest {
        val viewModel = viewModel()
        viewModel.effects.test {
            viewModel.uiState.test {
                awaitLoaded()
                viewModel.onAccessibilityAction()
                awaitItem()
                viewModel.onDisclosureAccepted()
                cancelAndIgnoreRemainingEvents()
            }
            assertEquals(PermissionsEffect.OpenSettings(SettingsTarget.ACCESSIBILITY), awaitItem())
        }
        val consent = settings.settings.first().accessibilityConsent
        assertEquals(ACCESSIBILITY_DISCLOSURE_VERSION, consent?.disclosureVersion)
        assertEquals(time.now(), consent?.acceptedAt)
    }

    @Test
    fun `accepting again after turning reels tracking off clears that choice`() = runTest {
        settings.emit(AppSettings(reelsTrackingTurnedOffByUser = true))
        val viewModel = viewModel()
        viewModel.effects.test {
            viewModel.uiState.test {
                awaitLoaded()
                viewModel.onAccessibilityAction()
                awaitItem()
                viewModel.onDisclosureAccepted()
                cancelAndIgnoreRemainingEvents()
            }
            // The settings screen opens last, after everything is saved.
            assertEquals(PermissionsEffect.OpenSettings(SettingsTarget.ACCESSIBILITY), awaitItem())
        }
        assertFalse(settings.settings.first().reelsTrackingTurnedOffByUser)
    }

    @Test
    fun `turning reels tracking off opens accessibility settings only if it can't be done here`() = runTest {
        var result = false
        val viewModel = PermissionsViewModel(permissions, settings, time, turnOffReelsTracking = { result })
        viewModel.effects.test {
            viewModel.onTurnOffReelsTracking()
            assertEquals(PermissionsEffect.OpenSettings(SettingsTarget.ACCESSIBILITY), awaitItem())
            result = true
            viewModel.onTurnOffReelsTracking()
            expectNoEvents()
        }
    }

    @Test
    fun `declining the disclosure opens nothing and records nothing`() = runTest {
        val viewModel = viewModel()
        viewModel.effects.test {
            viewModel.uiState.test {
                awaitLoaded()
                viewModel.onAccessibilityAction()
                assertTrue(awaitItem().showDisclosure)
                viewModel.onDisclosureDismissed()
                assertFalse(awaitItem().showDisclosure)
            }
            expectNoEvents()
        }
        assertEquals(null, settings.settings.first().accessibilityConsent)
    }

    @Test
    fun `consent for a service already switched on does not reopen settings`() = runTest {
        permissions.state.value = PermissionSnapshot(accessibilityServiceEnabled = true, accessibilityServiceRunning = true)
        val viewModel = viewModel()
        viewModel.effects.test {
            viewModel.uiState.test {
                awaitLoaded()
                viewModel.onAccessibilityAction()
                awaitItem()
                viewModel.onDisclosureAccepted()
                cancelAndIgnoreRemainingEvents()
            }
            expectNoEvents()
        }
    }

    @Test
    fun `with consent already given the button goes straight to settings`() = runTest {
        consented()
        val viewModel = viewModel()
        viewModel.effects.test {
            viewModel.uiState.test {
                awaitLoaded()
                viewModel.onAccessibilityAction()
                cancelAndIgnoreRemainingEvents()
            }
            assertEquals(PermissionsEffect.OpenSettings(SettingsTarget.ACCESSIBILITY), awaitItem())
        }
    }

    @Test
    fun `usage access button opens usage access settings`() = runTest {
        val viewModel = viewModel()
        viewModel.effects.test {
            viewModel.onEnableUsageAccess()
            assertEquals(PermissionsEffect.OpenSettings(SettingsTarget.USAGE_ACCESS), awaitItem())
        }
    }
}

/** The initial "loading" value may be conflated away, so wait for the first loaded state. */
private suspend fun ReceiveTurbine<PermissionsUiState>.awaitLoaded(): PermissionsUiState {
    var state = awaitItem()
    while (state.isLoading) state = awaitItem()
    return state
}
