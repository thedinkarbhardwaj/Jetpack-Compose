package com.sunpawtechnologies.scrollless.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sunpawtechnologies.scrollless.R
import com.sunpawtechnologies.scrollless.domain.model.ThemeMode
import com.sunpawtechnologies.scrollless.permissions.SettingsTarget
import com.sunpawtechnologies.scrollless.permissions.openSystemSettings
import com.sunpawtechnologies.scrollless.ui.components.ScrollLessTopAppBar
import com.sunpawtechnologies.scrollless.ui.components.StatusPill
import com.sunpawtechnologies.scrollless.ui.permissions.failureMessage
import com.sunpawtechnologies.scrollless.ui.util.ObserveAsEvents
import kotlinx.coroutines.launch
import com.sunpawtechnologies.scrollless.ui.components.SettingsRow
import com.sunpawtechnologies.scrollless.ui.theme.ScrollLessTheme
import com.sunpawtechnologies.scrollless.ui.theme.Spacing

@Composable
fun SettingsRoute(
    onOpenPermissions: () -> Unit,
    viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val resources = LocalResources.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    ObserveAsEvents(viewModel.effects) { effect ->
        val message = when (effect) {
            is SettingsEffect.OpenSettings -> if (context.openSystemSettings(effect.target)) {
                if (effect.target == SettingsTarget.ACCESSIBILITY) resources.getString(R.string.settings_turn_off_in_settings) else null
            } else {
                resources.getString(effect.target.failureMessage())
            }
            SettingsEffect.ReelsTrackingTurnedOff -> resources.getString(R.string.settings_reels_turned_off)
        }
        if (message != null) scope.launch { snackbarHostState.showSnackbar(message) }
    }
    SettingsScreen(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onTurnOffReelsTracking = viewModel::onTurnOffReelsTracking,
        onOpenDeveloperOptions = viewModel::onOpenDeveloperOptions,
        onThemeModeSelected = viewModel::onThemeModeSelected,
        onOverlayEnabledChanged = viewModel::onOverlayEnabledChanged,
        onResetOverlayPosition = viewModel::onResetOverlayPosition,
        onOpenPermissions = onOpenPermissions,
        onExclusionDraftChanged = viewModel::onExclusionDraftChanged,
        onAddExclusion = viewModel::onAddExclusion,
        onRemoveExclusion = viewModel::onRemoveExclusion,
    )
}

@Composable
fun SettingsScreen(
    uiState: SettingsUiState,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    onTurnOffReelsTracking: () -> Unit = {},
    onOpenDeveloperOptions: () -> Unit = {},
    onThemeModeSelected: (ThemeMode) -> Unit,
    onOverlayEnabledChanged: (Boolean) -> Unit,
    onResetOverlayPosition: () -> Unit,
    onOpenPermissions: () -> Unit,
    onExclusionDraftChanged: (String) -> Unit = {},
    onAddExclusion: () -> Unit = {},
    onRemoveExclusion: (String) -> Unit = {},
) {
    Scaffold(
        topBar = { ScrollLessTopAppBar(title = stringResource(R.string.settings_title)) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.xl),
        ) {
            if (uiState.isLoading) return@Column

            SettingsSection(stringResource(R.string.settings_tracking)) {
                SettingsRow(
                    icon = R.drawable.ic_check_circle,
                    title = stringResource(R.string.action_manage_permissions),
                    subtitle = stringResource(
                        when {
                            uiState.usageAccessGranted && uiState.reelsTrackingActive -> R.string.mode_advanced_active
                            uiState.reelsTrackingActive -> R.string.mode_reels_only
                            uiState.usageAccessGranted -> R.string.mode_basic_active
                            else -> R.string.mode_none
                        },
                    ),
                    onClick = onOpenPermissions,
                )
            }

            SettingsSection(stringResource(R.string.settings_payment_apps)) {
                PaymentSafetySection(uiState, onTurnOffReelsTracking, onOpenDeveloperOptions)
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                PaymentAppsSection(uiState, onExclusionDraftChanged, onAddExclusion, onRemoveExclusion)
            }

            SettingsSection(stringResource(R.string.settings_counter)) {
                SettingsRow(
                    icon = R.drawable.ic_visibility,
                    title = stringResource(R.string.settings_counter_show),
                    subtitle = stringResource(
                        if (uiState.reelsTrackingActive) R.string.settings_counter_show_body else R.string.settings_counter_needs_tracking,
                    ),
                    onClick = { onOverlayEnabledChanged(!uiState.overlayEnabled) },
                    trailing = {
                        Switch(checked = uiState.overlayEnabled, onCheckedChange = onOverlayEnabledChanged)
                    },
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                SettingsRow(
                    icon = R.drawable.ic_tune,
                    title = stringResource(R.string.settings_counter_position),
                    subtitle = stringResource(R.string.settings_counter_position_body),
                    trailing = {
                        TextButton(onClick = onResetOverlayPosition, enabled = uiState.overlayMoved) {
                            Text(stringResource(R.string.settings_counter_reset))
                        }
                    },
                )
            }

            SettingsSection(stringResource(R.string.settings_appearance)) {
                Column(Modifier.padding(Spacing.lg), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                    Text(stringResource(R.string.settings_theme), style = MaterialTheme.typography.bodyLarge)
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        ThemeMode.entries.forEachIndexed { index, mode ->
                            SegmentedButton(
                                selected = uiState.themeMode == mode,
                                onClick = { onThemeModeSelected(mode) },
                                shape = SegmentedButtonDefaults.itemShape(index, ThemeMode.entries.size),
                            ) { Text(stringResource(mode.labelRes())) }
                        }
                    }
                }
            }

            SettingsSection(stringResource(R.string.settings_privacy)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.lg),
                    verticalAlignment = Alignment.Top,
                    modifier = Modifier.padding(Spacing.lg),
                ) {
                    Icon(
                        painterResource(R.drawable.ic_shield),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        stringResource(R.string.settings_privacy_body),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .padding(horizontal = Spacing.xs)
                .semantics { heading() },
        )
        Card(
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            modifier = Modifier.fillMaxWidth(),
        ) { content() }
    }
}

private fun ThemeMode.labelRes(): Int = when (this) {
    ThemeMode.SYSTEM -> R.string.theme_system
    ThemeMode.LIGHT -> R.string.theme_light
    ThemeMode.DARK -> R.string.theme_dark
}

@Preview
@Composable
private fun SettingsScreenPreview() {
    ScrollLessTheme {
        SettingsScreen(
            uiState = SettingsUiState(isLoading = false, themeMode = ThemeMode.DARK, reelsTrackingActive = false, setupStepsDone = 1),
            onThemeModeSelected = {}, onOverlayEnabledChanged = {}, onResetOverlayPosition = {}, onOpenPermissions = {},
        )
    }
}

/** The counter never shows over payment/banking apps. Lists detected apps and user additions. */
@Composable
private fun PaymentAppsSection(
    uiState: SettingsUiState,
    onDraftChanged: (String) -> Unit,
    onAdd: () -> Unit,
    onRemove: (String) -> Unit,
) {
    Column(Modifier.padding(Spacing.lg), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.lg), verticalAlignment = Alignment.Top) {
            Icon(painterResource(R.drawable.ic_shield), contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                Text(
                    stringResource(R.string.settings_payment_apps_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = if (uiState.detectedPaymentApps.isEmpty()) {
                        stringResource(R.string.settings_payment_apps_none_detected)
                    } else {
                        stringResource(R.string.settings_payment_apps_detected, uiState.detectedPaymentApps.joinToString())
                    },
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        uiState.customExcludedPackages.forEach { packageName ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(packageName, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                TextButton(onClick = { onRemove(packageName) }) { Text(stringResource(R.string.settings_payment_apps_remove)) }
            }
        }
        OutlinedTextField(
            value = uiState.exclusionDraft,
            onValueChange = onDraftChanged,
            label = { Text(stringResource(R.string.settings_payment_apps_add_label)) },
            placeholder = { Text("com.example.bank") },
            singleLine = true,
            isError = uiState.exclusionDraftInvalid,
            supportingText = {
                Text(
                    stringResource(
                        if (uiState.exclusionDraftInvalid) R.string.settings_payment_apps_invalid else R.string.settings_payment_apps_add_hint,
                    ),
                )
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onAdd() }),
            modifier = Modifier.fillMaxWidth(),
        )
        FilledTonalButton(
            onClick = onAdd,
            enabled = uiState.exclusionDraft.isNotBlank() && !uiState.exclusionDraftInvalid,
            modifier = Modifier.fillMaxWidth(),
        ) { Text(stringResource(R.string.settings_payment_apps_add)) }
    }
}

/**
 * What can stop payment apps from opening, with the live state on this phone and one action each.
 * Both causes are outside ScrollLess's control: it can only report them and switch off its own service.
 */
@Composable
private fun PaymentSafetySection(
    uiState: SettingsUiState,
    onTurnOffReelsTracking: () -> Unit,
    onOpenDeveloperOptions: () -> Unit,
) {
    Column(Modifier.padding(Spacing.lg), verticalArrangement = Arrangement.spacedBy(Spacing.lg)) {
        Text(
            stringResource(R.string.payment_safety_intro),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        SafetyItem(
            title = stringResource(R.string.payment_safety_reels_title),
            body = stringResource(R.string.payment_safety_reels_body),
            isOn = uiState.reelsTrackingSwitchedOn,
            actionLabel = stringResource(R.string.action_turn_off_reels_tracking).takeIf { uiState.reelsTrackingSwitchedOn },
            onAction = onTurnOffReelsTracking,
        )
        SafetyItem(
            title = stringResource(R.string.payment_safety_developer_title),
            body = stringResource(R.string.payment_safety_developer_body),
            isOn = uiState.developerModeEnabled,
            actionLabel = stringResource(R.string.payment_safety_open_developer).takeIf { uiState.developerModeEnabled },
            onAction = onOpenDeveloperOptions,
        )
        Text(
            stringResource(R.string.payment_safety_footer),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** "On" is the state that can stop payment apps, so it's shown in amber with its fix. */
@Composable
private fun SafetyItem(title: String, body: String, isOn: Boolean, actionLabel: String?, onAction: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Text(title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            StatusPill(
                text = stringResource(if (isOn) R.string.payment_safety_state_on else R.string.payment_safety_state_off),
                icon = if (isOn) R.drawable.ic_warning else R.drawable.ic_check_circle,
                tint = if (isOn) ScrollLessTheme.colors.statusWarning else ScrollLessTheme.colors.statusGood,
            )
        }
        Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (actionLabel != null) {
            OutlinedButton(onClick = onAction, modifier = Modifier.fillMaxWidth()) { Text(actionLabel) }
        }
    }
}
