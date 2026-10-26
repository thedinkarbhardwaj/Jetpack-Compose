package com.sunpawtechnologies.scrollless.tracking.accessibility

import android.accessibilityservice.AccessibilityService
import com.sunpawtechnologies.scrollless.tracking.TrackingLog
import java.lang.ref.WeakReference

/** Lets the app turn its own Reels tracking off without sending the user to system settings. */
fun interface ReelsTrackingSwitch {
    /**
     * Turns off ScrollLess's Accessibility service. Returns false if the service isn't running
     * in this process (e.g. switched on but stopped by Android), in which case only the user can
     * switch it off, in Accessibility settings.
     */
    fun turnOff(): Boolean
}

/**
 * Holds the running service. [AccessibilityService.disableSelf] removes only ScrollLess's own
 * entry from the enabled-services list; other apps' services and their settings are untouched.
 * Turning it back on always goes through Android's Accessibility settings.
 */
class AccessibilityReelsTrackingSwitch : ReelsTrackingSwitch {
    @Volatile
    private var service: WeakReference<AccessibilityService>? = null

    fun attach(service: AccessibilityService) {
        this.service = WeakReference(service)
    }

    fun detach(service: AccessibilityService) {
        if (this.service?.get() === service) this.service = null
    }

    override fun turnOff(): Boolean {
        val running = service?.get() ?: return false
        TrackingLog.i("reels_tracking_turned_off_in_app")
        running.disableSelf()
        return true
    }
}
