package com.sunpawtechnologies.scrollless.permissions

import android.Manifest
import android.accessibilityservice.AccessibilityServiceInfo
import android.app.AppOpsManager
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.Process
import android.os.SystemClock
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import com.sunpawtechnologies.scrollless.domain.model.PermissionSnapshot
import com.sunpawtechnologies.scrollless.domain.repository.PermissionMonitor
import com.sunpawtechnologies.scrollless.tracking.TrackingLog
import com.sunpawtechnologies.scrollless.tracking.YOUTUBE_PACKAGE
import com.sunpawtechnologies.scrollless.tracking.accessibility.ScrollLessAccessibilityService
import com.sunpawtechnologies.scrollless.tracking.instagram.INSTAGRAM_PACKAGE
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Reads permission state from Android. It reports only what the system confirms and never
 * assumes a permission was granted because the user visited a settings screen.
 */
class AndroidPermissionMonitor(context: Context) : PermissionMonitor {

    private val appContext = context.applicationContext
    private val serviceComponent = ComponentName(appContext, ScrollLessAccessibilityService::class.java)

    /** Set by the service itself, which runs in this process. */
    @Volatile
    private var serviceConnected = false

    private val _state = MutableStateFlow(read())
    override val state: StateFlow<PermissionSnapshot> = _state.asStateFlow()

    override fun refresh() {
        val previous = _state.value
        val current = read()
        if (current != previous) {
            TrackingLog.i(
                "permission_state_changed",
                "usage_access" to current.usageAccessGranted,
                "accessibility_enabled" to current.accessibilityServiceEnabled,
                "accessibility_running" to current.accessibilityServiceRunning,
                "instagram_installed" to current.instagramInstalled,
                "youtube_installed" to current.youtubeInstalled,
                "battery_unrestricted" to current.ignoringBatteryOptimizations,
            )
        }
        _state.value = current
    }

    /** When the service last disconnected, to tell a brief system rebind from a real stop. */
    @Volatile
    private var disconnectedAtMs = 0L
    private val handler = Handler(Looper.getMainLooper())

    fun onServiceConnectionChanged(connected: Boolean) {
        serviceConnected = connected
        if (!connected) {
            disconnectedAtMs = SystemClock.elapsedRealtime()
            // Re-check once the grace period ends, so a real stop is reported without waiting for a resume.
            handler.postDelayed(::refresh, REBIND_GRACE_MS + 100)
        }
        refresh()
    }

    private fun read(): PermissionSnapshot {
        val enabled = isServiceEnabledInSettings()
        // Android sometimes unbinds and rebinds an enabled service within a second or two (seen when
        // leaving Accessibility settings). Treat that window as running instead of flashing "stopped".
        val restarting = enabled && SystemClock.elapsedRealtime() - disconnectedAtMs < REBIND_GRACE_MS
        return PermissionSnapshot(
            usageAccessGranted = hasUsageAccess(),
            accessibilityServiceEnabled = enabled,
            accessibilityServiceRunning = serviceConnected || restarting || isServiceBoundBySystem(),
            instagramInstalled = isInstalled(INSTAGRAM_PACKAGE),
            youtubeInstalled = isInstalled(YOUTUBE_PACKAGE),
            ignoringBatteryOptimizations = appContext.getSystemService(PowerManager::class.java)
                .isIgnoringBatteryOptimizations(appContext.packageName),
            developerModeEnabled = isGlobalSettingOn(Settings.Global.DEVELOPMENT_SETTINGS_ENABLED) ||
                isGlobalSettingOn(Settings.Global.ADB_ENABLED),
        )
    }

    /** Reads (never writes) a public system setting. */
    private fun isGlobalSettingOn(name: String): Boolean =
        Settings.Global.getInt(appContext.contentResolver, name, 0) != 0

    private fun hasUsageAccess(): Boolean {
        val appOps = appContext.getSystemService(AppOpsManager::class.java)
        // Both checks are deprecated in the newest SDK but are still how this app-op is read on
        // the Android versions this app supports.
        @Suppress("DEPRECATION")
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), appContext.packageName)
        } else {
            appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), appContext.packageName)
        }
        return when (mode) {
            AppOpsManager.MODE_ALLOWED -> true
            // MODE_DEFAULT means "decided by the permission grant" on some devices.
            AppOpsManager.MODE_DEFAULT ->
                appContext.checkSelfPermission(Manifest.permission.PACKAGE_USAGE_STATS) == PackageManager.PERMISSION_GRANTED
            else -> false
        }
    }

    /** The user's on/off switch, stored by the system as a colon-separated component list. */
    fun isServiceEnabledInSettings(): Boolean {
        val enabled = Settings.Secure.getString(
            appContext.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
        ) ?: return false
        return enabled.split(':').any { ComponentName.unflattenFromString(it) == serviceComponent }
    }

    /** Services Android has actually bound. This can lag behind the settings switch. */
    private fun isServiceBoundBySystem(): Boolean {
        val manager = appContext.getSystemService(AccessibilityManager::class.java)
        return manager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
            .any { it.resolveInfo.serviceInfo.let { info -> ComponentName(info.packageName, info.name) == serviceComponent } }
    }

    /** Needs the <queries> entry in the manifest on Android 11+. */
    fun isInstalled(packageName: String): Boolean = try {
        appContext.packageManager.getApplicationInfo(packageName, 0).enabled
    } catch (_: PackageManager.NameNotFoundException) {
        false
    }

    private companion object {
        const val REBIND_GRACE_MS = 5_000L
    }
}
