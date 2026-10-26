package com.sunpawtechnologies.scrollless.ui.analytics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sunpawtechnologies.scrollless.R
import com.sunpawtechnologies.scrollless.core.time.LocalPeriod
import com.sunpawtechnologies.scrollless.core.time.PeriodType
import com.sunpawtechnologies.scrollless.domain.usecase.BucketKey
import com.sunpawtechnologies.scrollless.domain.usecase.BucketState
import com.sunpawtechnologies.scrollless.domain.usecase.ChartBucket
import com.sunpawtechnologies.scrollless.domain.usecase.MetricReport
import com.sunpawtechnologies.scrollless.ui.components.EmptyState
import com.sunpawtechnologies.scrollless.ui.components.IconBadge
import com.sunpawtechnologies.scrollless.ui.components.ScrollLessCard
import com.sunpawtechnologies.scrollless.ui.components.ScrollLessTopAppBar
import com.sunpawtechnologies.scrollless.ui.components.UsageBarChart
import com.sunpawtechnologies.scrollless.ui.components.compactDuration
import com.sunpawtechnologies.scrollless.ui.components.formatDurationMillis
import com.sunpawtechnologies.scrollless.ui.theme.ScrollLessTheme
import com.sunpawtechnologies.scrollless.ui.theme.Spacing
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun AnalyticsRoute(
    onSetUpTracking: () -> Unit,
    viewModel: AnalyticsViewModel = viewModel(factory = AnalyticsViewModel.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LifecycleResumeEffect(viewModel) {
        viewModel.onResume()
        onPauseOrDispose {}
    }
    AnalyticsScreen(
        uiState = uiState,
        onPeriodTypeSelected = viewModel::onPeriodTypeSelected,
        onPrevious = viewModel::onPrevious,
        onNext = viewModel::onNext,
        onSetUpTracking = onSetUpTracking,
    )
}

@Composable
fun AnalyticsScreen(
    uiState: AnalyticsUiState,
    onPeriodTypeSelected: (PeriodType) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSetUpTracking: () -> Unit,
) {
    Scaffold(topBar = { ScrollLessTopAppBar(title = stringResource(R.string.analytics_title)) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        ) {
            val period = uiState.period
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                PeriodType.entries.forEachIndexed { index, type ->
                    SegmentedButton(
                        selected = period?.type == type,
                        onClick = { onPeriodTypeSelected(type) },
                        shape = SegmentedButtonDefaults.itemShape(index, PeriodType.entries.size),
                    ) { Text(stringResource(type.labelRes())) }
                }
            }
            if (period == null) {
                Box(Modifier.fillMaxWidth().padding(Spacing.xxl), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                return@Column
            }
            PeriodNavigator(period, uiState.canGoForward, uiState.isLoading, onPrevious, onNext)

            val reels = uiState.reels.takeIf { uiState.instagramInstalled }
            val time = uiState.instagramTime.takeIf { uiState.instagramInstalled }
            val youtube = uiState.youtubeTime.takeIf { uiState.youtubeInstalled }
            val anyTracked = listOfNotNull(reels, time, youtube).any { it.trackedBuckets > 0 }
            if (!uiState.isLoading && !anyTracked) {
                ScrollLessCard {
                    EmptyState(
                        icon = R.drawable.ic_bar_chart,
                        title = stringResource(R.string.analytics_empty_title),
                        body = stringResource(R.string.analytics_empty_body),
                        actionLabel = stringResource(R.string.action_set_up_tracking),
                        onAction = onSetUpTracking,
                    )
                }
            } else {
                reels?.let {
                    MetricChartCard(
                        title = stringResource(R.string.analytics_reels_title),
                        icon = R.drawable.ic_play_arrow,
                        color = ScrollLessTheme.colors.seriesReels,
                        report = it,
                        period = period,
                        isCurrentPeriod = uiState.isCurrentPeriod,
                        format = { value -> "≈$value" },
                        axisLabel = { value -> "$value" },
                        valueUnit = 1,
                        showLimitStats = true,
                        limit = uiState.reelsDailyLimit,
                    )
                }
                time?.let {
                    MetricChartCard(
                        title = stringResource(R.string.analytics_time_title),
                        icon = R.drawable.ic_schedule,
                        color = ScrollLessTheme.colors.seriesInstagramTime,
                        report = it,
                        period = period,
                        isCurrentPeriod = uiState.isCurrentPeriod,
                        format = { value -> formatDurationMillis(value) },
                        axisLabel = ::compactDuration,
                        valueUnit = 60_000,
                        showLimitStats = false,
                    )
                }
                youtube?.let {
                    MetricChartCard(
                        title = stringResource(R.string.analytics_youtube_title),
                        icon = R.drawable.ic_smart_display,
                        color = ScrollLessTheme.colors.seriesYoutubeTime,
                        report = it,
                        period = period,
                        isCurrentPeriod = uiState.isCurrentPeriod,
                        format = { value -> formatDurationMillis(value) },
                        axisLabel = ::compactDuration,
                        valueUnit = 60_000,
                        showLimitStats = false,
                    )
                }
                if (!uiState.instagramInstalled && youtube != null) {
                    Text(
                        stringResource(R.string.analytics_instagram_not_installed),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    stringResource(R.string.analytics_footnote),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun PeriodNavigator(period: LocalPeriod, canGoForward: Boolean, loading: Boolean, onPrevious: () -> Unit, onNext: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        IconButton(onClick = onPrevious) {
            Icon(
                painterResource(R.drawable.ic_chevron_right),
                contentDescription = stringResource(R.string.analytics_previous),
                modifier = Modifier.rotate(180f),
            )
        }
        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            Text(periodTitle(period), style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
            if (loading) CircularProgressIndicator(Modifier.padding(start = Spacing.sm).size(16.dp), strokeWidth = 2.dp)
        }
        IconButton(onClick = onNext, enabled = canGoForward) {
            Icon(painterResource(R.drawable.ic_chevron_right), contentDescription = stringResource(R.string.analytics_next))
        }
    }
}

@Composable
private fun MetricChartCard(
    title: String,
    icon: Int,
    color: Color,
    report: MetricReport,
    period: LocalPeriod,
    isCurrentPeriod: Boolean,
    format: @Composable (Long) -> String,
    axisLabel: (Long) -> String,
    valueUnit: Long,
    showLimitStats: Boolean,
    limit: Int? = null,
) {
    var selected by remember(period) { mutableStateOf<Int?>(null) }
    var showTable by rememberSaveable { mutableStateOf(false) }
    val labels = report.buckets.map { shortLabel(it.key, period.type) }
    val notTracked = stringResource(R.string.status_not_tracked)

    ScrollLessCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
            IconBadge(icon, tint = color)
            Text(title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f).semantics { heading() })
        }

        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.lg), modifier = Modifier.fillMaxWidth()) {
            Stat(
                label = stringResource(if (isCurrentPeriod) R.string.stat_total_so_far else R.string.stat_total),
                value = report.total?.let { format(it) } ?: notTracked,
                modifier = Modifier.weight(1f),
            )
            if (period.type != PeriodType.DAY) {
                Stat(
                    label = stringResource(if (period.type == PeriodType.YEAR) R.string.stat_monthly_average else R.string.stat_daily_average),
                    value = report.average?.let { format(it) } ?: notTracked,
                    modifier = Modifier.weight(1f),
                )
            }
            Stat(
                label = stringResource(R.string.stat_peak),
                value = report.peak?.let { longLabel(it.key) } ?: "—",
                modifier = Modifier.weight(1f),
            )
        }

        Text(comparisonText(report, period.type, format), style = MaterialTheme.typography.bodyMedium)
        if (showLimitStats) {
            report.daysAtOrOverLimit?.let {
                Text(
                    stringResource(R.string.stat_days_over_limit, it),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        val selectedBucket = selected?.let { report.buckets.getOrNull(it) }
        Text(
            text = selectedBucket?.let { "${longLabel(it.key)} · ${bucketValueText(it, format)}" }
                ?: stringResource(R.string.chart_tap_hint),
            style = MaterialTheme.typography.labelLarge,
            color = if (selectedBucket != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        UsageBarChart(
            buckets = report.buckets,
            seriesColor = color,
            axisLabel = axisLabel,
            bucketLabels = labels,
            labelEvery = labelEvery(period.type),
            selectedIndex = selected,
            onSelect = { selected = it },
            chartDescription = chartDescription(title, report, format),
            valueUnit = valueUnit,
            limit = limit?.toLong(),
            limitLabel = limit?.let { stringResource(R.string.chart_limit_label, it) },
        )
        ChartLegend()

        TextButton(onClick = { showTable = !showTable }) {
            Text(stringResource(if (showTable) R.string.chart_hide_table else R.string.chart_show_table))
        }
        if (showTable) {
            Column {
                report.buckets.filter { it.state != BucketState.FUTURE }.forEach { bucket ->
                    Row(Modifier.fillMaxWidth().padding(vertical = Spacing.xs)) {
                        Text(longLabel(bucket.key), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                        Text(bucketValueText(bucket, format), style = MaterialTheme.typography.bodyMedium)
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }
    }
}

@Composable
private fun Stat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun ChartLegend() {
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(width = 12.dp, height = 3.dp)
                .background(ScrollLessTheme.colors.notTracked),
        )
        Text(
            stringResource(R.string.chart_legend_not_tracked),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun bucketValueText(bucket: ChartBucket, format: @Composable (Long) -> String): String = when (bucket.state) {
    BucketState.TRACKED -> format(bucket.value ?: 0L)
    BucketState.NOT_TRACKED -> stringResource(R.string.status_not_tracked)
    BucketState.FUTURE -> "—"
}

@Composable
private fun comparisonText(report: MetricReport, type: PeriodType, format: @Composable (Long) -> String): String {
    val comparison = report.comparison ?: return stringResource(R.string.comparison_none)
    val previous = stringResource(
        when (type) {
            PeriodType.DAY -> R.string.previous_day
            PeriodType.WEEK -> R.string.previous_week
            PeriodType.MONTH -> R.string.previous_month
            PeriodType.YEAR -> R.string.previous_year
        },
    )
    val diff = comparison.difference
    return when {
        diff == 0L -> stringResource(R.string.comparison_same, previous)
        else -> {
            val amount = format(kotlin.math.abs(diff))
            val percent = comparison.percentChange?.let { " (${if (it > 0) "+" else ""}$it%)" }.orEmpty()
            stringResource(if (diff > 0) R.string.comparison_more else R.string.comparison_less, amount, previous) + percent
        }
    }
}

@Composable
private fun chartDescription(title: String, report: MetricReport, format: @Composable (Long) -> String): String {
    val total = report.total?.let { format(it) } ?: stringResource(R.string.status_not_tracked)
    return stringResource(R.string.chart_description, title, total, report.trackedBuckets, report.buckets.size)
}

private fun labelEvery(type: PeriodType) = when (type) {
    PeriodType.DAY -> 6
    PeriodType.WEEK -> 1
    PeriodType.MONTH -> 7
    PeriodType.YEAR -> 2
}

/** Week charts show weekday names; month charts show day numbers. */
private fun shortLabel(key: BucketKey, type: PeriodType): String = when (key) {
    is BucketKey.Hour -> "%02d".format(key.hour)
    is BucketKey.Day ->
        if (type == PeriodType.WEEK) key.date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()) else "${key.date.dayOfMonth}"
    is BucketKey.Month -> key.month.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())
}

private fun longLabel(key: BucketKey): String = when (key) {
    is BucketKey.Hour -> "%02d:00–%02d:00".format(key.hour, (key.hour + 1) % 24)
    is BucketKey.Day -> key.date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
    is BucketKey.Month -> "${key.month.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${key.month.year}"
}

private fun periodTitle(period: LocalPeriod): String {
    val medium = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
    return when (period.type) {
        PeriodType.DAY -> period.start.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL))
        PeriodType.WEEK -> "${period.start.format(medium)} – ${period.endExclusive.minusDays(1).format(medium)}"
        PeriodType.MONTH -> "${period.start.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${period.start.year}"
        PeriodType.YEAR -> period.start.year.toString()
    }
}

private fun PeriodType.labelRes(): Int = when (this) {
    PeriodType.DAY -> R.string.period_day
    PeriodType.WEEK -> R.string.period_week
    PeriodType.MONTH -> R.string.period_month
    PeriodType.YEAR -> R.string.period_year
}
