package com.sunpawtechnologies.scrollless.domain.policy

import com.sunpawtechnologies.scrollless.domain.model.DEFAULT_SENSITIVE_APPS
import com.sunpawtechnologies.scrollless.tracking.instagram.INSTAGRAM_PACKAGE
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PaymentAppSafetyTest {

    private val showing = CounterInputs(
        foregroundPackage = INSTAGRAM_PACKAGE,
        customExcludedPackages = emptySet(),
        consentGiven = true,
        counterEnabled = true,
        foregroundUncertain = false,
        hiddenForSession = false,
    )

    @Test
    fun `the counter shows over instagram when everything allows it`() {
        assertTrue(PaymentAppSafety.shouldShowCounter(showing))
    }

    @Test
    fun `the counter never shows over any built-in payment or banking app`() {
        DEFAULT_SENSITIVE_APPS.keys.forEach { payment ->
            assertFalse(payment, PaymentAppSafety.shouldShowCounter(showing.copy(foregroundPackage = payment)))
            assertTrue(payment, PaymentAppSafety.isProtected(payment, emptySet()))
        }
    }

    @Test
    fun `the payment apps found on the test phone are protected`() {
        listOf(
            "net.one97.paytm", // Paytm
            "com.google.android.apps.nbu.paisa.user", // Google Pay
            "com.sbi.lotusintouch", // SBI YONO
            "com.csam.icici.bank.imobile", // ICICI iMobile
        ).forEach { assertTrue(it, PaymentAppSafety.isProtected(it, emptySet())) }
    }

    @Test
    fun `user-added apps are protected too`() {
        val custom = setOf("com.example.bank")
        assertTrue(PaymentAppSafety.isProtected("com.example.bank", custom))
        assertFalse(PaymentAppSafety.shouldShowCounter(showing.copy(foregroundPackage = "com.example.bank", customExcludedPackages = custom)))
    }

    @Test
    fun `instagram added as protected hides the counter, which is also safe`() {
        assertFalse(PaymentAppSafety.shouldShowCounter(showing.copy(customExcludedPackages = setOf(INSTAGRAM_PACKAGE))))
    }

    @Test
    fun `other apps and unknown foreground never show the counter`() {
        assertFalse(PaymentAppSafety.shouldShowCounter(showing.copy(foregroundPackage = "com.whatsapp")))
        assertFalse(PaymentAppSafety.shouldShowCounter(showing.copy(foregroundPackage = null)))
        assertFalse(PaymentAppSafety.isProtected(null, emptySet()))
        assertFalse(PaymentAppSafety.isProtected("com.whatsapp", emptySet()))
    }

    @Test
    fun `when in doubt the counter hides`() {
        assertFalse(PaymentAppSafety.shouldShowCounter(showing.copy(foregroundUncertain = true)))
        assertFalse(PaymentAppSafety.shouldShowCounter(showing.copy(consentGiven = false)))
        assertFalse(PaymentAppSafety.shouldShowCounter(showing.copy(counterEnabled = false)))
        assertFalse(PaymentAppSafety.shouldShowCounter(showing.copy(hiddenForSession = true)))
    }
}
