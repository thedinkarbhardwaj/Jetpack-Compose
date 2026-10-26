package com.sunpawtechnologies.scrollless.tracking.usagestats

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Build
import com.sunpawtechnologies.scrollless.core.time.TimeProvider
import com.sunpawtechnologies.scrollless.domain.model.Platform
import com.sunpawtechnologies.scrollless.domain.model.TrackingSource
import com.sunpawtechnologies.scrollless.domain.model.UsageMode
import com.sunpawtechnologies.scrollless.domain.repository.PermissionMonitor
import com.sunpawtechnologies.scrollless.domain.repository.TrackingCoverageRepository
import com.sunpawtechnologies.scrollless.domain.repository.UsageRepository
import com.sunpawtechnologies.scrollless.tracking.TrackingLog
import com.sunpawtechnologies.scrollless.tracking.YOUTUBE_PACKAGE
import com.sunpawtechnologies.scrollless.tracking.instagram.INSTAGRAM_PACKAGE
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import java.time.Instant

fun interface UsageImporter {
    /** Imports recent foreground time into the database. Safe to call repeatedly. */
    suspend fun importRecentUsage()
}

/**
 * Reads Instagram foreground time from Android's usage history. Android records it whether or
 * not ScrollLess is running, so importing on demand is enough and needs no background service.
 */
class UsageStatsImporter(
    context: Context,
    private val permissionMonitor: PermissionMonitor,
    private val usageRepository: UsageRepository,
    private val coverageRepository: TrackingCoverageRepository,
    private val timeProvider: TimeProvider,
) : UsageImporter {

    private val usageStatsManager = context.getSystemService(UsageStatsManager::class.java)

    /** Overlapping imports (e.g. quick resume/pause) would only repeat the same work. */
    private val importMutex = Mutex()

    override suspend fun importRecentUsage() = withContext(Dispatchers.IO) {
        val permissions = permissionMonitor.state.value
        if (!permissions.usageAccessGranted) {
            TrackingLog.d("usage_import_skipped", "reason" to "no_usage_access")
            return@withContext
        }
        val targets = buildList {
            if (permissions.instagramInstalled) add(INSTAGRAM_PACKAGE to Platform.INSTAGRAM)
            if (permissions.youtubeInstalled) add(YOUTUBE_PACKAGE to Platform.YOUTUBE)
        }
        if (targets.isEmpty()) {
            TrackingLog.d("usage_import_skipped", "reason" to "no_tracked_app_installed")
            return@withContext
        }
        if (!importMutex.tryLock()) {
            TrackingLog.d("usage_import_skipped", "reason" to "already_running")
            return@withContext
        }
        try {
            importLocked(targets)
        } catch (e: SecurityException) {
            // Usage Access was revoked between the check and the query.
            TrackingLog.i("usage_import_failed", "reason" to "security_exception", "message" to e.message)
        } finally {
            importMutex.unlock()
        }
    }

    private suspend fun importLocked(targets: List<Pair<String, Platform>>) {
        val zone = timeProvider.zone()
        val now = timeProvider.now()
        val from = importWindowStart(timeProvider.today(), zone, coverageRepository.lastCoveredEnd(TrackingSource.USAGE_STATS))

        val raw = readRawEvents(from.toEpochMilli(), now.toEpochMilli())
        var sessionCount = 0
        for ((packageName, platform) in targets) {
            val sessions = buildForegroundSessions(
                raw.mapNotNull { it.forTarget(packageName) },
                now.toEpochMilli(),
                // ACTIVITY_STOPPED exists from Android 10; before that a pause is the only signal.
                pauseEndsSession = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q,
            )
            sessionCount += sessions.size
            sessions.forEach { session ->
                TrackingLog.d(
                    "foreground_session",
                    "app" to platform,
                    "start" to Instant.ofEpochMilli(session.startMs),
                    "end" to Instant.ofEpochMilli(session.endMs),
                    "seconds" to (session.endMs - session.startMs) / 1000,
                    "ongoing" to session.ongoing,
                )
                usageRepository.recordSession(
                    platform = platform,
                    mode = UsageMode.APP_FOREGROUND,
                    source = TrackingSource.USAGE_STATS,
                    start = Instant.ofEpochMilli(session.startMs),
                    endExclusive = Instant.ofEpochMilli(session.endMs),
                    zone = zone,
                )
            }
        }
        // Android keeps this history whether or not ScrollLess ran, so whatever it still remembers
        // counts as tracked. Screens hide an app's figures while that app isn't installed.
        val firstEvent = raw.minOfOrNull { it.timestampMs }?.let(Instant::ofEpochMilli)
        coverageStart(from, firstEvent, now, zone)?.let { coverageRepository.recordCovered(TrackingSource.USAGE_STATS, it, now) }
        TrackingLog.i(
            "usage_import_done",
            "events" to raw.size,
            "sessions" to sessionCount,
            "apps" to targets.map { it.second },
            "from" to from,
        )
    }

    /** A usage event reduced to the fields needed; not yet tied to a target app. */
    private class RawEvent(val timestampMs: Long, val type: Int, val packageName: String, val className: String?) {
        fun forTarget(target: String): ForegroundEvent? {
            val isTarget = packageName == target
            val kind = when (type) {
                EVENT_ACTIVITY_RESUMED -> when {
                    isTarget -> ForegroundEvent.Kind.TARGET_RESUMED
                    // Permission prompts, share sheets etc. sit on top of the app; the user hasn't left it.
                    packageName in SYSTEM_DIALOG_PACKAGES -> null
                    else -> ForegroundEvent.Kind.OTHER_APP_RESUMED
                }
                EVENT_ACTIVITY_PAUSED -> if (isTarget) ForegroundEvent.Kind.TARGET_PAUSED else null
                EVENT_ACTIVITY_STOPPED -> if (isTarget) ForegroundEvent.Kind.TARGET_STOPPED else null
                EVENT_SCREEN_NON_INTERACTIVE, EVENT_DEVICE_SHUTDOWN -> ForegroundEvent.Kind.DEVICE_INACTIVE
                else -> null
            } ?: return null
            return ForegroundEvent(timestampMs, kind, if (isTarget) className else null)
        }
    }

    private fun readRawEvents(fromMs: Long, toMs: Long): List<RawEvent> {
        val usageEvents = usageStatsManager.queryEvents(fromMs, toMs) ?: return emptyList()
        val result = mutableListOf<RawEvent>()
        val event = UsageEvents.Event()
        while (usageEvents.hasNextEvent()) {
            usageEvents.getNextEvent(event)
            if (event.eventType in RELEVANT_TYPES) {
                result += RawEvent(event.timeStamp, event.eventType, event.packageName, event.className)
            }
        }
        return result
    }

    private companion object {
        // Raw values: API 24 names some of these MOVE_TO_FOREGROUND/BACKGROUND, and newer types
        // simply never occur on older releases.
        const val EVENT_ACTIVITY_RESUMED = 1
        const val EVENT_ACTIVITY_PAUSED = 2
        const val EVENT_SCREEN_NON_INTERACTIVE = 16
        const val EVENT_ACTIVITY_STOPPED = 23
        const val EVENT_DEVICE_SHUTDOWN = 26
        val SYSTEM_DIALOG_PACKAGES = setOf(
            "android",
            "com.android.systemui",
            "com.android.permissioncontroller",
            "com.google.android.permissioncontroller",
        )
        val RELEVANT_TYPES = setOf(
            EVENT_ACTIVITY_RESUMED, EVENT_ACTIVITY_PAUSED, EVENT_ACTIVITY_STOPPED,
            EVENT_SCREEN_NON_INTERACTIVE, EVENT_DEVICE_SHUTDOWN,
        )
    }
}
