package com.stagirite.ledger.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.stagirite.ledger.data.Goal
import com.stagirite.ledger.data.Quotes
import com.stagirite.ledger.data.WeekReview
import com.stagirite.ledger.data.plural
import com.stagirite.ledger.data.weekReview
import java.time.LocalDate

/** The last seven days against the seven before; step back through earlier weeks. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeeklyReviewScreen(goals: List<Goal>, today: LocalDate, onOpen: (Goal) -> Unit, onBack: () -> Unit) {
    var weeksBack by rememberSaveable { mutableStateOf(0) }
    val end = today.minusWeeks(weeksBack.toLong())
    val week = weekReview(goals, end)
    val earliest = goals.minOfOrNull { it.since } ?: today

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Weekly review") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                },
                colors = plainTopBarColors(),
            )
        },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${week.start.format(mediumDate)} – ${week.end.format(mediumDate)}",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f).padding(start = 4.dp),
                )
                IconButton(onClick = { weeksBack++ }, enabled = week.start.isAfter(earliest)) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous week")
                }
                IconButton(onClick = { weeksBack-- }, enabled = weeksBack > 0) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next week")
                }
            }

            if (week.goals.isEmpty()) {
                LedgerCard { Text("No goals were being tracked this week.", style = MaterialTheme.typography.bodyMedium) }
            } else {
                WeekBody(week, onOpen)
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun WeekBody(week: WeekReview, onOpen: (Goal) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            LedgerCard {
                Text(headline(week), style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth()) {
                    Figure("${week.cleanDays}/${week.activeDays}", "Days kept", Modifier.weight(1f))
                    Figure("${week.slips}", "Slips · was ${week.slipsBefore}", Modifier.weight(1f))
                    Figure("${week.urges}", "Urges resisted · was ${week.urgesBefore}", Modifier.weight(1f))
                }
                if (week.milestones > 0) {
                    Text(
                        "${plural(week.milestones.toLong(), "milestone")} reached this week.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            SectionHeading("By goal", Modifier.padding(top = 8.dp))
            LedgerCard(padding = 0.dp) {
                week.goals.forEachIndexed { i, g ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { onOpen(g.goal) }
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        GoalIcon(g.goal.icon, tile = 32.dp)
                        if (g.goal.icon.isNotEmpty()) Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(g.goal.title, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            val parts = buildList {
                                add("${g.kept} of ${plural(g.due, "day")} kept")
                                if (g.slips > 0) add(plural(g.slips.toLong(), g.goal.slipWord))
                                if (g.urges > 0) add("${g.urges} resisted")
                                g.milestones.maxOrNull()?.let { add("reached ${plural(it, "day")}") }
                            }
                            Text(
                                parts.joinToString(" · "),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Text("${g.runAtEnd}", style = MaterialTheme.typography.titleLarge)
                    }
                    if (i < week.goals.lastIndex) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }

            if (LocalUiPrefs.current.quotes) {
                LedgerCard { QuoteBlock(if (week.slips == 0) Quotes.milestone[1] else Quotes.slip[5]) }
            }
    }
}

/** One plain sentence for the week. */
private fun headline(w: WeekReview): String = when {
    w.slips == 0 && w.activeDays > 0 -> "Every day kept this week."
    w.slips < w.slipsBefore -> "Fewer slips than last week."
    w.slips > w.slipsBefore -> "A harder week than the last."
    w.urges > w.urgesBefore -> "Steady, with more urges resisted."
    else -> "A week much like the last."
}
