package com.stagirite.ledger.ui

import androidx.compose.ui.semantics.onLongClick
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.selection.selectable
import androidx.compose.runtime.LaunchedEffect
import com.stagirite.ledger.reminders.Reminder
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import com.stagirite.ledger.data.Goal
import com.stagirite.ledger.data.GoalIcons
import com.stagirite.ledger.data.GoalKind
import com.stagirite.ledger.data.Milestones
import com.stagirite.ledger.data.SortOrder
import com.stagirite.ledger.data.Verdict
import com.stagirite.ledger.data.goalTemplates
import com.stagirite.ledger.data.Quotes
import com.stagirite.ledger.data.dayState
import com.stagirite.ledger.data.describeSpan
import com.stagirite.ledger.data.plural
import com.stagirite.ledger.security.Opened
import com.stagirite.ledger.support.Billing
import com.stagirite.ledger.data.progress
import com.stagirite.ledger.data.weekReview
import com.stagirite.ledger.data.stats
import kotlinx.coroutines.launch
import java.time.DayOfWeek
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
    /** Weekdays a Virtue is due; empty for every day. */
    val schedule: Set<DayOfWeek> = emptySet(),
)

/** What the goal screens need to know about private titles. */
class PrivacyUi(
    val hasPin: Boolean,
    val unlocked: Boolean,
    /** Ask for the PIN (or to set one up). */
    val requestPin: () -> Unit,
    /** The real title, note and slip notes of a private goal, when unlocked. */
    val open: (Goal) -> Opened?,
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
    onQuickSlip: (Goal, LocalDate) -> Unit,
    onQuickUrge: (Goal) -> Unit,
    onArchive: (Goal, Boolean) -> Unit,
    onTitleLongPress: () -> Unit,
    onSettings: () -> Unit,
    onWeekly: () -> Unit,
    quick: String? = null,
    onQuickHandled: () -> Unit = {},
    /** The Goals / Overview switcher. */
    bottomBar: @Composable () -> Unit = {},
    showHoldHint: Boolean = false,
    onHintSeen: () -> Unit = {},
) {
    var adding by rememberSaveable { mutableStateOf(false) }
    val plus = LocalPlus.current
    // The title starts large and tucks into a slim bar as the list scrolls.
    val scroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    var menu by remember { mutableStateOf(false) }
    var showArchived by rememberSaveable { mutableStateOf(false) }
    var actionsFor by rememberSaveable { mutableStateOf<String?>(null) }
    var quickLog by rememberSaveable { mutableStateOf(false) }
    val buzz = rememberBuzz()
    val newGoal = {
        // Free includes a few goals; nobody loses ones they already have.
        if (!plus.has && goals.size >= Billing.FREE_GOALS) {
            plus.offer("The free ledger holds ${Billing.FREE_GOALS} goals. Plus gives you as many as you like.")
        } else {
            adding = true
        }
    }

    LaunchedEffect(quick) {
        when (quick) {
            Reminder.QUICK_LOG -> quickLog = true
            Reminder.QUICK_NEW -> newGoal()
        }
        if (quick != null) onQuickHandled()
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scroll.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    // Long-pressing the name shows or hides private titles.
                    // The subtitle shows only while the bar is open; collapsed, it's just the name.
                    val open = 1f - scroll.state.collapsedFraction
                    Wordmark(
                        large = open > 0.5f,
                        subtitleAlpha = ((open - 0.5f) * 2f).coerceIn(0f, 1f),
                        modifier = Modifier
                            .pointerInput(Unit) { detectTapGestures(onLongPress = { onTitleLongPress() }) }
                            // Screen readers can't long-press a title, so offer it as an action.
                            .semantics { onLongClick(label = "Show or hide private titles") { onTitleLongPress(); true } },
                    )
                },
                scrollBehavior = scroll,
                actions = {
                    IconButton(onClick = onSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
                    }
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
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = bottomBar,
        floatingActionButton = {
            // Tap for a new goal; hold for quick logging (a slip, an urge resisted, the weekly review).
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shadowElevation = 6.dp,
                modifier = Modifier.size(60.dp),
            ) {
                Box(
                    Modifier.combinedClickable(
                        onClickLabel = "New goal",
                        onLongClickLabel = "Quick log",
                        onClick = newGoal,
                        onLongClick = {
                            buzz()
                            quickLog = true
                        },
                    ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "New goal. Hold for quick log")
                }
            }
        },
    ) { padding ->
        if (goals.isEmpty() && archived.isEmpty()) {
            EmptyState(Modifier.padding(padding))
        } else {
            LazyColumn(
                contentPadding = PaddingValues(start = Space.l, end = Space.l, top = Space.s, bottom = 112.dp),
                modifier = Modifier.padding(padding),
            ) {
                item(key = "date") {
                    Text(
                        today.format(todayHeading),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = Space.xs, bottom = Space.m),
                    )
                }
                if (showHoldHint && goals.isNotEmpty()) {
                    item(key = "hint") {
                        // Shown once: the hold gestures are otherwise easy to miss.
                        LedgerCard(modifier = Modifier.padding(bottom = Space.l)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "Tip: hold + to log a slip or an urge for any goal without opening it.",
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.weight(1f),
                                )
                                TextButton(onClick = onHintSeen) { Text("Got it") }
                            }
                        }
                    }
                }
                // The goals as a ledger: ruled lines, counts in a column on the right.
                items(goals, key = { it.id }) { goal ->
                    LedgerRow(
                        goal, today,
                        onClick = { onOpen(goal) },
                        onLongClick = { actionsFor = goal.id },
                        // Glides into place when sorted, added or archived.
                        modifier = Modifier.animateItem(),
                    )
                }
                if (goals.isNotEmpty()) item(key = "rule-end") { HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant) }
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
                        TextButton(onClick = { showArchived = !showArchived }, modifier = Modifier.padding(top = Space.l)) {
                            Text(if (showArchived) "Hide archived" else "Archived (${archived.size})")
                        }
                    }
                    if (showArchived) {
                        items(archived, key = { it.id }) { goal ->
                            LedgerCard(modifier = Modifier.padding(bottom = Space.s), onClick = { onOpen(goal) }) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    GoalIcon(goal.icon, tile = 36.dp)
                                    if (goal.icon.isNotEmpty()) Spacer(Modifier.width(12.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(goal.title, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(
                                            "Archived \u00B7 ${plural(goal.daysKept(today), "day")}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                onSlip = { day ->
                    actionsFor = null
                    onQuickSlip(goal, day)
                },
                onOpen = {
                    actionsFor = null
                    onOpen(goal)
                },
                onArchive = {
                    actionsFor = null
                    onArchive(goal, true)
                },
                onUrge = {
                    actionsFor = null
                    onQuickUrge(goal)
                },
            )
        }
    }

    if (quickLog) {
        QuickLogSheet(
            goals = goals,
            today = today,
            onDismiss = { quickLog = false },
            onNew = {
                quickLog = false
                newGoal()
            },
            onWeekly = {
                quickLog = false
                if (plus.has) onWeekly() else plus.offer("The weekly review is part of Plus.")
            },
            onSlip = { goal, day ->
                quickLog = false
                onQuickSlip(goal, day)
            },
            onUrge = {
                quickLog = false
                onQuickUrge(it)
            },
        )
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
    ) {
        // Sits in the upper part of the page, under the title, rather than floating mid-screen.
        Spacer(Modifier.weight(0.35f))
        Text("Day one starts\nwhen you name it.", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Normal))
        Spacer(Modifier.height(12.dp))
        Text(
            "Something to stop, or something to keep doing. Tap + to add it, and the ledger counts the days.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(32.dp))
        if (LocalUiPrefs.current.quotes) LedgerCard { QuoteBlock(Quotes.forEmptyLedger(LocalDate.now())) }
        Spacer(Modifier.weight(1f))
    }
}

/** One goal as a line in the ledger: icon and name on the left, the count on the right. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LedgerRow(goal: Goal, today: LocalDate, onClick: () -> Unit, onLongClick: () -> Unit, modifier: Modifier = Modifier) {
    val days = goal.daysKept(today)
    val next = Milestones.next(days, goal.currentStart)
    val milestoneToday = Milestones.isMilestone(days, goal.currentStart)
    Column(modifier.fillMaxWidth()) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Column(
            Modifier
                .fillMaxWidth()
                .combinedClickable(onClick = onClick, onLongClick = onLongClick, onLongClickLabel = "Quick actions")
                .padding(horizontal = Space.xs, vertical = Space.l),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (goal.icon.isNotEmpty()) {
                    Box(Modifier.size(28.dp), contentAlignment = Alignment.CenterStart) { EmojiGlyph(goal.icon, 20.sp) }
                    Spacer(Modifier.width(Space.m))
                }
                Column(Modifier.weight(1f)) {
                    // Kept to what changes how to read the row; the progress verdict lives on the goal's page.
                    if (goal.hasSchedule) {
                        Text(
                            scheduleText(goal.schedule).uppercase(),
                            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.4.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 2.dp),
                        )
                    }
                    Text(goal.title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(
                        when {
                            milestoneToday -> "Milestone today"
                            goal.target != null && days >= goal.target -> "Target of ${plural(goal.target, "day")} reached"
                            goal.target != null -> "$days of ${plural(goal.target, "day")}"
                            else -> "${plural(next - days, "day")} to ${Milestones.name(next, goal.currentStart) ?: plural(next, "day")}"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (milestoneToday) MaterialTheme.colorScheme.onBackground
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
                Spacer(Modifier.width(Space.l))
                Column(horizontalAlignment = Alignment.End) {
                    // Day zero reads as a start, not as nothing.
                    if (days == 0L) {
                        Text("Today", style = LedgerType.count.copy(fontSize = 26.sp, lineHeight = 40.sp).capped())
                    } else {
                        RollingNumber(days, LedgerType.count.capped())
                    }
                    Text(
                        when (days) {
                            0L -> if (goal.slips.isEmpty()) "started" else "begun again"
                            1L -> "day"
                            else -> "days"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(Space.m))
            ThinProgress(goal.targetProgress(today) ?: Milestones.progress(days, goal.currentStart))
        }
    }
}

/** Whether a slip can be logged on [day]: in range, due, and not logged already. */
private fun Goal.canSlipOn(day: LocalDate, today: LocalDate) =
    !day.isBefore(since) && !day.isAfter(today) && !isSlip(day) && isScheduled(day)

@Composable
private fun RowScope.QuickButton(text: String, enabled: Boolean, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.weight(1f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = if (enabled) 1f else 0.4f)),
    ) { Text(text, maxLines = 1, overflow = TextOverflow.Ellipsis) }
}

/** Holding +: log for any goal without opening it, or start something new. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuickLogSheet(
    goals: List<Goal>,
    today: LocalDate,
    onDismiss: () -> Unit,
    onNew: () -> Unit,
    onWeekly: () -> Unit,
    onSlip: (Goal, LocalDate) -> Unit,
    onUrge: (Goal) -> Unit,
) {
    val yesterday = today.minusDays(1)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            Modifier
                .padding(horizontal = Space.xl)
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(bottom = Space.l),
        ) {
            Text("Quick log", style = MaterialTheme.typography.titleLarge)
            Text(
                "Every day counts as kept unless you log otherwise. Hold + to open this any time.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(Space.m))
            SlipOption("New goal", "Something to stop or to keep doing", enabled = true, onClick = onNew)
            if (goals.isNotEmpty()) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                SlipOption("Weekly review", "How the last seven days went", enabled = true, onClick = onWeekly)
            }
            goals.forEach { goal ->
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Column(Modifier.padding(vertical = Space.m)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (goal.icon.isNotEmpty()) {
                            EmojiGlyph(goal.icon, 18.sp)
                            Spacer(Modifier.width(Space.s))
                        }
                        Text(goal.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Spacer(Modifier.height(Space.s))
                    Row(horizontalArrangement = Arrangement.spacedBy(Space.s)) {
                        if (goal.kind == GoalKind.Virtue) {
                            // A Virtue is assumed done; a slip on one usually only shows once the day is over.
                            QuickButton("Slipped yesterday", goal.canSlipOn(yesterday, today)) { onSlip(goal, yesterday) }
                            QuickButton("Slipped today", goal.canSlipOn(today, today)) { onSlip(goal, today) }
                        } else {
                            QuickButton(goal.urgeLabel, true) { onUrge(goal) }
                            QuickButton(if (goal.isSlip(today)) "Slip logged" else "Log a ${goal.slipWord}", goal.canSlipOn(today, today)) {
                                onSlip(goal, today)
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Long-press actions for a goal on the list. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuickActionsSheet(
    goal: Goal,
    today: LocalDate,
    onDismiss: () -> Unit,
    onSlip: (LocalDate) -> Unit,
    onOpen: () -> Unit,
    onArchive: () -> Unit,
    onUrge: () -> Unit,
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                GoalIcon(goal.icon, tile = 36.dp)
                if (goal.icon.isNotEmpty()) Spacer(Modifier.width(12.dp))
                Text(goal.title, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.height(12.dp))
            if (goal.kind == GoalKind.Virtue) {
                SlipOption("Slipped yesterday", yesterday.format(mediumDate), enabled = goal.canSlipOn(yesterday, today)) { onSlip(yesterday) }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                SlipOption("Slipped today", today.format(mediumDate), enabled = goal.canSlipOn(today, today)) { onSlip(today) }
            } else {
                SlipOption(goal.urgeLabel, "Log it for today", enabled = true, onClick = onUrge)
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                SlipOption("Log a ${goal.slipWord} today", today.format(mediumDate), enabled = goal.canSlipOn(today, today)) { onSlip(today) }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            SlipOption("Open", "Calendar, stats and history", enabled = true, onClick = onOpen)
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            SlipOption("Archive", "Hide it from the list; restore any time", enabled = true, onClick = onArchive)
        }
    }
}

// ---------------------------------------------------------------- Goal page

private enum class GoalTab(val label: String) { Calendar("Calendar"), Insights("Insights"), History("History") }

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
    /** False for a private goal while its PIN hasn't been entered. */
    canNote: Boolean = true,
    onNote: (LocalDate, String) -> Unit = { _, _ -> },
    onUrge: () -> Unit = {},
    onUndoUrge: () -> Unit = {},
) {
    val buzz = rememberBuzz()
    var notingDay by rememberSaveable { mutableStateOf<Long?>(null) }
    var editing by rememberSaveable { mutableStateOf(false) }
    var slipSheet by rememberSaveable { mutableStateOf(false) }
    var pickingSlipDay by rememberSaveable { mutableStateOf(false) }
    var movingStart by rememberSaveable { mutableStateOf(false) }
    var tappedDay by rememberSaveable { mutableStateOf<Long?>(null) }
    // Length of the run that just ended, while the "Day 0" screen is showing.
    var endedRun by rememberSaveable { mutableStateOf<Long?>(null) }
    var tab by rememberSaveable { mutableStateOf(GoalTab.Calendar) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val days = goal.daysKept(today)
    val next = Milestones.next(days, goal.currentStart)
    val beganAgainToday = goal.slips.lastOrNull() == today
    val slipWord = goal.slipWord

    /** Logs a slip. Today's gets the Day 0 screen; a past one gets an undo bar. */
    val logSlip: (LocalDate) -> Unit = { day ->
        buzz()
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
            // The count, set large on the open page, with what's next beneath it.
            val canSlipToday = !goal.isSlip(today) && goal.isScheduled(today)
            Column(
                Modifier.fillMaxWidth().padding(top = Space.l),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (goal.icon.isNotEmpty() || goal.kind != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (goal.icon.isNotEmpty()) EmojiGlyph(goal.icon, 18.sp)
                        if (goal.icon.isNotEmpty() && goal.kind != null) Spacer(Modifier.width(Space.s))
                        KindLabel(goal.kind)
                    }
                }
                val countDescription = Modifier.semantics(mergeDescendants = true) {
                    contentDescription = (if (days == 0L) "Started today" else plural(days, "day")) + "; " +
                        (goal.target?.let { "target ${plural(it, "day")}" } ?: "next milestone ${plural(next, "day")}")
                }
                if (days == 0L) {
                    // The first day reads as a beginning rather than a zero.
                    Text("Today", style = LedgerType.hero.copy(fontSize = 64.sp, lineHeight = 96.sp).capped(), modifier = countDescription)
                } else {
                    RollingNumber(days, LedgerType.hero.capped(), countDescription)
                }
                Text(
                    when (days) {
                        0L -> if (goal.slips.isEmpty()) "you started" else "you began again"
                        1L -> "day"
                        else -> "days"
                    },
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(Space.xl))
                Text(
                    when {
                        Milestones.isMilestone(days, goal.currentStart) -> "Milestone reached today"
                        goal.target != null && days >= goal.target -> "Target of ${plural(goal.target, "day")} reached"
                        goal.target != null -> "${plural(goal.target - days, "day")} to your target of ${goal.target}"
                        else -> "${plural(next - days, "day")} to ${Milestones.name(next, goal.currentStart) ?: plural(next, "day")}"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(Space.s))
                ThinProgress(goal.targetProgress(today) ?: Milestones.progress(days, goal.currentStart), Modifier.width(160.dp))
                TextButton(onClick = { movingStart = true }) {
                    Text(
                        "${describeSpan(goal.currentStart, today)} \u00B7 since ${goal.currentStart.format(mediumDate)}",
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
                Spacer(Modifier.height(Space.m))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Space.s)) {
                    // A Virtue is assumed done, so there's nothing to log on a good day: only a slip.
                    if (goal.kind != GoalKind.Virtue) Button(
                        onClick = {
                            buzz()
                            onUrge()
                            val count = goal.urgesOn(today) + 1
                            scope.launch {
                                val r = snackbar.showSnackbar(
                                    "${goal.urgeLabel} \u00B7 ${if (count == 1) "first" else "${count}\u00D7"} today",
                                    actionLabel = "Undo",
                                )
                                if (r == SnackbarResult.ActionPerformed) onUndoUrge()
                            }
                        },
                        modifier = Modifier.weight(1f).height(52.dp),
                    ) { Text(goal.urgeLabel, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                    // Tap to choose the day; press and hold to log today without a dialog.
                    HoldButton(
                        text = "Log a $slipWord",
                        onTap = { slipSheet = true },
                        // Not for Virtues: a slip on one is usually noticed later, so the day is picked from the sheet.
                        onHeld = if (canSlipToday && goal.kind != GoalKind.Virtue) ({ logSlip(today) }) else null,
                        modifier = Modifier.weight(1f).height(52.dp),
                    )
                }
                Spacer(Modifier.height(Space.s))
                val notes = listOfNotNull(
                    if (canSlipToday && goal.kind != GoalKind.Virtue) "Hold to log a $slipWord today" else null,
                    if (!goal.isScheduled(today)) "Rest day today \u00B7 ${scheduleText(goal.schedule)}" else null,
                    goal.urgesOn(today).takeIf { it > 0 }?.let { "${goal.urgesLabel} today: $it" },
                )
                notes.forEach {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.height(Space.m))
            }

            if (beganAgainToday) {
                LedgerCard {
                    Text(
                        "You began again today.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (LocalUiPrefs.current.quotes) {
                        Spacer(Modifier.height(16.dp))
                        QuoteBlock(Quotes.forSlip(goal))
                    }
                }
            }

            if (goal.reason.isNotBlank()) {
                LedgerCard {
                    SectionHeading("Why", Modifier.padding(start = 0.dp))
                    Text(goal.reason, style = MaterialTheme.typography.bodyLarge)
                }
            }

            TextTabs(GoalTab.entries.map { it.label }, tab.ordinal) { tab = GoalTab.entries[it] }

            Column(Modifier.padding(horizontal = Space.xs)) {
                when (tab) {
                    GoalTab.Calendar -> {
                        LedgerCalendar(
                            today = today,
                            earliest = goal.since,
                            stateOf = { goal.dayState(it, today) },
                            onDayClick = { if (goal.isScheduled(it) || goal.isSlip(it)) tappedDay = it.toEpochDay() },
                            slipLabel = slipWord.capitalised(),
                            urgesOf = { goal.urgesOn(it) },
                            showUrgeKey = goal.urges.isNotEmpty(),
                            showRestKey = goal.hasSchedule,
                        )
                        Text(
                            "Tap a day to log or remove a $slipWord.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                    GoalTab.Insights -> {
                        StatsPanel(goal, today)
                        Spacer(Modifier.height(Space.xl))
                        SectionHeading("Milestones", Modifier.padding(start = 0.dp))
                        MilestonesPanel(goal, today)
                    }
                    GoalTab.History -> HistoryPanel(goal, today, canNote) { notingDay = it.toEpochDay() }
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
            allowed = goal::isScheduled,
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

    notingDay?.let { epochDay ->
        val day = LocalDate.ofEpochDay(epochDay)
        var text by rememberSaveable(epochDay) { mutableStateOf(goal.notes[day] ?: "") }
        AlertDialog(
            onDismissRequest = { notingDay = null },
            title = { Text("Note on ${day.format(mediumDate)}") },
            text = {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("What happened?") },
                    placeholder = { Text("What led to it, how you felt, what might help next time") },
                    minLines = 3,
                    maxLines = 6,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onNote(day, text)
                    notingDay = null
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { notingDay = null }) { Text("Cancel") } },
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
        var note by rememberSaveable { mutableStateOf("") }
        val finish = {
            if (note.isNotBlank()) onNote(today, note)
            endedRun = null
        }
        AlertDialog(
            onDismissRequest = finish,
            title = { Text("Begin again") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        if (ended > 0) "Your run of ${plural(ended, "day")} is kept in the history. The count starts again today."
                        else "The count starts again today.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    if (canNote) {
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(
                            value = note,
                            onValueChange = { note = it },
                            label = { Text("What happened? (optional)") },
                            placeholder = { Text("Only you will see this") },
                            minLines = 2,
                            maxLines = 4,
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    Spacer(Modifier.height(20.dp))
                    QuoteBlock(Quotes.forSlip(goal))
                }
            },
            confirmButton = { TextButton(onClick = finish) { Text("Carry on") } },
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
            val todayOption = @Composable {
                SlipOption("Today", if (!goal.isScheduled(today)) "Rest day" else today.format(mediumDate), enabled = goal.canSlipOn(today, today)) { onPick(today) }
            }
            val yesterdayOption = @Composable {
                SlipOption("Yesterday", yesterday.format(mediumDate), enabled = goal.canSlipOn(yesterday, today)) { onPick(yesterday) }
            }
            // A slip on a Virtue is usually noticed afterwards, so Virtues offer yesterday first.
            if (goal.kind == GoalKind.Virtue) yesterdayOption() else todayOption()
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            if (goal.kind == GoalKind.Virtue) todayOption() else yesterdayOption()
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
    val plus = LocalPlus.current
    if (plus.has) {
        ProgressPanel(goal, progress)
    } else {
        // The verdict is shown either way; the detail behind it is Plus.
        SectionHeading("Progress", Modifier.padding(start = 0.dp))
        Text(progress.verdict.label, style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(4.dp))
        Text(
            "See how your runs and slips are changing over time, month by month.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        TextButton(onClick = { plus.offer("The Progress card is part of Plus.") }) { Text("Unlock with Plus") }
    }
    Spacer(Modifier.height(28.dp))
    SectionHeading("Figures", Modifier.padding(start = 0.dp))
    Row(Modifier.fillMaxWidth()) {
        Figure("${stats.current}", "Current", Modifier.weight(1f))
        Figure("${stats.longest}", "Longest", Modifier.weight(1f))
        Figure(formatAverage(stats.average), "Average", Modifier.weight(1f))
    }
    Row(Modifier.fillMaxWidth()) {
        Figure("${(stats.keptRate * 100).roundToInt()}%", "Days kept", Modifier.weight(1f))
        Figure("${stats.freshStarts}", "Begun again", Modifier.weight(1f))
        Figure("${stats.slipsLast30}", "${(goal.kind?.slips ?: "slips").capitalised()}, 30 days", Modifier.weight(1f))
    }
    if (stats.urgesTotal > 0) {
        Row(Modifier.fillMaxWidth()) {
            Figure("${stats.urgesTotal}", goal.urgesLabel, Modifier.weight(1f))
            Figure("${stats.urgesThisRun}", "This run", Modifier.weight(1f))
            Figure("${stats.urgesLast30}", "Last 30 days", Modifier.weight(1f))
        }
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
    val start = goal.currentStart
    // Looks a good way ahead: the next few, and at least the next yearly anniversary.
    val soon = generateSequence(Milestones.next(days, start)) { Milestones.next(it, start) }.take(4).toList()
    val nextAnniversary = Milestones.upTo(days + 400, start)
        .firstOrNull { it > days && Milestones.name(it, start)?.contains("year") == true }
    val upcoming = (soon + listOfNotNull(nextAnniversary)).distinct().sorted()
    val rows = Milestones.upTo(days, start) + upcoming
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
                    plural(m, "day") + (Milestones.name(m, start)?.let { " \u00B7 $it" } ?: ""),
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
private fun HistoryPanel(goal: Goal, today: LocalDate, canNote: Boolean, onNote: (LocalDate) -> Unit) {
    val runs = goal.runs(today).asReversed()
    if (goal.history.isNotEmpty() && canNote) {
        Text(
            "Tap a ${goal.slipWord} to add a note about it.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 4.dp),
        )
    }
    runs.forEachIndexed { i, run ->
        val current = i == 0
        // Every finished run ended in a slip on its end day.
        val slipDay = if (current) null else run.end
        val note = slipDay?.let { goal.notes[it] }
        Row(
            Modifier
                .fillMaxWidth()
                .then(if (slipDay != null && canNote) Modifier.clickable { onNote(slipDay) } else Modifier)
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Column(Modifier.weight(1f)) {
                Text(plural(run.days, "day"), style = MaterialTheme.typography.bodyLarge)
                Text(
                    "${run.start.format(mediumDate)} \u2013 " + if (current) "now" else run.end.format(mediumDate),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (note != null) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        note,
                        style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            when {
                current -> Text("Current", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                canNote -> Text(
                    if (note == null) "Add note" else "Edit",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
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
    var realTitle by rememberSaveable { mutableStateOf(opened?.title ?: "") }
    var reason by rememberSaveable { mutableStateOf(opened?.reason ?: editing?.reason ?: "") }
    var icon by rememberSaveable { mutableStateOf(editing?.icon ?: "") }
    // Every goal is one or the other; older goals without a kind are treated as Vices.
    var kind by rememberSaveable { mutableStateOf(editing?.kind ?: GoalKind.Vice) }
    var private by rememberSaveable { mutableStateOf(editing?.isPrivate == true) }
    var target by rememberSaveable { mutableStateOf(editing?.target) }
    var schedule by rememberSaveable { mutableStateOf<List<Int>>(editing?.schedule?.map { it.value } ?: emptyList()) }
    var sinceEpochDay by rememberSaveable { mutableStateOf((editing?.since ?: today).toEpochDay()) }
    var picking by rememberSaveable { mutableStateOf(false) }
    var pickingIcon by rememberSaveable { mutableStateOf(false) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    // Open from the start when editing a goal that already uses any of the extras.
    var more by rememberSaveable {
        mutableStateOf(editing != null && (editing.isPrivate || editing.target != null || editing.hasSchedule || editing.reason.isNotBlank()))
    }
    val since = LocalDate.ofEpochDay(sinceEpochDay)

    val canSavePrivate = !private || sealedAway || (privacy.unlocked && realTitle.isNotBlank())
    val valid = title.isNotBlank() && canSavePrivate

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(heading) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                // First, the one choice that shapes everything else: giving something up, or taking it up.
                Text("What kind of goal?", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(Space.s))
                GoalKind.entries.forEach { k ->
                    KindChoice(k, selected = kind == k) {
                        kind = k
                        if (k == GoalKind.Vice) schedule = emptyList()
                    }
                    Spacer(Modifier.height(Space.s))
                }

                if (editing == null && title.isBlank()) {
                    Spacer(Modifier.height(Space.xs))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(Space.s)) {
                        goalTemplates.filter { it.kind == kind }.forEach { t ->
                            SuggestionChip(
                                onClick = {
                                    title = t.title
                                    icon = t.icon
                                },
                                label = { Text(t.title) },
                                icon = { EmojiGlyph(t.icon, 16.sp) },
                            )
                        }
                    }
                }
                Spacer(Modifier.height(Space.m))
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
                    Spacer(Modifier.width(Space.m))
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text(if (private) "Shown title (cover)" else "Goal") },
                        placeholder = {
                            Text(
                                when {
                                    private -> "e.g. No more corn"
                                    kind == GoalKind.Virtue -> "e.g. One good deed a day"
                                    else -> "e.g. Drink less"
                                },
                            )
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                        modifier = Modifier.weight(1f),
                    )
                }

                Spacer(Modifier.height(Space.s))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Started", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    TextButton(onClick = { picking = true }) {
                        Text(if (since == today) "Today" else since.format(mediumDate))
                    }
                }

                // Everything else is optional, and tucked away until wanted.
                TextButton(onClick = { more = !more }, contentPadding = PaddingValues(0.dp)) {
                    Text(if (more) "Fewer options" else "More options")
                    Icon(
                        if (more) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                        contentDescription = null,
                        modifier = Modifier.padding(start = Space.xs),
                    )
                }
                if (more) {
                    if (kind == GoalKind.Virtue) {
                        Text("Which days?", style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(Space.xs))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            DayOfWeek.entries.forEach { d ->
                                FilterChip(
                                    selected = d.value in schedule,
                                    onClick = { schedule = if (d.value in schedule) schedule - d.value else schedule + d.value },
                                    label = { Text(d.getDisplayName(java.time.format.TextStyle.NARROW, java.util.Locale.getDefault())) },
                                )
                            }
                        }
                        Text(
                            if (schedule.isEmpty() || schedule.size == 7) "Every day."
                            else "${scheduleText(schedule.map(DayOfWeek::of).toSet())}. Other days are rest days and never count as slips.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(Space.m))
                    }

                    if (!sealedAway) {
                        OutlinedTextField(
                            value = reason,
                            onValueChange = { reason = it },
                            label = { Text(if (private) "Why it matters (hidden)" else "Why it matters") },
                            minLines = 2,
                            maxLines = 4,
                            enabled = !private || privacy.unlocked,
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(Modifier.height(Space.m))
                    }

                    Text("Target", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(Space.xs))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(Space.s)) {
                        listOf<Long?>(null, 30, 90, 180, 365).forEach { t ->
                            FilterChip(
                                selected = target == t,
                                onClick = { target = t },
                                label = { Text(t?.let { "$it days" } ?: "None") },
                            )
                        }
                    }

                    Spacer(Modifier.height(Space.m))
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
                        Spacer(Modifier.height(Space.s))
                        when {
                            sealedAway -> Text(
                                "The real name is hidden. Long-press the app\u2019s name on the Goals screen and enter your PIN to see or change it.",
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
                }
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
                            // Only Virtues have schedules; a Vice is every day.
                            schedule = if (kind == GoalKind.Virtue) schedule.map(DayOfWeek::of).toSet() else emptySet(),
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

/** One of the two kinds of goal, as a card to pick. */
@Composable
private fun KindChoice(kind: GoalKind, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .border(if (selected) 2.dp else 1.dp, if (selected) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.outlineVariant, shape)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = Space.m, vertical = Space.m),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(kind.choice, style = MaterialTheme.typography.titleSmall)
            Text(kind.blurb, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
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
                            ) { EmojiGlyph(e, 24.sp, decorative = false) }
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
    allowed: (LocalDate) -> Boolean = { true },
) {
    val state = rememberDatePickerState(
        initialSelectedDateMillis = initial.toEpochDay() * MILLIS_PER_DAY,
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                val day = utcTimeMillis / MILLIS_PER_DAY
                return day <= latest.toEpochDay() && (earliest == null || day >= earliest.toEpochDay()) &&
                    allowed(LocalDate.ofEpochDay(day))
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
