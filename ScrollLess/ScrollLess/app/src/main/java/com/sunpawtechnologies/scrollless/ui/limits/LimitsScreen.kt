package com.sunpawtechnologies.scrollless.ui.limits

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sunpawtechnologies.scrollless.R
import com.sunpawtechnologies.scrollless.domain.model.LimitMetric
import com.sunpawtechnologies.scrollless.domain.model.LimitUnit
import com.sunpawtechnologies.scrollless.domain.model.WARNING_THRESHOLD_RANGE
import com.sunpawtechnologies.scrollless.permissions.SettingsTarget
import com.sunpawtechnologies.scrollless.permissions.openSystemSettings
import com.sunpawtechnologies.scrollless.ui.components.IconBadge
import com.sunpawtechnologies.scrollless.ui.components.ScrollLessCard
import com.sunpawtechnologies.scrollless.ui.components.ScrollLessTopAppBar
import com.sunpawtechnologies.scrollless.ui.components.StatusPill
import com.sunpawtechnologies.scrollless.ui.theme.ScrollLessTheme
import com.sunpawtechnologies.scrollless.ui.theme.Spacing
import kotlin.math.roundToInt

@Composable
fun LimitsRoute(
    onSetUpTracking: () -> Unit = {},
    viewModel: LimitsViewModel = viewModel(factory = LimitsViewModel.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    LifecycleResumeEffect(viewModel) {
        viewModel.onResume()
        onPauseOrDispose {}
    }
    // Android 13+: ask in context, when the user turns reminders on. Older versions only have the app switch.
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) viewModel.onResume() else context.openSystemSettings(SettingsTarget.NOTIFICATIONS)
    }
    LimitsScreen(
        uiState = uiState,
        onDraftChanged = viewModel::onDraftChanged,
        onPresetSelected = viewModel::onPresetSelected,
        onSave = viewModel::onSave,
        onThresholdChanged = viewModel::onThresholdChanged,
        onThresholdChangeFinished = viewModel::onThresholdChangeFinished,
        onEnabledChanged = viewModel::onEnabledChanged,
        onAllowNotifications = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                context.openSystemSettings(SettingsTarget.NOTIFICATIONS)
            }
        },
        onSetUpTracking = onSetUpTracking,
    )
}

@Composable
fun LimitsScreen(
    uiState: LimitsUiState,
    onDraftChanged: (LimitMetric, String) -> Unit,
    onPresetSelected: (LimitMetric, Int) -> Unit,
    onSave: (LimitMetric) -> Unit,
    onThresholdChanged: (Int) -> Unit,
    onThresholdChangeFinished: () -> Unit,
    onEnabledChanged: (LimitMetric, Boolean) -> Unit = { _, _ -> },
    onAllowNotifications: () -> Unit = {},
    onSetUpTracking: () -> Unit = {},
) {
    Scaffold(topBar = { ScrollLessTopAppBar(title = stringResource(R.string.limits_title)) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        ) {
            Text(
                stringResource(R.string.limits_intro),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (!uiState.isLoading) {
                RemindersCard(uiState.notificationsAllowed, onAllowNotifications)
                uiState.fields.forEach { field ->
                    LimitCard(field, onDraftChanged, onPresetSelected, onSave, onEnabledChanged, onSetUpTracking)
                }
                ThresholdCard(uiState.warningThresholdPercent, onThresholdChanged, onThresholdChangeFinished)
            }
        }
    }
}

private data class MetricCopy(
    @DrawableRes val icon: Int,
    @StringRes val title: Int,
    @StringRes val unit: Int,
    @StringRes val note: Int,
    @StringRes val notMeasured: Int,
)

private fun LimitMetric.metricCopy(): MetricCopy = when (this) {
    LimitMetric.INSTAGRAM_APP_TIME ->
        MetricCopy(R.drawable.ic_schedule, R.string.limit_instagram_time_title, R.string.unit_minutes, R.string.limit_instagram_time_note, R.string.limit_needs_usage_access)
    LimitMetric.YOUTUBE_APP_TIME ->
        MetricCopy(R.drawable.ic_smart_display, R.string.limit_youtube_time_title, R.string.unit_minutes, R.string.limit_youtube_time_note, R.string.limit_needs_usage_access)
    LimitMetric.INSTAGRAM_REELS ->
        MetricCopy(R.drawable.ic_play_arrow, R.string.limit_reels_title, R.string.unit_reels, R.string.limit_reels_note, R.string.limit_needs_reels_tracking)
    // Not configurable (nothing measures them yet); never listed.
    LimitMetric.YOUTUBE_SHORTS, LimitMetric.YOUTUBE_REGULAR_VIDEO ->
        MetricCopy(R.drawable.ic_smart_display, R.string.limit_youtube_time_title, R.string.unit_minutes, R.string.limit_youtube_time_note, R.string.limit_needs_usage_access)
}

@Composable
private fun RemindersCard(allowed: Boolean, onAllow: () -> Unit) {
    ScrollLessCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
            IconBadge(R.drawable.ic_hourglass_top, tint = MaterialTheme.colorScheme.primary)
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.reminders_title), style = MaterialTheme.typography.titleSmall)
                Text(
                    stringResource(if (allowed) R.string.reminders_on_body else R.string.reminders_off_body),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (allowed) {
            StatusPill(stringResource(R.string.reminders_on), R.drawable.ic_check_circle, ScrollLessTheme.colors.statusGood)
        } else {
            Button(onClick = onAllow, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.reminders_allow)) }
        }
        Text(
            stringResource(R.string.reminders_timing),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun LimitCard(
    field: LimitField,
    onDraftChanged: (LimitMetric, String) -> Unit,
    onPresetSelected: (LimitMetric, Int) -> Unit,
    onSave: (LimitMetric) -> Unit,
    onEnabledChanged: (LimitMetric, Boolean) -> Unit,
    onSetUpTracking: () -> Unit,
) {
    val copy = field.metric.metricCopy()
    val unit = stringResource(copy.unit)
    ScrollLessCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
            IconBadge(copy.icon, tint = MaterialTheme.colorScheme.primary)
            Column(Modifier.weight(1f)) {
                Text(stringResource(copy.title), style = MaterialTheme.typography.titleSmall)
                Text(
                    if (field.enabled) stringResource(R.string.limit_current, field.saved, unit) else stringResource(R.string.limit_off),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            val description = stringResource(R.string.limit_switch_description, stringResource(copy.title))
            Switch(
                checked = field.enabled,
                onCheckedChange = { onEnabledChanged(field.metric, it) },
                modifier = Modifier.semantics { stateDescription = description },
            )
        }
        Text(stringResource(copy.note), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (!field.measuredNow) {
            StatusPill(stringResource(copy.notMeasured), R.drawable.ic_info, ScrollLessTheme.colors.statusWarning)
            TextButton(onClick = onSetUpTracking) { Text(stringResource(R.string.action_set_up_tracking)) }
        }
        AnimatedVisibility(visible = field.enabled) {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                if (field.justSaved) {
                    StatusPill(stringResource(R.string.limit_saved), R.drawable.ic_check_circle, ScrollLessTheme.colors.statusGood)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    field.presets.forEach { preset ->
                        FilterChip(
                            selected = field.saved == preset,
                            onClick = { onPresetSelected(field.metric, preset) },
                            label = { Text("$preset") },
                        )
                    }
                }
                val range = field.metric.validRange
                OutlinedTextField(
                    value = field.draft,
                    onValueChange = { onDraftChanged(field.metric, it) },
                    label = { Text(stringResource(R.string.limit_custom_label)) },
                    suffix = { Text(unit) },
                    singleLine = true,
                    isError = field.error != null,
                    supportingText = {
                        Text(
                            when (field.error) {
                                LimitError.NOT_A_NUMBER -> stringResource(R.string.limit_error_number)
                                LimitError.OUT_OF_RANGE -> stringResource(R.string.limit_error_range, range.first, range.last)
                                null -> stringResource(R.string.limit_range_hint, range.first, range.last)
                            },
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { if (field.canSave) onSave(field.metric) }),
                    modifier = Modifier.fillMaxWidth(),
                )
                Button(onClick = { onSave(field.metric) }, enabled = field.canSave, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.limit_save))
                }
            }
        }
    }
}

@Composable
private fun ThresholdCard(percent: Int, onChanged: (Int) -> Unit, onFinished: () -> Unit) {
    ScrollLessCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
            IconBadge(R.drawable.ic_warning, tint = ScrollLessTheme.colors.statusWarning)
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.threshold_title), style = MaterialTheme.typography.titleSmall)
                Text(
                    stringResource(R.string.threshold_body),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text("$percent%", style = MaterialTheme.typography.titleLarge)
        }
        val range = WARNING_THRESHOLD_RANGE
        val description = stringResource(R.string.threshold_state, percent)
        Slider(
            value = percent.toFloat(),
            onValueChange = { onChanged((it / 5).roundToInt() * 5) },
            onValueChangeFinished = onFinished,
            valueRange = range.first.toFloat()..range.last.toFloat(),
            steps = (range.last - range.first) / 5 - 1,
            modifier = Modifier.semantics { stateDescription = description },
        )
    }
}

@Preview
@Composable
private fun LimitsScreenPreview() {
    ScrollLessTheme {
        LimitsScreen(
            uiState = LimitsUiState(
                isLoading = false,
                fields = LimitMetric.configurable.map {
                    LimitField(
                        metric = it,
                        saved = it.defaultDailyLimit,
                        draft = if (it.unit == LimitUnit.COUNT) "5000" else "${it.defaultDailyLimit}",
                        error = null,
                        justSaved = false,
                        enabled = it != LimitMetric.YOUTUBE_APP_TIME,
                        measuredNow = it != LimitMetric.INSTAGRAM_REELS,
                    ).let { field -> field.copy(error = validateLimit(it, field.draft)) }
                },
                warningThresholdPercent = 80,
            ),
            onDraftChanged = { _, _ -> }, onPresetSelected = { _, _ -> }, onSave = {}, onThresholdChanged = {}, onThresholdChangeFinished = {},
        )
    }
}
