package com.sunpawtechnologies.scrollless.tracking.instagram

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InstagramReelsDetectorTest {

    private val detector = InstagramReelsDetector()
    private val reelsViewerId = InstagramReelsDetector.DEFAULT_REELS_VIEWER_IDS.first()

    private fun scroll(
        timestampMs: Long,
        from: Int = -1,
        to: Int = from,
        viewId: String? = reelsViewerId,
        identity: Int? = 1,
        packageName: String = INSTAGRAM_PACKAGE,
    ) = ObservedEvent(
        type = ObservedEventType.VIEW_SCROLLED,
        packageName = packageName,
        className = "androidx.recyclerview.widget.RecyclerView",
        sourceViewId = viewId,
        sourceIdentity = identity,
        fromIndex = from,
        toIndex = to,
        itemCount = 100,
        timestampMs = timestampMs,
    )

    private fun reject(reason: RejectReason) = DetectorDecision.Reject(reason)

    @Test
    fun `events from other apps are rejected`() {
        assertEquals(reject(RejectReason.NOT_INSTAGRAM), detector.onEvent(scroll(0, from = 0, packageName = "com.other.app")))
    }

    @Test
    fun `feed and stories scrolls are not counted as Reels`() {
        assertEquals(
            reject(RejectReason.NOT_REELS_VIEWER),
            detector.onEvent(scroll(0, from = 3, viewId = "$INSTAGRAM_PACKAGE:id/list")),
        )
        // Instagram's Stories viewer is internally called "reel_viewer" and must not match.
        assertEquals(
            reject(RejectReason.NOT_REELS_VIEWER),
            detector.onEvent(scroll(0, from = 1, viewId = "$INSTAGRAM_PACKAGE:id/reel_viewer_root")),
        )
        assertEquals(reject(RejectReason.NOT_REELS_VIEWER), detector.onEvent(scroll(0, from = 1, viewId = null)))
    }

    @Test
    fun `window changes are observed but not counted`() {
        val event = scroll(0).copy(type = ObservedEventType.WINDOW_STATE_CHANGED)
        assertEquals(DetectorDecision.Observed, detector.onEvent(event))
    }

    @Test
    fun `each settled reel counts once and mid-swipe events do not count`() {
        val first = detector.onEvent(scroll(1_000, from = 0))
        assertTrue(first is DetectorDecision.Count)

        assertEquals(reject(RejectReason.MID_SWIPE), detector.onEvent(scroll(2_000, from = 0, to = 1)))
        assertEquals(reject(RejectReason.MID_SWIPE), detector.onEvent(scroll(2_100, from = 0, to = 1)))

        val second = detector.onEvent(scroll(2_200, from = 1)) as DetectorDecision.Count
        assertEquals(CountMethod.ITEM_INDEX, second.method)
        assertEquals(1, second.itemIndex)
        assertNotEquals((first as DetectorDecision.Count).dedupKey, second.dedupKey)
    }

    @Test
    fun `repeated events for the showing reel are not counted again`() {
        detector.onEvent(scroll(1_000, from = 4))
        assertEquals(reject(RejectReason.SAME_REEL), detector.onEvent(scroll(1_100, from = 4)))
        assertEquals(reject(RejectReason.SAME_REEL), detector.onEvent(scroll(5_000, from = 4)))
    }

    @Test
    fun `swiping back to a reel already counted is not counted again`() {
        detector.onEvent(scroll(1_000, from = 0))
        detector.onEvent(scroll(2_000, from = 1))
        assertEquals(reject(RejectReason.ALREADY_COUNTED), detector.onEvent(scroll(3_000, from = 0)))
        assertTrue(detector.onEvent(scroll(4_000, from = 2)) is DetectorDecision.Count)
    }

    @Test
    fun `a reopened reels viewer starts a new session`() {
        val before = detector.onEvent(scroll(1_000, from = 0, identity = 1)) as DetectorDecision.Count
        val after = detector.onEvent(scroll(60_000, from = 0, identity = 2)) as DetectorDecision.Count
        assertNotEquals(before.dedupKey, after.dedupKey)
    }

    @Test
    fun `a long pause starts a new session`() {
        detector.onEvent(scroll(0, from = 0))
        val later = InstagramReelsDetector.SESSION_IDLE_TIMEOUT_MS + 1
        assertTrue(detector.onEvent(scroll(later, from = 0)) is DetectorDecision.Count)
    }

    @Test
    fun `reset after screen off starts a new session`() {
        detector.onEvent(scroll(0, from = 0))
        detector.reset()
        assertTrue(detector.onEvent(scroll(1_000, from = 0)) is DetectorDecision.Count)
    }

    @Test
    fun `dedup key is stable for the same session and index`() {
        val key = (detector.onEvent(scroll(1_000, from = 7)) as DetectorDecision.Count).dedupKey
        assertEquals("ig-reel:v${InstagramReelsDetector.VERSION}:1000:7", key)
    }

    @Test
    fun `without indices each separate swipe gesture counts once`() {
        val decisions = listOf(0L, 100L, 300L, 600L).map { detector.onEvent(scroll(it)) }
        assertEquals(1, decisions.count { it is DetectorDecision.Count })
        assertTrue(decisions.drop(1).all { it == reject(RejectReason.SAME_SWIPE_GESTURE) })

        val nextSwipe = detector.onEvent(scroll(1_500)) as DetectorDecision.Count
        assertEquals(CountMethod.SWIPE_GESTURE, nextSwipe.method)
    }

    @Test
    fun `continuous scrolling without a pause stays one gesture`() {
        // Each event is less than the gap after the previous one, so it's one long swipe.
        val decisions = (0..10).map { detector.onEvent(scroll(it * 500L)) }
        assertEquals(1, decisions.count { it is DetectorDecision.Count })
    }
}
