package com.sunpawtechnologies.scrollless.alerts

import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import com.sunpawtechnologies.scrollless.ScrollLessApp
import com.sunpawtechnologies.scrollless.tracking.TrackingLog
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

/**
 * Checks limits roughly every 15 minutes, Android's minimum for periodic jobs.
 *
 * Each run is a short Usage Access import plus a few database reads. Nothing runs between runs:
 * no service, no wake lock, no polling loop. Android may delay runs in Doze or under a phone
 * maker's battery manager, so an alert can arrive later than the exact moment of crossing.
 */
class LimitCheckJobService : JobService() {
    private var job: Job? = null

    override fun onStartJob(params: JobParameters): Boolean {
        val container = (application as ScrollLessApp).container
        job = container.appScope.launch {
            try {
                container.limitAlertChecker.check()
            } catch (e: RuntimeException) {
                // A failed run must not crash the app; the next run tries again.
                TrackingLog.i("limit_check_failed", "error" to e.javaClass.simpleName)
            } finally {
                jobFinished(params, false)
            }
        }
        return true
    }

    override fun onStopJob(params: JobParameters): Boolean {
        job?.cancel()
        return false // Periodic: the next run comes anyway.
    }
}

/** Schedules the periodic check only while it can do something useful. */
object LimitCheckScheduler {
    private const val JOB_ID = 4201

    /**
     * @param needed limits are on, something is measurable, and notifications are allowed.
     * Already-scheduled jobs are left alone, so app launches don't keep resetting the period.
     */
    fun sync(context: Context, needed: Boolean) {
        val scheduler = context.getSystemService(JobScheduler::class.java) ?: return
        val pending = scheduler.getPendingJob(JOB_ID)
        if (!needed) {
            if (pending != null) {
                scheduler.cancel(JOB_ID)
                TrackingLog.i("limit_check_unscheduled")
            }
            return
        }
        if (pending != null) return
        val info = JobInfo.Builder(JOB_ID, ComponentName(context, LimitCheckJobService::class.java))
            .setPeriodic(TimeUnit.MINUTES.toMillis(15), TimeUnit.MINUTES.toMillis(5))
            // Survives reboots (needs RECEIVE_BOOT_COMPLETED).
            .setPersisted(true)
            .build()
        val result = scheduler.schedule(info)
        TrackingLog.i("limit_check_scheduled", "ok" to (result == JobScheduler.RESULT_SUCCESS))
    }
}
