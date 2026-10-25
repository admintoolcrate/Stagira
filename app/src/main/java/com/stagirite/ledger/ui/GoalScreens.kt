package com.stagirite.ledger.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Switch
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stagirite.ledger.data.Goal
import com.stagirite.ledger.data.GoalIcons
import com.stagirite.ledger.data.GoalKind
import com.stagirite.ledger.data.Milestones
import com.stagirite.ledger.data.SortOrder
import com.stagirite.ledger.data.goalTemplates
import com.stagirite.ledger.data.Quotes
import com.stagirite.ledger.data.dayState
import com.stagirite.ledger.data.describeSpan
import com.stagirite.ledger.data.plural
import com.stagirite.ledger.data.progress
import com.stagirite.ledger.data.stats
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

private val todayHeading = DateTimeFormatter.ofPattern("EEEE d MMMM")

/** What the add/edit dialog hands back. [realTitle] is null when a private goal was edited while locked. */
data class GoalDraft(
    val title: String,
    val since: LocalDate,
    val reason: String,
    val icon: String,
    val kind: GoalKind?,
    val private: Boolean,
    val realTitle: String?,
    val target: Long? = null,
)

/** What the goal screens need to know about private titles. */
class PrivacyUi(
    val hasPin: Boolean,
    val unlocked: Boolean,
    /** Ask for the PIN (or to set one up). */
    val requestPin: () -> Unit,
    /** The real title and note of a private goal, when unlocked. */
    val open: (Goal) -> Pair<String, String>?,
)

private fun String.capitalised() = replaceFirstChar { it.uppercase() }

// ---------------------------------------------------------------- Goal list

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalListScreen(
    goals: List<Goal>,
    today: LocalDate,
    snackbar: SnackbarHostState,
    privacy: PrivacyUi,
    archived: List<Goal>,
    sortOrder: SortOrder,
    onSort: (SortOrder) -> Unit,
    onOpen: (Goal) -> Unit,
    onAdd: (GoalDraft) -> Unit,
    onQuickSlip: (Goal) -> Unit,
    onArchive: (Goal, Boolean) -> Unit,
    onTitleLongPress: () -> Unit,
    bottomBar: @Composable () -> Unit,
) {
    var adding by rememberSaveable { mutableStateOf(false) }
    var menu by remember { mutableStateOf(false) }
    var showArchived by rememberSaveable { mutableStateOf(false) }
    var actionsFor by rememberSaveable { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    // Long-pressing the name shows or hides private titles.
                    Column(Modifier.pointerInput(Unit) { detectTapGestures(onLongPress = { onTitleLongPress() }) }) {
                        Text("The Stagirite\u2019s Ledger")
                        Text(
                            today.format(todayHeading),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { menu = true }) {
                            Icon(Icons.Filled.MoreVert, contentDescription = "Sort")
                        }
                        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                            Text(
                                "SORT BY",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            )
                            SortOrder.entries.forEach { order ->
                                DropdownMenuItem(
                                    text = { Text(order.label) },
                                    onClick = {
                                        onSort(order)
                                        menu = false
                                    },
                                    trailingIcon = {
                                        if (order == sortOrder) Icon(Icons.Filled.Check, contentDescription = "Selected")
                                    },
                                )
                            }
                        }
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
        if (goals.isEmpty() && archived.isEmpty()) {
            EmptyState(Modifier.padding(padding))
        } else {
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(padding),
            ) {
                items(goals, key = { it.id }) { goal ->
                    GoalCard(goal, today, onClick = { onOpen(goal) }, onLongClick = { actionsFor = goal.id })
                }
                if (goals.isEmpty()) {
                    item {
                        Text(
                            "No active goals. Tap + to add one, or restore one below.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(8.dp),
                        )
                    }
                }
                if (archived.isNotEmpty()) {
                    item(key = "archived") {
                        TextButton(onClick = { showArchived = !showArchived }) {
                            Text(if (showArchived) "Hide archived" else "Archived (${archived.size})")
                        }
                    }
                    if (showArchived) {
                        items(archived, key = { it.id }) { goal ->
                            LedgerCard(onClick = { onOpen(goal) }) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    GoalIcon(goal.icon, tile = 36.dp)
                                    if (goal.icon.isNotEmpty()) Spacer(Modifier.width(12.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(goal.title, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(
                                            "Archived \u00B7 ${plural(goal.daysKept(today), "day")}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.outline,
                                        )
                                    }
                                    TextButton(onClick = { onArchive(goal, false) }) { Text("Restore") }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    actionsFor?.let { id ->
        val goal = goals.firstOrNull { it.id == id }
        if (goal == null) {
            actionsFor = null
        } else {
            QuickActionsSheet(
                goal = goal,
                today = today,
                onDismiss = { actionsFor = null },
                onSlip = {
                    actionsFor = null
                    onQuickSlip(goal)
                },
                onOpen = {
                    actionsFor = null
                    onOpen(goal)
                },
                onArchive = {
                    actionsFor = null
                    onArchive(goal, true)
                },
            )
        }
    }

    if (adding) {
        GoalDialog(
            heading = "New goal",
            confirm = "Add",
            today = today,
            privacy = privacy,
            onDismiss = { adding = false },
            onConfirm = {
                onAdd(it)
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
private fun GoalCard(goal: Goal, today: LocalDate, onClick: () -> Unit, onLongClick: () -> Unit) {
    val days = goal.daysKept(today)
    val next = Milestones.next(days)
    val milestoneToday = Milestones.isMilestone(days)
    LedgerCard(onClick = onClick, onLongClick = onLongClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (goal.icon.isNotEmpty()) {
                GoalIcon(goal.icon)
                Spacer(Modifier.width(14.dp))
            }
            Column(Modifier.weight(1f)) {
                KindLabel(goal.kind, Modifier.padding(bottom = 2.dp))
                Text(goal.title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(4.dp))
                Text(
                    when {
                        milestoneToday -> "Milestone today"
                        goal.target != null && days >= goal.target -> "Target of ${plural(goal.target, "day")} reached"
                        goal.target != null -> "$days of ${plural(goal.target, "day")}"
                        else -> "${plural(next - days, "day")} to ${plural(next, "day")}"
                    },
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
        ThinProgress(goal.targetProgress(today) ?: Milestones.progress(days))
    }
}

/** Long-press actions for a goal on the list. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuickActionsSheet(
    goal: Goal,
    today: LocalDate,
    onDismiss: () -> Unit,
    onSlip: () -> Unit,
    onOpen: () -> Unit,
    onArchive: () -> Unit,
) {
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                GoalIcon(goal.icon, tile = 36.dp)
                if (goal.icon.isNotEmpty()) Spacer(Modifier.width(12.dp))
                Text(goal.title, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.height(12.dp))
            SlipOption("Log a ${goal.slipWord} today", today.format(mediumDate), enabled = !goal.isSlip(today), onClick = onSlip)
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            SlipOption("Open", "Calendar, stats and history", enabled = true, onClick = onOpen)
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            SlipOption("Archive", "Hide it from the list; restore any time", enabled = true, onClick = onArchive)
        }
    }
}

// ---------------------------------------------------------------- Goal page

private enum class GoalTab(val label: String) { Calendar("Calendar"), Stats("Stats"), Milestones("Milestones"), History("History") }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalDetailScreen(
    goal: Goal,
    /** The title as stored: the cover name for a private goal. */
    cover: String,
    today: LocalDate,
    privacy: PrivacyUi,
    onBack: () -> Unit,
    onEdit: (GoalDraft) -> Unit,
    onSlip: (LocalDate) -> Unit,
    onRemoveSlip: (LocalDate) -> Unit,
    onMoveStart: (LocalDate) -> Unit,
    onArchive: (Boolean) -> Unit,
    onDelete: () -> Unit,
) {
    val haptics = LocalHapticFeedback.current
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
    val slipWord = goal.slipWord

    /** Logs a slip. Today's gets the Day 0 screen; a past one gets an undo bar. */
    val logSlip: (LocalDate) -> Unit = { day ->
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        if (day == today) {
            endedRun = days
            onSlip(day)
        } else {
            onSlip(day)
            scope.launch {
                val r = snackbar.showSnackbar("${slipWord.capitalised()} logged for ${day.format(mediumDate)}", actionLabel = "Undo")
                if (r == SnackbarResult.ActionPerformed) onRemoveSlip(day)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(goal.title, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                        // Shown only while private titles are unlocked, so a cover name gives nothing away.
                        if (goal.isPrivate && privacy.unlocked) {
                            Icon(
                                Icons.Filled.Lock,
                                contentDescription = "Private title",
                                modifier = Modifier.padding(start = 8.dp).size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
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
            if (goal.archived) {
                LedgerCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Archived. It\u2019s kept, but hidden from your list, overview and widget.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(onClick = { onArchive(false) }) { Text("Restore") }
                    }
                }
            }
            // The count
            LedgerCard(padding = 24.dp) {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    if (goal.icon.isNotEmpty() || goal.kind != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            GoalIcon(goal.icon, tile = 36.dp)
                            if (goal.icon.isNotEmpty() && goal.kind != null) Spacer(Modifier.width(10.dp))
                            KindLabel(goal.kind)
                        }
                        Spacer(Modifier.height(16.dp))
                    }
                    ProgressRing(goal.targetProgress(today) ?: Milestones.progress(days), Modifier.size(208.dp)) {
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
                        when {
                            Milestones.isMilestone(days) -> "Milestone reached today"
                            goal.target != null && days >= goal.target -> "Target of ${plural(goal.target, "day")} reached"
                            goal.target != null -> "${plural(goal.target - days, "day")} to your target of ${goal.target}"
                            else -> "${plural(next - days, "day")} to ${plural(next, "day")}"
                        },
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
                    ) { Text("Log a $slipWord") }
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
                            slipLabel = slipWord.capitalised(),
                        )
                        Text(
                            "Tap a day to log or remove a $slipWord.",
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
            privacy = privacy,
            editing = goal,
            cover = cover,
            latestSince = goal.slips.firstOrNull(),
            onDismiss = { editing = false },
            onConfirm = {
                onEdit(it)
                editing = false
            },
            onDelete = {
                editing = false
                onDelete()
            },
            onArchive = {
                editing = false
                onArchive(!goal.archived)
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
                    if (isSlip) "Remove the $slipWord on this day? The runs either side join back together."
                    else "Log a $slipWord on this day? The run it falls in is split in two.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    tappedDay = null
                    if (isSlip) onRemoveSlip(day) else logSlip(day)
                }) { Text(if (isSlip) "Remove $slipWord" else "Log $slipWord") }
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
            Text("Log a ${goal.slipWord}", style = MaterialTheme.typography.titleLarge)
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
    val progress = goal.progress(today)
    ProgressPanel(goal, progress)
    Spacer(Modifier.height(28.dp))
    SectionHeading("Figures", Modifier.padding(start = 0.dp))
    Row(Modifier.fillMaxWidth()) {
        Figure("${stats.current}", "Current", Modifier.weight(1f))
        Figure("${stats.longest}", "Longest", Modifier.weight(1f))
        Figure(formatAverage(stats.average), "Average", Modifier.weight(1f))
    }
    Row(Modifier.fillMaxWidth()) {
        Figure("${(stats.keptRate * 100).roundToInt()}%", "Days kept", Modifier.weight(1f))
        Figure("${stats.freshStarts}", "Fresh starts", Modifier.weight(1f))
        Figure("${stats.slipsLast30}", "${(goal.kind?.slips ?: "slips").capitalised()}, 30 days", Modifier.weight(1f))
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
        RunsChart(stats.runs, trend = progress.trend, median = progress.medianRun)
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
            "No ${goal.kind?.slips ?: "slips"} logged. Every day since ${goal.since.format(mediumDate)} has been kept.",
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
    privacy: PrivacyUi,
    onDismiss: () -> Unit,
    onConfirm: (GoalDraft) -> Unit,
    /** The goal being edited, as shown (real name if unlocked), or null for a new one. */
    editing: Goal? = null,
    /** The stored title: the cover name of a private goal. */
    cover: String? = null,
    latestSince: LocalDate? = null,
    onDelete: (() -> Unit)? = null,
    onArchive: (() -> Unit)? = null,
) {
    val opened = editing?.let(privacy.open)
    // A private goal edited while locked: its real name stays sealed and untouched.
    val sealedAway = editing?.isPrivate == true && opened == null
    var title by rememberSaveable { mutableStateOf(cover ?: editing?.title ?: "") }
    var realTitle by rememberSaveable { mutableStateOf(opened?.first ?: "") }
    var reason by rememberSaveable { mutableStateOf(opened?.second ?: editing?.reason ?: "") }
    var icon by rememberSaveable { mutableStateOf(editing?.icon ?: "") }
    var kind by rememberSaveable { mutableStateOf(editing?.kind) }
    var private by rememberSaveable { mutableStateOf(editing?.isPrivate == true) }
    var target by rememberSaveable { mutableStateOf(editing?.target) }
    var sinceEpochDay by rememberSaveable { mutableStateOf((editing?.since ?: today).toEpochDay()) }
    var picking by rememberSaveable { mutableStateOf(false) }
    var pickingIcon by rememberSaveable { mutableStateOf(false) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    val since = LocalDate.ofEpochDay(sinceEpochDay)

    val canSavePrivate = !private || sealedAway || (privacy.unlocked && realTitle.isNotBlank())
    val valid = title.isNotBlank() && canSavePrivate

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(heading) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                if (editing == null && title.isBlank()) {
                    Text("Start from one of these, or write your own.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        goalTemplates.forEach { t ->
                            SuggestionChip(
                                onClick = {
                                    title = t.title
                                    icon = t.icon
                                    kind = t.kind
                                },
                                label = { Text(t.title) },
                                icon = { EmojiGlyph(t.icon, 16.sp) },
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(52.dp)
                            .background(MaterialTheme.colorScheme.surfaceContainer, CircleShape)
                            .clickable { pickingIcon = true },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (icon.isEmpty()) {
                            Icon(Icons.Filled.Add, contentDescription = "Choose an icon", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            EmojiGlyph(icon, 26.sp)
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text(if (private) "Shown title (cover)" else "Goal") },
                        placeholder = { Text(if (private) "e.g. No more corn" else "e.g. No more alcohol") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                        modifier = Modifier.weight(1f),
                    )
                }

                Spacer(Modifier.height(16.dp))
                val kinds = listOf<GoalKind?>(null) + GoalKind.entries
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    kinds.forEachIndexed { i, k ->
                        SegmentedButton(
                            selected = kind == k,
                            onClick = { kind = k },
                            shape = SegmentedButtonDefaults.itemShape(i, kinds.size),
                            icon = {},
                            colors = SegmentedButtonDefaults.colors(
                                activeContainerColor = MaterialTheme.colorScheme.primary,
                                activeContentColor = MaterialTheme.colorScheme.onPrimary,
                            ),
                        ) { Text(k?.label ?: "Neither", maxLines = 1) }
                    }
                }
                Text(
                    kind?.blurb?.let { "$it." } ?: "Optional: mark it a vice to do less of, or a virtue to do more of.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp),
                )

                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Private title", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "Show a cover name; the real one needs your PIN.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = private,
                        onCheckedChange = { on ->
                            if (on && !privacy.unlocked) privacy.requestPin()
                            // Making it public again: the real name becomes the title.
                            if (!on && realTitle.isNotBlank()) title = realTitle
                            private = on
                        },
                        // A sealed goal can only be made public once its real name is visible.
                        enabled = !sealedAway,
                    )
                }
                if (private) {
                    Spacer(Modifier.height(8.dp))
                    when {
                        sealedAway -> Text(
                            "The real name is hidden. Long-press the app’s name on the Goals screen and enter your PIN to see or change it.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        !privacy.unlocked -> TextButton(onClick = privacy.requestPin) {
                            Text(if (privacy.hasPin) "Enter your PIN to add the real name" else "Set a PIN to use private titles")
                        }
                        else -> OutlinedTextField(
                            value = realTitle,
                            onValueChange = { realTitle = it },
                            label = { Text("Real title (hidden)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }

                if (!sealedAway) {
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = reason,
                        onValueChange = { reason = it },
                        label = { Text(if (private) "Why it matters (hidden, optional)" else "Why it matters (optional)") },
                        minLines = 2,
                        maxLines = 4,
                        enabled = !private || privacy.unlocked,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                Spacer(Modifier.height(12.dp))
                Text("Target (optional)", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(4.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf<Long?>(null, 30, 90, 180, 365).forEach { t ->
                        FilterChip(
                            selected = target == t,
                            onClick = { target = t },
                            label = { Text(t?.let { "$it days" } ?: "None") },
                        )
                    }
                }

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
                if (onDelete != null || onArchive != null) {
                    Spacer(Modifier.height(16.dp))
                    Row {
                        if (onArchive != null) {
                            TextButton(onClick = onArchive) { Text(if (editing?.archived == true) "Restore goal" else "Archive goal") }
                        }
                        if (onDelete != null) {
                            TextButton(
                                onClick = { confirmDelete = true },
                                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            ) { Text("Delete goal") }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        GoalDraft(
                            title = title,
                            since = since,
                            reason = reason,
                            icon = icon,
                            kind = kind,
                            private = private,
                            realTitle = if (private && !sealedAway) realTitle else null,
                            target = target,
                        )
                    )
                },
                enabled = valid,
            ) { Text(confirm) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )

    if (pickingIcon) {
        IconPickerDialog(
            current = icon,
            onDismiss = { pickingIcon = false },
            onPick = {
                icon = it
                pickingIcon = false
            },
        )
    }

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
            text = { Text("It and its history will be removed from all your devices. You can undo this straight after.") },
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

/** A grid of monochrome emoji to choose from, or none. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun IconPickerDialog(current: String, onDismiss: () -> Unit, onPick: (String) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Icon") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                GoalIcons.groups.forEach { (group, icons) ->
                    SectionHeading(group, Modifier.padding(top = 8.dp))
                    FlowRow {
                        icons.forEach { e ->
                            Box(
                                Modifier
                                    .size(48.dp)
                                    .padding(2.dp)
                                    .then(
                                        if (e == current) Modifier.border(BorderStroke(1.5.dp, MaterialTheme.colorScheme.onBackground), CircleShape)
                                        else Modifier
                                    )
                                    .clickable { onPick(e) },
                                contentAlignment = Alignment.Center,
                            ) { EmojiGlyph(e, 24.sp) }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
        dismissButton = { TextButton(onClick = { onPick("") }) { Text("No icon") } },
    )
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
