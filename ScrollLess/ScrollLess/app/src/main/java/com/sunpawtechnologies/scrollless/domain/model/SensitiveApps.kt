package com.sunpawtechnologies.scrollless.domain.model

/**
 * Payment and banking apps the Instagram counter must never appear over.
 *
 * Package names come from public UPI app listings (e.g. Razorpay's supported-apps table). Each
 * entry is also listed under <queries> in the manifest, so Settings can show which are actually
 * installed. Users can add more in Settings.
 */
val DEFAULT_SENSITIVE_APPS: Map<String, String> = linkedMapOf(
    "com.google.android.apps.nbu.paisa.user" to "Google Pay",
    "net.one97.paytm" to "Paytm",
    "com.paytm.business" to "Paytm for Business",
    "com.phonepe.app" to "PhonePe",
    "in.org.npci.upiapp" to "BHIM",
    "com.dreamplug.androidapp" to "CRED",
    "in.amazon.mShop.android.shopping" to "Amazon (Pay)",
    "com.mobikwik_new" to "MobiKwik",
    "com.google.android.apps.walletnfcrel" to "Google Wallet",
    "com.sbi.lotusintouch" to "SBI YONO",
    "com.snapwork.hdfc" to "HDFC Bank",
    "com.csam.icici.bank.imobile" to "ICICI iMobile",
    "com.axis.mobile" to "Axis Mobile",
    "com.msf.kbank.mobile" to "Kotak Bank",
)

/** Android package-name syntax, used to validate packages the user adds. */
private val PACKAGE_NAME = Regex("^[a-zA-Z][a-zA-Z0-9_]*(\\.[a-zA-Z][a-zA-Z0-9_]*)+$")

fun isValidPackageName(text: String): Boolean = PACKAGE_NAME.matches(text.trim())
