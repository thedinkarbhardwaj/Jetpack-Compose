package com.sunpawtechnologies.scrollless.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.sunpawtechnologies.scrollless.domain.usecase.BucketState
import com.sunpawtechnologies.scrollless.domain.usecase.ChartBucket
import com.sunpawtechnologies.scrollless.ui.theme.ScrollLessTheme
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow

/**
 * Single-series column chart for tracked usage.
 *
 * - Tracked buckets: columns ≤ 24dp wide, 4dp rounded tip, square at the baseline.
 * - Not-tracked buckets: a short muted dash on the baseline, so a gap never reads as zero.
 * - Future buckets: nothing.
 * - Optional dashed limit line.
 * - Tap a column to select it; the caller shows the details (the tooltip equivalent).
 *
 * @param valueUnit values are rounded to multiples of this for axis steps (60 000 for minutes).
 */
@Composable
fun UsageBarChart(
    buckets: List<ChartBucket>,
    seriesColor: Color,
    axisLabel: (Long) -> String,
    bucketLabels: List<String>,
    labelEvery: Int,
    selectedIndex: Int?,
    onSelect: (Int?) -> Unit,
    chartDescription: String,
    modifier: Modifier = Modifier,
    limit: Long? = null,
    limitLabel: String? = null,
    valueUnit: Long = 1,
) {
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val limitColor = MaterialTheme.colorScheme.onSurfaceVariant
    val notTrackedColor = ScrollLessTheme.colors.notTracked
    val selectionColor = MaterialTheme.colorScheme.surfaceVariant
    val currentOnSelect = rememberUpdatedState(onSelect)
    val currentSelected = rememberUpdatedState(selectedIndex)

    val maxValue = remember(buckets, limit) {
        val raw = maxOf(buckets.maxOfOrNull { it.value ?: 0L } ?: 0L, limit ?: 0L)
        niceCeil(raw, valueUnit)
    }
    val axisValues = remember(maxValue) { listOf(0L, maxValue / 2, maxValue) }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
            .semantics { contentDescription = chartDescription }
            .pointerInput(buckets.size) {
                detectTapGestures { tap ->
                    val left = AXIS_WIDTH.toPx()
                    val slot = (size.width - left) / buckets.size
                    val index = ((tap.x - left) / slot).toInt()
                    if (index in buckets.indices) {
                        currentOnSelect.value(if (currentSelected.value == index) null else index)
                    }
                }
            },
    ) {
        val left = AXIS_WIDTH.toPx()
        val bottomLabelSpace = 22.dp.toPx()
        val top = 14.dp.toPx()
        val chartHeight = size.height - bottomLabelSpace - top
        val baseline = top + chartHeight
        val slot = (size.width - left) / buckets.size
        val barWidth = minOf(24.dp.toPx(), slot * 0.62f)
        fun yFor(value: Long) = baseline - (value.toFloat() / maxValue) * chartHeight

        // Recessive grid with axis labels.
        axisValues.forEach { value ->
            val y = yFor(value)
            drawLine(gridColor, Offset(left, y), Offset(size.width, y), strokeWidth = 1.dp.toPx())
            val measured = textMeasurer.measure(axisLabel(value), labelStyle)
            drawText(measured, topLeft = Offset(left - measured.size.width - 6.dp.toPx(), y - measured.size.height / 2))
        }

        selectedIndex?.takeIf { it in buckets.indices }?.let { index ->
            drawRect(selectionColor.copy(alpha = 0.6f), Offset(left + slot * index, top), Size(slot, chartHeight))
        }

        val radius = 4.dp.toPx()
        buckets.forEachIndexed { index, bucket ->
            val centerX = left + slot * index + slot / 2
            val barLeft = centerX - barWidth / 2
            when (bucket.state) {
                BucketState.TRACKED -> {
                    val value = bucket.value ?: 0L
                    if (value > 0) {
                        val barTop = minOf(yFor(value), baseline - 2.dp.toPx())
                        val color = if (selectedIndex == null || selectedIndex == index) seriesColor else seriesColor.copy(alpha = 0.45f)
                        val path = Path().apply {
                            addRoundRect(
                                RoundRect(
                                    left = barLeft, top = barTop, right = barLeft + barWidth, bottom = baseline,
                                    topLeftCornerRadius = CornerRadius(radius), topRightCornerRadius = CornerRadius(radius),
                                    bottomRightCornerRadius = CornerRadius.Zero, bottomLeftCornerRadius = CornerRadius.Zero,
                                ),
                            )
                        }
                        drawPath(path, color)
                    }
                }
                BucketState.NOT_TRACKED -> drawRect(
                    notTrackedColor,
                    Offset(centerX - barWidth * 0.3f, baseline - 3.dp.toPx()),
                    Size(barWidth * 0.6f, 3.dp.toPx()),
                )
                BucketState.FUTURE -> Unit
            }
            if (index % labelEvery == 0 && index < bucketLabels.size) {
                val measured = textMeasurer.measure(bucketLabels[index], labelStyle)
                drawText(measured, topLeft = Offset(centerX - measured.size.width / 2, baseline + 6.dp.toPx()))
            }
        }

        if (limit != null && limit > 0) {
            val y = yFor(limit)
            drawLine(
                limitColor, Offset(left, y), Offset(size.width, y),
                strokeWidth = 1.5.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx())),
            )
            if (limitLabel != null) {
                val measured = textMeasurer.measure(limitLabel, labelStyle)
                drawText(measured, topLeft = Offset(size.width - measured.size.width, y - measured.size.height - 2.dp.toPx()))
            }
        }
    }
}

private val AXIS_WIDTH = 40.dp

/** Rounds up to 1, 2, 2.5 or 5 × a power of ten (in [unit]s), so axis steps are easy to read. */
internal fun niceCeil(value: Long, unit: Long = 1): Long {
    val units = maxOf(1.0, value.toDouble() / unit)
    val magnitude = 10.0.pow(floor(log10(units)))
    val step = listOf(1.0, 2.0, 2.5, 5.0, 10.0).first { it * magnitude >= units } * magnitude
    // Even maximum so the midline label is a whole number.
    val rounded = kotlin.math.ceil(step).toLong().let { if (it % 2 == 1L && it > 1) it + 1 else it }
    return maxOf(rounded, 2) * unit
}
