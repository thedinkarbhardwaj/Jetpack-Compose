package com.sunpawtechnologies.scrollless.tracking.usagestats

/** A usage event reduced to what's needed to reconstruct one app's foreground time. */
data class ForegroundEvent(
    val timestampMs: Long,
    val kind: Kind,
    /** Activity class for the target app's events, used to track multiple activities. */
    val className: String? = null,
) {
    enum class Kind {
        TARGET_RESUMED,
        TARGET_PAUSED,

        /** The target's activity is no longer visible (Android 10+). */
        TARGET_STOPPED,

        /** Another app came to the foreground. Ends the target session even if its pause was never logged. */
        OTHER_APP_RESUMED,

        /** Screen off or device shutdown. */
        DEVICE_INACTIVE,
    }
}

data class ForegroundSession(val startMs: Long, val endMs: Long, val ongoing: Boolean)

/**
 * Rebuilds foreground sessions of one app from time-ordered usage events.
 *
 * - The app is foreground while at least one of its activities is resumed.
 * - Pauses with no matching resume (it happened before the query window) are ignored.
 * - Sessions separated by less than [mergeGapMs] (moving between the app's own activities) merge.
 * - A session still open at the end of the events runs until [nowMs] and is marked ongoing.
 * - With [pauseEndsSession] false (Android 10+, which reports "stopped"), only a stop ends the
 *   target's activity. A pause alone happens whenever a dialog (e.g. a permission prompt) covers
 *   the app while it stays on screen, so it doesn't count as leaving.
 *
 * Session starts are real event timestamps, so re-running over an overlapping window yields
 * the same starts. Storage relies on that to replace sessions instead of duplicating them.
 */
fun buildForegroundSessions(
    events: List<ForegroundEvent>,
    nowMs: Long,
    mergeGapMs: Long = DEFAULT_MERGE_GAP_MS,
    pauseEndsSession: Boolean = true,
): List<ForegroundSession> {
    val raw = mutableListOf<ForegroundSession>()
    val resumedActivities = mutableSetOf<String?>()
    var start: Long? = null

    fun close(at: Long) {
        start?.let { if (at > it) raw += ForegroundSession(it, at, ongoing = false) }
        start = null
        resumedActivities.clear()
    }

    for (event in events.sortedBy { it.timestampMs }) {
        when (event.kind) {
            ForegroundEvent.Kind.TARGET_RESUMED -> {
                if (start == null) start = event.timestampMs
                resumedActivities += event.className
            }
            ForegroundEvent.Kind.TARGET_PAUSED -> {
                if (pauseEndsSession && resumedActivities.remove(event.className) && resumedActivities.isEmpty()) close(event.timestampMs)
            }
            ForegroundEvent.Kind.TARGET_STOPPED -> {
                if (resumedActivities.remove(event.className) && resumedActivities.isEmpty()) close(event.timestampMs)
            }
            ForegroundEvent.Kind.OTHER_APP_RESUMED,
            ForegroundEvent.Kind.DEVICE_INACTIVE -> close(event.timestampMs)
        }
    }
    start?.let { if (nowMs > it) raw += ForegroundSession(it, nowMs, ongoing = true) }

    return raw.fold(mutableListOf()) { merged, session ->
        val last = merged.lastOrNull()
        if (last != null && session.startMs - last.endMs <= mergeGapMs) {
            merged[merged.lastIndex] = last.copy(endMs = session.endMs, ongoing = session.ongoing)
        } else {
            merged += session
        }
        merged
    }
}

const val DEFAULT_MERGE_GAP_MS = 2_000L
