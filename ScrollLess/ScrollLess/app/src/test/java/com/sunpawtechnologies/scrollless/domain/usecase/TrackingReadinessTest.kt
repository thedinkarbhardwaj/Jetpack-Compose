package com.sunpawtechnologies.scrollless.domain.usecase

import com.sunpawtechnologies.scrollless.domain.model.PermissionSnapshot
import com.sunpawtechnologies.scrollless.domain.model.SetupRequirement
import com.sunpawtechnologies.scrollless.domain.model.TrackingAvailability
import com.sunpawtechnologies.scrollless.domain.model.UnavailableReason
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import com.sunpawtechnologies.scrollless.permissions.batteryGuideUrl
import org.junit.Test

class TrackingReadinessTest {

    private val allGranted = PermissionSnapshot(
        usageAccessGranted = true,
        accessibilityServiceEnabled = true,
        accessibilityServiceRunning = true,
        instagramInstalled = true,
    )

    @Test
    fun `everything granted means both metrics are active`() {
        assertEquals(TrackingAvailability.Active, TrackingReadiness.instagramTime(allGranted))
        assertEquals(TrackingAvailability.Active, TrackingReadiness.instagramReels(allGranted, consentGiven = true))
    }

    @Test
    fun `missing usage access only affects instagram time`() {
        val permissions = allGranted.copy(usageAccessGranted = false)
        assertEquals(
            TrackingAvailability.NotConfigured(SetupRequirement.USAGE_ACCESS),
            TrackingReadiness.instagramTime(permissions),
        )
        assertEquals(TrackingAvailability.Active, TrackingReadiness.instagramReels(permissions, consentGiven = true))
    }

    @Test
    fun `reels need the service switched on`() {
        assertEquals(
            TrackingAvailability.NotConfigured(SetupRequirement.ACCESSIBILITY_SERVICE),
            TrackingReadiness.instagramReels(allGranted.copy(accessibilityServiceEnabled = false), consentGiven = true),
        )
    }

    @Test
    fun `reels need consent even if the service was enabled directly in settings`() {
        assertEquals(
            TrackingAvailability.NotConfigured(SetupRequirement.ACCESSIBILITY_CONSENT),
            TrackingReadiness.instagramReels(allGranted, consentGiven = false),
        )
    }

    @Test
    fun `an enabled but stopped service is unavailable, not zero`() {
        assertEquals(
            TrackingAvailability.Unavailable(UnavailableReason.SERVICE_NOT_RUNNING),
            TrackingReadiness.instagramReels(allGranted.copy(accessibilityServiceRunning = false), consentGiven = true),
        )
    }

    @Test
    fun `nothing can be measured without instagram`() {
        val permissions = allGranted.copy(instagramInstalled = false)
        val expected = TrackingAvailability.Unavailable(UnavailableReason.INSTAGRAM_NOT_INSTALLED)
        assertEquals(expected, TrackingReadiness.instagramTime(permissions))
        assertEquals(expected, TrackingReadiness.instagramReels(permissions, consentGiven = true))
    }

    @Test
    fun `basic only is never reported as a problem`() {
        val basic = allGranted.copy(accessibilityServiceEnabled = false, accessibilityServiceRunning = false)
        assertEquals(TrackingMode.BASIC, TrackingReadiness.mode(basic, consentGiven = false))
        assertNull(TrackingReadiness.issue(basic, consentGiven = false, everOptedIn = false))
    }

    @Test
    fun `issues are reported one at a time, most actionable first`() {
        assertEquals(
            TrackingIssue.ADVANCED_TURNED_OFF,
            TrackingReadiness.issue(allGranted.copy(accessibilityServiceEnabled = false), consentGiven = true, everOptedIn = true),
        )
        assertEquals(
            TrackingIssue.ADVANCED_STOPPED,
            TrackingReadiness.issue(allGranted.copy(accessibilityServiceRunning = false), consentGiven = true, everOptedIn = true),
        )
        assertEquals(
            TrackingIssue.ADVANCED_NEEDS_CONSENT,
            TrackingReadiness.issue(allGranted, consentGiven = false, everOptedIn = true),
        )
        assertEquals(
            TrackingIssue.USAGE_ACCESS_MISSING,
            TrackingReadiness.issue(allGranted.copy(usageAccessGranted = false), consentGiven = true, everOptedIn = true),
        )
        assertNull(TrackingReadiness.issue(allGranted, consentGiven = true, everOptedIn = true))
    }

    @Test
    fun `youtube time needs only usage access`() {
        val basicOnly = PermissionSnapshot(usageAccessGranted = true, youtubeInstalled = true)
        assertEquals(TrackingAvailability.Active, TrackingReadiness.youtubeTime(basicOnly))
        assertEquals(
            TrackingAvailability.Unavailable(UnavailableReason.YOUTUBE_NOT_INSTALLED),
            TrackingReadiness.youtubeTime(basicOnly.copy(youtubeInstalled = false)),
        )
    }

    @Test
    fun `battery guide exists only for brands known to stop apps`() {
        assertEquals("https://dontkillmyapp.com/xiaomi", batteryGuideUrl("Redmi"))
        assertEquals("https://dontkillmyapp.com/samsung", batteryGuideUrl("samsung"))
        assertNull(batteryGuideUrl("Google"))
    }
}
