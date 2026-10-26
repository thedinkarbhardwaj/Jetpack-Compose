package com.sunpawtechnologies.scrollless.data.prefs

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.sunpawtechnologies.scrollless.domain.model.AccessibilityConsent
import com.sunpawtechnologies.scrollless.domain.model.AppSettings
import com.sunpawtechnologies.scrollless.domain.model.LimitMetric
import com.sunpawtechnologies.scrollless.domain.model.isLimitEnabled
import com.sunpawtechnologies.scrollless.domain.model.limitAlertKey
import com.sunpawtechnologies.scrollless.domain.model.OverlayPosition
import com.sunpawtechnologies.scrollless.domain.model.ThemeMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File
import java.time.Instant

/**
 * Runs under Robolectric so DataStore sees a real SDK level and replaces its file with
 * Files.move, as on a device. On the plain JVM, Build.VERSION.SDK_INT is 0, which forces
 * File.renameTo, and that can never replace an existing file on Windows.
 */
@RunWith(RobolectricTestRunner::class)
class DataStoreSettingsRepositoryTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val file: File by lazy { File(tempFolder.root, "settings.preferences_pb") }

    /** Runs [block] with a repository whose DataStore is shut down afterwards, like a process ending. */
    private suspend fun <T> withRepository(block: suspend (DataStoreSettingsRepository) -> T): T {
        val job = SupervisorJob()
        val dataStore = PreferenceDataStoreFactory.create(scope = CoroutineScope(Dispatchers.IO + job)) { file }
        return try {
            block(DataStoreSettingsRepository(dataStore))
        } finally {
            job.cancelAndJoin()
        }
    }

    @Test
    fun `fresh install uses defaults`() = runTest {
        withRepository { repository ->
            assertEquals(AppSettings(), repository.settings.first())
            // The optional counter is off until the user turns it on.
            assertEquals(false, repository.settings.first().overlayEnabled)
        }
    }

    @Test
    fun `theme and onboarding survive a restart`() = runTest {
        withRepository { repository ->
            repository.setThemeMode(ThemeMode.DARK)
            repository.setOnboardingCompleted(true)
        }
        withRepository { repository ->
            val settings = repository.settings.first()
            assertEquals(ThemeMode.DARK, settings.themeMode)
            assertEquals(true, settings.onboardingCompleted)
        }
    }

    @Test
    fun `accessibility consent is stored and can be withdrawn`() = runTest {
        val consent = AccessibilityConsent(disclosureVersion = 1, acceptedAt = Instant.ofEpochMilli(1_761_300_000_000))
        withRepository { repository ->
            repository.recordAccessibilityConsent(consent)
            assertEquals(consent, repository.settings.first().accessibilityConsent)

            repository.clearAccessibilityConsent()
            assertNull(repository.settings.first().accessibilityConsent)
        }
    }

    @Test
    fun `counter settings and warning threshold survive a restart`() = runTest {
        withRepository { repository ->
            repository.setOverlayEnabled(false)
            repository.setOverlayPosition(OverlayPosition(0.25f, 0.75f))
            repository.setWarningThresholdPercent(65)
        }
        withRepository { repository ->
            val settings = repository.settings.first()
            assertEquals(false, settings.overlayEnabled)
            assertEquals(OverlayPosition(0.25f, 0.75f), settings.overlayPosition)
            assertEquals(65, settings.warningThresholdPercent)

            repository.setOverlayPosition(null)
            assertNull(repository.settings.first().overlayPosition)
        }
    }

    @Test
    fun `switched-off limits and the reels turn-off choice survive a restart`() = runTest {
        withRepository { repository ->
            repository.setLimitEnabled(LimitMetric.YOUTUBE_APP_TIME, false)
            repository.setLimitEnabled(LimitMetric.INSTAGRAM_REELS, false)
            repository.setLimitEnabled(LimitMetric.INSTAGRAM_REELS, true)
            repository.setReelsTrackingTurnedOffByUser(true)
        }
        withRepository { repository ->
            val settings = repository.settings.first()
            assertEquals(setOf(LimitMetric.YOUTUBE_APP_TIME), settings.disabledLimits)
            assertEquals(false, settings.isLimitEnabled(LimitMetric.YOUTUBE_APP_TIME))
            assertEquals(true, settings.isLimitEnabled(LimitMetric.INSTAGRAM_APP_TIME))
            assertEquals(true, settings.reelsTrackingTurnedOffByUser)
        }
    }

    @Test
    fun `acknowledged limit notices keep only the current day`() = runTest {
        withRepository { repository ->
            repository.acknowledgeLimitAlert(limitAlertKey(20_385, LimitMetric.INSTAGRAM_APP_TIME), 20_385)
            repository.acknowledgeLimitAlert(limitAlertKey(20_385, LimitMetric.YOUTUBE_APP_TIME), 20_385)
            assertEquals(2, repository.settings.first().acknowledgedLimitAlerts.size)

            repository.acknowledgeLimitAlert(limitAlertKey(20_386, LimitMetric.INSTAGRAM_APP_TIME), 20_386)
            assertEquals(setOf("20386:INSTAGRAM_APP_TIME"), repository.settings.first().acknowledgedLimitAlerts)
        }
    }

    @Test
    fun `limits are never enabled for metrics nothing measures`() {
        assertEquals(false, AppSettings().isLimitEnabled(LimitMetric.YOUTUBE_SHORTS))
        assertEquals(false, AppSettings().isLimitEnabled(LimitMetric.YOUTUBE_REGULAR_VIDEO))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `out-of-range warning thresholds are rejected`() = runTest {
        withRepository { it.setWarningThresholdPercent(99) }
    }
}
