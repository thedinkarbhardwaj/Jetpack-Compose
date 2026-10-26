package com.sunpawtechnologies.scrollless.domain.model

/** What Android currently reports about the permissions and apps tracking depends on. */
data class PermissionSnapshot(
    val usageAccessGranted: Boolean = false,
    /** The user switched the service on in Accessibility settings. */
    val accessibilityServiceEnabled: Boolean = false,
    /** Android has actually bound the service. It can be enabled but stopped, e.g. by battery savers. */
    val accessibilityServiceRunning: Boolean = false,
    val instagramInstalled: Boolean = false,
    val youtubeInstalled: Boolean = false,
    /** False means Android may delay or stop ScrollLess in the background to save battery. */
    val ignoringBatteryOptimizations: Boolean = false,
    /**
     * Developer options or USB/Wireless debugging are on. Read only, never changed. Some payment
     * apps (seen with Paytm and SBI YONO) refuse to open while these are on, with or without ScrollLess.
     */
    val developerModeEnabled: Boolean = false,
)
