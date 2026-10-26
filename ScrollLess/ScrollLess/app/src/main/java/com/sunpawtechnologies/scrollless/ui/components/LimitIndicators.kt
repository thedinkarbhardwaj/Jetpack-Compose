package com.sunpawtechnologies.scrollless.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sunpawtechnologies.scrollless.R
import com.sunpawtechnologies.scrollless.domain.model.LimitStatus
import com.sunpawtechnologies.scrollless.ui.theme.ScrollLessTheme

/** Color for a limit status. Always shown together with [LimitStatusPill]'s icon and label. */
@Composable
fun LimitStatus.color(): Color = when (this) {
    LimitStatus.WITHIN -> MaterialTheme.colorScheme.primary
    LimitStatus.APPROACHING -> ScrollLessTheme.colors.statusWarning
    LimitStatus.REACHED -> ScrollLessTheme.colors.statusCritical
}

@Composable
fun LimitStatusPill(status: LimitStatus, modifier: Modifier = Modifier) {
    val (icon, label, tint) = when (status) {
        LimitStatus.WITHIN -> Triple(R.drawable.ic_check_circle, R.string.limit_status_within, ScrollLessTheme.colors.statusGood)
        LimitStatus.APPROACHING -> Triple(R.drawable.ic_warning, R.string.limit_status_approaching, ScrollLessTheme.colors.statusWarning)
        LimitStatus.REACHED -> Triple(R.drawable.ic_error, R.string.limit_status_reached, ScrollLessTheme.colors.statusCritical)
    }
    StatusPill(text = stringResource(label), icon = icon, tint = tint, modifier = modifier)
}

/**
 * A circular progress ring. [progress] above 1 draws a full ring; the label inside should show
 * the real value, so going over the limit is never hidden.
 */
@Composable
fun ProgressRing(
    progress: Float,
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = 132.dp,
    strokeWidth: Dp = 12.dp,
    trackColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    content: @Composable BoxScope.() -> Unit,
) {
    val animated by animateFloatAsState(progress.coerceIn(0f, 1f), animationSpec = tween(450), label = "ring")
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(size)) {
            val stroke = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
            val inset = strokeWidth.toPx() / 2
            val arcSize = androidx.compose.ui.geometry.Size(this.size.width - inset * 2, this.size.height - inset * 2)
            val topLeft = androidx.compose.ui.geometry.Offset(inset, inset)
            drawArc(trackColor, startAngle = -90f, sweepAngle = 360f, useCenter = false, topLeft = topLeft, size = arcSize, style = stroke)
            if (animated > 0f) {
                drawArc(color, startAngle = -90f, sweepAngle = 360f * animated, useCenter = false, topLeft = topLeft, size = arcSize, style = stroke)
            }
        }
        content()
    }
}
