package com.sunpawtechnologies.scrollless.domain.policy

import com.sunpawtechnologies.scrollless.domain.model.DEFAULT_SENSITIVE_APPS
import com.sunpawtechnologies.scrollless.tracking.instagram.INSTAGRAM_PACKAGE

/** Everything the counter decision depends on, so the rule can be tested without a device. */
data class CounterInputs(
    /** The app believed to be in front; null when unknown (e.g. screen off). */
    val foregroundPackage: String?,
    val customExcludedPackages: Set<String>,
    val consentGiven: Boolean,
    val counterEnabled: Boolean,
    /** The active-window check couldn't confirm which app is in front. */
    val foregroundUncertain: Boolean,
    /** The user long-pressed the counter during this Instagram visit. */
    val hiddenForSession: Boolean,
)

/**
 * The enforcement-path rules that keep ScrollLess away from payment and banking apps.
 *
 * The Accessibility service calls these on every decision, so the exclusion list is enforced
 * where it matters, not only shown in Settings. When in doubt, the counter is hidden.
 */
object PaymentAppSafety {

    /** A built-in payment/banking app or one the user added. Its events are never inspected. */
    fun isProtected(packageName: String?, customExcludedPackages: Set<String>): Boolean =
        packageName != null && (packageName in DEFAULT_SENSITIVE_APPS || packageName in customExcludedPackages)

    /**
     * The counter shows only over Instagram, only once the user agreed and switched it on, and
     * never while the foreground app is uncertain or protected. Instagram itself can't be
     * protected: a user adding it would simply never see the counter, which is also safe.
     */
    fun shouldShowCounter(inputs: CounterInputs): Boolean = with(inputs) {
        consentGiven &&
            counterEnabled &&
            !hiddenForSession &&
            !foregroundUncertain &&
            foregroundPackage == INSTAGRAM_PACKAGE &&
            !isProtected(foregroundPackage, customExcludedPackages)
    }
}
