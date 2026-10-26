package com.sunpawtechnologies.scrollless.alerts

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.os.Build
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.sunpawtechnologies.scrollless.MainActivity
import com.sunpawtechnologies.scrollless.R
import com.sunpawtechnologies.scrollless.domain.model.LimitMetric
import com.sunpawtechnologies.scrollless.domain.model.LimitUnit
import com.sunpawtechnologies.scrollless.domain.usecase.AlertLevel
import com.sunpawtechnologies.scrollless.domain.usecase.LimitAlert
import com.sunpawtechnologies.scrollless.domain.usecase.LimitNotifier
import com.sunpawtechnologies.scrollless.tracking.TrackingLog

/**
 * Posts limit alerts. One notification per metric: "reached" replaces that metric's warning.
 * Tapping it opens ScrollLess on the dashboard's limit-reached sheet. Nothing else is launched.
 */
class AndroidLimitNotifier(context: Context) : LimitNotifier {
    private val appContext = context.applicationContext
    private val manager = NotificationManagerCompat.from(appContext)

    override fun canNotify(): Boolean {
        val permitted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            appContext.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        if (!permitted || !manager.areNotificationsEnabled()) return false
        ensureChannel()
        return manager.getNotificationChannelCompat(CHANNEL_ID)?.importance != NotificationManagerCompat.IMPORTANCE_NONE
    }

    override fun notify(alert: LimitAlert) {
        ensureChannel()
        val res = appContext.resources
        val (title, text) = alertText(res, alert)
        val open = Intent(appContext, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            .putExtra(EXTRA_LIMIT_METRIC, alert.metric.name)
        val pending = PendingIntent.getActivity(
            appContext,
            alert.metric.ordinal,
            open,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_hourglass_top)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pending)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .build()
        try {
            manager.notify(NOTIFICATION_ID_BASE + alert.metric.ordinal, notification)
        } catch (e: SecurityException) {
            // Permission revoked between the check and the post.
            TrackingLog.i("limit_alert_post_failed", "error" to e.javaClass.simpleName)
        }
    }

    private fun ensureChannel() {
        manager.createNotificationChannel(
            NotificationChannelCompat.Builder(CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_DEFAULT)
                .setName(appContext.getString(R.string.notification_channel_limits))
                .setDescription(appContext.getString(R.string.notification_channel_limits_body))
                .build(),
        )
    }

    companion object {
        const val CHANNEL_ID = "limit_alerts"
        const val EXTRA_LIMIT_METRIC = "com.sunpawtechnologies.scrollless.extra.LIMIT_METRIC"
        private const val NOTIFICATION_ID_BASE = 1000

        /** Title and body. App time is always called "open time", never Reels or Shorts time. */
        fun alertText(res: Resources, alert: LimitAlert): Pair<String, String> {
            val app = res.getString(appNameRes(alert.metric))
            return if (alert.metric.unit == LimitUnit.MINUTES) {
                val used = formatDurationPlain(res, alert.value)
                when (alert.level) {
                    AlertLevel.WARNING -> res.getString(R.string.alert_time_warning_title, app, used, alert.limit) to
                        res.getString(R.string.alert_time_warning_body, app)
                    AlertLevel.REACHED -> res.getString(R.string.alert_time_reached_title, app, alert.limit) to
                        res.getString(R.string.alert_time_reached_body, app, used)
                }
            } else {
                when (alert.level) {
                    AlertLevel.WARNING -> res.getString(R.string.alert_reels_warning_title, alert.value, alert.limit) to
                        res.getString(R.string.alert_reels_body)
                    AlertLevel.REACHED -> res.getString(R.string.alert_reels_reached_title, alert.limit) to
                        res.getString(R.string.alert_reels_body)
                }
            }
        }

        fun appNameRes(metric: LimitMetric): Int = when (metric) {
            LimitMetric.YOUTUBE_APP_TIME, LimitMetric.YOUTUBE_SHORTS, LimitMetric.YOUTUBE_REGULAR_VIDEO -> R.string.app_youtube
            LimitMetric.INSTAGRAM_APP_TIME, LimitMetric.INSTAGRAM_REELS -> R.string.app_instagram
        }
    }
}

/** Same wording as the in-app duration format, for use outside Compose. */
fun formatDurationPlain(res: Resources, millis: Long): String {
    val totalMinutes = millis / 60_000
    val hours = (totalMinutes / 60).toInt()
    val minutes = (totalMinutes % 60).toInt()
    return when {
        hours > 0 -> res.getString(R.string.duration_hours_minutes, hours, minutes)
        minutes == 0 && millis > 0 -> res.getString(R.string.duration_under_minute)
        else -> res.getString(R.string.duration_minutes, minutes)
    }
}
