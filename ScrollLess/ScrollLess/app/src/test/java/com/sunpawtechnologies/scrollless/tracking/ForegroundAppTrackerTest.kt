package com.sunpawtechnologies.scrollless.tracking

import com.sunpawtechnologies.scrollless.tracking.instagram.INSTAGRAM_PACKAGE
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ForegroundAppTrackerTest {

    private val own = "com.sunpawtechnologies.scrollless"
    private val mainActivity = "$own.MainActivity"
    private val tracker = ForegroundAppTracker(own, mainActivity) { setOf("com.google.android.inputmethod.latin") }

    @Test
    fun `opening instagram makes it the foreground app`() {
        assertTrue(tracker.onWindowStateChanged(INSTAGRAM_PACKAGE, "com.instagram.mainactivity.MainActivity"))
        assertTrue(tracker.isInstagramForeground)
    }

    @Test
    fun `switching to another app leaves instagram`() {
        tracker.onWindowStateChanged(INSTAGRAM_PACKAGE, null)
        assertTrue(tracker.onWindowStateChanged("com.android.launcher3", "Launcher"))
        assertFalse(tracker.isInstagramForeground)
    }

    @Test
    fun `notification shade, system dialogs and keyboard do not hide the counter`() {
        tracker.onWindowStateChanged(INSTAGRAM_PACKAGE, null)
        assertFalse(tracker.onWindowStateChanged("com.android.systemui", "NotificationShade"))
        assertFalse(tracker.onWindowStateChanged("android", "ChooserActivity"))
        assertFalse(tracker.onWindowStateChanged("com.google.android.inputmethod.latin", "Keyboard"))
        assertTrue(tracker.isInstagramForeground)
    }

    @Test
    fun `the counter's own window is ignored but opening ScrollLess is not`() {
        tracker.onWindowStateChanged(INSTAGRAM_PACKAGE, null)
        assertFalse(tracker.onWindowStateChanged(own, "android.widget.LinearLayout"))
        assertTrue(tracker.isInstagramForeground)

        assertTrue(tracker.onWindowStateChanged(own, mainActivity))
        assertFalse(tracker.isInstagramForeground)
    }

    @Test
    fun `screen off leaves instagram and any instagram event brings it back`() {
        tracker.onWindowStateChanged(INSTAGRAM_PACKAGE, null)
        assertTrue(tracker.onScreenOff())
        assertFalse(tracker.isInstagramForeground)

        assertTrue(tracker.onInstagramEvent())
        assertTrue(tracker.isInstagramForeground)
        assertFalse(tracker.onInstagramEvent()) // no repeated "changed" for the same app
    }

    @Test
    fun `events without a package are ignored`() {
        tracker.onWindowStateChanged(INSTAGRAM_PACKAGE, null)
        assertFalse(tracker.onWindowStateChanged(null, null))
        assertEquals(INSTAGRAM_PACKAGE, tracker.foregroundPackage)
    }

    @Test
    fun `a direct active-window check catches apps whose events are hidden`() {
        tracker.onWindowStateChanged(INSTAGRAM_PACKAGE, null)
        // e.g. a payment app whose window events never reach accessibility services
        assertTrue(tracker.onActiveWindowObserved("com.google.android.apps.nbu.paisa.user"))
        assertFalse(tracker.isInstagramForeground)
    }

    @Test
    fun `active-window check ignores the shade and keyboard`() {
        tracker.onWindowStateChanged(INSTAGRAM_PACKAGE, null)
        assertFalse(tracker.onActiveWindowObserved("com.android.systemui"))
        assertFalse(tracker.onActiveWindowObserved("com.google.android.inputmethod.latin"))
        assertTrue(tracker.isInstagramForeground)
    }
}
