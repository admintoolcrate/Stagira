package com.stagirite.ledger.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stagirite.ledger.data.Goal
import com.stagirite.ledger.data.Milestones
import com.stagirite.ledger.data.Quotes
import com.stagirite.ledger.data.dayState
import com.stagirite.ledger.data.describeSpan
import com.stagirite.ledger.data.plural
import com.stagirite.ledger.data.stats
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

private val todayHeading = DateTimeFormatter.ofPattern("EEEE d MMMM")

// ---------------------------------------------------------------- Goal list

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalListScreen(
    goals: List<Goal>,
    today: LocalDate,
    snackbar: SnackbarHostState,
    onOpen: (Goal) -> Unit,
    onAdd: (String, LocalDate, String) -> Unit,
    bottomBar: @Composable () -> Unit,
) {
    var adding by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("The Stagirite’s Ledger")
                        Text(
                            today.format(todayHeading),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                colors = plainTopBarColors(),
            )
        },
        bottomBar = bottomBar,
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { adding = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ) {
                Icon(Icons.Filled.Add, contentDescription = "New goal")
            }
        },
    ) { padding ->
        if (goals.isEmpty()) {
            EmptyState(Modifier.padding(padding))
        } else {
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(padding),
            ) {
                items(goals, key = { it.id }) { goal -> GoalCard(goal, today, onClick = { onOpen(goal) }) }
            }
        }
    }

    if (adding) {
        GoalDialog(
            heading = "New goal",
            confirm = "Add",
            today = today,
            onDismiss = { adding = false },
            onConfirm = { title, since, reason ->
                onAdd(title, since, reason)
                adding = false
            },
        )
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Nothing in the ledger yet.", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Text(
            "Tap + and name something you’ll keep doing, or stop doing. The ledger counts the days.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(40.dp))
        LedgerCard { QuoteBlock(Quotes.emptyLedger) }
    }
}

@Composable
private fun GoalCard(goal: Goal, today: LocalDate, onClick: () -> Unit) {
    val days = goal.daysKept(today)
    val next = Milestones.next(days)
    val milestoneToday = Milestones.isMilestone(days)
    LedgerCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(goal.title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(4.dp))
                Text(
                    if (milestoneToday) "Milestone today"
                    else "${plural(next - days, "day")} to ${plural(next, "day")}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (milestoneToday) MaterialTheme.colorScheme.onBackground
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(16.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text("$days", style = MaterialTheme.typography.displaySmall)
                Text(
                    if (days == 1L) "day" else "days",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        ThinProgress(Milestones.progress(days))
    }
}

// ---------------------------------------------------------------- Goal page

private enum class GoalTab(val label: String) { Calendar("Calendar"), Stats("Stats"), Milestones("Milestones"), History("History") }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalDetailScreen(
    goal: Goal,
    today: LocalDate,
    onBack: () -> Unit,
    onEdit: (String, LocalDate, String) -> Unit,
    onSlip: (LocalDate) -> Unit,
    onRemoveSlip: (LocalDate) -> Unit,
    onMoveStart: (LocalDate) -> Unit,
    onDelete: () -> Unit,
) {
    var editing by rememberSaveable { mutableStateOf(false) }
    var slipSheet by rememberSaveable { mutableStateOf(false) }
    var pickingSlipDay by rememberSaveable { mutableStateOf(false) }
    var movingStart by rememberSaveable { mutableStateOf(false) }
    var tappedDay by rememberSaveable { mutableStateOf<Long?>(null) }
    // Length of the run that just ended, while the "Day 0" screen is showing.
    var endedRun by rememberSaveable { mutableStateOf<Long?>(null) }
    var tab by rememberSaveable { mutableStateOf(GoalTab.Calendar) }
    var month by rememberSaveable { mutableStateOf(YearMonth.from(today).toString()) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val days = goal.daysKept(today)
    val next = Milestones.next(days)
    val beganAgainToday = goal.slips.lastOrNull() == today

    /** Logs a slip. Today's gets the Day 0 screen; a past one gets an undo bar. */
    val logSlip: (LocalDate) -> Unit = { day ->
        if (day == today) {
            endedRun = days
            onSlip(day)
        } else {
            onSlip(day)
            scope.launch {
                val r = snackbar.showSnackbar("Slip logged for ${day.format(mediumDate)}", actionLabel = "Undo")
                if (r == SnackbarResult.ActionPerformed) onRemoveSlip(day)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(goal.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { editing = true }) {
                        Icon(Icons.Filled.Edit, contentDescription = "Edit goal")
                    }
                },
                colors = plainTopBarColors(),
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // The count
            LedgerCard(padding = 24.dp) {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    ProgressRing(Milestones.progress(days), Modifier.size(208.dp)) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("$days", style = MaterialTheme.typography.displayLarge, fontSize = 80.sp)
                            Text(
                                if (days == 1L) "day" else "days",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Text(
                        if (Milestones.isMilestone(days)) "Milestone reached today"
                        else "${plural(next - days, "day")} to ${plural(next, "day")}",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    TextButton(onClick = { movingStart = true }) {
                        Text(
                            "${describeSpan(goal.currentStart, today)} · since ${goal.currentStart.format(mediumDate)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Icon(
                            Icons.Filled.Edit,
                            contentDescription = "Change start date",
                            modifier = Modifier.padding(start = 6.dp).size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { slipSheet = true },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    ) { Text("Log a slip") }
                }
            }

            if (beganAgainToday) {
                LedgerCard {
                    Text(
                        "You began again today.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(16.dp))
                    QuoteBlock(Quotes.forSlip(goal))
                }
            }

            if (goal.reason.isNotBlank()) {
                LedgerCard {
                    SectionHeading("Why", Modifier.padding(start = 0.dp))
                    Text(goal.reason, style = MaterialTheme.typography.bodyLarge)
                }
            }

            Spacer(Modifier.height(4.dp))
            val tabs = GoalTab.entries
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                tabs.forEachIndexed { i, t ->
                    SegmentedButton(
                        selected = tab == t,
                        onClick = { tab = t },
                        shape = SegmentedButtonDefaults.itemShape(i, tabs.size),
                        icon = {},
                        colors = SegmentedButtonDefaults.colors(
                            activeContainerColor = MaterialTheme.colorScheme.primary,
                            activeContentColor = MaterialTheme.colorScheme.onPrimary,
                            inactiveContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        ),
                    ) { Text(t.label, maxLines = 1, fontSize = 12.sp) }
                }
            }

            LedgerCard {
                when (tab) {
                    GoalTab.Calendar -> {
                        MonthCalendar(
                            month = YearMonth.parse(month),
                            today = today,
                            earliest = YearMonth.from(goal.since),
                            stateOf = { goal.dayState(it, today) },
                            onMonthChange = { month = it.toString() },
                            onDayClick = { tappedDay = it.toEpochDay() },
                        )
                        Text(
                            "Tap a day to log or remove a slip.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                    GoalTab.Stats -> StatsPanel(goal, today)
                    GoalTab.Milestones -> MilestonesPanel(goal, today)
                    GoalTab.History -> HistoryPanel(goal, today)
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }

    if (slipSheet) {
        SlipSheet(
            goal = goal,
            today = today,
            onDismiss = { slipSheet = false },
            onPick = {
                slipSheet = false
                logSlip(it)
            },
            onAnotherDay = {
                slipSheet = false
                pickingSlipDay = true
            },
        )
    }

    if (pickingSlipDay) {
        DayPickerDialog(
            initial = today.minusDays(1).coerceAtLeast(goal.since),
            earliest = goal.since,
            latest = today,
            onDismiss = { pickingSlipDay = false },
            onPick = {
                pickingSlipDay = false
                if (!goal.isSlip(it)) logSlip(it)
            },
        )
    }

    if (movingStart) {
        DayPickerDialog(
            initial = goal.currentStart,
            earliest = goal.currentStartEarliest,
            latest = today,
            onDismiss = { movingStart = false },
            onPick = {
                movingStart = false
                onMoveStart(it)
            },
        )
    }

    if (editing) {
        GoalDialog(
            heading = "Edit goal",
            confirm = "Save",
            today = today,
            initialTitle = goal.title,
            initialSince = goal.since,
            initialReason = goal.reason,
            latestSince = goal.slips.firstOrNull(),
            onDismiss = { editing = false },
            onConfirm = { title, since, reason ->
                onEdit(title, since, reason)
                editing = false
            },
            onDelete = {
                editing = false
                onDelete()
            },
        )
    }

    tappedDay?.let { epochDay ->
        val day = LocalDate.ofEpochDay(epochDay)
        val isSlip = goal.isSlip(day)
        AlertDialog(
            onDismissRequest = { tappedDay = null },
            title = { Text(day.format(longDate)) },
            text = {
                Text(
                    if (isSlip) "Remove the slip on this day? The runs either side join back together."
                    else "Log a slip on this day? The run it falls in is split in two.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    tappedDay = null
                    if (isSlip) onRemoveSlip(day) else logSlip(day)
                }) { Text(if (isSlip) "Remove slip" else "Log slip") }
            },
            dismissButton = { TextButton(onClick = { tappedDay = null }) { Text("Cancel") } },
        )
    }

    endedRun?.let { ended ->
        AlertDialog(
            onDismissRequest = { endedRun = null },
            title = { Text("Day 0") },
            text = {
                Column {
                    Text(
                        if (ended > 0) "Your run of ${plural(ended, "day")} is kept in the history. The count starts again today."
                        else "The count starts again today.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(Modifier.height(20.dp))
                    QuoteBlock(Quotes.forSlip(goal))
                }
            },
            confirmButton = { TextButton(onClick = { endedRun = null }) { Text("Carry on") } },
            dismissButton = {
                TextButton(onClick = {
                    endedRun = null
                    onRemoveSlip(today)
                }) { Text("Undo") }
            },
        )
    }
}

/** Quick choices for when the slip happened. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SlipSheet(
    goal: Goal,
    today: LocalDate,
    onDismiss: () -> Unit,
    onPick: (LocalDate) -> Unit,
    onAnotherDay: () -> Unit,
) {
    val yesterday = today.minusDays(1)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            Modifier
                .padding(horizontal = 24.dp)
                .navigationBarsPadding()
                .padding(bottom = 16.dp),
        ) {
            Text("Log a slip", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(4.dp))
            Text(
                "It happens. The days you’ve kept stay in your history.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))
            SlipOption("Today", today.format(mediumDate), enabled = !goal.isSlip(today)) { onPick(today) }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            SlipOption(
                "Yesterday",
                yesterday.format(mediumDate),
                enabled = !yesterday.isBefore(goal.since) && !goal.isSlip(yesterday),
            ) { onPick(yesterday) }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            SlipOption("Another day…", "Pick from a calendar", enabled = true, onClick = onAnotherDay)
        }
    }
}

@Composable
private fun SlipOption(title: String, detail: String, enabled: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val color = if (enabled) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.outline
        Text(title, style = MaterialTheme.typography.bodyLarge, color = color, modifier = Modifier.weight(1f))
        Text(
            if (enabled) detail else "Already logged",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun StatsPanel(goal: Goal, today: LocalDate) {
    val stats = goal.stats(today)
    Row(Modifier.fillMaxWidth()) {
        Figure("${stats.current}", "Current", Modifier.weight(1f))
        Figure("${stats.longest}", "Longest", Modifier.weight(1f))
        Figure(formatAverage(stats.average), "Average", Modifier.weight(1f))
    }
    Row(Modifier.fillMaxWidth()) {
        Figure("${(stats.keptRate * 100).roundToInt()}%", "Days kept", Modifier.weight(1f))
        Figure("${stats.freshStarts}", "Fresh starts", Modifier.weight(1f))
        Figure("${stats.slipsLast30}", "Slips, 30 days", Modifier.weight(1f))
    }
    Text(
        "${plural(stats.daysKept, "day")} kept of ${plural(stats.daysTracked, "day")} since ${goal.since.format(mediumDate)}.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 4.dp),
    )
    if (stats.runs.size > 1) {
        Spacer(Modifier.height(24.dp))
        Text("Length of each run, in days", style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(4.dp))
        RunsChart(stats.runs)
    }
}

/** Milestones of the current run: reached ones with their dates, then the next few. */
@Composable
private fun MilestonesPanel(goal: Goal, today: LocalDate) {
    val days = goal.daysKept(today)
    val best = goal.longestStreak(today)
    val upcoming = generateSequence(Milestones.next(days)) { Milestones.next(it) }.take(3).toList()
    val rows = Milestones.upTo(days) + upcoming
    rows.forEachIndexed { i, m ->
        val reached = m <= days
        val isNext = m == upcoming.first()
        val date = goal.currentStart.plusDays(m)
        Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(22.dp)
                    .then(
                        if (reached) Modifier.background(MaterialTheme.colorScheme.onBackground, CircleShape)
                        else Modifier.border(
                            BorderStroke(if (isNext) 2.dp else 1.dp, if (isNext) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.outline),
                            CircleShape,
                        )
                    ),
                contentAlignment = Alignment.Center,
            ) {
                if (reached) Icon(Icons.Filled.Check, contentDescription = null, tint = MaterialTheme.colorScheme.background, modifier = Modifier.size(14.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    plural(m, "day") + (Milestones.name(m)?.let { " · $it" } ?: ""),
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (reached || isNext) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    when {
                        reached -> "Reached ${date.format(mediumDate)}"
                        isNext -> "${date.format(mediumDate)} · in ${plural(m - days, "day")}"
                        else -> date.format(mediumDate)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (m == best && best > days) {
                Text("Your best", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (i < rows.lastIndex) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
    if (best > days) {
        Spacer(Modifier.height(8.dp))
        Text(
            "Your longest run so far: ${plural(best, "day")}.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun HistoryPanel(goal: Goal, today: LocalDate) {
    val runs = goal.runs(today).asReversed()
    runs.forEachIndexed { i, run ->
        val current = i == 0
        Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(plural(run.days, "day"), style = MaterialTheme.typography.bodyLarge)
                Text(
                    "${run.start.format(mediumDate)} – " + if (current) "now" else run.end.format(mediumDate),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (current) Text("Current", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (i < runs.lastIndex) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
    if (goal.history.isEmpty()) {
        Text(
            "No slips logged. Every day since ${goal.since.format(mediumDate)} has been kept.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun formatAverage(avg: Double): String =
    if (avg < 10) "%.1f".format(avg) else avg.roundToInt().toString()

// ---------------------------------------------------------------- Dialogs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GoalDialog(
    heading: String,
    confirm: String,
    today: LocalDate,
    onDismiss: () -> Unit,
    onConfirm: (String, LocalDate, String) -> Unit,
    initialTitle: String = "",
    initialSince: LocalDate = today,
    initialReason: String = "",
    latestSince: LocalDate? = null,
    onDelete: (() -> Unit)? = null,
) {
    var title by rememberSaveable { mutableStateOf(initialTitle) }
    var reason by rememberSaveable { mutableStateOf(initialReason) }
    var sinceEpochDay by rememberSaveable { mutableStateOf(initialSince.toEpochDay()) }
    var picking by rememberSaveable { mutableStateOf(false) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    val since = LocalDate.ofEpochDay(sinceEpochDay)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(heading) },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Goal") },
                    placeholder = { Text("e.g. No more alcohol") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Why it matters (optional)") },
                    minLines = 2,
                    maxLines = 4,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Started", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    TextButton(onClick = { picking = true }) {
                        Text(if (since == today) "Today" else since.format(mediumDate))
                    }
                }
                Text(
                    "Started earlier? Pick the day you began and the count goes back to it.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (onDelete != null) {
                    Spacer(Modifier.height(16.dp))
                    TextButton(
                        onClick = { confirmDelete = true },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    ) { Text("Delete goal") }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(title, since, reason) },
                enabled = title.isNotBlank(),
            ) { Text(confirm) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )

    if (picking) {
        DayPickerDialog(
            initial = since,
            earliest = null,
            latest = latestSince ?: today,
            onDismiss = { picking = false },
            onPick = {
                sinceEpochDay = it.toEpochDay()
                picking = false
            },
        )
    }

    if (confirmDelete && onDelete != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete this goal?") },
            text = { Text("“$initialTitle” and its history will be removed from all your devices. You can undo this straight after.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    onDelete()
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
        )
    }
}

/** A date picker limited to [earliest]..[latest]. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayPickerDialog(
    initial: LocalDate,
    earliest: LocalDate?,
    latest: LocalDate,
    onDismiss: () -> Unit,
    onPick: (LocalDate) -> Unit,
) {
    val state = rememberDatePickerState(
        initialSelectedDateMillis = initial.toEpochDay() * MILLIS_PER_DAY,
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                val day = utcTimeMillis / MILLIS_PER_DAY
                return day <= latest.toEpochDay() && (earliest == null || day >= earliest.toEpochDay())
            }

            override fun isSelectableYear(year: Int): Boolean =
                year <= latest.year && (earliest == null || year >= earliest.year)
        },
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = { state.selectedDateMillis?.let { onPick(LocalDate.ofEpochDay(it / MILLIS_PER_DAY)) } },
                enabled = state.selectedDateMillis != null,
            ) { Text("OK") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    ) {
        DatePicker(state = state)
    }
}
