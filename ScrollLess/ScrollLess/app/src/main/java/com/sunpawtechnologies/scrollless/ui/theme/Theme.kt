package com.sunpawtechnologies.scrollless.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import com.sunpawtechnologies.scrollless.domain.model.ThemeMode

@Composable
fun ScrollLessTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    // Dynamic color is deliberately off: fixed tokens keep charts and warnings readable in both themes.
    CompositionLocalProvider(
        LocalScrollLessColors provides if (darkTheme) DarkScrollLessColors else LightScrollLessColors,
    ) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
            typography = Typography,
            shapes = Shapes,
            content = content,
        )
    }
}

object ScrollLessTheme {
    val colors: ScrollLessColors
        @Composable
        @ReadOnlyComposable
        get() = LocalScrollLessColors.current
}

@Composable
fun ThemeMode.isDarkTheme(): Boolean = when (this) {
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
}
