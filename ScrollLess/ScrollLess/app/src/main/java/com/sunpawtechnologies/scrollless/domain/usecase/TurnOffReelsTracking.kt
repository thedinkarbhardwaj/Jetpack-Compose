package com.sunpawtechnologies.scrollless.domain.usecase

import com.sunpawtechnologies.scrollless.domain.repository.SettingsRepository
import com.sunpawtechnologies.scrollless.tracking.accessibility.ReelsTrackingSwitch

/**
 * Switches off ScrollLess's own Accessibility service, e.g. so a payment app that refuses to
 * run alongside Accessibility services works again. Records that this was the user's choice, so
 * the dashboard doesn't report it as a problem. App-time tracking and limits are unaffected.
 */
class TurnOffReelsTracking(
    private val settingsRepository: SettingsRepository,
    private val reelsTrackingSwitch: ReelsTrackingSwitch,
) {
    /**
     * @return true if it was switched off here; false if the service isn't running in this
     *   process, so the user has to switch it off in Accessibility settings.
     */
    suspend operator fun invoke(): Boolean {
        settingsRepository.setReelsTrackingTurnedOffByUser(true)
        return reelsTrackingSwitch.turnOff()
    }
}
