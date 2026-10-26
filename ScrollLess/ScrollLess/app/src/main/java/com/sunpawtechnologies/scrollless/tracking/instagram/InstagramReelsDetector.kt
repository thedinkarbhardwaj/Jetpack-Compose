package com.sunpawtechnologies.scrollless.tracking.instagram

const val INSTAGRAM_PACKAGE = "com.instagram.android"

enum class ObservedEventType { WINDOW_STATE_CHANGED, VIEW_SCROLLED, OTHER }

/**
 * The structural parts of an accessibility event the detector needs. No text or content
 * descriptions are captured.
 */
data class ObservedEvent(
    val type: ObservedEventType,
    val packageName: String?,
    val className: String?,
    /** Resource ID of the view that scrolled, e.g. `com.instagram.android:id/...`. */
    val sourceViewId: String?,
    /** Changes when the scrolled view is a different View instance (AccessibilityNodeInfo identity). */
    val sourceIdentity: Int?,
    /** First/last visible item index reported by list-like views, or -1 if unreported. */
    val fromIndex: Int,
    val toIndex: Int,
    val itemCount: Int,
    /** Wall-clock time, used both for gaps and as a stable part of dedup keys. */
    val timestampMs: Long,
)

enum class CountMethod {
    /** The pager reported which item is showing. The most reliable signal. */
    ITEM_INDEX,

    /** The pager reported no indices, so each separate swipe gesture is counted. Less precise. */
    SWIPE_GESTURE,
}

enum class RejectReason {
    NOT_INSTAGRAM,
    UNSUPPORTED_EVENT,

    /** A scroll from something other than the Reels viewer, e.g. the home feed or comments. */
    NOT_REELS_VIEWER,

    /** The pager is between two Reels. Counting waits until it settles on one. */
    MID_SWIPE,

    /** Another scroll event for the Reel already showing. */
    SAME_REEL,

    /** The user swiped back to a Reel already counted in this session. */
    ALREADY_COUNTED,

    /** More events from the same swipe gesture (fallback mode). */
    SAME_SWIPE_GESTURE,
}

sealed interface DetectorDecision {
    data class Count(val dedupKey: String, val method: CountMethod, val itemIndex: Int?) : DetectorDecision
    data class Reject(val reason: RejectReason) : DetectorDecision

    /** Relevant for diagnostics but not countable (e.g. an Instagram screen change). */
    data object Observed : DetectorDecision
}

/**
 * Estimates Reels viewed from scroll events of Instagram's Reels viewer.
 *
 * A Reel is counted when the viewer settles on an item index not yet counted in the current
 * session. If the viewer reports no indices, one count per separate swipe gesture is the fallback.
 * Both are estimates: Instagram provides no public signal for "a Reel was watched".
 *
 * Pure Kotlin, so every rule is unit-tested without a device.
 */
class InstagramReelsDetector(
    private val reelsViewerIds: Set<String> = DEFAULT_REELS_VIEWER_IDS,
) {
    private var session: Session? = null

    private class Session(
        val startedAtMs: Long,
        val viewerIdentity: Int?,
        var lastEventMs: Long,
        var settledIndex: Int? = null,
        val countedIndices: MutableSet<Int> = mutableSetOf(),
        var lastSwipeEventMs: Long? = null,
    )

    fun onEvent(event: ObservedEvent): DetectorDecision {
        if (event.packageName != INSTAGRAM_PACKAGE) return DetectorDecision.Reject(RejectReason.NOT_INSTAGRAM)
        return when (event.type) {
            ObservedEventType.WINDOW_STATE_CHANGED -> DetectorDecision.Observed
            ObservedEventType.OTHER -> DetectorDecision.Reject(RejectReason.UNSUPPORTED_EVENT)
            ObservedEventType.VIEW_SCROLLED -> onScroll(event)
        }
    }

    /** Ends the current session, e.g. when the screen turns off. The next Reels scroll starts a new one. */
    fun reset() {
        session = null
    }

    private fun onScroll(event: ObservedEvent): DetectorDecision {
        if (event.sourceViewId !in reelsViewerIds) return DetectorDecision.Reject(RejectReason.NOT_REELS_VIEWER)
        val current = sessionFor(event)
        current.lastEventMs = event.timestampMs
        return if (event.fromIndex >= 0 && event.toIndex >= 0) {
            countByIndex(current, event)
        } else {
            countBySwipe(current, event)
        }
    }

    private fun sessionFor(event: ObservedEvent): Session {
        val existing = session
        val isSameSession = existing != null &&
            existing.viewerIdentity == event.sourceIdentity &&
            event.timestampMs - existing.lastEventMs <= SESSION_IDLE_TIMEOUT_MS
        if (isSameSession) return existing
        // A new viewer instance (Reels reopened) restarts its item indices, so start a fresh session.
        return Session(startedAtMs = event.timestampMs, viewerIdentity = event.sourceIdentity, lastEventMs = event.timestampMs)
            .also { session = it }
    }

    private fun countByIndex(session: Session, event: ObservedEvent): DetectorDecision {
        if (event.fromIndex != event.toIndex) return DetectorDecision.Reject(RejectReason.MID_SWIPE)
        val index = event.fromIndex
        if (index == session.settledIndex) return DetectorDecision.Reject(RejectReason.SAME_REEL)
        session.settledIndex = index
        if (!session.countedIndices.add(index)) return DetectorDecision.Reject(RejectReason.ALREADY_COUNTED)
        return DetectorDecision.Count(
            dedupKey = "ig-reel:v$VERSION:${session.startedAtMs}:$index",
            method = CountMethod.ITEM_INDEX,
            itemIndex = index,
        )
    }

    private fun countBySwipe(session: Session, event: ObservedEvent): DetectorDecision {
        val previous = session.lastSwipeEventMs
        val isNewGesture = previous == null || event.timestampMs - previous >= SWIPE_GESTURE_GAP_MS
        session.lastSwipeEventMs = event.timestampMs
        if (!isNewGesture) return DetectorDecision.Reject(RejectReason.SAME_SWIPE_GESTURE)
        return DetectorDecision.Count(
            dedupKey = "ig-reel-swipe:v$VERSION:${session.startedAtMs}:${event.timestampMs}",
            method = CountMethod.SWIPE_GESTURE,
            itemIndex = null,
        )
    }

    companion object {
        /** Stored with each count. Bump when the rules change so old and new estimates can be told apart. */
        const val VERSION = 1

        /**
         * Resource IDs of Instagram's Reels viewer. Instagram calls Reels "clips" internally;
         * "reel_viewer" is its Stories viewer and must not match. These IDs aren't public and can
         * change in any Instagram update. Check `scroll_unrecognized` lines in Logcat on a real
         * device and update this set if needed.
         */
        val DEFAULT_REELS_VIEWER_IDS = setOf("$INSTAGRAM_PACKAGE:id/clips_viewer_view_pager")

        /** No Reels scroll for this long ends the session, even without a screen-off event. */
        const val SESSION_IDLE_TIMEOUT_MS = 10 * 60 * 1000L

        /** Fallback mode: scroll events closer together than this belong to the same swipe. */
        const val SWIPE_GESTURE_GAP_MS = 700L
    }
}
