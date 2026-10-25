package com.stagirite.ledger.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stagirite.ledger.Goal
import com.stagirite.ledger.GoalStore
import com.stagirite.ledger.Milestones
import com.stagirite.ledger.describeSpan
import com.stagirite.ledger.plural
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private const val MILLIS_PER_DAY = 86_400_000L

/** Today's date, refreshed so the counts roll over at midnight while the app is open. */
@Composable
private fun rememberToday(): State<LocalDate> {
    val today = remember { mutableStateOf(LocalDate.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            today.value = LocalDate.now()
            delay(15_000)
        }
    }
    return today
}

@Composable
fun LedgerApp(store: GoalStore) {
    val today by rememberToday()
    var openId by rememberSaveable { mutableStateOf<String?>(null) }
    val open = store.goals.firstOrNull { it.id == openId }

    if (open == null) {
        GoalListScreen(
            goals = store.goals,
            today = today,
            onOpen = { openId = it.id },
            onAdd = store::add,
        )
    } else {
        BackHandler { openId = null }
        GoalDetailScreen(
            goal = open,
            today = today,
            onBack = { openId = null },
            onReset = { store.update(open.reset(today)) },
            onDelete = {
                store.delete(open.id)
                openId = null
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GoalListScreen(
    goals: List<Goal>,
    today: LocalDate,
    onOpen: (Goal) -> Unit,
    onAdd: (String, LocalDate) -> Unit,
) {
    var adding by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("The Stagirite's Ledger") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { adding = true },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("New goal") },
            )
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
                items(goals, key = { it.id }) { goal ->
                    GoalCard(goal, today, onClick = { onOpen(goal) })
                }
            }
        }
    }

    if (adding) {
        AddGoalDialog(
            today = today,
            onDismiss = { adding = false },
            onConfirm = { title, start ->
                onAdd(title, start)
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
        Text(
            "No entries yet",
            style = MaterialTheme.typography.headlineSmall,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Name a habit to keep, and the ledger will count each day you keep it.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(32.dp))
        Quote(
            "One swallow does not make a spring, nor does one fine day; " +
                "and so too one day, or a short time, does not make a man blessed and happy.",
        )
    }
}

@Composable
private fun Quote(text: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            "“$text”",
            style = MaterialTheme.typography.bodyLarge,
            fontStyle = FontStyle.Italic,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "— Aristotle, Nicomachean Ethics",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.outline,
        )
    }
}

@Composable
private fun GoalCard(goal: Goal, today: LocalDate, onClick: () -> Unit) {
    val days = goal.daysKept(today)
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    goal.title,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f),
                )
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "$days",
                        style = MaterialTheme.typography.displaySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        if (days == 1L) "day" else "days",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { Milestones.progress(days) },
                modifier = Modifier.fillMaxWidth(),
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Next milestone: ${plural(Milestones.next(days), "day")}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GoalDetailScreen(
    goal: Goal,
    today: LocalDate,
    onBack: () -> Unit,
    onReset: () -> Unit,
    onDelete: () -> Unit,
) {
    var confirmReset by rememberSaveable { mutableStateOf(false) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    val days = goal.daysKept(today)
    val next = Milestones.next(days)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(goal.title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { confirmDelete = true }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete goal")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(16.dp))
            Text(
                "$days",
                fontSize = 112.sp,
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                if (days == 1L) "day kept" else "days kept",
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                describeSpan(goal.startDate, today),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                "since ${goal.startDate.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG))}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(32.dp))
            LinearProgressIndicator(
                progress = { Milestones.progress(days) },
                modifier = Modifier.fillMaxWidth(),
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "${plural(next - days, "day")} to go until ${plural(next, "day")}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(32.dp))
            HorizontalDivider()
            Row(Modifier.padding(vertical = 16.dp)) {
                Stat("Longest streak", plural(goal.longestStreak(today), "day"), Modifier.weight(1f))
                Stat("Fresh starts", "${goal.resets}", Modifier.weight(1f))
            }
            HorizontalDivider()

            Spacer(Modifier.height(32.dp))
            OutlinedButton(onClick = { confirmReset = true }) {
                Text("I slipped — begin again")
            }

            Spacer(Modifier.height(40.dp))
            Quote("We become just by doing just acts, temperate by doing temperate acts, brave by doing brave acts.")
        }
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text("Begin again?") },
            text = {
                Text(
                    "Your count returns to 0 starting today. " +
                        "Your longest streak of ${plural(goal.longestStreak(today), "day")} is kept.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onReset()
                    confirmReset = false
                }) { Text("Begin again") }
            },
            dismissButton = {
                TextButton(onClick = { confirmReset = false }) { Text("Cancel") }
            },
        )
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete “${goal.title}”?") },
            text = { Text("This removes the goal and its history from the ledger.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    onDelete()
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun Stat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleLarge)
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddGoalDialog(
    today: LocalDate,
    onDismiss: () -> Unit,
    onConfirm: (String, LocalDate) -> Unit,
) {
    var title by rememberSaveable { mutableStateOf("") }
    var startEpochDay by rememberSaveable { mutableStateOf(today.toEpochDay()) }
    var picking by rememberSaveable { mutableStateOf(false) }
    val start = LocalDate.ofEpochDay(startEpochDay)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("A new goal") },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("What will you keep?") },
                    placeholder = { Text("e.g. No more alcohol") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Starting", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.width(8.dp))
                    TextButton(onClick = { picking = true }) {
                        Text(
                            if (start == today) "Today"
                            else start.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)),
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(title, start) },
                enabled = title.isNotBlank(),
            ) { Text("Add") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )

    if (picking) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = startEpochDay * MILLIS_PER_DAY,
            selectableDates = object : SelectableDates {
                // Only past days and today: you can't have started in the future.
                override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                    utcTimeMillis / MILLIS_PER_DAY <= today.toEpochDay()

                override fun isSelectableYear(year: Int): Boolean = year <= today.year
            },
        )
        DatePickerDialog(
            onDismissRequest = { picking = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { startEpochDay = it / MILLIS_PER_DAY }
                    picking = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { picking = false }) { Text("Cancel") }
            },
        ) {
            DatePicker(state = state)
        }
    }
}
