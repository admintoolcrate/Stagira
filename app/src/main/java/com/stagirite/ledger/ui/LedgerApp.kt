package com.stagirite.ledger.ui

import androidx.activity.compose.BackHandler
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
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import com.stagirite.ledger.data.Settings
import com.stagirite.ledger.security.AppLock
import com.stagirite.ledger.security.PrivateTitles
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
fun LedgerApp(store: GoalStore, settings: Settings, sync: SyncManager, lock: AppLock, vault: PrivateTitles) {
    val today by rememberToday()
    var tab by rememberSaveable { mutableStateOf(Tab.Goals) }
    var openId by rememberSaveable { mutableStateOf<String?>(null) }
    var pinFlow by remember { mutableStateOf<PinFlow?>(null) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val navBar: @Composable () -> Unit = { LedgerNavBar(tab) { tab = it } }

    // Goals as they should be shown: real titles only while private titles are unlocked.
    val shown = sortGoals(store.goals.map(vault::reveal), settings.sortOrder, today)
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current

    // Keep the home-screen widget in step with the lock and sort order.
    LaunchedEffect(settings.appLock, settings.sortOrder) { runCatching { LedgerWidget.refresh(context) } }
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
        else -> vault.seal(draft.realTitle, draft.reason)
    }

    if (!settings.guideSeen) {
        GuideScreen(onDone = { settings.updateGuideSeen(true) })
        return
    }

    if (openGoal != null && rawOpen != null) {
        BackHandler { openId = null }
        GoalDetailScreen(
            goal = openGoal,
            cover = rawOpen.title,
            today = today,
            privacy = privacy,
            onBack = { openId = null },
            onEdit = { d ->
                store.edit(rawOpen.id, d.title, d.since, d.reason, d.icon, d.kind, sealedFor(d, rawOpen), d.target)
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
                tab = Tab.Goals
                if (removed != null) {
                    scope.launch {
                        val result = snackbar.showSnackbar("Goal deleted", actionLabel = "Undo", duration = SnackbarDuration.Long)
                        if (result == SnackbarResult.ActionPerformed) store.restore(removed)
                    }
                }
            },
        )
    } else {
        if (tab != Tab.Goals) BackHandler { tab = Tab.Goals }
        when (tab) {
            Tab.Goals -> GoalListScreen(
                goals = shown,
                today = today,
                snackbar = snackbar,
                privacy = privacy,
                archived = store.archived.map(vault::reveal),
                sortOrder = settings.sortOrder,
                onSort = settings::updateSortOrder,
                onOpen = { openId = it.id },
                onAdd = { d -> store.add(d.title, d.since, d.reason, d.icon, d.kind, sealedFor(d, null), d.target) },
                onQuickSlip = { g ->
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    store.recordSlip(g.id, today)
                    scope.launch {
                        val r = snackbar.showSnackbar("${g.slipWord.replaceFirstChar { it.uppercase() }} logged for today", actionLabel = "Undo")
                        if (r == SnackbarResult.ActionPerformed) store.removeSlip(g.id, today)
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
                bottomBar = navBar,
            )
            Tab.Overview -> OverviewScreen(shown, today, onOpen = { openId = it.id }, bottomBar = navBar)
            Tab.Settings -> SettingsScreen(
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
                onShowGuide = { settings.updateGuideSeen(false) },
                bottomBar = navBar,
            )
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
            busy = busy,
            error = error,
            onDismiss = { pinFlow = null },
            onConfirm = { pin ->
                scope.launch {
                    busy = true
                    when (flow) {
                        PinFlow.Unlock -> {
                            if (vault.unlock(pin, store.snapshot)) pinFlow = null else error = "That PIN didn\u2019t work."
                        }
                        PinFlow.Set -> {
                            vault.setPin(pin)
                            pinFlow = null
                        }
                        PinFlow.Change -> {
                            val old = vault.setPin(pin)
                            store.updateAll { g ->
                                val (t, r) = vault.open(g, old) ?: return@updateAll null
                                g.copy(sealed = vault.seal(t, r))
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
        Milestones.isMilestone(d) && !settings.isCelebrated(celebrationKey(g, d))
    }
    if (milestoneGoal != null && openGoal == null) {
        val d = milestoneGoal.daysKept(today)
        LaunchedEffect(milestoneGoal.id, d) { haptics.performHapticFeedback(HapticFeedbackType.LongPress) }
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
                Milestones.name(days)?.let {
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
