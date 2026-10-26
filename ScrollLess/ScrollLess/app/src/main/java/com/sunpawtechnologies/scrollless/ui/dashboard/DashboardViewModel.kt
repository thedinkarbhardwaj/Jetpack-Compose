package com.sunpawtechnologies.scrollless.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.sunpawtechnologies.scrollless.core.time.TimeProvider
import com.sunpawtechnologies.scrollless.di.appContainer
import com.sunpawtechnologies.scrollless.domain.model.AppSettings
import com.sunpawtechnologies.scrollless.domain.model.ContentKind
import com.sunpawtechnologies.scrollless.domain.model.LimitMetric
import com.sunpawtechnologies.scrollless.domain.model.LimitStatus
import com.sunpawtechnologies.scrollless.domain.model.PermissionSnapshot
import com.sunpawtechnologies.scrollless.domain.model.Platform
import com.sunpawtechnologies.scrollless.domain.model.TrackingAvailability
import com.sunpawtechnologies.scrollless.domain.model.TrackingSource
import com.sunpawtechnologies.scrollless.domain.model.UsageMode
import com.sunpawtechnologies.scrollless.domain.model.hasCurrentAccessibilityConsent
import com.sunpawtechnologies.scrollless.domain.model.isLimitEnabled
import com.sunpawtechnologies.scrollless.domain.model.limitAlertKey
import com.sunpawtechnologies.scrollless.domain.repository.LimitRepository
import com.sunpawtechnologies.scrollless.domain.repository.PermissionMonitor
import com.sunpawtechnologies.scrollless.domain.repository.SettingsRepository
import com.sunpawtechnologies.scrollless.domain.repository.TrackingCoverageRepository
import com.sunpawtechnologies.scrollless.domain.repository.UsageRepository
import com.sunpawtechnologies.scrollless.domain.usecase.LimitAlertRules
import com.sunpawtechnologies.scrollless.domain.usecase.TrackingIssue
import com.sunpawtechnologies.scrollless.domain.usecase.TrackingMode
import com.sunpawtechnologies.scrollless.domain.usecase.TrackingReadiness
import com.sunpawtechnologies.scrollless.tracking.TrackingLog
import com.sunpawtechnologies.scrollless.tracking.usagestats.UsageImporter
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.LocalDate

/** A metric that is either measured, or not measurable for a stated reason. Never a silent zero. */
sealed interface MetricState<out T> {
    data object Loading : MetricState<Nothing>
    data class Unmeasured(val availability: TrackingAvailability) : MetricState<Nothing>
    data class Measured<T>(val value: T) : MetricState<T>
}

enum class DayPart { MORNING, AFTERNOON, EVENING }

/** Yesterday's totals. A field is null when that metric wasn't tracked yesterday. */
data class YesterdaySummary(val reels: Int?, val instagramTime: Duration?)

/** A switched-on limit and where today's measured value stands against it. */
data class LimitProgress(val limit: Int, val status: LimitStatus)

/**
 * "You've reached your limit", shown once per limit and day (or when opened from a reminder).
 * @param value today's value in base units (milliseconds or items).
 */
data class LimitNotice(val metric: LimitMetric, val status: LimitStatus, val value: Long, val limit: Int)

data class DashboardUiState(
    val dayPart: DayPart = DayPart.MORNING,
    val reels: MetricState<Int> = MetricState.Loading,
    val reelsDailyLimit: Int = LimitMetric.INSTAGRAM_REELS.defaultDailyLimit,
    /** Null when Reels aren't measured or their limit is off. */
    val reelsStatus: LimitStatus? = null,
    /** Instagram foreground time: all of Instagram, not only Reels. */
    val instagramTime: MetricState<Duration> = MetricState.Loading,
    val instagramLimit: LimitProgress? = null,
    /** YouTube app time, all content (Basic tracking). Shorts counts aren't available. */
    val youtubeTime: MetricState<Duration> = MetricState.Loading,
    val youtubeLimit: LimitProgress? = null,
    val yesterday: YesterdaySummary? = null,
    val mode: TrackingMode = TrackingMode.NONE,
    /** At most one problem, shown as a small message with one fix. Choosing Basic only is not a problem. */
    val issue: TrackingIssue? = null,
    /** ScrollLess's Accessibility service is switched on, so payment-app guidance and the off switch are shown. */
    val reelsTrackingSwitchedOn: Boolean = false,
    val limitNotice: LimitNotice? = null,
) {
    /** The most urgent status among the limits that are on and measured. */
    val worstStatus: LimitStatus?
        get() = listOfNotNull(instagramLimit?.status, youtubeLimit?.status, reelsStatus).maxByOrNull { it.ordinal }
}

sealed interface DashboardEffect {
    /** Reels tracking couldn't be switched off in-app; the user has to do it in Accessibility settings. */
    data object TurnOffInSettings : DashboardEffect
    data object ReelsTrackingTurnedOff : DashboardEffect
}

private data class Usage(
    /** The day these values belong to, so a date change never mixes old values with the new day. */
    val day: LocalDate,
    val reels: Int,
    val instagramTime: Duration,
    val limits: Map<LimitMetric, Int>,
    val reelsYesterday: Int,
    val timeYesterday: Duration,
    val youtubeTime: Duration,
)

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModel(
    permissionMonitor: PermissionMonitor,
    private val settingsRepository: SettingsRepository,
    usageRepository: UsageRepository,
    limitRepository: LimitRepository,
    private val coverageRepository: TrackingCoverageRepository,
    private val usageImporter: UsageImporter,
    private val timeProvider: TimeProvider,
    private val turnOffReelsTracking: (suspend () -> Boolean)? = null,
) : ViewModel() {

    /** Re-evaluated on every resume, so the dashboard rolls over to the new day after midnight. */
    private val today = MutableStateFlow(timeProvider.today())

    /** Sources that were tracking at some point yesterday. Loaded on resume. */
    private val trackedYesterday = MutableStateFlow<Set<TrackingSource>>(emptySet())

    /** A limit the user came here for from a reminder; shown even if dismissed earlier today. */
    private val requestedNotice = MutableStateFlow<LimitMetric?>(null)

    private val _effects = Channel<DashboardEffect>(Channel.BUFFERED)
    val effects: Flow<DashboardEffect> = _effects.receiveAsFlow()

    private val usage = today.flatMapLatest { day ->
        val yesterday = day.minusDays(1)
        combine(
            usageRepository.observeContentViewCount(ContentKind.INSTAGRAM_REEL, day),
            usageRepository.observeUsageDuration(Platform.INSTAGRAM, UsageMode.APP_FOREGROUND, day),
            limitRepository.observeLimitsOn(day),
            usageRepository.observeContentViewCount(ContentKind.INSTAGRAM_REEL, yesterday),
            usageRepository.observeUsageDuration(Platform.INSTAGRAM, UsageMode.APP_FOREGROUND, yesterday),
        ) { reels, time, limits, reelsYesterday, timeYesterday ->
            Usage(day, reels, time, limits, reelsYesterday, timeYesterday, Duration.ZERO)
        }.combine(usageRepository.observeUsageDuration(Platform.YOUTUBE, UsageMode.APP_FOREGROUND, day)) { usage, youtube ->
            usage.copy(youtubeTime = youtube)
        }
    }

    private val inputs = combine(permissionMonitor.state, settingsRepository.settings, usage, trackedYesterday) { p, s, u, t ->
        Inputs(p, s, u, t)
    }

    private data class Inputs(
        val permissions: PermissionSnapshot,
        val settings: AppSettings,
        val usage: Usage,
        val tracked: Set<TrackingSource>,
    )

    val uiState: StateFlow<DashboardUiState> =
        combine(inputs, requestedNotice) { (permissions, settings, usage, tracked), requested ->
            val consent = settings.hasCurrentAccessibilityConsent()
            val reels = metric(TrackingReadiness.instagramReels(permissions, consent), usage.reels)
            val instagramTime = metric(TrackingReadiness.instagramTime(permissions), usage.instagramTime)
            val youtubeTime = metric(TrackingReadiness.youtubeTime(permissions), usage.youtubeTime)

            val values = buildMap {
                (instagramTime as? MetricState.Measured)?.let { put(LimitMetric.INSTAGRAM_APP_TIME, it.value.toMillis()) }
                (youtubeTime as? MetricState.Measured)?.let { put(LimitMetric.YOUTUBE_APP_TIME, it.value.toMillis()) }
                (reels as? MetricState.Measured)?.let { put(LimitMetric.INSTAGRAM_REELS, it.value.toLong()) }
            }
            val statuses = values.filterKeys(settings::isLimitEnabled).mapValues { (metric, value) ->
                LimitAlertRules.status(metric, value, usage.limits.getValue(metric), settings.warningThresholdPercent)
            }
            fun progress(metric: LimitMetric) = statuses[metric]?.let { LimitProgress(usage.limits.getValue(metric), it) }

            DashboardUiState(
                dayPart = dayPart(),
                reels = reels,
                reelsDailyLimit = usage.limits.getValue(LimitMetric.INSTAGRAM_REELS),
                reelsStatus = statuses[LimitMetric.INSTAGRAM_REELS],
                instagramTime = instagramTime,
                instagramLimit = progress(LimitMetric.INSTAGRAM_APP_TIME),
                youtubeTime = youtubeTime,
                youtubeLimit = progress(LimitMetric.YOUTUBE_APP_TIME),
                yesterday = if (permissions.instagramInstalled) yesterday(tracked, usage) else null,
                mode = TrackingReadiness.mode(permissions, consent),
                // Turning Reels tracking off from inside ScrollLess is a choice, not a problem to report.
                issue = TrackingReadiness.issue(
                    permissions,
                    consent,
                    everOptedIn = settings.accessibilityConsent != null && !settings.reelsTrackingTurnedOffByUser,
                ),
                reelsTrackingSwitchedOn = permissions.accessibilityServiceEnabled,
                limitNotice = limitNotice(statuses, values, usage.limits, settings, usage.day, requested),
            )
        }
            .distinctUntilChanged()
            .onEach {
                TrackingLog.d(
                    "dashboard_update",
                    "reels" to it.reels.describe(),
                    "instagram_time" to it.instagramTime.describe(),
                    "mode" to it.mode,
                    "issue" to it.issue,
                    "notice" to it.limitNotice?.metric,
                )
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardUiState())

    /** "Use Basic only": withdraw the Reels-tracking opt-in, so its absence is no longer reported. */
    fun onUseBasicOnly() {
        viewModelScope.launch { settingsRepository.clearAccessibilityConsent() }
    }

    fun onResume() {
        val day = timeProvider.today()
        today.value = day
        viewModelScope.launch {
            usageImporter.importRecentUsage()
            val yesterday = day.minusDays(1)
            trackedYesterday.value = TrackingSource.entries.filterTo(mutableSetOf()) { source ->
                coverageRepository.trackedDays(source, yesterday, yesterday, timeProvider.zone(), timeProvider.now()).isNotEmpty()
            }
        }
    }

    /** The user tapped a limit reminder for [metric]. */
    fun onOpenedFromReminder(metric: LimitMetric) {
        requestedNotice.value = metric
    }

    /** Any choice in the notice (break, dismiss, change limit) means it was seen today. */
    fun onLimitNoticeHandled() {
        val notice = uiState.value.limitNotice
        requestedNotice.value = null
        if (notice == null || notice.status != LimitStatus.REACHED) return
        val day = today.value.toEpochDay()
        viewModelScope.launch { settingsRepository.acknowledgeLimitAlert(limitAlertKey(day, notice.metric), day) }
    }

    fun onTurnOffReelsTracking() {
        val turnOff = turnOffReelsTracking ?: return
        viewModelScope.launch {
            _effects.send(if (turnOff()) DashboardEffect.ReelsTrackingTurnedOff else DashboardEffect.TurnOffInSettings)
        }
    }

    private fun limitNotice(
        statuses: Map<LimitMetric, LimitStatus>,
        values: Map<LimitMetric, Long>,
        limits: Map<LimitMetric, Int>,
        settings: AppSettings,
        day: LocalDate,
        requested: LimitMetric?,
    ): LimitNotice? {
        fun notice(metric: LimitMetric) = LimitNotice(metric, statuses.getValue(metric), values.getValue(metric), limits.getValue(metric))
        // From a reminder: show whatever stands now (a raised limit may have made it "within").
        if (requested != null && statuses[requested] != null && statuses[requested] != LimitStatus.WITHIN) return notice(requested)
        // Otherwise: the first reached limit not yet dismissed today, in display order.
        return LimitMetric.configurable.firstOrNull { metric ->
            statuses[metric] == LimitStatus.REACHED && limitAlertKey(day.toEpochDay(), metric) !in settings.acknowledgedLimitAlerts
        }?.let(::notice)
    }

    private fun yesterday(tracked: Set<TrackingSource>, usage: Usage): YesterdaySummary? {
        val summary = YesterdaySummary(
            reels = usage.reelsYesterday.takeIf { TrackingSource.ACCESSIBILITY in tracked },
            instagramTime = usage.timeYesterday.takeIf { TrackingSource.USAGE_STATS in tracked },
        )
        return summary.takeIf { it.reels != null || it.instagramTime != null }
    }

    private fun dayPart(): DayPart = when (timeProvider.now().atZone(timeProvider.zone()).hour) {
        in 5..11 -> DayPart.MORNING
        in 12..16 -> DayPart.AFTERNOON
        else -> DayPart.EVENING
    }

    private fun <T> metric(availability: TrackingAvailability, value: T): MetricState<T> =
        if (availability == TrackingAvailability.Active) MetricState.Measured(value) else MetricState.Unmeasured(availability)

    private fun MetricState<*>.describe(): String = when (this) {
        MetricState.Loading -> "loading"
        is MetricState.Unmeasured -> "unmeasured($availability)"
        is MetricState.Measured -> "measured($value)"
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val container = appContainer()
                DashboardViewModel(
                    permissionMonitor = container.permissionMonitor,
                    settingsRepository = container.settingsRepository,
                    usageRepository = container.usageRepository,
                    limitRepository = container.limitRepository,
                    coverageRepository = container.trackingCoverageRepository,
                    usageImporter = container.usageImporter,
                    timeProvider = container.timeProvider,
                    turnOffReelsTracking = container.turnOffReelsTracking::invoke,
                )
            }
        }
    }
}
