package com.sunpawtechnologies.scrollless

import android.app.Application
import android.os.Build
import com.sunpawtechnologies.scrollless.di.AppContainer
import com.sunpawtechnologies.scrollless.tracking.TrackingLog

class ScrollLessApp : Application() {
    val container: AppContainer by lazy { AppContainer(this) }

    override fun onCreate() {
        super.onCreate()
        // A new process. If this appears without the user opening the app, Android or the phone's
        // battery manager killed and restarted it (e.g. to rebind the accessibility service).
        TrackingLog.i("app_process_started", "manufacturer" to Build.MANUFACTURER, "sdk" to Build.VERSION.SDK_INT)
    }
}
