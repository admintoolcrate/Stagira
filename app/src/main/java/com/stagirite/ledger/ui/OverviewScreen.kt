package com.stagirite.ledger.ui

import com.stagirite.ledger.beta.BetaUpdates
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import com.stagirite.ledger.data.weekReview
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

/**
 * The Overview tab: how all goals are going together, apart from the ledger
 * itself. The summary, this week, what's coming up, and one calendar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OverviewDashboard(
    goals: List<Goal>,
    today: LocalDate,
    onOpen: (Goal) -> Unit,
    onWeekly: () -> Unit,
    onSettings: () -> Unit,
    bottomBar: @Composable () -> Unit,
    /** A gentle nudge to keep a copy of the ledger, when there's none. */
    showBackupPrompt: Boolean,
    onSetUpBackup: () -> Unit,
    onDismissBackup: () -> Unit,
    /** A newer beta, for test copies installed from GitHub. */
    update: BetaUpdates.Release? = null,
    onUpdate: () -> Unit = {},
) {
    val plus = LocalPlus.current
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Overview") },
                actions = {
                    IconButton(onClick = onSettings) { Icon(Icons.Filled.Settings, contentDescription = "Settings") }
                },
                colors = plainTopBarColors(),
            )
        },
        bottomBar = bottomBar,
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(start = Space.l, end = Space.l, top = Space.s, bottom = Space.xxl),
            verticalArrangement = Arrangement.spacedBy(Space.m),
            modifier = Modifier.padding(padding),
        ) {
            if (update != null) {
                item(key = "update") {
                    LedgerCard(onClick = onUpdate) {
                        Text("A new beta is ready", style = MaterialTheme.typography.titleSmall)
                        Spacer(Modifier.height(Space.xs))
                        Text(
                            "${update.title}. Download it and install it over this copy; your goals stay.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        TextButton(onClick = onUpdate) { Text("Download") }
                    }
                }
            }
            if (showBackupPrompt) {
                item(key = "backup") {
                    LedgerCard {
                        Text("Keep a copy of your ledger", style = MaterialTheme.typography.titleSmall)
                        Spacer(Modifier.height(Space.xs))
                        Text(
                            "Your goals live only on this phone. Choose a place for an automatic backup, such as " +
                                "your Drive or Downloads, so a lost phone doesn\u2019t mean a lost ledger.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Row {
                            TextButton(onClick = onSetUpBackup) { Text("Set up backup") }
                            TextButton(onClick = onDismissBackup) { Text("Not now") }
                        }
                    }
                }
            }
            if (goals.isEmpty()) {
                item(key = "empty") {
                    Text(
                        "Once you have a goal, how they\u2019re all going shows here.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(Space.xs),
                    )
                }
                return@LazyColumn
            }
            item(key = "summary") { SummaryCard(goals, today) }
            item(key = "week") {
                val w = weekReview(goals, today)
                LedgerCard(onClick = { if (plus.has) onWeekly() else plus.offer("The weekly review is part of Plus.") }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("This week", style = MaterialTheme.typography.titleSmall)
                            Text(
                                "${w.cleanDays} of ${w.activeDays} days kept \u00B7 ${plural(w.slips.toLong(), "slip")}" +
                                    if (w.urges > 0) " \u00B7 ${w.urges} resisted" else "",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Open weekly review")
                    }
                }
            }
            if (upcomingMilestones(goals, today).isNotEmpty()) {
                item(key = "coming-heading") { SectionHeading("Coming up", Modifier.padding(top = Space.l)) }
                item(key = "coming") { ComingUpCard(goals, today, onOpen) }
            }
            item(key = "calendar-heading") { SectionHeading("All goals", Modifier.padding(top = Space.l)) }
            item(key = "calendar") { CombinedCalendarCard(goals, today) }
        }
    }
}

/** Switches between the ledger and the overview. */
@Composable
fun HomeNav(overview: Boolean, onSelect: (overview: Boolean) -> Unit) {
    NavigationBar(containerColor = MaterialTheme.colorScheme.background, tonalElevation = 0.dp) {
        val colors = NavigationBarItemDefaults.colors(
            selectedIconColor = MaterialTheme.colorScheme.onPrimary,
            indicatorColor = MaterialTheme.colorScheme.primary,
        )
        NavigationBarItem(
            selected = !overview,
            onClick = { onSelect(false) },
            icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = null) },
            label = { Text("Goals") },
            colors = colors,
        )
        NavigationBarItem(
            selected = overview,
            onClick = { onSelect(true) },
            icon = { Icon(Icons.Filled.DateRange, contentDescription = null) },
            label = { Text("Overview") },
            colors = colors,
        )
    }
}

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
                "Kept this month",
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
