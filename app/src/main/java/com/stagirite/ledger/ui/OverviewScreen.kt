package com.stagirite.ledger.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.stagirite.ledger.data.DayState
import com.stagirite.ledger.data.Goal
import com.stagirite.ledger.data.overviewDayState
import com.stagirite.ledger.data.plural
import com.stagirite.ledger.data.stats
import com.stagirite.ledger.data.upcomingMilestones
import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OverviewScreen(
    goals: List<Goal>,
    today: LocalDate,
    onOpen: (Goal) -> Unit,
    bottomBar: @Composable () -> Unit,
) {
    var month by rememberSaveable { mutableStateOf(YearMonth.from(today).toString()) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Overview") }, colors = plainTopBarColors()) },
        bottomBar = bottomBar,
    ) { padding ->
        if (goals.isEmpty()) {
            Column(
                Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .padding(40.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.height(80.dp))
                Text(
                    "Add a goal and your overview appears here.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            OverviewContent(goals, today, month, { month = it }, onOpen, Modifier.padding(padding))
        }
    }
}

@Composable
private fun OverviewContent(
    goals: List<Goal>,
    today: LocalDate,
    month: String,
    onMonth: (String) -> Unit,
    onOpen: (Goal) -> Unit,
    modifier: Modifier,
) {
    val stats = goals.map { it.stats(today) }
    val best = goals.zip(stats).maxBy { it.second.current }
    val monthStart = YearMonth.from(today).atDay(1)
    val monthStates = (0 until today.dayOfMonth)
        .map { overviewDayState(goals, monthStart.plusDays(it.toLong()), today) }
        .filter { it != DayState.Untracked }
    val monthClean = monthStates.count { it == DayState.Kept }
    val keptAll = stats.sumOf { it.daysKept }
    val trackedAll = stats.sumOf { it.daysTracked }.coerceAtLeast(1)

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
    ) {
        LedgerCard {
            Row(Modifier.fillMaxWidth()) {
                Figure("${goals.size}", if (goals.size == 1) "Goal" else "Goals", Modifier.weight(1f))
                Figure("$keptAll", "Days kept", Modifier.weight(1f))
            }
            Row(Modifier.fillMaxWidth()) {
                Figure("${(keptAll.toDouble() / trackedAll * 100).roundToInt()}%", "Of all days kept", Modifier.weight(1f))
                Figure(
                    if (monthStates.isEmpty()) "\u2013" else "$monthClean/${monthStates.size}",
                    "Clean days this month",
                    Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                "Longest current run: ${best.first.title}, ${plural(best.second.current, "day")}.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(Modifier.height(28.dp))
        SectionHeading("Coming up")
        LedgerCard(padding = 0.dp) {
            val ups = upcomingMilestones(goals, today)
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

        Spacer(Modifier.height(28.dp))
        SectionHeading("All goals")
        LedgerCard {
            MonthCalendar(
                month = YearMonth.parse(month),
                today = today,
                earliest = YearMonth.from(goals.minOf { it.since }),
                stateOf = { overviewDayState(goals, it, today) },
                onMonthChange = { onMonth(it.toString()) },
            )
            Text(
                "A day counts as kept when no goal slipped that day.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
        }

        Spacer(Modifier.height(28.dp))
        SectionHeading("By goal")
        LedgerCard(padding = 0.dp) {
            goals.zip(stats).forEachIndexed { i, (goal, s) ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { onOpen(goal) }
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(goal.title, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            "Longest ${plural(s.longest, "day")} \u00B7 ${(s.keptRate * 100).roundToInt()}% kept",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text("${s.current}", style = MaterialTheme.typography.titleLarge)
                }
                if (i < goals.lastIndex) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
        Spacer(Modifier.height(40.dp))
    }
}
