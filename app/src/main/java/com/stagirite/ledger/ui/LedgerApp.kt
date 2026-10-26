package com.stagirite.ledger.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.stagirite.ledger.data.Goal
import com.stagirite.ledger.data.GoalStore
import com.stagirite.ledger.data.Milestones
import com.stagirite.ledger.data.Quotes
import com.stagirite.ledger.data.plural
import com.stagirite.ledger.data.sortGoals
import com.stagirite.ledger.widget.LedgerWidget
import androidx.compose.ui.platform.LocalContext
import com.stagirite.ledger.data.Settings
import com.stagirite.ledger.security.AppLock
import com.stagirite.ledger.security.PrivateTitles
import com.stagirite.ledger.security.UnlockResult
import com.stagirite.ledger.sync.SyncManager
import com.stagirite.ledger.sync.SyncState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate

const val MIN_PASSPHRASE = 8

/** Today's date, refreshed so the counts roll over at midnight while the app is open. */
@Composable
fun rememberToday(): State<LocalDate> {
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
fun LedgerApp(
    store: GoalStore,
    settings: Settings,
    sync: SyncManager,
    lock: AppLock,
    vault: PrivateTitles,
    support: SupportUi,
    /** Set when the weekly-review notification was tapped. */
    openWeekly: Boolean = false,
    onWeeklyOpened: () -> Unit = {},
    /** A goal tapped on the home-screen widget. */
    openGoal: String? = null,
    onGoalOpened: () -> Unit = {},
    /** A home-screen action from a shortcut or the reminder. */
    quick: String? = null,
    onQuickHandled: () -> Unit = {},
) {
    val context = LocalContext.current
    var plusReason by rememberSaveable { mutableStateOf<String?>(null) }
    val plus = PlusUi(has = support.billing.plus, offer = { plusReason = it })

    // A purchase or tip went through: say thank you once.
    val thanks = support.billing.thanks
    LaunchedEffect(thanks) {
        if (thanks != null) {
            Toast.makeText(context, thanks, Toast.LENGTH_LONG).show()
            support.billing.thanks = null
            plusReason = null
        }
    }

    CompositionLocalProvider(LocalPlus provides plus, LocalSupport provides support) {
        LedgerAppContent(store, settings, sync, lock, vault, openWeekly, onWeeklyOpened, openGoal, onGoalOpened, quick, onQuickHandled)
        plusReason?.let { PlusSheet(it, support, onDismiss = { plusReason = null }) }
    }
}

@Composable
private fun LedgerAppContent(
    store: GoalStore,
    settings: Settings,
    sync: SyncManager,
    lock: AppLock,
    vault: PrivateTitles,
    openWeekly: Boolean,
    onWeeklyOpened: () -> Unit,
    openGoalRequest: String?,
    onGoalOpened: () -> Unit,
    quick: String?,
    onQuickHandled: () -> Unit,
) {
    val today by rememberToday()
    var inSettings by rememberSaveable { mutableStateOf(false) }
    var inWeekly by rememberSaveable { mutableStateOf(false) }
    // Home has two tabs: the ledger of goals, and the overview of them all.
    var onOverview by rememberSaveable { mutableStateOf(false) }
    var openId by rememberSaveable { mutableStateOf<String?>(null) }
    LaunchedEffect(openWeekly) {
        if (openWeekly) {
            openId = null
            inWeekly = true
            onWeeklyOpened()
        }
    }
    // A shortcut's action belongs to the home page, so go there first.
    LaunchedEffect(quick) {
        if (quick != null) {
            openId = null
            inSettings = false
            inWeekly = false
            onOverview = false
        }
    }
    LaunchedEffect(openGoalRequest) {
        if (openGoalRequest != null) {
            if (store.find(openGoalRequest) != null) {
                inSettings = false
                inWeekly = false
                openId = openGoalRequest
            }
            onGoalOpened()
        }
    }
    var pinFlow by remember { mutableStateOf<PinFlow?>(null) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Goals as they should be shown: real titles only while private titles are unlocked.
    val shown = sortGoals(store.goals.map(vault::reveal), settings.sortOrder, today)
    val context = LocalContext.current
    val buzz = rememberBuzz()

    LaunchedEffect(Unit) {
        if (store.recoveredFromDamage) {
            snackbar.showSnackbar(
                "Your saved ledger couldn\u2019t be read, so the last good copy was restored. The damaged copy was kept.",
                duration = SnackbarDuration.Long,
            )
        }
    }

    // Keep the home-screen widget in step with the lock and sort order.
    val hasPlus = LocalPlus.current.has
    LaunchedEffect(settings.appLock, settings.sortOrder, settings.widgetStyle, hasPlus) { runCatching { LedgerWidget.refresh(context) } }
    val rawOpen = openId?.let(store::find)
    val openGoal = rawOpen?.let(vault::reveal)
    val anySealed = store.goals.any { it.isPrivate }

    val requestPin: () -> Unit = {
        pinFlow = if (vault.hasPin || anySealed) PinFlow.Unlock else PinFlow.Set
    }
    val privacy = PrivacyUi(
        hasPin = vault.hasPin || anySealed,
        unlocked = vault.unlocked,
        requestPin = requestPin,
        open = { vault.open(it) },
    )

    /** Turns a draft into what's stored: private goals keep only a sealed copy of the real name. */
    fun sealedFor(draft: GoalDraft, existing: Goal?): String? = when {
        !draft.private -> null
        draft.realTitle == null -> existing?.sealed
        // Keep notes on its slips: those already sealed, or (when it's just been made private) the plain ones.
        else -> vault.seal(draft.realTitle, draft.reason, existing?.let { vault.open(it)?.notes ?: it.notes } ?: emptyMap())
    }

    if (!settings.guideSeen) {
        GuideScreen(
            onDone = { settings.updateGuideSeen(true) },
            // A first visit ends by naming a goal; shown again later, it's just the guide.
            onCreate = if (store.goals.isEmpty() && store.archived.isEmpty()) {
                { d -> store.add(d.title, d.since, d.reason, d.icon, d.kind, null, d.target, d.schedule) }
            } else {
                null
            },
        )
        return
    }

    val screen = when {
        rawOpen != null -> "goal:${rawOpen.id}"
        inWeekly -> "weekly"
        inSettings -> "settings"
        onOverview -> "overview"
        else -> "home"
    }
    BackHandler(enabled = screen != "home") {
        when {
            openId != null -> openId = null
            inWeekly -> inWeekly = false
            inSettings -> inSettings = false
            else -> onOverview = false
        }
    }
    val homeNav = @Composable { HomeNav(onOverview) { onOverview = it } }
    val setUpBackup = rememberBackupSetup(store, settings, lock)
    // Nudge only people with no copy anywhere, after their first week, and not more than monthly.
    val now = System.currentTimeMillis()
    val showBackupPrompt = store.goals.isNotEmpty() &&
        settings.backupUri == null && sync.state == SyncState.Off &&
        now - settings.firstUseAt > 7 * 86_400_000L &&
        now - settings.lastBackupAt > 30 * 86_400_000L &&
        now > settings.backupNudgeAfter

    AnimatedContent(
        targetState = screen,
        transitionSpec = {
            // Deeper screens come in from the right; going back reverses it. The two tabs just cross-fade.
            val tabs = setOf("home", "overview")
            if (initialState in tabs && targetState in tabs) {
                fadeIn(tween(200)) togetherWith fadeOut(tween(120))
            } else {
                val dx = if (initialState in tabs) 1 else -1
                (fadeIn(tween(240)) + slideInHorizontally(tween(280)) { dx * it / 10 }) togetherWith
                    (fadeOut(tween(160)) + slideOutHorizontally(tween(280)) { -dx * it / 14 })
            }
        },
        label = "screen",
    ) { target ->
        val targetRaw = if (target.startsWith("goal:")) store.find(target.removePrefix("goal:")) else null
        when {
        targetRaw != null -> {
            val rawOpen = targetRaw
            val openGoal = vault.reveal(targetRaw)
            GoalDetailScreen(
                goal = openGoal,
                cover = rawOpen.title,
                today = today,
                privacy = privacy,
                onBack = { openId = null },
                onEdit = { d ->
                    // Made public again: its sealed notes come back out into the clear.
                    val unsealedNotes = if (!d.private && rawOpen.isPrivate) vault.open(rawOpen)?.notes else null
                    store.edit(rawOpen.id, d.title, d.since, d.reason, d.icon, d.kind, sealedFor(d, rawOpen), d.target, unsealedNotes, d.schedule)
                },
                canNote = !rawOpen.isPrivate || vault.unlocked,
            onUrge = { store.resistUrge(rawOpen.id, today) },
            onUndoUrge = { store.unresistUrge(rawOpen.id, today) },
                onNote = { day, text ->
                    if (rawOpen.isPrivate) {
                        val o = vault.open(rawOpen)
                        if (o != null) {
                            val notes = if (text.isBlank()) o.notes - day else o.notes + (day to text.trim())
                            store.setSealed(rawOpen.id, vault.seal(o.title, o.reason, notes))
                        }
                    } else {
                        store.setNote(rawOpen.id, day, text)
                    }
                },
                onArchive = { archive ->
                    store.setArchived(rawOpen.id, archive)
                    if (archive) {
                        openId = null
                        scope.launch {
                            val r = snackbar.showSnackbar("Goal archived", actionLabel = "Undo")
                            if (r == SnackbarResult.ActionPerformed) store.setArchived(rawOpen.id, false)
                        }
                    }
                },
                onSlip = { store.recordSlip(rawOpen.id, it) },
                onRemoveSlip = { store.removeSlip(rawOpen.id, it) },
                onMoveStart = { store.moveCurrentStart(rawOpen.id, it) },
                onDelete = {
                    val removed = store.delete(rawOpen.id)
                    openId = null
                    if (removed != null) {
                        scope.launch {
                            val result = snackbar.showSnackbar("Goal deleted", actionLabel = "Undo", duration = SnackbarDuration.Long)
                            if (result == SnackbarResult.ActionPerformed) store.restore(removed)
                        }
                    }
                },
            )
        }
        target == "home" -> {
            GoalListScreen(
                goals = shown,
                today = today,
                snackbar = snackbar,
                privacy = privacy,
                archived = store.archived.map(vault::reveal),
                sortOrder = settings.sortOrder,
                onSort = settings::updateSortOrder,
                onOpen = { openId = it.id },
                onAdd = { d -> store.add(d.title, d.since, d.reason, d.icon, d.kind, sealedFor(d, null), d.target, d.schedule) },
                onQuickSlip = { g, day ->
                    buzz()
                    store.recordSlip(g.id, day)
                    scope.launch {
                        val r = snackbar.showSnackbar(
                            "${g.slipWord.replaceFirstChar { it.uppercase() }} logged for ${if (day == today) "today" else "yesterday"}",
                            actionLabel = "Undo",
                        )
                        if (r == SnackbarResult.ActionPerformed) store.removeSlip(g.id, day)
                    }
                },
                onQuickUrge = { g ->
                buzz()
                store.resistUrge(g.id, today)
                scope.launch {
                    val r = snackbar.showSnackbar("${g.urgeLabel} \u00B7 logged for today", actionLabel = "Undo")
                    if (r == SnackbarResult.ActionPerformed) store.unresistUrge(g.id, today)
                }
            },
            onArchive = { g, archive ->
                    store.setArchived(g.id, archive)
                    if (archive) scope.launch {
                        val r = snackbar.showSnackbar("Goal archived", actionLabel = "Undo")
                        if (r == SnackbarResult.ActionPerformed) store.setArchived(g.id, false)
                    }
                },
                onTitleLongPress = {
                    if (vault.unlocked) {
                        vault.lock()
                        scope.launch { snackbar.showSnackbar("Private titles hidden") }
                    } else if (privacy.hasPin) {
                        pinFlow = PinFlow.Unlock
                    }
                },
                onSettings = { inSettings = true },
                onWeekly = { inWeekly = true },
                quick = quick,
                onQuickHandled = onQuickHandled,
                bottomBar = homeNav,
                showHoldHint = !settings.holdHintSeen,
                onHintSeen = settings::updateHoldHintSeen,
            )
        }
        target == "overview" -> {
            OverviewDashboard(
                goals = shown,
                today = today,
                onOpen = { openId = it.id },
                onWeekly = { inWeekly = true },
                onSettings = { inSettings = true },
                bottomBar = homeNav,
                showBackupPrompt = showBackupPrompt,
                onSetUpBackup = setUpBackup,
                onDismissBackup = { settings.snoozeBackupNudge() },
            )
        }
        target == "weekly" -> {
            WeeklyReviewScreen(shown, today, onOpen = { openId = it.id }, onBack = { inWeekly = false })
        }
        target == "settings" -> {
            SettingsScreen(
                store, settings, sync, lock,
                privateTitles = PrivateTitlesUi(
                    hasPin = privacy.hasPin,
                    unlocked = vault.unlocked,
                    onSetUp = requestPin,
                    onChange = { pinFlow = if (vault.unlocked) PinFlow.Change else PinFlow.Unlock },
                    onForget = {
                        // Real names can't be read without the PIN, so private goals keep their cover names.
                        store.updateAll { g -> if (g.isPrivate) g.copy(sealed = null, reason = "") else null }
                        vault.forget()
                    },
                ),
                onShowGuide = {
                    inSettings = false
                    settings.updateGuideSeen(false)
                },
                onBack = { inSettings = false },
            )
        }
        }
    }

    pinFlow?.let { flow ->
        var busy by remember(flow) { mutableStateOf(false) }
        var error by remember(flow) { mutableStateOf<String?>(null) }
        PinDialog(
            title = when (flow) {
                PinFlow.Unlock -> "Show private titles"
                PinFlow.Set -> "Set a PIN for private titles"
                PinFlow.Change -> "New PIN"
            },
            message = when (flow) {
                PinFlow.Unlock -> "Enter your private-titles PIN."
                PinFlow.Set -> "This PIN reveals the real names of private goals. It\u2019s separate from your phone\u2019s lock. " +
                    "If you forget it, the real names can\u2019t be recovered; the cover names stay."
                PinFlow.Change -> "Private goals will be re-sealed with the new PIN."
            },
            confirm = if (flow == PinFlow.Unlock) "Show" else "Save",
            repeat = flow != PinFlow.Unlock,
            minLength = if (flow == PinFlow.Unlock) 4 else PrivateTitles.MIN_NEW_PIN,
            busy = busy,
            error = error,
            onDismiss = { pinFlow = null },
            onConfirm = { pin ->
                scope.launch {
                    busy = true
                    when (flow) {
                        PinFlow.Unlock -> when (val r = vault.unlock(pin, store.snapshot)) {
                            UnlockResult.Unlocked -> pinFlow = null
                            UnlockResult.Wrong -> error = "That PIN didn\u2019t work."
                            is UnlockResult.Wait -> error = "Too many wrong PINs. Try again in " +
                                (if (r.millis < 60_000) "${(r.millis + 999) / 1000} seconds" else "${(r.millis + 59_999) / 60_000} minutes") + "."
                        }
                        PinFlow.Set -> {
                            vault.setPin(pin)
                            pinFlow = null
                        }
                        PinFlow.Change -> {
                            val old = vault.setPin(pin)
                            store.updateAll { g ->
                                val o = vault.open(g, old) ?: return@updateAll null
                                g.copy(sealed = vault.seal(o.title, o.reason, o.notes))
                            }
                            pinFlow = null
                        }
                    }
                    busy = false
                }
            },
        )
    }

    // Celebrate a milestone reached today, once per goal and run.
    val milestoneGoal = shown.firstOrNull { g ->
        val d = g.daysKept(today)
        Milestones.isMilestone(d, g.currentStart) && !settings.isCelebrated(celebrationKey(g, d))
    }
    if (milestoneGoal != null && openGoal == null && settings.celebrations) {
        val d = milestoneGoal.daysKept(today)
        LaunchedEffect(milestoneGoal.id, d) { buzz() }
        MilestoneDialog(milestoneGoal, d, onDismiss = { settings.markCelebrated(celebrationKey(milestoneGoal, d)) })
    }

    when (val state = sync.state) {
        SyncState.ChoosePassphrase -> PassphraseDialog(
            title = "Encrypt your synced ledger?",
            message = "With a passphrase, your goals are encrypted on this phone before upload. " +
                "Google can't read them, and neither can anyone else. You'll need the passphrase " +
                "on each new device. If you forget it, the synced copy can't be recovered.",
            confirm = "Encrypt",
            dismiss = "Don't encrypt",
            needsConfirmation = true,
            busy = sync.busy,
            onConfirm = { sync.choosePassphrase(it) },
            onDismiss = { sync.choosePassphrase(null) },
        )
        is SyncState.EnterPassphrase -> PassphraseDialog(
            title = "Enter sync passphrase",
            message = "Your synced ledger is encrypted. Enter the passphrase you chose on your other device.",
            error = if (state.wrongAttempt) "That passphrase didn't work." else null,
            confirm = "Unlock",
            dismiss = "Disconnect",
            needsConfirmation = false,
            busy = sync.busy,
            onConfirm = { sync.submitPassphrase(it) },
            onDismiss = { sync.disconnect(deleteCloudCopy = false) },
        )
        SyncState.EncryptionRemoved -> AlertDialog(
            onDismissRequest = {},
            title = { Text("Synced copy isn\u2019t encrypted") },
            text = {
                Text(
                    "The copy of your ledger in Google Drive is no longer encrypted. If you turned encryption off " +
                        "on another device, keep it off. If you didn\u2019t, encrypt it again: this phone still has " +
                        "your key and will replace the copy with an encrypted one.",
                )
            },
            confirmButton = { TextButton(onClick = sync::restoreEncryption, enabled = !sync.busy) { Text("Encrypt again") } },
            dismissButton = { TextButton(onClick = sync::acceptEncryptionRemoved, enabled = !sync.busy) { Text("Keep it off") } },
        )
        else -> Unit
    }
}

@Composable
fun PassphraseDialog(
    title: String,
    message: String,
    confirm: String,
    dismiss: String,
    needsConfirmation: Boolean,
    busy: Boolean,
    onConfirm: (CharArray) -> Unit,
    onDismiss: () -> Unit,
    error: String? = null,
) {
    var pass by remember { mutableStateOf("") }
    var again by remember { mutableStateOf("") }
    val valid = pass.length >= MIN_PASSPHRASE && (!needsConfirmation || pass == again)

    AlertDialog(
        // Must be answered explicitly; tapping outside does nothing.
        onDismissRequest = {},
        title = { Text(title) },
        text = {
            Column {
                Text(message, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = pass,
                    onValueChange = { pass = it },
                    label = { Text("Passphrase") },
                    supportingText = { if (needsConfirmation) Text("At least $MIN_PASSPHRASE characters") },
                    isError = error != null,
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth(),
                )
                if (needsConfirmation) {
                    OutlinedTextField(
                        value = again,
                        onValueChange = { again = it },
                        label = { Text("Repeat passphrase") },
                        isError = again.isNotEmpty() && again != pass,
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                if (error != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(pass.toCharArray()) }, enabled = valid && !busy) {
                Text(if (busy) "Working…" else confirm)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !busy) { Text(dismiss) }
        },
    )
}

private fun celebrationKey(goal: Goal, days: Long) = "${goal.id}:${goal.currentStart.toEpochDay()}:$days"

@Composable
private fun MilestoneDialog(goal: Goal, days: Long, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(plural(days, "day"), style = MaterialTheme.typography.displaySmall)
                Milestones.name(days, goal.currentStart)?.let {
                    Text(it, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
        text = {
            Column {
                Text(
                    "${goal.title}: ${plural(days, "day")} kept, since ${goal.currentStart.format(mediumDate)}.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(20.dp))
                QuoteBlock(Quotes.forMilestone(goal, days))
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Onward") } },
    )
}

private enum class PinFlow { Unlock, Set, Change }
