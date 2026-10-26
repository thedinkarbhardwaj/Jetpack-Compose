package com.sunpawtechnologies.scrollless.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** App-specific color roles that Material 3 doesn't define. */
@Immutable
data class ScrollLessColors(
    val warning: Color,
    val onWarning: Color,
    val warningContainer: Color,
    val onWarningContainer: Color,
    /** Used for "not tracked" states, which must never look like a zero value. */
    val notTracked: Color,
    /** Chart series, one fixed color per entity (validated for CVD and contrast in both themes). */
    val seriesReels: Color,
    val seriesInstagramTime: Color,
    val seriesYoutubeTime: Color,
    /** Limit status. Always paired with an icon and label, never color alone. */
    val statusGood: Color,
    val statusWarning: Color,
    val statusCritical: Color,
    /** Background for the hero card gradient. */
    val heroStart: Color,
    val heroEnd: Color,
)

internal val LightScrollLessColors = ScrollLessColors(
    warning = Color(0xFF7A5900),
    onWarning = Color(0xFFFFFFFF),
    warningContainer = Color(0xFFFFDF9E),
    onWarningContainer = Color(0xFF261A00),
    notTracked = Color(0xFF6F7979),
    seriesReels = Color(0xFF2A78D6),
    seriesInstagramTime = Color(0xFFEB6834),
    seriesYoutubeTime = Color(0xFF1BAF7A),
    statusGood = Color(0xFF0CA30C),
    statusWarning = Color(0xFFFAB219),
    statusCritical = Color(0xFFD03B3B),
    heroStart = Color(0xFFD7F3F1),
    heroEnd = Color(0xFFE9EFEE),
)

internal val DarkScrollLessColors = ScrollLessColors(
    warning = Color(0xFFF4BF48),
    onWarning = Color(0xFF402D00),
    warningContainer = Color(0xFF5C4300),
    onWarningContainer = Color(0xFFFFDF9E),
    notTracked = Color(0xFF889392),
    seriesReels = Color(0xFF3987E5),
    seriesInstagramTime = Color(0xFFD95926),
    seriesYoutubeTime = Color(0xFF199E70),
    statusGood = Color(0xFF0CA30C),
    statusWarning = Color(0xFFFAB219),
    statusCritical = Color(0xFFD03B3B),
    heroStart = Color(0xFF15302F),
    heroEnd = Color(0xFF1A2121),
)

internal val LocalScrollLessColors = staticCompositionLocalOf { LightScrollLessColors }
