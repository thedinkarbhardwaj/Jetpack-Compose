package com.sunpawtechnologies.scrollless.di

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.CreationExtras
import com.sunpawtechnologies.scrollless.ScrollLessApp
import com.sunpawtechnologies.scrollless.alerts.AndroidLimitNotifier
import com.sunpawtechnologies.scrollless.alerts.LimitCheckScheduler
import com.sunpawtechnologies.scrollless.data.repository.RoomAlertLog
import com.sunpawtechnologies.scrollless.domain.model.hasCurrentAccessibilityConsent
import com.sunpawtechnologies.scrollless.domain.model.isLimitEnabled
import com.sunpawtechnologies.scrollless.domain.usecase.LimitAlertChecker
import com.sunpawtechnologies.scrollless.domain.usecase.LimitAlertRules
import com.sunpawtechnologies.scrollless.domain.usecase.TurnOffReelsTracking
import com.sunpawtechnologies.scrollless.tracking.accessibility.AccessibilityReelsTrackingSwitch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import com.sunpawtechnologies.scrollless.core.time.SystemTimeProvider
import com.sunpawtechnologies.scrollless.core.time.TimeProvider
import com.sunpawtechnologies.scrollless.data.db.ScrollLessDatabase
import com.sunpawtechnologies.scrollless.data.prefs.DataStoreSettingsRepository
import com.sunpawtechnologies.scrollless.data.repository.RoomLimitRepository
import com.sunpawtechnologies.scrollless.data.repository.RoomTrackingCoverageRepository
import com.sunpawtechnologies.scrollless.data.repository.RoomUsageRepository
import com.sunpawtechnologies.scrollless.domain.repository.LimitRepository
import com.sunpawtechnologies.scrollless.domain.repository.SettingsRepository
import com.sunpawtechnologies.scrollless.domain.repository.TrackingCoverageRepository
import com.sunpawtechnologies.scrollless.domain.repository.UsageRepository
import com.sunpawtechnologies.scrollless.permissions.AndroidPermissionMonitor
import com.sunpawtechnologies.scrollless.tracking.usagestats.UsageImporter
import com.sunpawtechnologies.scrollless.tracking.usagestats.UsageStatsImporter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/** App-wide singletons, wired by hand. Everything is lazy so startup only pays for what it uses. */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val timeProvider: TimeProvider = SystemTimeProvider

    /** For work that must outlive a screen or the service instance, e.g. finishing a database write. */
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val database: ScrollLessDatabase by lazy { ScrollLessDatabase.build(appContext) }

    val settingsRepository: SettingsRepository by lazy {
        DataStoreSettingsRepository(
            PreferenceDataStoreFactory.create { appContext.preferencesDataStoreFile(SETTINGS_FILE) },
        )
    }

    val limitRepository: LimitRepository by lazy { RoomLimitRepository(database.limitDao()) }

    val usageRepository: UsageRepository by lazy {
        RoomUsageRepository(database.contentViewDao(), database.usageSessionDao())
    }

    val trackingCoverageRepository: TrackingCoverageRepository by lazy {
        RoomTrackingCoverageRepository(database.trackingCoverageDao())
    }

    val permissionMonitor: AndroidPermissionMonitor by lazy { AndroidPermissionMonitor(appContext) }

    val usageImporter: UsageImporter by lazy {
        UsageStatsImporter(appContext, permissionMonitor, usageRepository, trackingCoverageRepository, timeProvider)
    }

    val reelsTrackingSwitch = AccessibilityReelsTrackingSwitch()

    val turnOffReelsTracking: TurnOffReelsTracking by lazy { TurnOffReelsTracking(settingsRepository, reelsTrackingSwitch) }

    val limitNotifier: AndroidLimitNotifier by lazy { AndroidLimitNotifier(appContext) }

    val limitAlertChecker: LimitAlertChecker by lazy {
        LimitAlertChecker(
            permissionMonitor = permissionMonitor,
            settingsRepository = settingsRepository,
            usageRepository = usageRepository,
            limitRepository = limitRepository,
            usageImporter = usageImporter,
            alertLog = RoomAlertLog(database.warningDao()),
            notifier = limitNotifier,
            timeProvider = timeProvider,
        )
    }

    /**
     * Schedules the periodic limit check only while it can do something: a limit is on for a
     * metric that's measured, and notifications are allowed. Call after permissions may have changed.
     */
    fun syncLimitCheckSchedule() {
        appScope.launch {
            val settings = settingsRepository.settings.first()
            val measured = LimitAlertRules.measurableMetrics(permissionMonitor.state.value, settings.hasCurrentAccessibilityConsent())
            val needed = limitNotifier.canNotify() && measured.any(settings::isLimitEnabled)
            LimitCheckScheduler.sync(appContext, needed)
        }
    }

    private companion object {
        const val SETTINGS_FILE = "settings"
    }
}

/** For ViewModel factories: `initializer { MyViewModel(appContainer().someRepository) }`. */
fun CreationExtras.appContainer(): AppContainer = (this[APPLICATION_KEY] as ScrollLessApp).container
