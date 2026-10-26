package com.sunpawtechnologies.scrollless.ui.dashboard

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sunpawtechnologies.scrollless.R
import com.sunpawtechnologies.scrollless.domain.model.LimitMetric
import com.sunpawtechnologies.scrollless.domain.model.LimitStatus
import com.sunpawtechnologies.scrollless.domain.model.LimitUnit
import com.sunpawtechnologies.scrollless.domain.model.SetupRequirement
import com.sunpawtechnologies.scrollless.domain.model.TrackingAvailability
import com.sunpawtechnologies.scrollless.domain.model.UnavailableReason
import com.sunpawtechnologies.scrollless.domain.usecase.TrackingIssue
import com.sunpawtechnologies.scrollless.domain.usecase.TrackingMode
import com.sunpawtechnologies.scrollless.permissions.SettingsTarget
import com.sunpawtechnologies.scrollless.permissions.openSystemSettings
import com.sunpawtechnologies.scrollless.ui.components.IconBadge
import com.sunpawtechnologies.scrollless.ui.components.LimitStatusPill
import com.sunpawtechnologies.scrollless.ui.components.ProgressRing
import com.sunpawtechnologies.scrollless.ui.components.ScrollLessCard
import com.sunpawtechnologies.scrollless.ui.components.SectionHeader
import com.sunpawtechnologies.scrollless.ui.components.StatusPill
import com.sunpawtechnologies.scrollless.ui.components.color
import com.sunpawtechnologies.scrollless.ui.components.formatDuration
import com.sunpawtechnologies.scrollless.ui.theme.ScrollLessTheme
import com.sunpawtechnologies.scrollless.ui.theme.Spacing
import com.sunpawtechnologies.scrollless.ui.util.ObserveAsEvents
import kotlinx.coroutines.launch
import java.time.Duration

@Composable
fun DashboardRoute(
    onOpenSettings: () -> Unit,
    onOpenLimits: () -> Unit,
    onOpenAnalytics: () -> Unit,
    onSetUpTracking: () -> Unit,
    /** Set when the app was opened from a limit reminder. */
    reminderMetric: LimitMetric? = null,
    onReminderConsumed: () -> Unit = {},
    viewModel: DashboardViewModel = viewModel(factory = DashboardViewModel.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val resources = LocalResources.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    LifecycleResumeEffect(viewModel) {
        viewModel.onResume()
        onPauseOrDispose {}
    }
    LaunchedEffect(reminderMetric) {
        if (reminderMetric != null) {
            viewModel.onOpenedFromReminder(reminderMetric)
            onReminderConsumed()
        }
    }
    ObserveAsEvents(viewModel.effects) { effect ->
        val message = when (effect) {
            DashboardEffect.ReelsTrackingTurnedOff -> resources.getString(R.string.settings_reels_turned_off)
            DashboardEffect.TurnOffInSettings -> {
                context.openSystemSettings(SettingsTarget.ACCESSIBILITY)
                resources.getString(R.string.settings_turn_off_in_settings)
            }
        }
        scope.launch { snackbarHostState.showSnackbar(message) }
    }
    Box(Modifier.fillMaxSize()) {
        DashboardScreen(
            uiState = uiState,
            onOpenSettings = onOpenSettings,
            onOpenLimits = onOpenLimits,
            onOpenAnalytics = onOpenAnalytics,
            onSetUpTracking = onSetUpTracking,
            onUseBasicOnly = viewModel::onUseBasicOnly,
            onTurnOffReelsTracking = viewModel::onTurnOffReelsTracking,
        )
        SnackbarHost(snackbarHostState, Modifier.align(Alignment.BottomCenter))
    }
    uiState.limitNotice?.let { notice ->
        LimitNoticeSheet(
            notice = notice,
            onTakeBreak = {
                viewModel.onLimitNoticeHandled()
                context.goToHomeScreen()
            },
            onChangeLimit = {
                viewModel.onLimitNoticeHandled()
                onOpenLimits()
            },
            onDismiss = viewModel::onLimitNoticeHandled,
        )
    }
}

@Composable
fun DashboardScreen(
    uiState: DashboardUiState,
    onOpenSettings: () -> Unit,
    onOpenLimits: () -> Unit,
    onOpenAnalytics: () -> Unit,
    onSetUpTracking: () -> Unit,
    onUseBasicOnly: () -> Unit = {},
    onTurnOffReelsTracking: () -> Unit = {},
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
        verticalArrangement = Arrangement.spacedBy(Spacing.lg),
    ) {
        Header(uiState, onOpenSettings)

        uiState.issue?.let { IssueCard(it, onSetUpTracking, onUseBasicOnly) }

        AppTimeCard(
            title = R.string.metric_instagram_time_title,
            note = R.string.metric_instagram_time_note,
            icon = R.drawable.ic_schedule,
            tint = ScrollLessTheme.colors.seriesInstagramTime,
            state = uiState.instagramTime,
            limit = uiState.instagramLimit,
            hero = true,
            onSetUp = onSetUpTracking,
        )

        AppTimeCard(
            title = R.string.metric_youtube_time_title,
            note = R.string.metric_youtube_time_note,
            icon = R.drawable.ic_smart_display,
            tint = ScrollLessTheme.colors.seriesYoutubeTime,
            state = uiState.youtubeTime,
            limit = uiState.youtubeLimit,
            hero = false,
            onSetUp = onSetUpTracking,
        )

        ReelsCard(uiState, onSetUpTracking)

        if (uiState.reelsTrackingSwitchedOn) PaymentAppsHint(onTurnOffReelsTracking)

        SectionHeader(
            title = stringResource(R.string.dashboard_insights),
            actionLabel = stringResource(R.string.action_view_analytics),
            onAction = onOpenAnalytics,
        )
        InsightsCard(uiState)

        LimitsCtaCard(onOpenLimits)

        Spacer(Modifier.size(Spacing.sm))
    }
}

@Composable
private fun Header(uiState: DashboardUiState, onOpenSettings: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = stringResource(
                    when (uiState.dayPart) {
                        DayPart.MORNING -> R.string.greeting_morning
                        DayPart.AFTERNOON -> R.string.greeting_afternoon
                        DayPart.EVENING -> R.string.greeting_evening
                    },
                ),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                text = stringResource(wellbeingMessage(uiState.worstStatus)),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = onOpenSettings) {
            Icon(painterResource(R.drawable.ic_settings), contentDescription = stringResource(R.string.settings_title))
        }
    }
    if (uiState.mode != TrackingMode.NONE && uiState.issue == null) {
        StatusPill(
            text = stringResource(if (uiState.mode == TrackingMode.ADVANCED) R.string.mode_advanced_active else R.string.mode_basic_active),
            icon = R.drawable.ic_check_circle,
            tint = ScrollLessTheme.colors.statusGood,
        )
    }
}

@StringRes
private fun wellbeingMessage(status: LimitStatus?): Int = when (status) {
    LimitStatus.APPROACHING -> R.string.wellbeing_approaching
    LimitStatus.REACHED -> R.string.wellbeing_reached
    else -> R.string.tagline
}

/** A small status message with exactly one fix (plus "use Basic only" where that's a fair choice). */
@Composable
private fun IssueCard(issue: TrackingIssue, onFix: () -> Unit, onUseBasicOnly: () -> Unit) {
    val (title, body, action) = when (issue) {
        TrackingIssue.INSTAGRAM_NOT_INSTALLED ->
            Triple(R.string.dashboard_instagram_missing_title, R.string.dashboard_instagram_missing_body, null)
        TrackingIssue.ADVANCED_STOPPED ->
            Triple(R.string.issue_stopped_title, R.string.issue_stopped_body, R.string.action_fix_tracking)
        TrackingIssue.ADVANCED_TURNED_OFF ->
            Triple(R.string.issue_turned_off_title, R.string.issue_turned_off_body, R.string.action_turn_back_on)
        TrackingIssue.ADVANCED_NEEDS_CONSENT ->
            Triple(R.string.issue_needs_consent_title, R.string.issue_needs_consent_body, R.string.accessibility_review_disclosure)
        TrackingIssue.USAGE_ACCESS_MISSING ->
            Triple(R.string.overview_not_configured_title, R.string.overview_not_configured_body, R.string.action_set_up_tracking)
    }
    ScrollLessCard(
        containerColor = ScrollLessTheme.colors.warningContainer,
        contentColor = ScrollLessTheme.colors.onWarningContainer,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md), verticalAlignment = Alignment.Top) {
            Icon(painterResource(R.drawable.ic_info), contentDescription = null)
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                Text(stringResource(title), style = MaterialTheme.typography.titleSmall)
                Text(stringResource(body), style = MaterialTheme.typography.bodyMedium)
            }
        }
        if (action != null) {
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm), verticalAlignment = Alignment.CenterVertically) {
                Button(onClick = onFix) { Text(stringResource(action)) }
                if (issue == TrackingIssue.ADVANCED_TURNED_OFF) {
                    TextButton(onClick = onUseBasicOnly) { Text(stringResource(R.string.action_use_basic_only)) }
                }
            }
        }
    }
}

/**
 * App open time against its limit. The hero variant sits on the brand gradient. The minutes count
 * up to the new value in 600 ms when data changes; the ring sweeps with it.
 */
@Composable
private fun AppTimeCard(
    @StringRes title: Int,
    @StringRes note: Int,
    @DrawableRes icon: Int,
    tint: Color,
    state: MetricState<Duration>,
    limit: LimitProgress?,
    hero: Boolean,
    onSetUp: () -> Unit,
) {
    val colors = ScrollLessTheme.colors
    val container = if (hero) {
        Modifier.background(Brush.linearGradient(listOf(colors.heroStart, colors.heroEnd)))
    } else {
        Modifier.background(MaterialTheme.colorScheme.surfaceContainer)
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .then(container)
            .padding(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
            IconBadge(icon, tint = tint)
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(title),
                    style = if (hero) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleSmall,
                    modifier = Modifier.semantics { heading() },
                )
                Text(stringResource(note), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            limit?.let { LimitStatusPill(it.status) }
        }
        when (state) {
            MetricState.Loading -> Text(stringResource(R.string.metric_loading))
            is MetricState.Unmeasured -> UnmeasuredContent(state.availability, onSetUp)
            is MetricState.Measured -> MeasuredTime(state.value, limit, ringSize = if (hero) 132.dp else 96.dp)
        }
    }
}

@Composable
private fun MeasuredTime(time: Duration, limit: LimitProgress?, ringSize: Dp) {
    val minutes by animateIntAsState(time.toMinutes().toInt(), animationSpec = tween(600), label = "minutes")
    val shown = formatDuration(if (minutes == time.toMinutes().toInt()) time else Duration.ofMinutes(minutes.toLong()))
    if (limit == null) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Text(shown, style = MaterialTheme.typography.headlineMedium)
            Text(
                stringResource(R.string.metric_no_limit),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.xl)) {
        ProgressRing(
            progress = time.toMillis() / (limit.limit * 60_000f),
            color = limit.status.color(),
            size = ringSize,
            strokeWidth = if (ringSize < 120.dp) 9.dp else 12.dp,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    shown,
                    style = if (ringSize < 120.dp) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center,
                )
                Text(
                    stringResource(R.string.metric_of_minutes, limit.limit),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs), modifier = Modifier.weight(1f)) {
            val leftMinutes = limit.limit - time.toMinutes()
            Text(
                text = if (leftMinutes > 0) {
                    stringResource(R.string.metric_minutes_left, leftMinutes.toInt())
                } else {
                    stringResource(R.string.metric_minutes_over, (-leftMinutes).toInt())
                },
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(R.string.metric_percent_of_limit, (time.toMinutes() * 100 / limit.limit).toInt()),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Optional, Accessibility-based. When it isn't set up, a short explanation replaces the count. */
@Composable
private fun ReelsCard(uiState: DashboardUiState, onSetUp: () -> Unit) {
    ScrollLessCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
            IconBadge(R.drawable.ic_play_arrow, tint = MaterialTheme.colorScheme.primary)
            Text(
                stringResource(R.string.metric_reels_title),
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier
                    .weight(1f)
                    .semantics { heading() },
            )
            uiState.reelsStatus?.let { LimitStatusPill(it) }
        }
        when (val reels = uiState.reels) {
            MetricState.Loading -> Text(stringResource(R.string.metric_loading))
            is MetricState.Unmeasured -> {
                Text(
                    stringResource(R.string.reels_optional_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (reels.availability is TrackingAvailability.NotConfigured ||
                    (reels.availability as? TrackingAvailability.Unavailable)?.reason == UnavailableReason.SERVICE_NOT_RUNNING
                ) {
                    TextButton(onClick = onSetUp) { Text(stringResource(R.string.reels_optional_action)) }
                }
            }
            is MetricState.Measured -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.xl)) {
                val limit = uiState.reelsDailyLimit
                val count by animateIntAsState(reels.value, animationSpec = tween(600), label = "reels")
                if (uiState.reelsStatus != null) {
                    ProgressRing(progress = count.toFloat() / limit, color = uiState.reelsStatus.color(), size = 96.dp, strokeWidth = 9.dp) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("≈$count", style = MaterialTheme.typography.titleLarge)
                            Text(
                                stringResource(R.string.metric_of_limit, limit),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                } else {
                    Text("≈$count", style = MaterialTheme.typography.headlineMedium)
                }
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs), modifier = Modifier.weight(1f)) {
                    if (uiState.reelsStatus != null) {
                        val left = limit - reels.value
                        Text(
                            text = if (left > 0) stringResource(R.string.metric_reels_left, left) else stringResource(R.string.metric_reels_over, -left),
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                    Text(
                        text = stringResource(R.string.metric_reels_estimate_short),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/** While ScrollLess's Accessibility service is on: the payment-app trade-off and a one-tap off switch. */
@Composable
private fun PaymentAppsHint(onTurnOff: () -> Unit) {
    ScrollLessCard(containerColor = MaterialTheme.colorScheme.surfaceContainerLow) {
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md), verticalAlignment = Alignment.Top) {
            Icon(painterResource(R.drawable.ic_shield), contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs), modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.payment_hint_title), style = MaterialTheme.typography.titleSmall)
                Text(
                    stringResource(R.string.payment_hint_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        OutlinedButton(onClick = onTurnOff, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.action_turn_off_reels_tracking))
        }
    }
}

@Composable
private fun InsightsCard(uiState: DashboardUiState) {
    ScrollLessCard {
        InsightRow(
            label = stringResource(R.string.insight_tracked_today),
            value = (uiState.instagramTime as? MetricState.Measured)?.let { formatDuration(it.value) }
                ?: stringResource(R.string.status_not_tracked),
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        val yesterday = uiState.yesterday
        if (yesterday == null) {
            Text(
                stringResource(R.string.insight_no_yesterday),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Text(stringResource(R.string.insight_yesterday), style = MaterialTheme.typography.labelLarge)
            InsightRow(
                label = stringResource(R.string.insight_reels_yesterday),
                value = yesterday.reels?.let { "≈$it" } ?: stringResource(R.string.status_not_tracked),
            )
            InsightRow(
                label = stringResource(R.string.insight_time_yesterday),
                value = yesterday.instagramTime?.let { formatDuration(it) } ?: stringResource(R.string.status_not_tracked),
            )
        }
    }
}

@Composable
private fun InsightRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.titleSmall)
    }
}

@Composable
private fun LimitsCtaCard(onOpenLimits: () -> Unit) {
    ScrollLessCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
            IconBadge(R.drawable.ic_hourglass_top, tint = MaterialTheme.colorScheme.primary)
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.limits_cta_title), style = MaterialTheme.typography.titleSmall)
                Text(
                    stringResource(R.string.limits_cta_body),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        OutlinedButton(onClick = onOpenLimits, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.action_adjust_limits))
        }
    }
}

/** Explains why a metric isn't shown and offers the fix. Never a zero. */
@Composable
private fun UnmeasuredContent(availability: TrackingAvailability, onSetUp: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        Text(
            stringResource(availability.statusLabel()),
            style = MaterialTheme.typography.titleSmall,
            color = ScrollLessTheme.colors.notTracked,
        )
        Text(stringResource(availability.explanation()), style = MaterialTheme.typography.bodyMedium)
        if (availability !is TrackingAvailability.Unavailable || availability.reason == UnavailableReason.SERVICE_NOT_RUNNING) {
            OutlinedButton(onClick = onSetUp) { Text(stringResource(R.string.action_set_up_tracking)) }
        }
    }
}

/**
 * The limit-reached experience: a calm sheet with three choices. Nothing is blocked; "Take a
 * break" just goes to the home screen. Shown once per limit per day, or when opened from a reminder.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LimitNoticeSheet(notice: LimitNotice, onTakeBreak: () -> Unit, onChangeLimit: () -> Unit, onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val app = stringResource(
        if (notice.metric == LimitMetric.YOUTUBE_APP_TIME) R.string.app_youtube else R.string.app_instagram,
    )
    val reached = notice.status == LimitStatus.REACHED
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = Spacing.xl, vertical = Spacing.md),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            IconBadge(
                if (reached) R.drawable.ic_hourglass_top else R.drawable.ic_warning,
                tint = notice.status.color(),
            )
            Text(
                text = if (notice.metric == LimitMetric.INSTAGRAM_REELS) {
                    stringResource(if (reached) R.string.notice_reels_reached_title else R.string.notice_reels_close_title)
                } else {
                    stringResource(if (reached) R.string.notice_time_reached_title else R.string.notice_time_close_title, app)
                },
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                text = if (notice.metric.unit == LimitUnit.MINUTES) {
                    stringResource(R.string.notice_time_body, app, formatDuration(Duration.ofMillis(notice.value)), notice.limit)
                } else {
                    stringResource(R.string.notice_reels_body, notice.value.toInt(), notice.limit)
                },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Button(onClick = onTakeBreak, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.notice_take_break)) }
            OutlinedButton(onClick = onChangeLimit, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.notice_change_limit)) }
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.notice_dismiss)) }
        }
    }
}

/** "Take a break": shows the home screen. Only the launcher is started; no other app is touched. */
private fun Context.goToHomeScreen() {
    try {
        startActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (_: ActivityNotFoundException) {
        // No launcher to show; staying in ScrollLess is fine.
    }
}

@StringRes
private fun TrackingAvailability.statusLabel(): Int = when (this) {
    TrackingAvailability.Active -> R.string.status_tracking_active
    is TrackingAvailability.NotConfigured -> R.string.status_not_configured
    is TrackingAvailability.Unavailable -> R.string.status_tracking_unavailable
}

@StringRes
private fun TrackingAvailability.explanation(): Int = when (this) {
    TrackingAvailability.Active -> R.string.status_tracking_active
    is TrackingAvailability.NotConfigured -> when (requirement) {
        SetupRequirement.USAGE_ACCESS -> R.string.metric_needs_usage_access
        SetupRequirement.ACCESSIBILITY_SERVICE -> R.string.metric_needs_accessibility
        SetupRequirement.ACCESSIBILITY_CONSENT -> R.string.metric_needs_consent
    }
    is TrackingAvailability.Unavailable -> when (reason) {
        UnavailableReason.SERVICE_NOT_RUNNING -> R.string.metric_service_not_running
        UnavailableReason.INSTAGRAM_NOT_INSTALLED -> R.string.metric_instagram_not_installed
        UnavailableReason.YOUTUBE_NOT_INSTALLED -> R.string.metric_youtube_not_installed
    }
}

@Preview
@Composable
private fun DashboardActivePreview() {
    ScrollLessTheme {
        androidx.compose.material3.Surface {
            DashboardScreen(
                uiState = DashboardUiState(
                    dayPart = DayPart.EVENING,
                    reels = MetricState.Measured(42),
                    reelsStatus = LimitStatus.APPROACHING,
                    instagramTime = MetricState.Measured(Duration.ofMinutes(48)),
                    instagramLimit = LimitProgress(60, LimitStatus.APPROACHING),
                    yesterday = YesterdaySummary(reels = 34, instagramTime = Duration.ofMinutes(55)),
                    mode = TrackingMode.ADVANCED,
                    youtubeTime = MetricState.Measured(Duration.ofMinutes(25)),
                    youtubeLimit = LimitProgress(60, LimitStatus.WITHIN),
                    reelsTrackingSwitchedOn = true,
                ),
                onOpenSettings = {}, onOpenLimits = {}, onOpenAnalytics = {}, onSetUpTracking = {},
            )
        }
    }
}

@Preview
@Composable
private fun DashboardBasicPreview() {
    ScrollLessTheme(darkTheme = true) {
        androidx.compose.material3.Surface {
            DashboardScreen(
                uiState = DashboardUiState(
                    reels = MetricState.Unmeasured(TrackingAvailability.NotConfigured(SetupRequirement.ACCESSIBILITY_SERVICE)),
                    instagramTime = MetricState.Measured(Duration.ofMinutes(72)),
                    instagramLimit = LimitProgress(60, LimitStatus.REACHED),
                    youtubeTime = MetricState.Measured(Duration.ofMinutes(41)),
                    mode = TrackingMode.BASIC,
                ),
                onOpenSettings = {}, onOpenLimits = {}, onOpenAnalytics = {}, onSetUpTracking = {},
            )
        }
    }
}
