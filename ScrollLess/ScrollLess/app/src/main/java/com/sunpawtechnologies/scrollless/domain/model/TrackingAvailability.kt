package com.sunpawtechnologies.scrollless.domain.model

enum class SetupRequirement { USAGE_ACCESS, ACCESSIBILITY_SERVICE, ACCESSIBILITY_CONSENT }

enum class UnavailableReason { INSTAGRAM_NOT_INSTALLED, YOUTUBE_NOT_INSTALLED, SERVICE_NOT_RUNNING }

/** Whether a metric can be measured right now. Anything other than [Active] must not be shown as zero. */
sealed interface TrackingAvailability {
    data object Active : TrackingAvailability

    /** The user still has to grant or accept something. */
    data class NotConfigured(val requirement: SetupRequirement) : TrackingAvailability

    /** Set up, but measurement can't happen for a reason outside the user's setup. */
    data class Unavailable(val reason: UnavailableReason) : TrackingAvailability
}
