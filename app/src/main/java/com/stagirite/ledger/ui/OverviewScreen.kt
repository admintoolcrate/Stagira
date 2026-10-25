package com.stagirite.ledger.ui

import androidx.compose.foundation.clickable
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.stagirite.ledger.data.DayState
import com.stagirite.ledger.data.Goal
import com.stagirite.ledger.data.GoalKind
import com.stagirite.ledger.data.overviewDayState
import com.stagirite.ledger.data.plural
import com.stagirite.ledger.data.stats
import com.stagirite.ledger.data.upcomingMilestones
import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.roundToInt

// The overview, as cards on the home page: a summary above the goals,
// and what's coming up plus a combined calendar below them.

@Composable
fun SummaryCard(goals: List<Goal>, today: LocalDate) {
    val stats = goals.map { it.stats(today) }
    val keptAll = stats.sumOf { it.daysKept }
    val trackedAll = stats.sumOf { it.daysTracked }.coerceAtLeast(1)
    val monthStart = YearMonth.from(today).atDay(1)
    val monthStates = (0 until today.dayOfMonth)
        .map { overviewDayState(goals, monthStart.plusDays(it.toLong()), today) }
        .filter { it != DayState.Untracked && it != DayState.Rest }
    val monthClean = monthStates.count { it == DayState.Kept }
    val vices = goals.count { it.kind == GoalKind.Vice }
    val virtues = goals.count { it.kind == GoalKind.Virtue }

    LedgerCard {
        Row(Modifier.fillMaxWidth()) {
            Figure("$keptAll", "Days kept", Modifier.weight(1f))
            Figure("${(keptAll.toDouble() / trackedAll * 100).roundToInt()}%", "Of all days", Modifier.weight(1f))
            Figure(
                if (monthStates.isEmpty()) "–" else "$monthClean/${monthStates.size}",
                "Clean this month",
                Modifier.weight(1f),
            )
        }
        val counts = listOfNotNull(
            plural(goals.size.toLong(), "goal"),
            vices.takeIf { it > 0 }?.let { if (it == 1) "1 Vice" else "$it Vices" },
            virtues.takeIf { it > 0 }?.let { if (it == 1) "1 Virtue" else "$it Virtues" },
        )
        Text(
            counts.joinToString(" · "),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun ComingUpCard(goals: List<Goal>, today: LocalDate, onOpen: (Goal) -> Unit) {
    val ups = upcomingMilestones(goals, today)
    LedgerCard(padding = 0.dp) {
        ups.forEachIndexed { i, up ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { onOpen(up.goal) }
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(up.goal.title, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        "${plural(up.milestone, "day")} on ${today.plusDays(up.inDays).format(mediumDate)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    if (up.inDays == 1L) "tomorrow" else "in ${plural(up.inDays, "day")}",
                    style = MaterialTheme.typography.labelLarge,
                )
            }
            if (i < ups.lastIndex) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
    }
}

@Composable
fun CombinedCalendarCard(goals: List<Goal>, today: LocalDate) {
    LedgerCard {
        LedgerCalendar(
            today = today,
            earliest = goals.minOf { it.since },
            stateOf = { overviewDayState(goals, it, today) },
            urgesOf = { d -> goals.sumOf { it.urgesOn(d) } },
            showUrgeKey = goals.any { it.urges.isNotEmpty() },
            showRestKey = goals.any { it.hasSchedule },
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "A day counts as kept when no goal slipped that day.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
