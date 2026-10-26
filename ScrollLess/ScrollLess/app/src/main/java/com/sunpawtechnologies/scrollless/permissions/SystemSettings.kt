package com.sunpawtechnologies.scrollless.permissions

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.core.net.toUri
import com.sunpawtechnologies.scrollless.tracking.TrackingLog

enum class SettingsTarget { USAGE_ACCESS, ACCESSIBILITY, APP_DETAILS, BATTERY, NOTIFICATIONS, DEVELOPER_OPTIONS }

/**
 * Opens the system screen where the user grants a permission, trying the most specific screen
 * first. Returns false if no screen could be opened, so the UI can explain the manual path.
 */
fun Context.openSystemSettings(target: SettingsTarget): Boolean {
    val packageUri = "package:$packageName".toUri()
    val candidates = when (target) {
        // The package URI jumps straight to this app on Android 10+; some devices only support the list.
        SettingsTarget.USAGE_ACCESS -> listOf(
            Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS, packageUri),
            Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS),
        )
        SettingsTarget.ACCESSIBILITY -> listOf(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        SettingsTarget.APP_DETAILS -> listOf(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, packageUri))
        // The battery list where the user can choose "Unrestricted"; App info as fallback.
        // (No direct "ignore optimizations" request: that needs a policy-restricted permission.)
        SettingsTarget.BATTERY -> listOf(
            Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS),
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, packageUri),
        )
        // This app's notification switches (Android 8+); App info as fallback.
        SettingsTarget.NOTIFICATIONS -> listOfNotNull(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
            } else {
                null
            },
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, packageUri),
        )
        // Only opens the screen. ScrollLess never changes developer settings itself.
        SettingsTarget.DEVELOPER_OPTIONS -> listOf(
            Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS),
            Intent(Settings.ACTION_SETTINGS),
        )
    }
    for (intent in candidates) {
        try {
            startActivity(intent)
            TrackingLog.d("settings_opened", "target" to target, "action" to intent.action, "with_package" to (intent.data != null))
            return true
        } catch (_: ActivityNotFoundException) {
            TrackingLog.d("settings_intent_unavailable", "target" to target, "with_package" to (intent.data != null))
        }
    }
    TrackingLog.i("settings_open_failed", "target" to target)
    return false
}

/** Opens a web page in the user's browser. Returns false if no browser is available. */
fun Context.openWebPage(url: String): Boolean = try {
    startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
    true
} catch (_: ActivityNotFoundException) {
    false
}
