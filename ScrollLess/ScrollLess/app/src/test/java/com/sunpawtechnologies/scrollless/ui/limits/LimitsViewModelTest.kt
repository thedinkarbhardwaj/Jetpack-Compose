package com.sunpawtechnologies.scrollless.ui.limits

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import com.sunpawtechnologies.scrollless.domain.model.AppSettings
import com.sunpawtechnologies.scrollless.domain.model.LimitMetric
import com.sunpawtechnologies.scrollless.domain.model.PermissionSnapshot
import com.sunpawtechnologies.scrollless.testing.FakeLimitRepository
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
import java.time.LocalDate

class LimitsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val limits = FakeLimitRepository()
    private val settings = FakeSettingsRepository().apply { emit(AppSettings()) }
    private val time = FixedTimeProvider()
    private val today = LocalDate.parse("2026-10-24")

    private fun viewModel() = LimitsViewModel(limits, settings, time)

    private fun LimitsUiState.field(metric: LimitMetric) = fields.first { it.metric == metric }

    @Test
    fun `shows defaults for unconfigured limits`() = runTest {
        viewModel().uiState.test {
            val state = awaitLoaded()
            assertEquals(50, state.field(LimitMetric.INSTAGRAM_REELS).saved)
            assertEquals(60, state.field(LimitMetric.INSTAGRAM_APP_TIME).saved)
            assertEquals(60, state.field(LimitMetric.YOUTUBE_APP_TIME).saved)
            assertFalse(state.field(LimitMetric.INSTAGRAM_REELS).canSave)
        }
    }

    @Test
    fun `a valid custom limit is saved from today`() = runTest {
        val viewModel = viewModel()
        viewModel.uiState.test {
            awaitLoaded()
            viewModel.onDraftChanged(LimitMetric.INSTAGRAM_REELS, "30")
            assertTrue(awaitItem().field(LimitMetric.INSTAGRAM_REELS).canSave)
            viewModel.onSave(LimitMetric.INSTAGRAM_REELS)
            var state = awaitItem()
            while (!state.field(LimitMetric.INSTAGRAM_REELS).justSaved) state = awaitItem()
            assertEquals(30, state.field(LimitMetric.INSTAGRAM_REELS).saved)
        }
        assertEquals(today, limits.history(LimitMetric.INSTAGRAM_REELS).single().effectiveFrom)
    }

    @Test
    fun `invalid input shows an error and is never saved`() = runTest {
        val viewModel = viewModel()
        viewModel.uiState.test {
            awaitLoaded()
            viewModel.onDraftChanged(LimitMetric.INSTAGRAM_REELS, "0")
            val field = awaitItem().field(LimitMetric.INSTAGRAM_REELS)
            assertEquals(LimitError.OUT_OF_RANGE, field.error)
            assertFalse(field.canSave)
            viewModel.onSave(LimitMetric.INSTAGRAM_REELS)
            expectNoEvents()
        }
        assertTrue(limits.history(LimitMetric.INSTAGRAM_REELS).isEmpty())
    }

    @Test
    fun `non-digits are stripped from input`() = runTest {
        val viewModel = viewModel()
        viewModel.uiState.test {
            awaitLoaded()
            viewModel.onDraftChanged(LimitMetric.YOUTUBE_APP_TIME, "4a5-")
            assertEquals("45", awaitItem().field(LimitMetric.YOUTUBE_APP_TIME).draft)
        }
    }

    @Test
    fun `presets save immediately`() = runTest {
        val viewModel = viewModel()
        viewModel.uiState.test {
            awaitLoaded()
            viewModel.onPresetSelected(LimitMetric.YOUTUBE_APP_TIME, 90)
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals(90, limits.history(LimitMetric.YOUTUBE_APP_TIME).single().value)
    }

    @Test
    fun `only measurable limits are offered, app time first`() = runTest {
        viewModel().uiState.test {
            val metrics = awaitLoaded().fields.map { it.metric }
            assertEquals(listOf(LimitMetric.INSTAGRAM_APP_TIME, LimitMetric.YOUTUBE_APP_TIME, LimitMetric.INSTAGRAM_REELS), metrics)
            // Nothing measures Shorts or regular videos separately, so those limits can't be set.
            assertFalse(LimitMetric.YOUTUBE_SHORTS in metrics)
            assertFalse(LimitMetric.YOUTUBE_REGULAR_VIDEO in metrics)
        }
    }

    @Test
    fun `switching a limit off is persisted and re-checks the schedule`() = runTest {
        var syncs = 0
        val viewModel = LimitsViewModel(limits, settings, time, onLimitSetupChanged = { syncs++ })
        viewModel.uiState.test {
            assertTrue(awaitLoaded().field(LimitMetric.YOUTUBE_APP_TIME).enabled)
            viewModel.onEnabledChanged(LimitMetric.YOUTUBE_APP_TIME, false)
            var state = awaitItem()
            while (state.field(LimitMetric.YOUTUBE_APP_TIME).enabled) state = awaitItem()
            assertTrue("other limits are untouched", state.field(LimitMetric.INSTAGRAM_APP_TIME).enabled)
        }
        assertEquals(setOf(LimitMetric.YOUTUBE_APP_TIME), settings.settings.first().disabledLimits)
        assertEquals(1, syncs)
    }

    @Test
    fun `time limits are measured with usage access, reels only with reels tracking`() = runTest {
        val permissions = FakePermissionMonitor(
            PermissionSnapshot(usageAccessGranted = true, instagramInstalled = true, youtubeInstalled = true),
        )
        LimitsViewModel(limits, settings, time, permissions).uiState.test {
            val state = awaitLoaded()
            assertTrue(state.field(LimitMetric.INSTAGRAM_APP_TIME).measuredNow)
            assertTrue(state.field(LimitMetric.YOUTUBE_APP_TIME).measuredNow)
            assertFalse(state.field(LimitMetric.INSTAGRAM_REELS).measuredNow)
        }
    }

    @Test
    fun `notification state is re-read on resume`() = runTest {
        var allowed = false
        val viewModel = LimitsViewModel(limits, settings, time, notificationsAllowed = { allowed })
        viewModel.uiState.test {
            assertFalse(awaitLoaded().notificationsAllowed)
            allowed = true
            viewModel.onResume()
            assertTrue(awaitItem().notificationsAllowed)
        }
    }

    @Test
    fun `warning threshold is persisted when the slider is released`() = runTest {
        val viewModel = viewModel()
        viewModel.uiState.test {
            assertEquals(80, awaitLoaded().warningThresholdPercent)
            viewModel.onThresholdChanged(70)
            assertEquals(70, awaitItem().warningThresholdPercent)
            assertEquals(80, settings.settings.first().warningThresholdPercent)
            viewModel.onThresholdChangeFinished()
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals(70, settings.settings.first().warningThresholdPercent)
    }
}

private suspend fun ReceiveTurbine<LimitsUiState>.awaitLoaded(): LimitsUiState {
    var state = awaitItem()
    while (state.isLoading) state = awaitItem()
    return state
}
