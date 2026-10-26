package com.sunpawtechnologies.scrollless.ui.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.sunpawtechnologies.scrollless.core.time.LocalPeriod
import com.sunpawtechnologies.scrollless.core.time.PeriodType
import com.sunpawtechnologies.scrollless.core.time.TimeProvider
import com.sunpawtechnologies.scrollless.di.appContainer
import com.sunpawtechnologies.scrollless.domain.model.ContentKind
import com.sunpawtechnologies.scrollless.domain.model.LimitMetric
import com.sunpawtechnologies.scrollless.domain.model.Platform
import com.sunpawtechnologies.scrollless.domain.model.TrackingSource
import com.sunpawtechnologies.scrollless.domain.model.UsageMode
import com.sunpawtechnologies.scrollless.domain.model.isLimitEnabled
import com.sunpawtechnologies.scrollless.domain.repository.LimitRepository
import com.sunpawtechnologies.scrollless.domain.repository.SettingsRepository
import com.sunpawtechnologies.scrollless.domain.repository.PermissionMonitor
import com.sunpawtechnologies.scrollless.domain.repository.TrackingCoverageRepository
import com.sunpawtechnologies.scrollless.domain.repository.UsageRepository
import com.sunpawtechnologies.scrollless.domain.repository.limitOn
import com.sunpawtechnologies.scrollless.domain.usecase.AnalyticsCalculator
import com.sunpawtechnologies.scrollless.domain.usecase.MetricReport
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.time.LocalDate

data class AnalyticsUiState(
    val isLoading: Boolean = true,
    val period: LocalPeriod? = null,
    val reels: MetricReport? = null,
    val instagramTime: MetricReport? = null,
    /** YouTube app time (Basic tracking): Shorts and regular videos together. */
    val youtubeTime: MetricReport? = null,
    /** Charts for an app are replaced by a short note while it isn't installed. */
    val instagramInstalled: Boolean = true,
    val youtubeInstalled: Boolean = true,
    /** Daily Reels limit for the limit line; only for week/month, where bars are days. */
    val reelsDailyLimit: Int? = null,
    /** The selected period contains today, so its totals are "so far". */
    val isCurrentPeriod: Boolean = true,
    val canGoForward: Boolean = false,
)

private data class Selection(val type: PeriodType, val anchor: LocalDate, val refresh: Int)

@OptIn(ExperimentalCoroutinesApi::class)
class AnalyticsViewModel(
    private val usageRepository: UsageRepository,
    private val limitRepository: LimitRepository,
    private val coverageRepository: TrackingCoverageRepository,
    private val timeProvider: TimeProvider,
    private val permissionMonitor: PermissionMonitor? = null,
    private val settingsRepository: SettingsRepository? = null,
) : ViewModel() {

    private val selection = MutableStateFlow(Selection(PeriodType.WEEK, timeProvider.today(), refresh = 0))
    private val loading = MutableStateFlow(true)

    val uiState: StateFlow<AnalyticsUiState> = combine(selection.mapLatest(::load), loading) { state, isLoading ->
        state.copy(isLoading = isLoading)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AnalyticsUiState())

    fun onPeriodTypeSelected(type: PeriodType) = selection.update { it.copy(type = type, anchor = timeProvider.today()) }

    fun onPrevious() = selection.update { it.copy(anchor = periodOf(it).previous(timeProvider.weekFields()).start) }

    fun onNext() = selection.update { current ->
        val next = periodOf(current).endExclusive
        if (next.isAfter(timeProvider.today())) current else current.copy(anchor = next)
    }

    /** Reloads, e.g. after returning from Instagram, keeping the selected period. */
    fun onResume() = selection.update { it.copy(refresh = it.refresh + 1) }

    private fun periodOf(selection: Selection) =
        LocalPeriod.containing(selection.type, selection.anchor, timeProvider.weekFields())

    private suspend fun load(selection: Selection): AnalyticsUiState {
        loading.value = true
        val today = timeProvider.today()
        val zone = timeProvider.zone()
        val now = timeProvider.now()
        val period = periodOf(selection)
        val previous = period.previous(timeProvider.weekFields())
        val from = previous.start
        val to = period.endExclusive.minusDays(1)
        val currentHour = now.atZone(zone).hour
        val isDay = period.type == PeriodType.DAY

        val reelsHistory = limitRepository.history(LimitMetric.INSTAGRAM_REELS)
        val enabled = settingsRepository?.settings?.first()
        // Time limits are in minutes; the reports are in milliseconds. A switched-off limit has no "days over".
        suspend fun timeLimitOn(metric: LimitMetric): ((LocalDate) -> Int)? {
            if (enabled != null && !enabled.isLimitEnabled(metric)) return null
            val history = limitRepository.history(metric)
            return { day -> history.limitOn(day, metric.defaultDailyLimit) * 60_000 }
        }
        val reels = AnalyticsCalculator.report(
            period = period,
            previous = previous,
            dailyValues = usageRepository.contentViewCountsByDay(ContentKind.INSTAGRAM_REEL, from, to),
            trackedDays = coverageRepository.trackedDays(TrackingSource.ACCESSIBILITY, from, to, zone, now),
            today = today,
            currentHour = currentHour,
            hourlyValues = if (isDay) usageRepository.contentViewCountsByHour(ContentKind.INSTAGRAM_REEL, period.start) else emptyMap(),
            limitOn = { reelsHistory.limitOn(it, LimitMetric.INSTAGRAM_REELS.defaultDailyLimit) },
        )
        val time = AnalyticsCalculator.report(
            period = period,
            previous = previous,
            dailyValues = usageRepository.usageMillisByDay(Platform.INSTAGRAM, UsageMode.APP_FOREGROUND, from, to),
            trackedDays = coverageRepository.trackedDays(TrackingSource.USAGE_STATS, from, to, zone, now),
            today = today,
            currentHour = currentHour,
            hourlyValues = if (isDay) usageRepository.usageMillisByHour(Platform.INSTAGRAM, UsageMode.APP_FOREGROUND, period.start) else emptyMap(),
            limitOn = timeLimitOn(LimitMetric.INSTAGRAM_APP_TIME),
        )
        val youtube = AnalyticsCalculator.report(
            period = period,
            previous = previous,
            dailyValues = usageRepository.usageMillisByDay(Platform.YOUTUBE, UsageMode.APP_FOREGROUND, from, to),
            trackedDays = coverageRepository.trackedDays(TrackingSource.USAGE_STATS, from, to, zone, now),
            today = today,
            currentHour = currentHour,
            hourlyValues = if (isDay) usageRepository.usageMillisByHour(Platform.YOUTUBE, UsageMode.APP_FOREGROUND, period.start) else emptyMap(),
            limitOn = timeLimitOn(LimitMetric.YOUTUBE_APP_TIME),
        )
        val permissions = permissionMonitor?.state?.value
        loading.value = false
        return AnalyticsUiState(
            isLoading = false,
            period = period,
            reels = reels,
            instagramTime = time,
            youtubeTime = youtube,
            instagramInstalled = permissions?.instagramInstalled ?: true,
            youtubeInstalled = permissions?.youtubeInstalled ?: true,
            reelsDailyLimit = if (period.type == PeriodType.WEEK || period.type == PeriodType.MONTH) {
                reelsHistory.limitOn(minOf(to, today), LimitMetric.INSTAGRAM_REELS.defaultDailyLimit)
            } else {
                null
            },
            isCurrentPeriod = today in period,
            canGoForward = !period.endExclusive.isAfter(today),
        )
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val container = appContainer()
                AnalyticsViewModel(
                    container.usageRepository,
                    container.limitRepository,
                    container.trackingCoverageRepository,
                    container.timeProvider,
                    container.permissionMonitor,
                    container.settingsRepository,
                )
            }
        }
    }
}
