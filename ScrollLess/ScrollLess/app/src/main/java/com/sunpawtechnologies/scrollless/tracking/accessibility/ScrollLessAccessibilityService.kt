package com.sunpawtechnologies.scrollless.tracking.accessibility

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.view.inputmethod.InputMethodManager
import androidx.core.content.ContextCompat
import com.sunpawtechnologies.scrollless.MainActivity
import com.sunpawtechnologies.scrollless.ScrollLessApp
import com.sunpawtechnologies.scrollless.domain.model.AppSettings
import com.sunpawtechnologies.scrollless.domain.model.ContentKind
import com.sunpawtechnologies.scrollless.domain.model.CoverageEndReason
import com.sunpawtechnologies.scrollless.domain.model.LimitMetric
import com.sunpawtechnologies.scrollless.domain.model.OverlayPosition
import com.sunpawtechnologies.scrollless.domain.model.TrackingSource
import com.sunpawtechnologies.scrollless.domain.model.hasCurrentAccessibilityConsent
import com.sunpawtechnologies.scrollless.domain.model.limitStatus
import com.sunpawtechnologies.scrollless.domain.policy.CounterInputs
import com.sunpawtechnologies.scrollless.domain.policy.PaymentAppSafety
import com.sunpawtechnologies.scrollless.tracking.ForegroundAppTracker
import com.sunpawtechnologies.scrollless.tracking.TrackingLog
import com.sunpawtechnologies.scrollless.tracking.instagram.DetectorDecision
import com.sunpawtechnologies.scrollless.tracking.instagram.INSTAGRAM_PACKAGE
import com.sunpawtechnologies.scrollless.tracking.instagram.InstagramReelsDetector
import com.sunpawtechnologies.scrollless.tracking.instagram.ObservedEvent
import com.sunpawtechnologies.scrollless.tracking.instagram.ObservedEventType
import com.sunpawtechnologies.scrollless.tracking.instagram.RejectReason
import com.sunpawtechnologies.scrollless.tracking.overlay.CounterContent
import com.sunpawtechnologies.scrollless.tracking.overlay.ReelsCounterOverlay
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate

/**
 * Estimates Instagram Reels and shows the floating counter over Instagram.
 *
 * Runs while enabled in system settings, whether or not the ScrollLess UI is open.
 * - Outside Instagram it receives only window-change events and reads only their package name,
 *   to notice when Instagram comes to the front.
 * - While Instagram is in front it also receives scroll events, and only Instagram's are
 *   inspected (structural fields only: no text).
 * - Nothing is counted or shown until the user has accepted the in-app disclosure.
 *
 * Events arrive on the main thread. Database work goes to the app scope.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ScrollLessAccessibilityService : AccessibilityService() {

    private val container by lazy { (application as ScrollLessApp).container }
    private val detector = InstagramReelsDetector()
    private lateinit var foreground: ForegroundAppTracker
    private lateinit var overlay: ReelsCounterOverlay

    /** Lives as long as this service instance. */
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private var consentGiven = false
    private var loggedMissingConsent = false
    private var coverageId: Deferred<Long>? = null
    private var stopped = false
    /** Applied (allApps, scrollEvents) scope; null until first applied. */
    private var appliedScope: Pair<Boolean, Boolean>? = null
    private var hiddenForSession = false
    private var overlayModel: OverlayModel? = null
    private var inputMethodPackages: Set<String> = emptySet()

    /** User-added payment/banking apps; the built-in list is applied by [PaymentAppSafety]. */
    private var customExcludedPackages: Set<String> = emptySet()

    /** True when the watchdog can't confirm Instagram is the active window. The counter hides until it can. */
    private var foregroundUncertain = false
    private var uncertainChecks = 0
    private val handler = Handler(Looper.getMainLooper())

    /** The day the counter shows. Re-read each time Instagram comes to the front, so it rolls over at midnight. */
    private val counterDay = MutableStateFlow(LocalDate.MIN)

    private data class OverlayModel(val settings: AppSettings, val count: Int, val limit: Int)

    private val screenOffReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            TrackingLog.d("instagram_session_reset", "reason" to "screen_off")
            detector.reset()
            if (foreground.onScreenOff()) onForegroundChanged()
        }
    }

    override fun onServiceConnected() {
        TrackingLog.i("accessibility_service_connected", "detector_version" to InstagramReelsDetector.VERSION)
        inputMethodPackages = getSystemService(InputMethodManager::class.java)
            ?.enabledInputMethodList?.map { it.packageName }?.toSet().orEmpty()
        foreground = ForegroundAppTracker(packageName, MainActivity::class.java.name) { inputMethodPackages }
        overlay = ReelsCounterOverlay(this, overlayListener)
        counterDay.value = container.timeProvider.today()
        applyEventScope()

        container.reelsTrackingSwitch.attach(this)
        // Running again means the user switched it back on in Accessibility settings.
        container.appScope.launch {
            val settings = container.settingsRepository
            if (settings.settings.first().reelsTrackingTurnedOffByUser) settings.setReelsTrackingTurnedOffByUser(false)
        }
        container.permissionMonitor.onServiceConnectionChanged(connected = true)
        ContextCompat.registerReceiver(
            this,
            screenOffReceiver,
            IntentFilter(Intent.ACTION_SCREEN_OFF),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )

        serviceScope.launch { observeOverlayModel() }

        val coverage = container.trackingCoverageRepository
        coverageId = container.appScope.async {
            coverage.closeAbandoned(TrackingSource.ACCESSIBILITY)
            coverage.start(TrackingSource.ACCESSIBILITY, container.timeProvider.now())
        }
    }

    /** Keeps the counter in sync with the stored count, limit and settings. */
    private suspend fun observeOverlayModel() {
        val usage = container.usageRepository
        val limits = container.limitRepository
        val today = counterDay.flatMapLatest { day ->
            combine(usage.observeContentViewCount(ContentKind.INSTAGRAM_REEL, day), limits.observeLimitsOn(day)) { count, map ->
                count to map.getValue(LimitMetric.INSTAGRAM_REELS)
            }
        }
        combine(container.settingsRepository.settings, today) { settings, (count, limit) ->
            OverlayModel(settings, count, limit)
        }.distinctUntilChanged().collect { model ->
            val consent = model.settings.hasCurrentAccessibilityConsent()
            if (consent != consentGiven) {
                consentGiven = consent
                loggedMissingConsent = false
                TrackingLog.i("accessibility_consent", "given" to consent)
            }
            customExcludedPackages = model.settings.customExcludedPackages
            overlayModel = model
            TrackingLog.d("overlay_model", "count" to model.count, "limit" to model.limit, "enabled" to model.settings.overlayEnabled)
            onCounterSettingsMaybeChanged()
            renderOverlay()
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        val eventPackage = event.packageName?.toString()
        if (eventPackage != null && PaymentAppSafety.isProtected(eventPackage, customExcludedPackages)) {
            // Payment or banking app: hide at once and read nothing else from it.
            if (foreground.onActiveWindowObserved(eventPackage)) onForegroundChanged()
            return
        }
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            foreground.onWindowStateChanged(eventPackage, event.className?.toString())
        ) {
            onForegroundChanged()
        }
        // Other apps: the package name above is all that is ever read.
        if (eventPackage != INSTAGRAM_PACKAGE) return
        if (foreground.onInstagramEvent()) onForegroundChanged()
        if (foregroundUncertain) setForegroundUncertain(false)

        val observed = event.toObservedEvent(container.timeProvider.now().toEpochMilli())
        TrackingLog.d(
            "accessibility_event",
            "type" to observed.type,
            "class" to observed.className,
            "view_id" to observed.sourceViewId,
            "from" to observed.fromIndex,
            "to" to observed.toIndex,
            "items" to observed.itemCount,
        )

        if (!consentGiven) {
            if (!loggedMissingConsent) {
                TrackingLog.i("reel_rejected", "reason" to "NO_CONSENT")
                loggedMissingConsent = true
            }
            return
        }

        when (val decision = detector.onEvent(observed)) {
            is DetectorDecision.Count -> onCountCandidate(decision, observed.timestampMs)
            is DetectorDecision.Reject -> logRejection(decision.reason, observed)
            DetectorDecision.Observed -> TrackingLog.d("instagram_window_changed", "class" to observed.className)
        }
    }

    private fun onForegroundChanged() {
        val inInstagram = foreground.isInstagramForeground
        TrackingLog.d(
            if (inInstagram) "instagram_foreground" else "instagram_background",
            "foreground_package" to foreground.foregroundPackage,
        )
        applyEventScope()
        uncertainChecks = 0
        foregroundUncertain = false
        syncWatchdog()
        if (inInstagram) {
            counterDay.value = container.timeProvider.today()
        } else {
            // "Hide" lasts for one Instagram visit.
            hiddenForSession = false
        }
        renderOverlay()
    }

    /** The optional Instagram counter is on. Only then does the service look beyond Instagram. */
    private fun counterEnabled(): Boolean = consentGiven && overlayModel?.settings?.overlayEnabled == true

    /**
     * Keeps the service's event scope as narrow as the enabled features allow:
     * - Counter off: Instagram only, window and scroll events. No other app is observed.
     * - Counter on: window changes from all apps (package name only) so the counter can hide
     *   outside Instagram, plus Instagram's scroll events while it is in front.
     */
    private fun applyEventScope() {
        val allApps = counterEnabled()
        val scroll = !allApps || foreground.isInstagramForeground
        val desired = allApps to scroll
        if (appliedScope == desired) return
        val info = serviceInfo ?: return
        info.packageNames = if (allApps) null else arrayOf(INSTAGRAM_PACKAGE)
        info.eventTypes = if (scroll) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or AccessibilityEvent.TYPE_VIEW_SCROLLED
        } else {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
        }
        serviceInfo = info
        appliedScope = desired
        TrackingLog.i("event_scope_changed", "all_apps" to allApps, "scroll_events" to scroll)
    }

    private var lastCounterEnabled = false

    private fun onCounterSettingsMaybeChanged() {
        val enabled = counterEnabled()
        if (enabled == lastCounterEnabled) return
        lastCounterEnabled = enabled
        if (enabled) {
            // With the narrow scope we never saw the user leave Instagram, so the foreground may be
            // stale. Stay hidden until the watchdog confirms Instagram is really in front.
            foregroundUncertain = true
            uncertainChecks = 0
        }
        applyEventScope()
        syncWatchdog()
    }

    /** The active-window check is only needed to place and hide the counter. */
    private fun syncWatchdog() {
        handler.removeCallbacks(foregroundWatchdog)
        if (counterEnabled() && foreground.isInstagramForeground) {
            handler.postDelayed(foregroundWatchdog, WATCHDOG_INTERVAL_MS)
        }
    }

    /**
     * While Instagram is believed to be in front, re-checks the active window twice a second.
     *
     * Window events alone aren't enough: apps can mark windows as accessibility-data-sensitive
     * (common in payment apps), and those events never reach this service. Without this check the
     * counter could stay on top of a payment app, which then rejects touches as "obscured".
     * Only the active window's package name is read.
     */
    private val foregroundWatchdog = object : Runnable {
        override fun run() {
            if (!foreground.isInstagramForeground || stopped) return
            val active = activeWindowPackage()
            when {
                active == INSTAGRAM_PACKAGE -> {
                    uncertainChecks = 0
                    if (foregroundUncertain) setForegroundUncertain(false)
                }
                active == null -> {
                    // Unknown or hidden window: after two misses (~1 s), hide to be safe.
                    if (++uncertainChecks >= 2 && !foregroundUncertain) setForegroundUncertain(true)
                }
                foreground.onActiveWindowObserved(active) -> {
                    onForegroundChanged()
                    return
                }
            }
            handler.postDelayed(this, WATCHDOG_INTERVAL_MS)
        }
    }

    private fun setForegroundUncertain(uncertain: Boolean) {
        foregroundUncertain = uncertain
        if (!uncertain) uncertainChecks = 0
        TrackingLog.d("foreground_uncertain", "uncertain" to uncertain)
        renderOverlay()
    }

    private fun renderOverlay() {
        if (!::overlay.isInitialized) return
        val model = overlayModel
        val show = model != null && PaymentAppSafety.shouldShowCounter(
            CounterInputs(
                foregroundPackage = foreground.foregroundPackage,
                customExcludedPackages = customExcludedPackages,
                consentGiven = consentGiven,
                counterEnabled = model.settings.overlayEnabled,
                foregroundUncertain = foregroundUncertain,
                hiddenForSession = hiddenForSession,
            ),
        )
        if (model == null || !show) {
            overlay.hide()
            return
        }
        overlay.show(
            CounterContent(
                count = model.count,
                limit = model.limit,
                status = limitStatus(model.count.toLong(), model.limit.toLong(), model.settings.warningThresholdPercent),
            ),
            model.settings.overlayPosition,
        )
    }

    private val overlayListener = object : ReelsCounterOverlay.Listener {
        override fun onMoved(position: OverlayPosition) {
            container.appScope.launch { container.settingsRepository.setOverlayPosition(position) }
        }

        override fun onHiddenForSession() {
            hiddenForSession = true
        }
    }

    private fun onCountCandidate(decision: DetectorDecision.Count, timestampMs: Long) {
        if (!foreground.isInstagramForeground) {
            TrackingLog.d("reel_rejected", "reason" to "NOT_FOREGROUND", "foreground_package" to foreground.foregroundPackage)
            return
        }
        // Second check against the window the user is actually looking at.
        val activePackage = activeWindowPackage()
        if (activePackage != null && activePackage != INSTAGRAM_PACKAGE) {
            TrackingLog.d("reel_rejected", "reason" to "NOT_FOREGROUND", "active_package" to activePackage)
            return
        }
        TrackingLog.d(
            "reel_accepted",
            "method" to decision.method,
            "index" to decision.itemIndex,
            "foreground_verified" to (activePackage != null),
        )
        val zone = container.timeProvider.zone()
        val occurredAt = Instant.ofEpochMilli(timestampMs)
        container.appScope.launch {
            val isNew = container.usageRepository.recordContentView(
                kind = ContentKind.INSTAGRAM_REEL,
                occurredAt = occurredAt,
                zone = zone,
                dedupKey = decision.dedupKey,
                detectorVersion = InstagramReelsDetector.VERSION,
            )
            TrackingLog.d("reel_persisted", "stored" to isNew, "duplicate" to !isNew)
            // A Reel-limit crossing is noticed right away; app-time limits are left to the periodic check.
            if (isNew) container.limitAlertChecker.check(importUsage = false)
        }
    }

    private fun logRejection(reason: RejectReason, observed: ObservedEvent) {
        if (reason == RejectReason.NOT_REELS_VIEWER && observed.type == ObservedEventType.VIEW_SCROLLED) {
            // Feed, comments, etc. Logging the view ID helps tune the Reels viewer ID on real devices.
            TrackingLog.d("scroll_unrecognized", "view_id" to observed.sourceViewId, "class" to observed.className)
        } else {
            TrackingLog.d("reel_rejected", "reason" to reason)
        }
    }

    private fun activeWindowPackage(): String? {
        val root = try {
            rootInActiveWindow
        } catch (_: RuntimeException) {
            null
        } ?: return null
        return root.packageName?.toString().also { root.recycleCompat() }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (::overlay.isInitialized) overlay.onScreenChanged()
    }

    override fun onInterrupt() = Unit

    override fun onUnbind(intent: Intent?): Boolean {
        // Still switched on in Settings means Android (or a battery manager) unbound it, not the user.
        val stillEnabled = container.permissionMonitor.isServiceEnabledInSettings()
        stopTracking(if (stillEnabled) CoverageEndReason.SERVICE_INTERRUPTED else CoverageEndReason.STOPPED_BY_USER)
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        TrackingLog.i("accessibility_service_destroyed")
        stopTracking(CoverageEndReason.SERVICE_INTERRUPTED)
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun stopTracking(reason: CoverageEndReason) {
        if (stopped) return
        stopped = true
        TrackingLog.i("accessibility_service_disconnected", "reason" to reason)
        container.reelsTrackingSwitch.detach(this)
        handler.removeCallbacks(foregroundWatchdog)
        if (::overlay.isInitialized) overlay.hide()
        runCatching { unregisterReceiver(screenOffReceiver) }
        detector.reset()
        container.permissionMonitor.onServiceConnectionChanged(connected = false)
        val pendingId = coverageId ?: return
        val endedAt = container.timeProvider.now()
        container.appScope.launch {
            container.trackingCoverageRepository.end(pendingId.await(), endedAt, reason)
        }
    }
}

/** Extracts only structural fields. The source node is released right away. */
private fun AccessibilityEvent.toObservedEvent(nowMs: Long): ObservedEvent {
    val type = when (eventType) {
        AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> ObservedEventType.WINDOW_STATE_CHANGED
        AccessibilityEvent.TYPE_VIEW_SCROLLED -> ObservedEventType.VIEW_SCROLLED
        else -> ObservedEventType.OTHER
    }
    var viewId: String? = null
    var identity: Int? = null
    if (type == ObservedEventType.VIEW_SCROLLED) {
        source?.let { node ->
            viewId = node.viewIdResourceName
            identity = node.hashCode() // Derived from the view's accessibility id and window id.
            node.recycleCompat()
        }
    }
    return ObservedEvent(
        type = type,
        packageName = packageName?.toString(),
        className = className?.toString(),
        sourceViewId = viewId,
        sourceIdentity = identity,
        fromIndex = fromIndex,
        toIndex = toIndex,
        itemCount = itemCount,
        timestampMs = nowMs,
    )
}

private fun AccessibilityNodeInfo.recycleCompat() {
    // Nodes are recycled automatically from Android 13.
    @Suppress("DEPRECATION")
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) recycle()
}

/** Active-window check interval while Instagram is in front: two cheap IPC calls a second. */
private const val WATCHDOG_INTERVAL_MS = 500L
