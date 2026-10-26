package com.sunpawtechnologies.scrollless.ui.dashboard

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import com.sunpawtechnologies.scrollless.domain.model.ACCESSIBILITY_DISCLOSURE_VERSION
import com.sunpawtechnologies.scrollless.domain.model.AccessibilityConsent
import com.sunpawtechnologies.scrollless.domain.model.AppSettings
import com.sunpawtechnologies.scrollless.domain.model.LimitMetric
import com.sunpawtechnologies.scrollless.domain.model.LimitStatus
import com.sunpawtechnologies.scrollless.domain.model.TrackingSource
import com.sunpawtechnologies.scrollless.domain.model.PermissionSnapshot
import com.sunpawtechnologies.scrollless.domain.model.SetupRequirement
import com.sunpawtechnologies.scrollless.domain.model.TrackingAvailability
import com.sunpawtechnologies.scrollless.domain.model.UnavailableReason
import com.sunpawtechnologies.scrollless.domain.usecase.TrackingIssue
import com.sunpawtechnologies.scrollless.domain.usecase.TrackingMode
import com.sunpawtechnologies.scrollless.testing.FakeLimitRepository
import com.sunpawtechnologies.scrollless.testing.FakePermissionMonitor
import com.sunpawtechnologies.scrollless.testing.FakeSettingsRepository
import com.sunpawtechnologies.scrollless.testing.FakeTrackingCoverageRepository
import com.sunpawtechnologies.scrollless.testing.FakeUsageImporter
import com.sunpawtechnologies.scrollless.testing.FakeUsageRepository
import com.sunpawtechnologies.scrollless.testing.FixedTimeProvider
import com.sunpawtechnologies.scrollless.testing.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.time.LocalDate

class DashboardViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val allGranted = PermissionSnapshot(
        usageAccessGranted = true,
        accessibilityServiceEnabled = true,
        accessibilityServiceRunning = true,
        instagramInstalled = true,
    )
    private val time = FixedTimeProvider(Instant.parse("2026-10-24T06:30:00Z")) // 12:00 in Kolkata
    private val today = LocalDate.parse("2026-10-24")
    private val permissions = FakePermissionMonitor(PermissionSnapshot(instagramInstalled = true))
    private val settings = FakeSettingsRepository().apply { emit(AppSettings()) }
    private val usage = FakeUsageRepository()
    private val importer = FakeUsageImporter()
    private val coverage = FakeTrackingCoverageRepository()

    private fun viewModel() =
        DashboardViewModel(permissions, settings, usage, FakeLimitRepository(), coverage, importer, time)

    private fun giveConsent() = settings.emit(
        AppSettings(accessibilityConsent = AccessibilityConsent(ACCESSIBILITY_DISCLOSURE_VERSION, time.now())),
    )

    @Test
    fun `without permissions nothing is shown as zero`() = runTest {
        usage.setReels(today, 0)
        viewModel().uiState.test {
            val state = awaitReady()
            assertEquals(
                MetricState.Unmeasured(TrackingAvailability.NotConfigured(SetupRequirement.ACCESSIBILITY_SERVICE)),
                state.reels,
            )
            assertEquals(
                MetricState.Unmeasured(TrackingAvailability.NotConfigured(SetupRequirement.USAGE_ACCESS)),
                state.instagramTime,
            )
            assertEquals(TrackingIssue.USAGE_ACCESS_MISSING, state.issue)
        }
    }

    @Test
    fun `with full setup the stored values are shown and update live`() = runTest {
        permissions.state.value = allGranted
        giveConsent()
        usage.setReels(today, 12)
        usage.setInstagramTime(today, Duration.ofMinutes(42))

        viewModel().uiState.test {
            val state = awaitReady()
            assertEquals(MetricState.Measured(12), state.reels)
            assertEquals(MetricState.Measured(Duration.ofMinutes(42)), state.instagramTime)
            assertEquals(50, state.reelsDailyLimit)
            assertNull(state.issue)

            usage.setReels(today, 13)
            assertEquals(MetricState.Measured(13), awaitItem().reels)
        }
    }

    @Test
    fun `revoking usage access switches instagram time to not configured`() = runTest {
        permissions.state.value = allGranted
        giveConsent()
        viewModel().uiState.test {
            assertEquals(MetricState.Measured(Duration.ZERO), awaitReady().instagramTime)

            permissions.state.value = allGranted.copy(usageAccessGranted = false)
            val state = awaitItem()
            assertEquals(
                MetricState.Unmeasured(TrackingAvailability.NotConfigured(SetupRequirement.USAGE_ACCESS)),
                state.instagramTime,
            )
            assertEquals(TrackingIssue.USAGE_ACCESS_MISSING, state.issue)
        }
    }

    @Test
    fun `a stopped service is reported as tracking unavailable`() = runTest {
        permissions.state.value = allGranted.copy(accessibilityServiceRunning = false)
        giveConsent()
        viewModel().uiState.test {
            val state = awaitReady()
            assertEquals(
                MetricState.Unmeasured(TrackingAvailability.Unavailable(UnavailableReason.SERVICE_NOT_RUNNING)),
                state.reels,
            )
            assertEquals(TrackingIssue.ADVANCED_STOPPED, state.issue)
        }
    }

    @Test
    fun `service enabled without in-app consent is not counted`() = runTest {
        permissions.state.value = allGranted
        viewModel().uiState.test {
            assertEquals(
                MetricState.Unmeasured(TrackingAvailability.NotConfigured(SetupRequirement.ACCESSIBILITY_CONSENT)),
                awaitReady().reels,
            )
        }
    }

    @Test
    fun `resuming imports usage and rolls over to a new day`() = runTest {
        permissions.state.value = allGranted
        giveConsent()
        usage.setReels(today, 30)
        usage.setReels(today.plusDays(1), 2)
        val viewModel = viewModel()

        viewModel.uiState.test {
            assertEquals(MetricState.Measured(30), awaitReady().reels)

            time.instant = Instant.parse("2026-10-24T19:00:00Z") // 00:30 on Oct 25 in Kolkata
            viewModel.onResume()
            assertEquals(MetricState.Measured(2), awaitItem().reels)
        }
        assertEquals(1, importer.importCount)
    }

    @Test
    fun `limit status follows the warning threshold`() = runTest {
        permissions.state.value = allGranted
        settings.emit(
            AppSettings(
                accessibilityConsent = AccessibilityConsent(ACCESSIBILITY_DISCLOSURE_VERSION, time.now()),
                warningThresholdPercent = 80,
            ),
        )
        usage.setReels(today, 39)
        viewModel().uiState.test {
            assertEquals(LimitStatus.WITHIN, awaitReady().reelsStatus)
            usage.setReels(today, 40) // 80% of the default 50
            assertEquals(LimitStatus.APPROACHING, awaitItem().reelsStatus)
            usage.setReels(today, 50)
            assertEquals(LimitStatus.REACHED, awaitItem().reelsStatus)
            usage.setReels(today, 61) // counting continues past the limit
            assertEquals(MetricState.Measured(61), awaitItem().reels)
        }
    }

    @Test
    fun `choosing basic only is a mode, not a problem`() = runTest {
        permissions.state.value = allGranted.copy(accessibilityServiceEnabled = false)
        viewModel().uiState.test {
            val state = awaitReady()
            assertEquals(TrackingMode.BASIC, state.mode)
            assertNull(state.issue)
            assertNull(state.reelsStatus)
        }
    }

    @Test
    fun `advanced mode when reels tracking runs`() = runTest {
        permissions.state.value = allGranted
        giveConsent()
        viewModel().uiState.test { assertEquals(TrackingMode.ADVANCED, awaitReady().mode) }
    }

    @Test
    fun `reels tracking switched off after opting in is reported, and use basic only clears it`() = runTest {
        permissions.state.value = allGranted.copy(accessibilityServiceEnabled = false, accessibilityServiceRunning = false)
        giveConsent()
        val viewModel = viewModel()
        viewModel.uiState.test {
            assertEquals(TrackingIssue.ADVANCED_TURNED_OFF, awaitReady().issue)
            viewModel.onUseBasicOnly()
            var state = awaitItem()
            while (state.issue != null) state = awaitItem()
            assertEquals(TrackingMode.BASIC, state.mode)
        }
    }

    @Test
    fun `basic tracking measures youtube time without accessibility`() = runTest {
        permissions.state.value = PermissionSnapshot(usageAccessGranted = true, instagramInstalled = true, youtubeInstalled = true)
        viewModel().uiState.test {
            val state = awaitReady()
            assertEquals(MetricState.Measured(Duration.ZERO), state.youtubeTime)
            assertEquals(TrackingMode.BASIC, state.mode)
            assertNull(state.issue)
        }
    }

    @Test
    fun `no yesterday comparison when instagram is not installed`() = runTest {
        permissions.state.value = allGranted.copy(instagramInstalled = false)
        usage.setInstagramTime(today.minusDays(1), Duration.ZERO)
        coverage.tracked[TrackingSource.USAGE_STATS] = setOf(today.minusDays(1))
        val viewModel = viewModel()
        viewModel.uiState.test {
            awaitReady()
            viewModel.onResume()
            // Give the resume work a chance to emit; yesterday must stay hidden.
            expectNoEvents()
            assertNull(viewModel.uiState.value.yesterday)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `yesterday is shown only for sources that tracked yesterday`() = runTest {
        permissions.state.value = allGranted
        giveConsent()
        usage.setReels(today.minusDays(1), 34)
        usage.setInstagramTime(today.minusDays(1), Duration.ofMinutes(50))
        coverage.tracked[TrackingSource.USAGE_STATS] = setOf(today.minusDays(1))
        val viewModel = viewModel()

        viewModel.uiState.test {
            assertNull(awaitReady().yesterday)
            viewModel.onResume()
            var state = awaitItem()
            while (state.yesterday == null) state = awaitItem()
            assertEquals(YesterdaySummary(reels = null, instagramTime = Duration.ofMinutes(50)), state.yesterday)
        }
    }
}

class DashboardLimitsTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val basic = PermissionSnapshot(usageAccessGranted = true, instagramInstalled = true, youtubeInstalled = true)
    private val time = FixedTimeProvider(Instant.parse("2026-10-24T06:30:00Z"))
    private val today = LocalDate.parse("2026-10-24")
    private val permissions = FakePermissionMonitor(basic)
    private val settings = FakeSettingsRepository().apply { emit(AppSettings()) }
    private val usage = FakeUsageRepository()

    private fun viewModel(turnOff: (suspend () -> Boolean)? = null) = DashboardViewModel(
        permissions, settings, usage, FakeLimitRepository(), FakeTrackingCoverageRepository(), FakeUsageImporter(), time, turnOff,
    )

    @Test
    fun `app time limits work without accessibility`() = runTest {
        usage.setInstagramTime(today, Duration.ofMinutes(50))
        usage.setYouTubeTime(today, Duration.ofMinutes(5))
        viewModel().uiState.test {
            val state = awaitReady()
            assertEquals(LimitProgress(60, LimitStatus.APPROACHING), state.instagramLimit)
            assertEquals(LimitProgress(60, LimitStatus.WITHIN), state.youtubeLimit)
            assertNull("reels aren't measured, so no reels status", state.reelsStatus)
            assertEquals(LimitStatus.APPROACHING, state.worstStatus)
        }
    }

    @Test
    fun `without usage access there is no limit status, never a fake within`() = runTest {
        permissions.state.value = basic.copy(usageAccessGranted = false)
        usage.setInstagramTime(today, Duration.ofMinutes(90))
        viewModel().uiState.test {
            val state = awaitReady()
            assertNull(state.instagramLimit)
            assertNull(state.limitNotice)
        }
    }

    @Test
    fun `a switched-off limit still shows the time but no status`() = runTest {
        settings.setLimitEnabled(LimitMetric.INSTAGRAM_APP_TIME, false)
        usage.setInstagramTime(today, Duration.ofMinutes(90))
        viewModel().uiState.test {
            val state = awaitReady()
            assertEquals(MetricState.Measured(Duration.ofMinutes(90)), state.instagramTime)
            assertNull(state.instagramLimit)
            assertNull(state.limitNotice)
        }
    }

    @Test
    fun `the limit-reached notice shows once and stays dismissed for the day`() = runTest {
        usage.setInstagramTime(today, Duration.ofMinutes(61))
        val viewModel = viewModel()
        viewModel.uiState.test {
            val notice = awaitReady().limitNotice
            assertEquals(LimitMetric.INSTAGRAM_APP_TIME, notice?.metric)
            assertEquals(LimitStatus.REACHED, notice?.status)
            viewModel.onLimitNoticeHandled()
            var state = awaitItem()
            while (state.limitNotice != null) state = awaitItem()
        }
        // A fresh screen (e.g. reopening the app) doesn't show it again today.
        viewModel().uiState.test { assertNull(awaitReady().limitNotice) }
    }

    @Test
    fun `opening from a reminder shows that limit even after dismissal`() = runTest {
        usage.setYouTubeTime(today, Duration.ofMinutes(50)) // approaching, not reached: no automatic notice
        val viewModel = viewModel()
        viewModel.uiState.test {
            assertNull(awaitReady().limitNotice)
            viewModel.onOpenedFromReminder(LimitMetric.YOUTUBE_APP_TIME)
            val notice = awaitItem().limitNotice
            assertEquals(LimitMetric.YOUTUBE_APP_TIME, notice?.metric)
            assertEquals(LimitStatus.APPROACHING, notice?.status)
        }
    }

    @Test
    fun `reels tracking turned off in the app is a choice, not a problem`() = runTest {
        permissions.state.value = basic
        settings.emit(
            AppSettings(
                accessibilityConsent = AccessibilityConsent(ACCESSIBILITY_DISCLOSURE_VERSION, time.now()),
                reelsTrackingTurnedOffByUser = true,
            ),
        )
        viewModel().uiState.test {
            val state = awaitReady()
            assertNull(state.issue)
            assertEquals(TrackingMode.BASIC, state.mode)
            assertEquals(false, state.reelsTrackingSwitchedOn)
        }
    }

    @Test
    fun `turning off falls back to accessibility settings when the service isn't running here`() = runTest {
        val viewModel = viewModel(turnOff = { false })
        viewModel.effects.test {
            viewModel.onTurnOffReelsTracking()
            assertEquals(DashboardEffect.TurnOffInSettings, awaitItem())
        }
        val ok = viewModel(turnOff = { true })
        ok.effects.test {
            ok.onTurnOffReelsTracking()
            assertEquals(DashboardEffect.ReelsTrackingTurnedOff, awaitItem())
        }
    }
}

/** The initial "loading" value may be conflated away, so wait for the first loaded state. */
private suspend fun ReceiveTurbine<DashboardUiState>.awaitReady(): DashboardUiState {
    var state = awaitItem()
    while (state.reels == MetricState.Loading) state = awaitItem()
    return state
}
