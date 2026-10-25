package com.stagirite.ledger.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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
import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.roundToInt

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
        topBar = { TopAppBar(title = { Text("The Stagirite’s Ledger") }, colors = plainTopBarColors()) },
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
            LazyColumn(contentPadding = PaddingValues(bottom = 96.dp), modifier = Modifier.padding(padding)) {
                items(goals, key = { it.id }) { goal ->
                    GoalRow(goal, today, onClick = { onOpen(goal) })
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
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
            .padding(40.dp),
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
        Spacer(Modifier.height(48.dp))
        QuoteBlock(Quotes.emptyLedger)
    }
}

@Composable
private fun GoalRow(goal: Goal, today: LocalDate, onClick: () -> Unit) {
    val days = goal.daysKept(today)
    Column(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 18.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(goal.title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(2.dp))
                Text(
                    if (Milestones.isMilestone(days)) "Milestone today: ${plural(days, "day")}"
                    else "Next: ${plural(Milestones.next(days), "day")}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (Milestones.isMilestone(days)) MaterialTheme.colorScheme.onBackground
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("$days", style = MaterialTheme.typography.displaySmall)
                Text(
                    if (days == 1L) "day" else "days",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        ThinProgress(Milestones.progress(days))
    }
}

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
    var movingStart by rememberSaveable { mutableStateOf(false) }
    var editing by rememberSaveable { mutableStateOf(false) }
    var confirmReset by rememberSaveable { mutableStateOf(false) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    var pickingPastSlip by rememberSaveable { mutableStateOf(false) }
    // A day tapped in the calendar, waiting for "record slip" / "remove slip".
    var tappedDay by rememberSaveable { mutableStateOf<Long?>(null) }
    // Length of the run that just ended, while the "Day 0" screen is showing.
    var endedRun by rememberSaveable { mutableStateOf<Long?>(null) }
    var month by rememberSaveable { mutableStateOf(YearMonth.from(today).toString()) }

    val days = goal.daysKept(today)
    val next = Milestones.next(days)
    val stats = goal.stats(today)
    val beganAgainToday = goal.slips.lastOrNull() == today

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { editing = true }) {
                        Icon(Icons.Filled.Edit, contentDescription = "Edit goal")
                    }
                    IconButton(onClick = { confirmDelete = true }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete goal")
                    }
                },
                colors = plainTopBarColors(),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
        ) {
            // The count
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(goal.title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
                Spacer(Modifier.height(24.dp))
                Text("$days", style = MaterialTheme.typography.displayLarge, fontSize = 120.sp)
                Text(
                    if (days == 1L) "day" else "days",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(16.dp))
                Text(describeSpan(goal.currentStart, today), style = MaterialTheme.typography.bodyMedium)
                TextButton(onClick = { movingStart = true }) {
                    Text(
                        "since ${goal.currentStart.format(longDate)}",
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
                Spacer(Modifier.height(32.dp))
                ThinProgress(Milestones.progress(days))
                Spacer(Modifier.height(8.dp))
                Text(
                    if (Milestones.isMilestone(days)) "Milestone reached today: ${plural(days, "day")}."
                    else "${plural(next - days, "day")} until ${plural(next, "day")}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (goal.reason.isNotBlank()) {
                Spacer(Modifier.height(32.dp))
                SectionHeading("Why")
                Text(goal.reason, style = MaterialTheme.typography.bodyLarge)
            }

            Spacer(Modifier.height(32.dp))
            if (beganAgainToday) {
                Text(
                    "You began again today.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(16.dp))
                QuoteBlock(Quotes.forSlip(goal))
                Spacer(Modifier.height(16.dp))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!beganAgainToday) {
                    OutlinedButton(onClick = { confirmReset = true }) { Text("Begin again") }
                }
                TextButton(onClick = { pickingPastSlip = true }) { Text("Record a past slip") }
            }

            Spacer(Modifier.height(40.dp))
            SectionHeading("Calendar")
            MonthCalendar(
                month = YearMonth.parse(month),
                today = today,
                earliest = YearMonth.from(goal.since),
                stateOf = { goal.dayState(it, today) },
                onMonthChange = { month = it.toString() },
                onDayClick = { tappedDay = it.toEpochDay() },
            )
            Text(
                "Tap a day to record or remove a slip.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )

            Spacer(Modifier.height(40.dp))
            SectionHeading("Stats")
            Row(Modifier.fillMaxWidth()) {
                Figure("${stats.current}", "Current run", Modifier.weight(1f))
                Figure("${stats.longest}", "Longest run", Modifier.weight(1f))
                Figure(formatAverage(stats.average), "Average run", Modifier.weight(1f))
            }
            Row(Modifier.fillMaxWidth()) {
                Figure("${(stats.keptRate * 100).roundToInt()}%", "Days kept", Modifier.weight(1f))
                Figure("${stats.freshStarts}", "Fresh starts", Modifier.weight(1f))
                Figure("${stats.slipsLast30}", "Slips, 30 days", Modifier.weight(1f))
            }
            Text(
                "${plural(stats.daysKept, "day")} kept out of ${plural(stats.daysTracked, "day")} since " +
                    goal.since.format(mediumDate) + ".",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )

            if (stats.runs.size > 1) {
                Spacer(Modifier.height(32.dp))
                Text("Length of each run, in days", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(4.dp))
                RunsChart(stats.runs)
            }

            if (goal.history.isNotEmpty()) {
                Spacer(Modifier.height(40.dp))
                SectionHeading("History")
                goal.history.asReversed().forEach { run ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
                        Text(plural(run.days, "day"), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                        Text(
                            "${run.start.format(mediumDate)} – ${run.end.format(mediumDate)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
            Spacer(Modifier.height(48.dp))
        }
    }

    val recordSlip: (LocalDate) -> Unit = { day ->
        if (day == today) endedRun = days
        onSlip(day)
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
        )
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text("Begin again?") },
            text = { Text("The count returns to 0 from today. This run of ${plural(days, "day")} is kept in the history.") },
            confirmButton = {
                TextButton(onClick = {
                    recordSlip(today)
                    confirmReset = false
                }) { Text("Begin again") }
            },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("Cancel") } },
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

    if (pickingPastSlip) {
        DayPickerDialog(
            initial = today,
            earliest = goal.since,
            latest = today,
            onDismiss = { pickingPastSlip = false },
            onPick = {
                pickingPastSlip = false
                recordSlip(it)
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
                    else "Record a slip on this day? The run it falls in is split in two.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (isSlip) onRemoveSlip(day) else recordSlip(day)
                    tappedDay = null
                }) { Text(if (isSlip) "Remove slip" else "Record slip") }
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
        )
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete this goal?") },
            text = { Text("“${goal.title}” and its history will be removed from all your devices. You can undo this straight after.") },
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

private fun formatAverage(avg: Double): String =
    if (avg < 10) "%.1f".format(avg) else avg.roundToInt().toString()

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
) {
    var title by rememberSaveable { mutableStateOf(initialTitle) }
    var reason by rememberSaveable { mutableStateOf(initialReason) }
    var sinceEpochDay by rememberSaveable { mutableStateOf(initialSince.toEpochDay()) }
    var picking by rememberSaveable { mutableStateOf(false) }
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
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(title, since, reason) }, enabled = title.isNotBlank()) { Text(confirm) }
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
