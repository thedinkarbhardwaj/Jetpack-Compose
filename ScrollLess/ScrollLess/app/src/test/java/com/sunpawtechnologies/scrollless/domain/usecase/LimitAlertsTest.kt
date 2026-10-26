package com.sunpawtechnologies.scrollless.domain.usecase

import com.sunpawtechnologies.scrollless.domain.model.ACCESSIBILITY_DISCLOSURE_VERSION
import com.sunpawtechnologies.scrollless.domain.model.AccessibilityConsent
import com.sunpawtechnologies.scrollless.domain.model.AppSettings
import com.sunpawtechnologies.scrollless.domain.model.LimitMetric
import com.sunpawtechnologies.scrollless.domain.model.LimitStatus
import com.sunpawtechnologies.scrollless.domain.model.PermissionSnapshot
import com.sunpawtechnologies.scrollless.testing.FakeLimitRepository
import com.sunpawtechnologies.scrollless.testing.FakePermissionMonitor
import com.sunpawtechnologies.scrollless.testing.FakeSettingsRepository
import com.sunpawtechnologies.scrollless.testing.FakeUsageImporter
import com.sunpawtechnologies.scrollless.testing.FakeUsageRepository
import com.sunpawtechnologies.scrollless.testing.FixedTimeProvider
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.time.LocalDate

class LimitAlertsTest {

    // --- Pure rules ---

    @Test
    fun `minute limits are compared in milliseconds`() {
        assertEquals(3_600_000L, LimitAlertRules.limitInBaseUnits(LimitMetric.INSTAGRAM_APP_TIME, 60))
        assertEquals(50L, LimitAlertRules.limitInBaseUnits(LimitMetric.INSTAGRAM_REELS, 50))
    }

    @Test
    fun `levels follow the warning threshold and the limit`() {
        val metric = LimitMetric.INSTAGRAM_APP_TIME
        assertNull(LimitAlertRules.levelFor(metric, minutes(47), limit = 60, warningThresholdPercent = 80))
        assertEquals(AlertLevel.WARNING, LimitAlertRules.levelFor(metric, minutes(48), 60, 80))
        assertEquals(AlertLevel.REACHED, LimitAlertRules.levelFor(metric, minutes(60), 60, 80))
        assertEquals(AlertLevel.REACHED, LimitAlertRules.levelFor(metric, minutes(95), 60, 80))
        // One second short of the limit is still only a warning.
        assertEquals(AlertLevel.WARNING, LimitAlertRules.levelFor(metric, minutes(60) - 1_000, 60, 80))
    }

    @Test
    fun `dedup keys separate warning and reached`() {
        assertEquals(80, LimitAlertRules.thresholdKey(AlertLevel.WARNING, 80))
        assertEquals(100, LimitAlertRules.thresholdKey(AlertLevel.REACHED, 80))
    }

    @Test
    fun `only metrics with a working source are measurable`() {
        assertTrue(LimitAlertRules.measurableMetrics(PermissionSnapshot(), reelsConsent = true).isEmpty())
        assertEquals(
            setOf(LimitMetric.INSTAGRAM_APP_TIME, LimitMetric.YOUTUBE_APP_TIME),
            LimitAlertRules.measurableMetrics(basic, reelsConsent = false),
        )
        assertEquals(
            setOf(LimitMetric.INSTAGRAM_APP_TIME, LimitMetric.YOUTUBE_APP_TIME, LimitMetric.INSTAGRAM_REELS),
            LimitAlertRules.measurableMetrics(advanced, reelsConsent = true),
        )
        // Service on but no consent: Reels aren't counted, so they can't be "within" or "over".
        assertTrue(LimitMetric.INSTAGRAM_REELS !in LimitAlertRules.measurableMetrics(advanced, reelsConsent = false))
        // YouTube not installed: no YouTube alert.
        assertTrue(LimitMetric.YOUTUBE_APP_TIME !in LimitAlertRules.measurableMetrics(basic.copy(youtubeInstalled = false), false))
    }

    // --- Checker ---

    private val basic = PermissionSnapshot(usageAccessGranted = true, instagramInstalled = true, youtubeInstalled = true)
    private val advanced = basic.copy(accessibilityServiceEnabled = true, accessibilityServiceRunning = true)
    private val time = FixedTimeProvider(Instant.parse("2026-10-24T06:30:00Z")) // 12:00 in Kolkata
    private val today = LocalDate.parse("2026-10-24")
    private val permissions = FakePermissionMonitor(basic)
    private val settings = FakeSettingsRepository().apply { emit(AppSettings()) }
    private val usage = FakeUsageRepository()
    private val limits = FakeLimitRepository()
    private val importer = FakeUsageImporter()
    private val alertLog = InMemoryAlertLog()
    private val notifier = RecordingNotifier()

    private fun checker() = LimitAlertChecker(permissions, settings, usage, limits, importer, alertLog, notifier, time)

    @Test
    fun `each level is sent once per day, even when checked repeatedly`() = runTest {
        val checker = checker()
        usage.setInstagramTime(today, Duration.ofMinutes(30))
        assertTrue(checker.check().isEmpty())

        usage.setInstagramTime(today, Duration.ofMinutes(50)) // 83% of the default 60
        assertEquals(listOf(AlertLevel.WARNING), checker.check().map { it.level })
        assertTrue("no repeat", checker.check().isEmpty())

        usage.setInstagramTime(today, Duration.ofMinutes(61))
        assertEquals(listOf(AlertLevel.REACHED), checker.check().map { it.level })
        assertTrue("no repeat", checker.check().isEmpty())
        assertTrue("no repeat after more use", checker.check().isEmpty())

        assertEquals(2, notifier.sent.size)
        assertEquals(LimitMetric.INSTAGRAM_APP_TIME, notifier.sent.last().metric)
    }

    @Test
    fun `jumping past the limit sends only reached`() = runTest {
        usage.setInstagramTime(today, Duration.ofMinutes(75))
        assertEquals(listOf(AlertLevel.REACHED), checker().check().map { it.level })
    }

    @Test
    fun `instagram and youtube are separate limits`() = runTest {
        usage.setInstagramTime(today, Duration.ofMinutes(10))
        usage.setYouTubeTime(today, Duration.ofMinutes(65))
        val sent = checker().check()
        assertEquals(listOf(LimitMetric.YOUTUBE_APP_TIME to AlertLevel.REACHED), sent.map { it.metric to it.level })
    }

    @Test
    fun `a new local day starts fresh`() = runTest {
        val checker = checker()
        usage.setInstagramTime(today, Duration.ofMinutes(61))
        assertEquals(1, checker.check().size)

        time.instant = Instant.parse("2026-10-24T19:00:00Z") // 00:30 on the 25th in Kolkata
        val tomorrow = LocalDate.parse("2026-10-25")
        assertTrue("yesterday's usage doesn't count today", checker.check().isEmpty())
        usage.setInstagramTime(tomorrow, Duration.ofMinutes(61))
        val sent = checker.check()
        assertEquals(1, sent.size)
        assertEquals(tomorrow.toEpochDay(), sent.single().epochDay)
    }

    @Test
    fun `nothing is sent or recorded while notifications are off`() = runTest {
        notifier.allowed = false
        usage.setInstagramTime(today, Duration.ofMinutes(61))
        val checker = checker()
        assertTrue(checker.check().isEmpty())
        assertTrue(alertLog.fired.isEmpty())
        assertEquals("no work done at all", 0, importer.importCount)

        notifier.allowed = true
        assertEquals("allowed later the same day: still delivered", 1, checker.check().size)
    }

    @Test
    fun `switched-off limits never alert`() = runTest {
        settings.setLimitEnabled(LimitMetric.INSTAGRAM_APP_TIME, false)
        usage.setInstagramTime(today, Duration.ofMinutes(500))
        assertTrue(checker().check().isEmpty())
    }

    @Test
    fun `revoked usage access means no time alerts, even with stored data`() = runTest {
        usage.setInstagramTime(today, Duration.ofMinutes(61))
        permissions.state.value = basic.copy(usageAccessGranted = false)
        assertTrue(checker().check().isEmpty())
    }

    @Test
    fun `the user's own limit value is used`() = runTest {
        limits.setLimit(LimitMetric.INSTAGRAM_APP_TIME, 30, today)
        usage.setInstagramTime(today, Duration.ofMinutes(31))
        val alert = checker().check().single()
        assertEquals(AlertLevel.REACHED, alert.level)
        assertEquals(30, alert.limit)
    }

    @Test
    fun `reels alert only with reels tracking running and agreed to`() = runTest {
        usage.setReels(today, 60)
        assertTrue(checker().check().isEmpty())

        permissions.state.value = advanced
        settings.emit(AppSettings(accessibilityConsent = AccessibilityConsent(ACCESSIBILITY_DISCLOSURE_VERSION, time.now())))
        val alert = checker().check().single()
        assertEquals(LimitMetric.INSTAGRAM_REELS, alert.metric)
        assertEquals(60L, alert.value)
    }

    @Test
    fun `usage is imported and permissions refreshed only when asked`() = runTest {
        val checker = checker()
        checker.check(importUsage = false)
        assertEquals(0, importer.importCount)
        assertEquals(0, permissions.refreshCount)
        checker.check()
        assertEquals(1, importer.importCount)
        assertEquals(1, permissions.refreshCount)
    }

    @Test
    fun `the dashboard and alerts agree on status`() {
        assertEquals(LimitStatus.APPROACHING, LimitAlertRules.status(LimitMetric.YOUTUBE_APP_TIME, minutes(48), 60, 80))
    }

    private fun minutes(m: Long) = m * 60_000

    private class InMemoryAlertLog : AlertLog {
        val fired = mutableSetOf<Triple<Long, LimitMetric, Int>>()
        override suspend fun markFired(epochDay: Long, metric: LimitMetric, thresholdPercent: Int, at: Instant): Boolean =
            fired.add(Triple(epochDay, metric, thresholdPercent))
    }

    private class RecordingNotifier : LimitNotifier {
        var allowed = true
        val sent = mutableListOf<LimitAlert>()
        override fun canNotify() = allowed
        override fun notify(alert: LimitAlert) {
            sent += alert
        }
    }
}
