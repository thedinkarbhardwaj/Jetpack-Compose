package com.sunpawtechnologies.scrollless.tracking

import android.util.Log
import com.sunpawtechnologies.scrollless.BuildConfig

/**
 * Structured tracking diagnostics, e.g. `event=reel_rejected reason=MID_SWIPE index=4`.
 * Filter Logcat by tag `ScrollLessTracking`.
 *
 * Only structural data is logged (event types, view IDs, list indices, timestamps). Never log
 * on-screen text, content descriptions, or anything the user typed.
 */
object TrackingLog {
    const val TAG = "ScrollLessTracking"

    /** Lifecycle and state changes. Logged in every build. */
    fun i(event: String, vararg fields: Pair<String, Any?>) {
        Log.i(TAG, format(event, fields))
    }

    /** Per-event detail. Debug builds only, because it can fire several times per second. */
    fun d(event: String, vararg fields: Pair<String, Any?>) {
        if (BuildConfig.DEBUG) Log.d(TAG, format(event, fields))
    }

    private fun format(event: String, fields: Array<out Pair<String, Any?>>): String =
        buildString {
            append("event=").append(event)
            fields.forEach { (key, value) -> append(' ').append(key).append('=').append(value) }
        }
}
