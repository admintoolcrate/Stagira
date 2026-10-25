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
import com.stagirite.ledger.data.Settings
import com.stagirite.ledger.security.AppLock
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
fun LedgerApp(store: GoalStore, settings: Settings, sync: SyncManager, lock: AppLock) {
    val today by rememberToday()
    var tab by rememberSaveable { mutableStateOf(Tab.Goals) }
    var openId by rememberSaveable { mutableStateOf<String?>(null) }
    val openGoal = openId?.let(store::find)
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val navBar: @Composable () -> Unit = { LedgerNavBar(tab) { tab = it } }

    if (openGoal != null) {
        BackHandler { openId = null }
        GoalDetailScreen(
            goal = openGoal,
            today = today,
            onBack = { openId = null },
            onEdit = { title, since, reason -> store.edit(openGoal.id, title, since, reason) },
            onSlip = { store.recordSlip(openGoal.id, it) },
            onRemoveSlip = { store.removeSlip(openGoal.id, it) },
            onMoveStart = { store.moveCurrentStart(openGoal.id, it) },
            onDelete = {
                val removed = store.delete(openGoal.id)
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
                goals = store.goals,
                today = today,
                snackbar = snackbar,
                onOpen = { openId = it.id },
                onAdd = store::add,
                bottomBar = navBar,
            )
            Tab.Overview -> OverviewScreen(store.goals, today, onOpen = { openId = it.id }, bottomBar = navBar)
            Tab.Settings -> SettingsScreen(store, settings, sync, lock, bottomBar = navBar)
        }
    }

    // Celebrate a milestone reached today, once per goal and run.
    val milestoneGoal = store.goals.firstOrNull { g ->
        val d = g.daysKept(today)
        Milestones.isMilestone(d) && !settings.isCelebrated(celebrationKey(g, d))
    }
    if (milestoneGoal != null && openGoal == null) {
        val d = milestoneGoal.daysKept(today)
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
