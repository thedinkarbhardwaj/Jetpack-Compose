package com.sunpawtechnologies.scrollless.tracking

import com.sunpawtechnologies.scrollless.tracking.instagram.INSTAGRAM_PACKAGE

/**
 * Works out which app is in front from window-state events, using package names only.
 *
 * Windows that sit on top of an app without replacing it (the notification shade, system
 * dialogs, the keyboard, and the counter overlay itself) are ignored, so they don't hide the
 * counter. Any event from Instagram re-asserts that Instagram is in front.
 */
class ForegroundAppTracker(
    private val ownPackage: String,
    /** The only ScrollLess window that means "the user opened ScrollLess". */
    private val ownActivityClass: String,
    private val overlayPackages: () -> Set<String> = { emptySet() },
) {
    var foregroundPackage: String? = null
        private set

    val isInstagramForeground: Boolean get() = foregroundPackage == INSTAGRAM_PACKAGE

    /** Returns true if the foreground app changed. */
    fun onWindowStateChanged(packageName: String?, className: String?): Boolean {
        if (packageName == null || packageName in SYSTEM_OVERLAY_PACKAGES || packageName in overlayPackages()) return false
        // The counter overlay belongs to this package; only the real app screen counts.
        if (packageName == ownPackage && className != ownActivityClass) return false
        return setForeground(packageName)
    }

    /**
     * The package of the window the user is actually interacting with, from a direct check.
     * Unlike window events, this sees apps whose events are hidden from accessibility services.
     * Returns true if the foreground app changed.
     */
    fun onActiveWindowObserved(packageName: String): Boolean {
        if (packageName in SYSTEM_OVERLAY_PACKAGES || packageName in overlayPackages()) return false
        return setForeground(packageName)
    }

    /** Returns true if the foreground app changed. */
    fun onInstagramEvent(): Boolean = setForeground(INSTAGRAM_PACKAGE)

    /** Returns true if the foreground app changed. */
    fun onScreenOff(): Boolean = setForeground(null)

    private fun setForeground(packageName: String?): Boolean {
        if (packageName == foregroundPackage) return false
        foregroundPackage = packageName
        return true
    }

    private companion object {
        /** Shade, quick settings, volume panel, share sheets and permission dialogs. */
        val SYSTEM_OVERLAY_PACKAGES = setOf("com.android.systemui", "android")
    }
}
