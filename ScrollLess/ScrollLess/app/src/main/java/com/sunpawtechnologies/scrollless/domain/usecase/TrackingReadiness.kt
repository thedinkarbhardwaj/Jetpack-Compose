package com.sunpawtechnologies.scrollless.domain.usecase

import com.sunpawtechnologies.scrollless.domain.model.PermissionSnapshot
import com.sunpawtechnologies.scrollless.domain.model.SetupRequirement
import com.sunpawtechnologies.scrollless.domain.model.TrackingAvailability
import com.sunpawtechnologies.scrollless.domain.model.UnavailableReason

/**
 * - BASIC: Usage Access only. App time for Instagram and YouTube; no Reel counts.
 * - ADVANCED: also the Accessibility service: estimated Reels and the Instagram counter.
 */
enum class TrackingMode { NONE, BASIC, ADVANCED }

/** The single most useful thing to tell the user, each with one fix. Null means nothing is wrong. */
enum class TrackingIssue {
    INSTAGRAM_NOT_INSTALLED,

    /** Reels tracking is switched on but Android isn't running it (battery saver, OEM task killer). */
    ADVANCED_STOPPED,

    /** The user opted in to Reels tracking earlier, but it's now switched off (e.g. after an app update). */
    ADVANCED_TURNED_OFF,

    /** The service is on, but the current disclosure hasn't been accepted, so nothing is counted. */
    ADVANCED_NEEDS_CONSENT,

    /** Not even Basic tracking is set up. */
    USAGE_ACCESS_MISSING,
}

/** Decides what can be measured, from permission state and consent. Pure, so every case is tested. */
object TrackingReadiness {

    /** Instagram foreground time comes from Usage Access. */
    fun instagramTime(permissions: PermissionSnapshot): TrackingAvailability = when {
        !permissions.instagramInstalled -> TrackingAvailability.Unavailable(UnavailableReason.INSTAGRAM_NOT_INSTALLED)
        !permissions.usageAccessGranted -> TrackingAvailability.NotConfigured(SetupRequirement.USAGE_ACCESS)
        else -> TrackingAvailability.Active
    }

    /** YouTube foreground time (all content: Shorts and regular videos together) comes from Usage Access. */
    fun youtubeTime(permissions: PermissionSnapshot): TrackingAvailability = when {
        !permissions.youtubeInstalled -> TrackingAvailability.Unavailable(UnavailableReason.YOUTUBE_NOT_INSTALLED)
        !permissions.usageAccessGranted -> TrackingAvailability.NotConfigured(SetupRequirement.USAGE_ACCESS)
        else -> TrackingAvailability.Active
    }

    /** Reels estimates come from the Accessibility service, which only counts after in-app consent. */
    fun instagramReels(permissions: PermissionSnapshot, consentGiven: Boolean): TrackingAvailability = when {
        !permissions.instagramInstalled -> TrackingAvailability.Unavailable(UnavailableReason.INSTAGRAM_NOT_INSTALLED)
        !permissions.accessibilityServiceEnabled -> TrackingAvailability.NotConfigured(SetupRequirement.ACCESSIBILITY_SERVICE)
        !consentGiven -> TrackingAvailability.NotConfigured(SetupRequirement.ACCESSIBILITY_CONSENT)
        !permissions.accessibilityServiceRunning -> TrackingAvailability.Unavailable(UnavailableReason.SERVICE_NOT_RUNNING)
        else -> TrackingAvailability.Active
    }

    fun mode(permissions: PermissionSnapshot, consentGiven: Boolean): TrackingMode = when {
        permissions.accessibilityServiceEnabled && consentGiven && permissions.accessibilityServiceRunning -> TrackingMode.ADVANCED
        permissions.usageAccessGranted -> TrackingMode.BASIC
        else -> TrackingMode.NONE
    }

    /**
     * @param consentGiven the current disclosure version was accepted.
     * @param everOptedIn any disclosure version was accepted, i.e. the user chose Reels tracking
     *   at some point. Choosing Basic only is never reported as a problem.
     */
    fun issue(permissions: PermissionSnapshot, consentGiven: Boolean, everOptedIn: Boolean): TrackingIssue? = when {
        !permissions.instagramInstalled && !permissions.youtubeInstalled -> TrackingIssue.INSTAGRAM_NOT_INSTALLED
        permissions.accessibilityServiceEnabled && !consentGiven -> TrackingIssue.ADVANCED_NEEDS_CONSENT
        permissions.accessibilityServiceEnabled && !permissions.accessibilityServiceRunning -> TrackingIssue.ADVANCED_STOPPED
        everOptedIn && !permissions.accessibilityServiceEnabled -> TrackingIssue.ADVANCED_TURNED_OFF
        !permissions.usageAccessGranted -> TrackingIssue.USAGE_ACCESS_MISSING
        else -> null
    }
}
