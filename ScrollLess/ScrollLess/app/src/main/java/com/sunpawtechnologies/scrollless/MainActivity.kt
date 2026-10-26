package com.sunpawtechnologies.scrollless

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sunpawtechnologies.scrollless.alerts.AndroidLimitNotifier
import com.sunpawtechnologies.scrollless.domain.model.LimitMetric
import com.sunpawtechnologies.scrollless.ui.navigation.ScrollLessNavHost
import com.sunpawtechnologies.scrollless.ui.theme.ScrollLessTheme
import com.sunpawtechnologies.scrollless.ui.theme.isDarkTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels { MainViewModel.Factory }

    /** The limit whose reminder opened the app, until the dashboard has shown it. */
    private var reminderMetric by mutableStateOf<LimitMetric?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        // Hold the splash until the saved theme is known, so the first frame uses the right colors.
        splashScreen.setKeepOnScreenCondition { viewModel.uiState.value is MainUiState.Loading }
        enableEdgeToEdge()
        reminderMetric = intent.reminderMetric()

        setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val ready = uiState as? MainUiState.Ready
            if (ready != null) {
                val darkTheme = ready.themeMode.isDarkTheme()
                SystemBarsEffect(darkTheme)
                ScrollLessTheme(darkTheme = darkTheme) {
                    // Decide the start screen once; later onboarding changes are handled by navigation.
                    val startWithOnboarding = rememberSaveable { !ready.onboardingCompleted }
                    ScrollLessNavHost(
                        startWithOnboarding = startWithOnboarding,
                        reminderMetric = reminderMetric,
                        onReminderConsumed = {
                            reminderMetric = null
                            // So a rotation or recreation doesn't show it again.
                            intent.removeExtra(AndroidLimitNotifier.EXTRA_LIMIT_METRIC)
                        },
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        reminderMetric = intent.reminderMetric()
    }

    private fun Intent.reminderMetric(): LimitMetric? =
        getStringExtra(AndroidLimitNotifier.EXTRA_LIMIT_METRIC)?.let { name -> LimitMetric.entries.firstOrNull { it.name == name } }

    override fun onResume() {
        super.onResume()
        // The user may have just changed a permission in system settings. Runs before the screens' ON_RESUME.
        val container = (application as ScrollLessApp).container
        container.permissionMonitor.refresh()
        container.syncLimitCheckSchedule()
    }

    /** Keeps status- and navigation-bar icons readable when the in-app theme differs from the system's. */
    @Composable
    private fun SystemBarsEffect(darkTheme: Boolean) {
        DisposableEffect(darkTheme) {
            enableEdgeToEdge(
                statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { darkTheme },
                navigationBarStyle = SystemBarStyle.auto(LIGHT_SCRIM, DARK_SCRIM) { darkTheme },
            )
            onDispose {}
        }
    }

    private companion object {
        // Same as the androidx.activity defaults for three-button navigation.
        val LIGHT_SCRIM = Color.argb(0xe6, 0xFF, 0xFF, 0xFF)
        val DARK_SCRIM = Color.argb(0x80, 0x1b, 0x1b, 0x1b)
    }
}
