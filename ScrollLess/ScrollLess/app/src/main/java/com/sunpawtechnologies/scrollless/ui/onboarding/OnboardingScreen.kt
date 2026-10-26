package com.sunpawtechnologies.scrollless.ui.onboarding

import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sunpawtechnologies.scrollless.R
import com.sunpawtechnologies.scrollless.ui.components.IconBadge
import com.sunpawtechnologies.scrollless.ui.theme.ScrollLessTheme
import com.sunpawtechnologies.scrollless.ui.theme.Spacing
import com.sunpawtechnologies.scrollless.ui.util.ObserveAsEvents

@Composable
fun OnboardingRoute(
    onFinished: () -> Unit,
    viewModel: OnboardingViewModel = viewModel(factory = OnboardingViewModel.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ObserveAsEvents(viewModel.effects) { effect ->
        when (effect) {
            OnboardingEffect.Finished -> onFinished()
        }
    }
    BackHandler(enabled = uiState.step == OnboardingStep.DAILY_LIMIT, onBack = viewModel::onBack)
    OnboardingScreen(
        uiState = uiState,
        onGetStarted = viewModel::onGetStartedClicked,
        onLimitSelected = viewModel::onLimitSelected,
        onContinue = viewModel::onContinueClicked,
        onSkip = viewModel::onSkipClicked,
    )
}

/** Welcome, then the daily limit. Permission setup follows on its own screen. */
@Composable
fun OnboardingScreen(
    uiState: OnboardingUiState,
    onGetStarted: () -> Unit,
    onLimitSelected: (Int) -> Unit = {},
    onContinue: () -> Unit = {},
    onSkip: () -> Unit = {},
) {
    Scaffold { padding ->
        // Forward: the new step slides in from the end. Back: from the start. Short and cheap.
        AnimatedContent(
            targetState = uiState.step,
            transitionSpec = {
                val forward = targetState.ordinal > initialState.ordinal
                (slideInHorizontally(tween(260)) { if (forward) it / 4 else -it / 4 } + fadeIn(tween(260))) togetherWith
                    (slideOutHorizontally(tween(200)) { if (forward) -it / 4 else it / 4 } + fadeOut(tween(200)))
            },
            label = "onboarding_step",
            modifier = Modifier.padding(padding),
        ) { step ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Spacing.xl, vertical = Spacing.xxl),
                verticalArrangement = Arrangement.spacedBy(Spacing.lg),
            ) {
                when (step) {
                    OnboardingStep.WELCOME -> WelcomeStep(onGetStarted)
                    OnboardingStep.DAILY_LIMIT -> DailyLimitStep(uiState, onLimitSelected, onContinue, onSkip)
                }
            }
        }
    }
}

@Composable
private fun WelcomeStep(onGetStarted: () -> Unit) {
    BrandMark()
    Spacer(Modifier.height(Spacing.sm))
    Text(
        text = stringResource(R.string.onboarding_welcome_title),
        style = MaterialTheme.typography.headlineMedium,
        modifier = Modifier.semantics { heading() },
    )
    Text(
        text = stringResource(R.string.tagline),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
    )
    Text(
        text = stringResource(R.string.onboarding_welcome_body),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(Spacing.sm))
    Feature(R.drawable.ic_hourglass_top, R.string.onboarding_feature_limits)
    Feature(R.drawable.ic_play_arrow, R.string.onboarding_feature_count)
    Feature(R.drawable.ic_shield, R.string.onboarding_feature_privacy)
    Spacer(Modifier.height(Spacing.lg))
    Button(
        onClick = onGetStarted,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
    ) {
        Text(stringResource(R.string.onboarding_get_started))
    }
}

@Composable
private fun DailyLimitStep(
    uiState: OnboardingUiState,
    onLimitSelected: (Int) -> Unit,
    onContinue: () -> Unit,
    onSkip: () -> Unit,
) {
    Text(
        text = stringResource(R.string.onboarding_step_of, 2, 2),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
    )
    Text(
        text = stringResource(R.string.onboarding_limit_title),
        style = MaterialTheme.typography.headlineSmall,
        modifier = Modifier.semantics { heading() },
    )
    Text(
        text = stringResource(R.string.onboarding_limit_body),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    val shown by animateIntAsState(uiState.dailyLimitMinutes, animationSpec = tween(300), label = "limit")
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(Brush.linearGradient(listOf(ScrollLessTheme.colors.heroStart, ScrollLessTheme.colors.heroEnd)))
            .padding(Spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        Text(stringResource(R.string.onboarding_limit_value, shown), style = MaterialTheme.typography.displaySmall)
        Text(
            stringResource(R.string.onboarding_limit_value_caption),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        OnboardingUiState.LIMIT_CHOICES.forEach { minutes ->
            FilterChip(
                selected = uiState.dailyLimitMinutes == minutes,
                onClick = { onLimitSelected(minutes) },
                label = { Text(stringResource(R.string.duration_minutes, minutes)) },
            )
        }
    }
    Text(
        text = stringResource(R.string.onboarding_limit_footnote),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(Spacing.sm))
    Button(
        onClick = onContinue,
        enabled = !uiState.isCompleting,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
    ) {
        Text(stringResource(R.string.onboarding_limit_continue))
    }
    TextButton(onClick = onSkip, enabled = !uiState.isCompleting, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.onboarding_limit_skip))
    }
}

/** The launcher icon's mark on its gradient, so onboarding and the home screen icon match. */
@Composable
private fun BrandMark() {
    Box(
        modifier = Modifier
            .size(88.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF129088), Color(0xFF0A3B44)))),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.ic_launcher_foreground),
            contentDescription = null,
            modifier = Modifier.size(132.dp),
        )
    }
}

@Composable
private fun Feature(@DrawableRes icon: Int, @StringRes text: Int) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.lg)) {
        IconBadge(icon = icon, tint = MaterialTheme.colorScheme.primary)
        Text(stringResource(text), style = MaterialTheme.typography.bodyLarge)
    }
}

@Preview
@Composable
private fun OnboardingScreenPreview() {
    ScrollLessTheme {
        OnboardingScreen(uiState = OnboardingUiState(), onGetStarted = {})
    }
}

@Preview
@Composable
private fun OnboardingLimitPreview() {
    ScrollLessTheme(darkTheme = true) {
        OnboardingScreen(uiState = OnboardingUiState(step = OnboardingStep.DAILY_LIMIT, dailyLimitMinutes = 45), onGetStarted = {})
    }
}
