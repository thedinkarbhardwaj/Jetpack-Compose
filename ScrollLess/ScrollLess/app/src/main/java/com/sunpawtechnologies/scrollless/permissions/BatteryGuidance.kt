package com.sunpawtechnologies.scrollless.permissions

/**
 * Manufacturers whose battery managers commonly stop background apps or unbind accessibility
 * services, mapped to their page on dontkillmyapp.com (a community-maintained guide with
 * per-brand steps). Following those steps helps but guarantees nothing.
 */
private val AGGRESSIVE_BRANDS = mapOf(
    "xiaomi" to "xiaomi", "redmi" to "xiaomi", "poco" to "xiaomi",
    "oppo" to "oppo", "realme" to "realme", "oneplus" to "oneplus",
    "vivo" to "vivo", "iqoo" to "vivo",
    "huawei" to "huawei", "honor" to "honor",
    "samsung" to "samsung", "asus" to "asus", "meizu" to "meizu",
    "tecno" to "tecno", "infinix" to "infinix", "motorola" to "motorola",
)

/** The dontkillmyapp.com page for this manufacturer, or null if it isn't known to be aggressive. */
fun batteryGuideUrl(manufacturer: String): String? =
    AGGRESSIVE_BRANDS[manufacturer.trim().lowercase()]?.let { "https://dontkillmyapp.com/$it" }
