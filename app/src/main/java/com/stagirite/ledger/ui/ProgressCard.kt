package com.stagirite.ledger.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
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
import androidx.compose.ui.unit.sp
import com.stagirite.ledger.data.Goal
import com.stagirite.ledger.data.MonthKept
import com.stagirite.ledger.data.Progress
import com.stagirite.ledger.data.Verdict
import com.stagirite.ledger.data.plural
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt
import kotlin.math.roundToLong

/** One sentence on why the verdict is what it is. */
private fun explain(p: Progress, slips: String): String {
    if (p.verdict == Verdict.Unbroken) return "No $slips since you began."
    if (p.verdict == Verdict.TooSoon) return "A trend shows once there are a few runs to compare."
    val parts = buildList {
        val r = p.recentMedian
        val e = p.earlierMedian
        if (r != null && e != null) {
            val ratio = r / maxOf(e, 1.0)
            add(
                when {
                    ratio >= 1.25 -> "your runs are getting longer"
                    ratio <= 0.8 -> "your runs have been shorter"
                    else -> "your runs are about as long as before"
                }
            )
        }
        val now = p.slipsRecent
        val before = p.slipsBefore
        if (now != null && before != null && now + before > 0) {
            add(
                when {
                    now < before -> "$slips are less frequent"
                    now > before -> "$slips have been more frequent"
                    else -> "$slips are as frequent as before"
                }
            )
        }
    }
    return parts.joinToString(", and ").replaceFirstChar { it.uppercase() } + "."
}

private fun arrow(now: Double, before: Double, higherIsBetter: Boolean): String = when {
    now == before -> "→"
    (now > before) == higherIsBetter -> "↑"
    else -> "↓"
}

private fun days(d: Double): String = plural(d.roundToLong(), "day")

/** The progress card: verdict, the comparisons behind it, and kept days by month. */
@Composable
fun ProgressPanel(goal: Goal, p: Progress) {
    val slips = goal.kind?.slips ?: "slips"
    val slipsTitle = slips.replaceFirstChar { it.uppercase() }

    SectionHeading("Progress", Modifier.padding(start = 0.dp))
    Text(p.verdict.label, style = MaterialTheme.typography.headlineSmall)
    Spacer(Modifier.height(4.dp))
    Text(explain(p, slips), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Spacer(Modifier.height(16.dp))

    val rows = buildList {
        if (p.recentMedian != null && p.earlierMedian != null) {
            add(
                Triple(
                    "Typical run",
                    "${days(p.recentMedian)} · was ${days(p.earlierMedian)}",
                    arrow(p.recentMedian, p.earlierMedian, higherIsBetter = true),
                )
            )
        }
        if (p.slipsRecent != null && p.slipsBefore != null) {
            add(
                Triple(
                    "$slipsTitle, last ${p.windowDays} days",
                    "${p.slipsRecent} · was ${p.slipsBefore}",
                    arrow(p.slipsRecent.toDouble(), p.slipsBefore.toDouble(), higherIsBetter = false),
                )
            )
        }
        if (p.closeRecent != null && p.closeBefore != null && p.closeRecent + p.closeBefore > 0) {
            add(
                Triple(
                    "Back-to-back $slips",
                    "${p.closeRecent} · was ${p.closeBefore}",
                    arrow(p.closeRecent.toDouble(), p.closeBefore.toDouble(), higherIsBetter = false),
                )
            )
        }
        add(Triple("New personal bests", "${p.newBests}", ""))
    }
    rows.forEachIndexed { i, (label, value, mark) ->
        Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            Text(value, style = MaterialTheme.typography.bodyMedium)
            if (mark.isNotEmpty()) {
                Text(mark, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(start = 8.dp))
            }
        }
        if (i < rows.lastIndex) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
    Spacer(Modifier.height(8.dp))
    Text(
        "Typical run is the median of your last ${Progress.RECENT} runs against the ${Progress.RECENT} before. " +
            "Back-to-back means within ${Progress.CLOSE_DAYS} days of the one before. A new personal best is a run " +
            "longer than every run before it.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.outline,
    )

    if (p.months.isNotEmpty()) {
        Spacer(Modifier.height(24.dp))
        Text("Days kept, by month", style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(4.dp))
        MonthKeptChart(p.months)
    }
}

/**
 * Share of days kept each month, 0–100%. One series, so no legend; the latest
 * month is labelled and a tap shows any other.
 */
@Composable
fun MonthKeptChart(months: List<MonthKept>, modifier: Modifier = Modifier) {
    var selected by remember(months) { mutableStateOf<Int?>(null) }
    val measurer = rememberTextMeasurer()
    val ink = MaterialTheme.colorScheme.onBackground
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val rule = MaterialTheme.colorScheme.outlineVariant
    val labelStyle = MaterialTheme.typography.labelSmall.copy(color = muted)
    val locale = Locale.getDefault()

    Column(modifier.fillMaxWidth()) {
        val shown = selected?.let { months.getOrNull(it) } ?: months.last()
        Text(
            "${shown.month.month.getDisplayName(TextStyle.FULL, locale)}: ${shown.kept} of ${plural(shown.tracked, "day")} kept " +
                "(${(shown.rate * 100).roundToInt()}%)",
            style = MaterialTheme.typography.bodySmall,
            color = muted,
        )
        Spacer(Modifier.height(8.dp))
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(110.dp)
                .semantics {
                    contentDescription = "Days kept by month: " +
                        months.joinToString { "${it.month.month.getDisplayName(TextStyle.SHORT, locale)} ${(it.rate * 100).roundToInt()}%" }
                }
                .pointerInput(months) {
                    detectTapGestures { pos ->
                        val slot = size.width / months.size.coerceAtLeast(1)
                        selected = (pos.x / slot).toInt().coerceIn(0, months.lastIndex)
                    }
                },
        ) {
            val axisSpace = 16.dp.toPx()
            val plotBottom = size.height - axisSpace
            val plotHeight = plotBottom - 14.dp.toPx()
            val slot = size.width / months.size
            val barWidth = (slot - 2.dp.toPx()).coerceAtMost(32.dp.toPx())
            val radius = 4.dp.toPx()
            drawLine(rule, Offset(0f, plotBottom), Offset(size.width, plotBottom), strokeWidth = 1.dp.toPx())
            val active = selected ?: months.lastIndex
            months.forEachIndexed { i, m ->
                val h = (m.rate * plotHeight).toFloat()
                val left = i * slot + (slot - barWidth) / 2
                val top = plotBottom - h
                if (h > 0f) {
                    drawPath(
                        Path().apply {
                            addRoundRect(
                                RoundRect(
                                    left = left, top = top, right = left + barWidth, bottom = plotBottom,
                                    topLeftCornerRadius = CornerRadius(radius), topRightCornerRadius = CornerRadius(radius),
                                )
                            )
                        },
                        ink.copy(alpha = if (i == active) 1f else 0.35f),
                    )
                }
                val name = measurer.measure(m.month.month.getDisplayName(TextStyle.NARROW, locale), labelStyle)
                drawText(name, topLeft = Offset(left + barWidth / 2 - name.size.width / 2, size.height - name.size.height))
                if (i == active) {
                    val pct = measurer.measure("${(m.rate * 100).roundToInt()}%", labelStyle)
                    drawText(
                        pct,
                        topLeft = Offset(
                            (left + barWidth / 2 - pct.size.width / 2).coerceIn(0f, size.width - pct.size.width),
                            top - pct.size.height - 2.dp.toPx(),
                        ),
                    )
                }
            }
        }
    }
}
