package com.sunpawtechnologies.scrollless.ui.permissions

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sunpawtechnologies.scrollless.R
import com.sunpawtechnologies.scrollless.permissions.SettingsTarget
import com.sunpawtechnologies.scrollless.permissions.openSystemSettings
import com.sunpawtechnologies.scrollless.permissions.openWebPage
import com.sunpawtechnologies.scrollless.ui.components.IconBadge
import com.sunpawtechnologies.scrollless.ui.components.StatusPill
import com.sunpawtechnologies.scrollless.ui.components.ScrollLessTopAppBar
import com.sunpawtechnologies.scrollless.ui.theme.ScrollLessTheme
import com.sunpawtechnologies.scrollless.ui.util.ObserveAsEvents
import kotlinx.coroutines.launch

@Composable
fun PermissionsRoute(
    fromOnboarding: Boolean,
    onBack: () -> Unit,
    onContinue: () -> Unit,
    viewModel: PermissionsViewModel = viewModel(factory = PermissionsViewModel.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val resources = LocalResources.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    ObserveAsEvents(viewModel.effects) { effect ->
        when (effect) {
            is PermissionsEffect.OpenSettings -> if (!context.openSystemSettings(effect.target)) {
                val message = resources.getString(effect.target.failureMessage())
                scope.launch { snackbarHostState.showSnackbar(message) }
            }
            is PermissionsEffect.OpenWebPage -> if (!context.openWebPage(effect.url)) {
                val message = resources.getString(R.string.settings_open_failed_browser, effect.url)
                scope.launch { snackbarHostState.showSnackbar(message) }
            }
        }
    }

    PermissionsScreen(
        uiState = uiState,
        fromOnboarding = fromOnboarding,
        snackbarHostState = snackbarHostState,
        onBack = onBack,
        onContinue = onContinue,
        onEnableUsageAccess = viewModel::onEnableUsageAccess,
        onAccessibilityAction = viewModel::onAccessibilityAction,
        onOpenAppInfo = viewModel::onOpenAppInfo,
        onTurnOffReelsTracking = viewModel::onTurnOffReelsTracking,
        onDisclosureAccepted = viewModel::onDisclosureAccepted,
        onDisclosureDismissed = viewModel::onDisclosureDismissed,
        onOpenBatterySettings = viewModel::onOpenBatterySettings,
        onOpenBatteryGuide = viewModel::onOpenBatteryGuide,
    )
}

@Composable
fun PermissionsScreen(
    uiState: PermissionsUiState,
    fromOnboarding: Boolean,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onContinue: () -> Unit,
    onEnableUsageAccess: () -> Unit,
    onAccessibilityAction: () -> Unit,
    onOpenAppInfo: () -> Unit,
    onDisclosureAccepted: () -> Unit,
    onDisclosureDismissed: () -> Unit,
    onOpenBatterySettings: () -> Unit = {},
    onOpenBatteryGuide: () -> Unit = {},
    onTurnOffReelsTracking: () -> Unit = {},
) {
    Scaffold(
        topBar = {
            ScrollLessTopAppBar(
                title = stringResource(R.string.permissions_title),
                onBack = if (fromOnboarding) null else onBack,
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(R.string.permissions_intro_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                text = stringResource(R.string.permissions_intro_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (!uiState.isLoading) {
                ModeSummary(uiState)

                if (!uiState.instagramInstalled) {
                    NoticeCard(
                        text = stringResource(R.string.instagram_missing_card),
                        container = ScrollLessTheme.colors.warningContainer,
                        content = ScrollLessTheme.colors.onWarningContainer,
                    )
                }

                SectionTitle(R.string.setup_section_basic, R.string.setup_section_basic_body)
                PermissionCard(
                    title = stringResource(R.string.usage_access_title),
                    icon = R.drawable.ic_schedule,
                    status = if (uiState.usageAccessGranted) StatusChip.Enabled else StatusChip.NotEnabled,
                    why = R.string.usage_access_why,
                    feature = R.string.usage_access_feature,
                    declined = R.string.usage_access_declined,
                    steps = if (uiState.usageAccessGranted) null else R.string.usage_access_steps,
                    actionLabel = if (uiState.usageAccessGranted) null else R.string.usage_access_enable,
                    onAction = onEnableUsageAccess,
                )

                SectionTitle(R.string.setup_section_advanced, R.string.setup_section_advanced_body)
                var showAdvanced by rememberSaveable { mutableStateOf(false) }
                val advancedUntouched = uiState.accessibilityStatus == AccessibilityStatus.NOT_ENABLED && !uiState.consentGiven
                if (fromOnboarding && advancedUntouched && !showAdvanced) {
                    // First run: Advanced is optional, so only describe it until the user chooses it.
                    OutlinedButton(onClick = { showAdvanced = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.setup_show_advanced))
                    }
                } else {
                    AccessibilityCard(uiState, onAccessibilityAction, onOpenAppInfo, onTurnOffReelsTracking)
                }
                if (uiState.accessibilityStatus == AccessibilityStatus.ACTIVE || uiState.accessibilityStatus == AccessibilityStatus.NOT_RUNNING) {
                    BatteryCard(uiState, onOpenBatterySettings, onOpenBatteryGuide)
                }

                if (fromOnboarding) {
                    Button(onClick = onContinue, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.permissions_continue))
                    }
                    Text(
                        text = stringResource(R.string.permissions_continue_later),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }

    if (uiState.showDisclosure) {
        AccessibilityDisclosureDialog(onAccept = onDisclosureAccepted, onDismiss = onDisclosureDismissed)
    }
}

@Composable
private fun AccessibilityCard(
    uiState: PermissionsUiState,
    onAccessibilityAction: () -> Unit,
    onOpenAppInfo: () -> Unit,
    onTurnOff: () -> Unit,
) {
    val status = uiState.accessibilityStatus
    PermissionCard(
        title = stringResource(R.string.accessibility_title),
        icon = R.drawable.ic_play_arrow,
        status = when (status) {
            AccessibilityStatus.NOT_ENABLED -> StatusChip.NotEnabled
            AccessibilityStatus.NEEDS_CONSENT -> StatusChip.NeedsConsent
            AccessibilityStatus.NOT_RUNNING -> StatusChip.NotRunning
            AccessibilityStatus.ACTIVE -> StatusChip.Enabled
        },
        why = R.string.accessibility_why,
        feature = R.string.accessibility_feature,
        declined = R.string.accessibility_declined,
        steps = when (status) {
            AccessibilityStatus.NOT_ENABLED -> R.string.accessibility_steps
            AccessibilityStatus.NOT_RUNNING -> R.string.accessibility_not_running_help
            else -> null
        },
        actionLabel = when (status) {
            AccessibilityStatus.NOT_ENABLED ->
                if (uiState.consentGiven) R.string.accessibility_open_settings else R.string.accessibility_enable
            AccessibilityStatus.NEEDS_CONSENT -> R.string.accessibility_review_disclosure
            AccessibilityStatus.NOT_RUNNING -> R.string.accessibility_open_settings
            AccessibilityStatus.ACTIVE -> null
        },
        onAction = onAccessibilityAction,
    ) {
        if (status == AccessibilityStatus.NOT_ENABLED || status == AccessibilityStatus.NOT_RUNNING) {
            Text(
                text = stringResource(R.string.accessibility_restricted_help),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(onClick = onOpenAppInfo) { Text(stringResource(R.string.open_app_info)) }
        }
        PaymentAppNote()
        if (status != AccessibilityStatus.NOT_ENABLED) {
            OutlinedButton(onClick = onTurnOff, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.action_turn_off_reels_tracking))
            }
        }
    }
}

/** Which tracking mode is in effect. Basic is a complete choice, not an unfinished setup. */
@Composable
private fun ModeSummary(uiState: PermissionsUiState) {
    val (label, tint) = when {
        uiState.accessibilityStatus == AccessibilityStatus.ACTIVE && uiState.usageAccessGranted ->
            R.string.mode_advanced_active to ScrollLessTheme.colors.statusGood
        uiState.accessibilityStatus == AccessibilityStatus.ACTIVE -> R.string.mode_reels_only to ScrollLessTheme.colors.statusGood
        uiState.usageAccessGranted -> R.string.mode_basic_active to ScrollLessTheme.colors.statusGood
        else -> R.string.mode_none to ScrollLessTheme.colors.statusWarning
    }
    StatusPill(
        text = stringResource(label),
        icon = if (tint == ScrollLessTheme.colors.statusGood) R.drawable.ic_check_circle else R.drawable.ic_info,
        tint = tint,
    )
}

@Composable
private fun SectionTitle(@StringRes title: Int, @StringRes body: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(top = 8.dp)) {
        Text(
            stringResource(title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.semantics { heading() },
        )
        Text(stringResource(body), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/**
 * Shown once Reels tracking is on. Explains, with evidence where we have it, how the phone's
 * battery management can stop tracking, and links to the fix. Never promises it will work.
 */
@Composable
private fun BatteryCard(uiState: PermissionsUiState, onOpenBatterySettings: () -> Unit, onOpenGuide: () -> Unit) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                IconBadge(icon = R.drawable.ic_shield, tint = MaterialTheme.colorScheme.primary)
                Text(
                    stringResource(R.string.battery_title),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier
                        .weight(1f)
                        .semantics { heading() },
                )
            }
            Text(
                text = if (uiState.recentInterruptions > 0) {
                    pluralStringResource(R.plurals.battery_interruptions, uiState.recentInterruptions, uiState.recentInterruptions)
                } else {
                    stringResource(R.string.battery_body)
                },
                style = MaterialTheme.typography.bodyMedium,
            )
            StatusPill(
                text = stringResource(if (uiState.batteryUnrestricted) R.string.battery_unrestricted else R.string.battery_optimized),
                icon = if (uiState.batteryUnrestricted) R.drawable.ic_check_circle else R.drawable.ic_warning,
                tint = if (uiState.batteryUnrestricted) ScrollLessTheme.colors.statusGood else ScrollLessTheme.colors.statusWarning,
            )
            if (!uiState.batteryUnrestricted) {
                Button(onClick = onOpenBatterySettings, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.battery_open_settings))
                }
            }
            if (uiState.batteryGuideUrl != null) {
                TextButton(onClick = onOpenGuide) { Text(stringResource(R.string.battery_open_guide)) }
            }
            Text(
                stringResource(R.string.battery_disclaimer),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private enum class StatusChip(@StringRes val label: Int) {
    Enabled(R.string.permission_status_enabled),
    NotEnabled(R.string.permission_status_not_enabled),
    NeedsConsent(R.string.permission_status_needs_consent),
    NotRunning(R.string.permission_status_not_running),
}

@Composable
private fun PermissionCard(
    title: String,
    @DrawableRes icon: Int,
    status: StatusChip,
    @StringRes why: Int,
    @StringRes feature: Int,
    @StringRes declined: Int,
    @StringRes steps: Int?,
    @StringRes actionLabel: Int?,
    onAction: () -> Unit,
    extra: @Composable () -> Unit = {},
) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                IconBadge(icon = icon, tint = MaterialTheme.colorScheme.primary)
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier
                        .weight(1f)
                        .semantics { heading() },
                )
                StatusLabel(status)
            }
            LabeledText(R.string.permission_why_label, why)
            LabeledText(R.string.permission_feature_label, feature)
            LabeledText(R.string.permission_declined_label, declined)
            if (steps != null) {
                Text(stringResource(steps), style = MaterialTheme.typography.bodyMedium)
            }
            if (actionLabel != null) {
                Button(onClick = onAction, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(actionLabel))
                }
            }
            extra()
        }
    }
}

@Composable
private fun StatusLabel(status: StatusChip) {
    val colors = ScrollLessTheme.colors
    val (container, content) = when (status) {
        StatusChip.Enabled -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
        StatusChip.NotEnabled, StatusChip.NeedsConsent -> colors.warningContainer to colors.onWarningContainer
        StatusChip.NotRunning -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
    }
    Surface(color = container, contentColor = content, shape = MaterialTheme.shapes.small) {
        Text(
            text = stringResource(status.label),
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}

@Composable
private fun LabeledText(@StringRes label: Int, @StringRes body: Int) {
    Column {
        Text(
            text = stringResource(label),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = stringResource(body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun NoticeCard(text: String, container: Color, content: Color) {
    Card(
        colors = CardDefaults.cardColors(containerColor = container, contentColor = content),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(text = text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(16.dp))
    }
}

/** The trade-off of Reels tracking, stated before and after turning it on. */
@Composable
private fun PaymentAppNote() {
    NoticeCard(
        text = stringResource(R.string.accessibility_payment_note),
        container = ScrollLessTheme.colors.warningContainer,
        content = ScrollLessTheme.colors.onWarningContainer,
    )
}

/**
 * Google Play's prominent disclosure: shown in-app, before Accessibility settings, with explicit
 * consent. It also states the payment-app trade-off, so the user can choose Basic tracking instead.
 */
@Composable
private fun AccessibilityDisclosureDialog(onAccept: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.disclosure_title)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(stringResource(R.string.disclosure_body))
                Surface(
                    color = ScrollLessTheme.colors.warningContainer,
                    contentColor = ScrollLessTheme.colors.onWarningContainer,
                    shape = MaterialTheme.shapes.medium,
                ) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            stringResource(R.string.disclosure_banking_title),
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.semantics { heading() },
                        )
                        Text(stringResource(R.string.disclosure_banking_body), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onAccept) { Text(stringResource(R.string.disclosure_accept)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.disclosure_decline)) } },
    )
}

@StringRes
fun SettingsTarget.failureMessage(): Int = when (this) {
    SettingsTarget.USAGE_ACCESS -> R.string.settings_open_failed_usage
    SettingsTarget.ACCESSIBILITY -> R.string.settings_open_failed_accessibility
    SettingsTarget.APP_DETAILS -> R.string.settings_open_failed_app_info
    SettingsTarget.BATTERY -> R.string.settings_open_failed_battery
    SettingsTarget.NOTIFICATIONS -> R.string.settings_open_failed_notifications
    SettingsTarget.DEVELOPER_OPTIONS -> R.string.settings_open_failed_developer
}

@Preview
@Composable
private fun PermissionsScreenPreview() {
    ScrollLessTheme {
        PermissionsScreen(
            uiState = PermissionsUiState(
                isLoading = false,
                usageAccessGranted = true,
                accessibilityStatus = AccessibilityStatus.NOT_RUNNING,
                consentGiven = true,
            ),
            fromOnboarding = true,
            snackbarHostState = remember { SnackbarHostState() },
            onBack = {},
            onContinue = {},
            onEnableUsageAccess = {},
            onAccessibilityAction = {},
            onOpenAppInfo = {},
            onDisclosureAccepted = {},
            onDisclosureDismissed = {},
        )
    }
}
