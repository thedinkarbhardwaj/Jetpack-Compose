package com.sunpawtechnologies.scrollless.tracking.usagestats

import com.sunpawtechnologies.scrollless.tracking.usagestats.ForegroundEvent.Kind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ForegroundSessionsTest {

    private fun resumed(t: Long, activity: String = "Main") = ForegroundEvent(t, Kind.TARGET_RESUMED, activity)
    private fun paused(t: Long, activity: String = "Main") = ForegroundEvent(t, Kind.TARGET_PAUSED, activity)
    private fun otherApp(t: Long) = ForegroundEvent(t, Kind.OTHER_APP_RESUMED)
    private fun screenOff(t: Long) = ForegroundEvent(t, Kind.DEVICE_INACTIVE)

    @Test
    fun `resume to pause is one session`() {
        val sessions = buildForegroundSessions(listOf(resumed(1_000), paused(61_000)), nowMs = 100_000)
        assertEquals(listOf(ForegroundSession(1_000, 61_000, ongoing = false)), sessions)
    }

    @Test
    fun `moving between the app's own activities stays one session`() {
        val events = listOf(resumed(0, "Main"), paused(10_000, "Main"), resumed(10_200, "Reels"), paused(30_000, "Reels"))
        assertEquals(listOf(ForegroundSession(0, 30_000, ongoing = false)), buildForegroundSessions(events, nowMs = 40_000))
    }

    @Test
    fun `another app coming to the front ends the session even without a pause`() {
        val sessions = buildForegroundSessions(listOf(resumed(0), otherApp(20_000)), nowMs = 90_000)
        assertEquals(listOf(ForegroundSession(0, 20_000, ongoing = false)), sessions)
    }

    @Test
    fun `screen off ends the session`() {
        val sessions = buildForegroundSessions(listOf(resumed(0), screenOff(5_000), paused(5_100)), nowMs = 90_000)
        assertEquals(listOf(ForegroundSession(0, 5_000, ongoing = false)), sessions)
    }

    @Test
    fun `a pause without a matching resume is ignored`() {
        // The resume happened before the query window.
        val sessions = buildForegroundSessions(listOf(paused(1_000), resumed(5_000), paused(9_000)), nowMs = 10_000)
        assertEquals(listOf(ForegroundSession(5_000, 9_000, ongoing = false)), sessions)
    }

    @Test
    fun `a session still open runs until now and is marked ongoing`() {
        val sessions = buildForegroundSessions(listOf(resumed(1_000)), nowMs = 4_000)
        assertEquals(listOf(ForegroundSession(1_000, 4_000, ongoing = true)), sessions)
    }

    @Test
    fun `separate visits stay separate`() {
        val events = listOf(resumed(0), paused(10_000), otherApp(10_500), resumed(60_000), paused(70_000))
        val sessions = buildForegroundSessions(events, nowMs = 80_000)
        assertEquals(listOf(0L to 10_000L, 60_000L to 70_000L), sessions.map { it.startMs to it.endMs })
    }

    @Test
    fun `background time between visits is never counted`() {
        val events = listOf(resumed(0), paused(10_000), resumed(3_600_000), paused(3_610_000))
        val total = buildForegroundSessions(events, nowMs = 4_000_000).sumOf { it.endMs - it.startMs }
        assertEquals(20_000L, total)
    }

    @Test
    fun `importing again later gives the same session start`() {
        // Session starts are dedup keys in storage, so they must not depend on when the import runs.
        val early = buildForegroundSessions(listOf(resumed(1_000)), nowMs = 5_000)
        val later = buildForegroundSessions(listOf(resumed(1_000), paused(9_000), resumed(20_000)), nowMs = 25_000)
        assertEquals(early.first().startMs, later.first().startMs)
        assertEquals(9_000L, later.first().endMs)
        assertTrue(later.last().ongoing)
    }

    @Test
    fun `events out of order are sorted first`() {
        val sessions = buildForegroundSessions(listOf(paused(9_000), resumed(1_000)), nowMs = 10_000)
        assertEquals(listOf(ForegroundSession(1_000, 9_000, ongoing = false)), sessions)
    }

    private fun stopped(t: Long, activity: String = "Main") = ForegroundEvent(t, Kind.TARGET_STOPPED, activity)

    @Test
    fun `on Android 10+ a dialog pausing the app does not end the session`() {
        // A permission prompt pauses the app (its own RESUMED is filtered out as a system dialog), then the app resumes.
        val events = listOf(resumed(0), paused(1_000), resumed(5_000), paused(70_000), otherApp(70_100), stopped(70_500))
        val sessions = buildForegroundSessions(events, nowMs = 80_000, pauseEndsSession = false)
        assertEquals(listOf(ForegroundSession(0, 70_100, ongoing = false)), sessions)
    }

    @Test
    fun `on Android 10+ a stop ends the session even without another app resuming`() {
        val events = listOf(resumed(0), paused(9_000), stopped(9_400))
        assertEquals(listOf(ForegroundSession(0, 9_400, ongoing = false)), buildForegroundSessions(events, 20_000, pauseEndsSession = false))
    }

    @Test
    fun `on Android 10+ moving between the app's own activities stays one session`() {
        val events = listOf(resumed(0, "A"), paused(5_000, "A"), resumed(5_100, "B"), stopped(5_600, "A"), stopped(9_000, "B"))
        assertEquals(listOf(ForegroundSession(0, 9_000, ongoing = false)), buildForegroundSessions(events, 20_000, pauseEndsSession = false))
    }
}
