package com.stagirite.ledger.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.stagirite.ledger.data.Streak
import com.stagirite.ledger.data.plural

private const val MAX_BARS = 30

/**
 * Length of each run, oldest to newest; the last bar is the current run.
 * One series, so no legend. Only the longest and current runs are labelled;
 * tap a bar for its dates.
 */
@Composable
fun RunsChart(runs: List<Streak>, modifier: Modifier = Modifier) {
    val shown = runs.takeLast(MAX_BARS)
    val longestIndex = shown.indices.maxByOrNull { shown[it].days } ?: 0
    val currentIndex = shown.lastIndex
    var selected by remember(runs) { mutableStateOf<Int?>(null) }
    val measurer = rememberTextMeasurer()
    val ink = MaterialTheme.colorScheme.onBackground
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val rule = MaterialTheme.colorScheme.outlineVariant
    val labelStyle = MaterialTheme.typography.labelSmall.copy(color = muted)

    Column(modifier.fillMaxWidth()) {
        val sel = selected?.let { shown.getOrNull(it) }
        Text(
            if (sel == null) "Tap a bar for its dates"
            else "${plural(sel.days, "day")} · ${sel.start.format(mediumDate)} – " +
                if (selected == currentIndex) "now" else sel.end.format(mediumDate),
            style = MaterialTheme.typography.bodySmall,
            color = muted,
        )
        Spacer(Modifier.height(8.dp))
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(140.dp)
                .semantics {
                    contentDescription = "Run lengths: " + shown.joinToString { "${it.days} days" }
                }
                .pointerInput(shown) {
                    detectTapGestures { pos ->
                        val slot = size.width / shown.size.coerceAtLeast(1)
                        selected = (pos.x / slot).toInt().coerceIn(0, shown.lastIndex)
                    }
                },
        ) {
            if (shown.isEmpty()) return@Canvas
            val labelSpace = 16.dp.toPx()
            val plotHeight = size.height - labelSpace
            val max = shown.maxOf { it.days }.coerceAtLeast(1)
            val slot = size.width / shown.size
            val gap = 2.dp.toPx()
            val barWidth = (slot - gap).coerceAtMost(28.dp.toPx())
            val radius = 4.dp.toPx().coerceAtMost(barWidth / 2)

            drawLine(rule, Offset(0f, size.height), Offset(size.width, size.height), strokeWidth = 1.dp.toPx())
            shown.forEachIndexed { i, run ->
                val h = (run.days.toFloat() / max) * plotHeight
                val left = i * slot + (slot - barWidth) / 2
                val top = size.height - h
                val alpha = if (selected == null || selected == i) 1f else 0.35f
                if (h > 0f) {
                    // Rounded at the top only; flat where it meets the baseline.
                    val path = Path().apply {
                        addRoundRect(
                            RoundRect(
                                left = left, top = top, right = left + barWidth, bottom = size.height,
                                topLeftCornerRadius = CornerRadius(radius),
                                topRightCornerRadius = CornerRadius(radius),
                            )
                        )
                    }
                    drawPath(path, ink.copy(alpha = alpha))
                }
                if (i == longestIndex || i == currentIndex) {
                    val text = if (i == currentIndex) "${run.days} now" else "${run.days}"
                    val layout = measurer.measure(text, labelStyle)
                    val x = (left + barWidth / 2 - layout.size.width / 2)
                        .coerceIn(0f, size.width - layout.size.width)
                    drawText(layout, topLeft = Offset(x, top - layout.size.height - 2.dp.toPx()))
                }
            }
        }
        if (runs.size > MAX_BARS) {
            Spacer(Modifier.height(4.dp))
            Text(
                "Latest $MAX_BARS of ${runs.size} runs",
                style = MaterialTheme.typography.labelSmall,
                color = muted,
            )
        }
    }
}

