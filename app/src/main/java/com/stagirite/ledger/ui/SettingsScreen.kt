package com.stagirite.ledger.ui

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import android.text.format.DateFormat
import android.text.format.DateUtils
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.remember
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.stagirite.ledger.data.LockDelay
import com.stagirite.ledger.reminders.Reminder
import com.stagirite.ledger.security.AppLock
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.stagirite.ledger.data.GoalStore
import com.stagirite.ledger.data.LedgerCodec
import com.stagirite.ledger.data.Settings
import com.stagirite.ledger.data.ThemeMode
import com.stagirite.ledger.sync.SyncManager
import com.stagirite.ledger.sync.SyncState
import kotlinx.coroutines.launch
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    store: GoalStore,
    settings: Settings,
    sync: SyncManager,
    lock: AppLock,
    bottomBar: @Composable () -> Unit,
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("Settings") }, colors = plainTopBarColors()) },
        bottomBar = bottomBar,
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            Section("Appearance") {
                val modes = ThemeMode.entries
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    modes.forEachIndexed { i, mode ->
                        SegmentedButton(
                            selected = settings.theme == mode,
                            onClick = { settings.updateTheme(mode) },
                            shape = SegmentedButtonDefaults.itemShape(i, modes.size),
                            icon = {},
                            colors = SegmentedButtonDefaults.colors(
                                activeContainerColor = MaterialTheme.colorScheme.primary,
                                activeContentColor = MaterialTheme.colorScheme.onPrimary,
                            ),
                        ) { Text(mode.name) }
                    }
                }
            }

            Section("Lock") { LockSection(settings, lock) }

            Section("Reminder") { ReminderSection(settings, lock) }

            Section("Sync") { SyncSection(sync, lock) }

            Section("Privacy") {
                SwitchRow(
                    title = "Hide in recent apps",
                    detail = if (settings.appLock) "On while the app lock is on."
                    else "Blank the app in the app switcher and block screenshots.",
                    checked = settings.secureWindow,
                    onChange = settings::updateHideInRecents,
                    enabled = !settings.appLock,
                )
                Spacer(Modifier.height(12.dp))
                Note(
                    "Your ledger lives on this phone. There are no ads, no analytics and no accounts " +
                        "of ours. The only time anything leaves the device is if you turn on sync, " +
                        "and then only to your own Google Drive.",
                )
            }

            Section("Your data") { DataSection(store, sync, lock) }

            Section("Sources") {
                Note(
                    "Quotations are from Aristotle, Nicomachean Ethics, translated by H. Rackham " +
                        "(Loeb Classical Library; London: William Heinemann, 1926), as published by " +
                        "the Perseus Digital Library under CC BY-SA 4.0. References give book and " +
                        "chapter, then the standard Bekker page.",
                )
            }

            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
private fun SyncSection(sync: SyncManager, lock: AppLock) {
    val context = LocalContext.current
    val activity = context as Activity
    val scope = rememberCoroutineScope()
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) sync.onAuthorizationResult(activity, result.data)
    }
    val connect: () -> Unit = {
        scope.launch {
            sync.connect(activity)?.let {
                lock.expectReturn()
                launcher.launch(IntentSenderRequest.Builder(it).build())
            }
        }
    }
    var disconnecting by rememberSaveable { mutableStateOf(false) }
    var changingPassphrase by rememberSaveable { mutableStateOf(false) }
    var removingPassphrase by rememberSaveable { mutableStateOf(false) }
    val state = sync.state

    if (state == SyncState.Off) {
        Note(
            "Link a Google account to keep your ledger in step across devices. It's stored in a " +
                "hidden folder in your own Google Drive that only this app can open — the app " +
                "can't see anything else in your Drive. You can add a passphrase so it's " +
                "encrypted before it leaves your phone.",
        )
        sync.connectError?.let {
            Spacer(Modifier.height(12.dp))
            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }
        Spacer(Modifier.height(16.dp))
        OutlinedButton(onClick = connect, enabled = !sync.busy) { Text("Connect Google account") }
        return
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(sync.email ?: "Google account", style = MaterialTheme.typography.bodyLarge)
            val status = when (state) {
                is SyncState.Failed -> state.message
                SyncState.NeedsSignIn -> "Google needs you to sign in again."
                is SyncState.EnterPassphrase -> "Waiting for passphrase."
                else -> sync.lastSynced?.let {
                    "Synced " + DateUtils.getRelativeTimeSpanString(it, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS)
                } ?: "Not synced yet"
            }
            Text(
                status,
                style = MaterialTheme.typography.bodySmall,
                color = if (state is SyncState.Failed) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (sync.busy) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
    }
    Spacer(Modifier.height(12.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (state == SyncState.NeedsSignIn) {
            OutlinedButton(onClick = connect, enabled = !sync.busy) { Text("Sign in") }
        } else {
            OutlinedButton(onClick = { sync.requestSync() }, enabled = !sync.busy) { Text("Sync now") }
        }
        TextButton(onClick = { disconnecting = true }, enabled = !sync.busy) { Text("Disconnect") }
    }

    Spacer(Modifier.height(16.dp))
    SettingRow(
        title = "End-to-end encryption",
        detail = if (sync.encrypted) "On — Google can't read your synced ledger."
        else "Off — add a passphrase to encrypt the synced copy.",
        action = if (sync.encrypted) "Change" else "Turn on",
        enabled = state == SyncState.Idle && !sync.busy,
        onClick = { changingPassphrase = true },
    )
    if (sync.encrypted) {
        TextButton(
            onClick = { removingPassphrase = true },
            enabled = state == SyncState.Idle && !sync.busy,
        ) { Text("Remove passphrase") }
    }

    if (removingPassphrase) {
        AlertDialog(
            onDismissRequest = { removingPassphrase = false },
            title = { Text("Remove encryption?") },
            text = { Text("Your synced ledger will be stored in your Drive without encryption.") },
            confirmButton = {
                TextButton(onClick = {
                    sync.changePassphrase(null)
                    removingPassphrase = false
                }) { Text("Remove") }
            },
            dismissButton = {
                TextButton(onClick = { removingPassphrase = false }) { Text("Cancel") }
            },
        )
    }

    if (changingPassphrase) {
        PassphraseDialog(
            title = if (sync.encrypted) "New passphrase" else "Set a passphrase",
            message = "Your synced ledger will be re-encrypted. Other devices will ask for the " +
                "new passphrase. If you forget it, the synced copy can't be recovered.",
            confirm = "Save",
            dismiss = "Cancel",
            needsConfirmation = true,
            busy = sync.busy,
            onConfirm = {
                sync.changePassphrase(it)
                changingPassphrase = false
            },
            onDismiss = { changingPassphrase = false },
        )
    }

    if (disconnecting) {
        var deleteCloud by rememberSaveable { mutableStateOf(true) }
        AlertDialog(
            onDismissRequest = { disconnecting = false },
            title = { Text("Disconnect Google account?") },
            text = {
                Column {
                    Text(
                        "Your ledger stays on this phone. The app's access to your Google account " +
                            "is revoked.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { deleteCloud = !deleteCloud },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(checked = deleteCloud, onCheckedChange = { deleteCloud = it })
                        Text("Also delete the synced copy from Google Drive", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    sync.disconnect(deleteCloud)
                    disconnecting = false
                }) { Text("Disconnect") }
            },
            dismissButton = {
                TextButton(onClick = { disconnecting = false }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun DataSection(store: GoalStore, sync: SyncManager, lock: AppLock) {
    val context = LocalContext.current
    val exporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val ok = runCatching {
            context.contentResolver.openOutputStream(uri)!!.use {
                it.write(LedgerCodec.encode(store.goals).toByteArray())
            }
        }.isSuccess
        Toast.makeText(context, if (ok) "Exported" else "Couldn't export", Toast.LENGTH_SHORT).show()
    }
    val importer = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val goals = runCatching {
            context.contentResolver.openInputStream(uri)!!.use { LedgerCodec.decode(it.reader().readText()) }
        }.getOrNull()
        if (goals == null) {
            Toast.makeText(context, "That isn't a ledger export", Toast.LENGTH_SHORT).show()
        } else {
            store.mergeIn(goals)
            sync.requestSync()
            Toast.makeText(context, "Imported ${goals.size} goals", Toast.LENGTH_SHORT).show()
        }
    }

    SettingRow(
        title = "Export",
        detail = "Save your goals to a file. The file isn't encrypted.",
        action = "Export",
        onClick = {
            lock.expectReturn()
            exporter.launch("ledger-${LocalDate.now()}.json")
        },
    )
    Spacer(Modifier.height(12.dp))
    SettingRow(
        title = "Import",
        detail = "Merge goals from an exported file.",
        action = "Import",
        onClick = {
            lock.expectReturn()
            importer.launch(arrayOf("application/json", "text/plain", "application/octet-stream"))
        },
    )
}

@Composable
private fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    Spacer(Modifier.height(24.dp))
    SectionHeading(title)
    LedgerCard(content = content)
}

@Composable
private fun Note(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun SettingRow(
    title: String,
    detail: String,
    action: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Note(detail)
        }
        Spacer(Modifier.width(12.dp))
        TextButton(onClick = onClick, enabled = enabled) { Text(action) }
    }
}

@Composable
private fun SwitchRow(
    title: String,
    detail: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
    enabled: Boolean = true,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Note(detail)
        }
        Spacer(Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = onChange, enabled = enabled)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LockSection(settings: Settings, lock: AppLock) {
    val activity = LocalContext.current as FragmentActivity
    val available = remember { AppLock.available(activity) }
    SwitchRow(
        title = "App lock",
        detail = if (available) "Ask for your fingerprint, face or screen lock to open the ledger."
        else "Set up a fingerprint or screen lock in your phone\u2019s settings to use this.",
        checked = settings.appLock,
        enabled = available || settings.appLock,
        onChange = { on ->
            // Confirm it works before relying on it, and before turning it off.
            lock.prompt(activity, if (on) "Turn on app lock" else "Turn off app lock") {
                settings.updateAppLock(on)
            }
        },
    )
    if (settings.appLock) {
        Spacer(Modifier.height(12.dp))
        val delays = LockDelay.entries
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            delays.forEachIndexed { i, delay ->
                SegmentedButton(
                    selected = settings.lockDelay == delay,
                    onClick = { settings.updateLockDelay(delay) },
                    shape = SegmentedButtonDefaults.itemShape(i, delays.size),
                    icon = {},
                    colors = SegmentedButtonDefaults.colors(
                        activeContainerColor = MaterialTheme.colorScheme.primary,
                        activeContentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                ) { Text(delay.label, maxLines = 1) }
            }
        }
        Spacer(Modifier.height(4.dp))
        Note("When to lock after you leave the app.")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReminderSection(settings: Settings, lock: AppLock) {
    val context = LocalContext.current
    var pickingTime by rememberSaveable { mutableStateOf(false) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        settings.updateReminder(granted)
        Reminder.apply(context, settings)
    }
    val time = LocalTime.of(settings.reminderMinutes / 60, settings.reminderMinutes % 60)

    SwitchRow(
        title = "Daily reminder",
        detail = "A quiet nudge once a day. It never names your goals.",
        checked = settings.reminder,
        onChange = { on ->
            if (on && Build.VERSION.SDK_INT >= 33 &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
            ) {
                lock.expectReturn()
                permission.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                settings.updateReminder(on)
                Reminder.apply(context, settings)
            }
        },
    )
    if (settings.reminder) {
        Spacer(Modifier.height(4.dp))
        SettingRow(
            title = "Time",
            detail = time.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)),
            action = "Change",
            onClick = { pickingTime = true },
        )
    }

    if (pickingTime) {
        val state = rememberTimePickerState(
            initialHour = time.hour,
            initialMinute = time.minute,
            is24Hour = DateFormat.is24HourFormat(context),
        )
        AlertDialog(
            onDismissRequest = { pickingTime = false },
            title = { Text("Reminder time") },
            text = { TimePicker(state = state) },
            confirmButton = {
                TextButton(onClick = {
                    settings.updateReminder(true, state.hour * 60 + state.minute)
                    Reminder.apply(context, settings)
                    pickingTime = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { pickingTime = false }) { Text("Cancel") } },
        )
    }
}
