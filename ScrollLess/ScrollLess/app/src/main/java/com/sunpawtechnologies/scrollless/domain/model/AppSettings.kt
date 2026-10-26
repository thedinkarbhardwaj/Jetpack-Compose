package com.sunpawtechnologies.scrollless.domain.model

import java.time.Instant

enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** Record of the user accepting the in-app Accessibility disclosure, required by Play policy. */
data class AccessibilityConsent(
    val disclosureVersion: Int,
    val acceptedAt: Instant,
)

/**
 * Where the user dragged the Instagram counter, as fractions (0..1) of the usable screen width
 * and height, so it lands in the same relative spot on any screen size or density.
 */
data class OverlayPosition(val xFraction: Float, val yFraction: Float) {
    init {
        require(xFraction in 0f..1f && yFraction in 0f..1f) { "Position fractions must be in 0..1" }
    }
}

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val onboardingCompleted: Boolean = false,
    val accessibilityConsent: AccessibilityConsent? = null,
    /** Off by default: the counter is optional and needs the service to observe other apps. */
    val overlayEnabled: Boolean = false,
    /** Null until the user drags the counter; the overlay then uses its default spot. */
    val overlayPosition: OverlayPosition? = null,
    val warningThresholdPercent: Int = DEFAULT_WARNING_THRESHOLD_PERCENT,
    /** Extra packages, added by the user, the counter must never appear over. */
    val customExcludedPackages: Set<String> = emptySet(),
    /** Limits the user switched off: still measured and shown, but no ring, status or alert. */
    val disabledLimits: Set<LimitMetric> = emptySet(),
    /**
     * The user turned Reels tracking off from inside ScrollLess (e.g. for a payment app), so its
     * absence is their choice, not a problem to report. Cleared when the service runs again.
     */
    val reelsTrackingTurnedOffByUser: Boolean = false,
    /** "Limit reached" notices the user already dismissed, as [limitAlertKey]s. Only today's are kept. */
    val acknowledgedLimitAlerts: Set<String> = emptySet(),
)

/** Identifies one metric's limit on one local day, e.g. "20750:INSTAGRAM_APP_TIME". */
fun limitAlertKey(epochDay: Long, metric: LimitMetric): String = "$epochDay:${metric.name}"

fun AppSettings.isLimitEnabled(metric: LimitMetric): Boolean = metric.measurable && metric !in disabledLimits

/**
 * Bump when the disclosure text changes materially, so users are asked to agree again.
 * v2: the service also notices which app is in front, to show the counter only in Instagram.
 * v3: explains that some payment and banking apps restrict use while any third-party
 *     Accessibility service is on, and how to turn Reels tracking off.
 */
const val ACCESSIBILITY_DISCLOSURE_VERSION = 3

const val DEFAULT_WARNING_THRESHOLD_PERCENT = 80
val WARNING_THRESHOLD_RANGE = 50..95

/** True if the user accepted the current version of the Accessibility disclosure. */
fun AppSettings.hasCurrentAccessibilityConsent(): Boolean =
    (accessibilityConsent?.disclosureVersion ?: 0) >= ACCESSIBILITY_DISCLOSURE_VERSION
