package com.sunpawtechnologies.scrollless.ui.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.sunpawtechnologies.scrollless.R
import com.sunpawtechnologies.scrollless.domain.model.LimitMetric
import com.sunpawtechnologies.scrollless.ui.analytics.AnalyticsRoute
import com.sunpawtechnologies.scrollless.ui.dashboard.DashboardRoute
import com.sunpawtechnologies.scrollless.ui.limits.LimitsRoute
import com.sunpawtechnologies.scrollless.ui.onboarding.OnboardingRoute
import com.sunpawtechnologies.scrollless.ui.permissions.PermissionsRoute
import com.sunpawtechnologies.scrollless.ui.settings.SettingsRoute
import kotlinx.serialization.Serializable
import kotlin.reflect.KClass

@Serializable data object OnboardingDestination
@Serializable data object DashboardDestination
@Serializable data object LimitsDestination
@Serializable data object AnalyticsDestination

/** @param fromOnboarding shows a Continue button instead of Back, as the final onboarding step. */
@Serializable data class PermissionsDestination(val fromOnboarding: Boolean = false)

@Serializable data object SettingsDestination

/** The four bottom-bar destinations. Other screens open full-screen on top. */
private enum class TopLevel(val route: Any, val routeClass: KClass<*>, @DrawableRes val icon: Int, @StringRes val label: Int) {
    HOME(DashboardDestination, DashboardDestination::class, R.drawable.ic_home, R.string.nav_home),
    ANALYTICS(AnalyticsDestination, AnalyticsDestination::class, R.drawable.ic_bar_chart, R.string.nav_analytics),
    LIMITS(LimitsDestination, LimitsDestination::class, R.drawable.ic_hourglass_top, R.string.nav_limits),
    SETTINGS(SettingsDestination, SettingsDestination::class, R.drawable.ic_settings, R.string.nav_settings),
}

@Composable
fun ScrollLessNavHost(
    startWithOnboarding: Boolean,
    reminderMetric: LimitMetric? = null,
    onReminderConsumed: () -> Unit = {},
    navController: NavHostController = rememberNavController(),
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val destination = backStackEntry?.destination
    val currentTopLevel = TopLevel.entries.firstOrNull { item -> destination?.hasRoute(item.routeClass) == true }

    fun openTopLevel(item: TopLevel) {
        navController.navigate(item.route) {
            // Standard bottom-bar behavior: one copy of each tab, state kept when switching.
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }
    val openPermissions = { navController.navigate(PermissionsDestination()) }

    // A limit reminder was tapped: show the dashboard, which presents that limit.
    LaunchedEffect(reminderMetric) {
        if (reminderMetric != null && currentTopLevel != TopLevel.HOME && !startWithOnboarding) openTopLevel(TopLevel.HOME)
    }

    Scaffold(
        bottomBar = {
            if (currentTopLevel != null) {
                NavigationBar {
                    TopLevel.entries.forEach { item ->
                        NavigationBarItem(
                            selected = item == currentTopLevel,
                            onClick = { if (item != currentTopLevel) openTopLevel(item) },
                            icon = { Icon(painterResource(item.icon), contentDescription = null) },
                            label = { Text(stringResource(item.label)) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = if (startWithOnboarding) OnboardingDestination else DashboardDestination,
            modifier = Modifier
                .padding(padding)
                .consumeWindowInsets(padding),
            // Short cross-fade only: cheap on budget phones.
            enterTransition = { fadeIn(tween(180)) },
            exitTransition = { fadeOut(tween(120)) },
        ) {
            composable<OnboardingDestination> {
                OnboardingRoute(
                    onFinished = {
                        navController.navigate(PermissionsDestination(fromOnboarding = true)) {
                            popUpTo<OnboardingDestination> { inclusive = true }
                        }
                    },
                )
            }
            composable<DashboardDestination> {
                DashboardRoute(
                    onOpenSettings = dropUnlessResumed { openTopLevel(TopLevel.SETTINGS) },
                    onOpenLimits = dropUnlessResumed { openTopLevel(TopLevel.LIMITS) },
                    onOpenAnalytics = dropUnlessResumed { openTopLevel(TopLevel.ANALYTICS) },
                    onSetUpTracking = dropUnlessResumed { openPermissions() },
                    reminderMetric = reminderMetric,
                    onReminderConsumed = onReminderConsumed,
                )
            }
            composable<AnalyticsDestination> {
                AnalyticsRoute(onSetUpTracking = dropUnlessResumed { openPermissions() })
            }
            composable<LimitsDestination> { LimitsRoute(onSetUpTracking = dropUnlessResumed { openPermissions() }) }
            composable<SettingsDestination> {
                SettingsRoute(onOpenPermissions = dropUnlessResumed { openPermissions() })
            }
            composable<PermissionsDestination> { entry ->
                val route = entry.toRoute<PermissionsDestination>()
                PermissionsRoute(
                    fromOnboarding = route.fromOnboarding,
                    onBack = dropUnlessResumed { navController.popBackStack() },
                    onContinue = dropUnlessResumed {
                        navController.navigate(DashboardDestination) {
                            popUpTo<PermissionsDestination> { inclusive = true }
                        }
                    },
                )
            }
        }
    }
}
