package com.sunpawtechnologies.scrollless.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.sunpawtechnologies.scrollless.R
import java.time.Duration

/** "1 h 5 min", "12 min", or "< 1 min" for short but non-zero time. */
@Composable
fun formatDuration(duration: Duration): String {
    val hours = duration.toHours().toInt()
    val minutes = (duration.toMinutes() % 60).toInt()
    return when {
        hours > 0 -> stringResource(R.string.duration_hours_minutes, hours, minutes)
        minutes == 0 && !duration.isZero -> stringResource(R.string.duration_under_minute)
        else -> stringResource(R.string.duration_minutes, minutes)
    }
}

@Composable
fun formatDurationMillis(millis: Long): String = formatDuration(Duration.ofMillis(millis))

/** Compact form for chart axes: "45m", "2h", "1.5h". */
fun compactDuration(millis: Long): String {
    val minutes = millis / 60_000
    return when {
        minutes < 60 -> "${minutes}m"
        minutes % 60 == 0L -> "${minutes / 60}h"
        else -> "%.1fh".format(minutes / 60.0)
    }
}
