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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.stagirite.ledger.data.GoalStore
import com.stagirite.ledger.data.Settings
import com.stagirite.ledger.sync.SyncManager
import com.stagirite.ledger.sync.SyncState
import kotlinx.coroutines.delay
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
fun LedgerApp(store: GoalStore, settings: Settings, sync: SyncManager) {
    val today by rememberToday()
    // "list", "settings", or the id of an open goal.
    var screen by rememberSaveable { mutableStateOf("list") }
    val openGoal = store.find(screen)

    when {
        screen == "settings" -> {
            BackHandler { screen = "list" }
            SettingsScreen(store, settings, sync, onBack = { screen = "list" })
        }
        openGoal != null -> {
            BackHandler { screen = "list" }
            GoalDetailScreen(
                goal = openGoal,
                today = today,
                onBack = { screen = "list" },
                onEdit = { title, start -> store.edit(openGoal.id, title, start) },
                onReset = { store.reset(openGoal.id, today) },
                onDelete = {
                    store.delete(openGoal.id)
                    screen = "list"
                },
            )
        }
        else -> GoalListScreen(
            goals = store.goals,
            today = today,
            onOpen = { screen = it.id },
            onAdd = store::add,
            onSettings = { screen = "settings" },
        )
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
