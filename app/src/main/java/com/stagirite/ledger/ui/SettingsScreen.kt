package com.stagirite.ledger.ui

import com.stagirite.ledger.backup.AutoBackup
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ButtonDefaults
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
import com.stagirite.ledger.feedback.ReportKind
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
import com.stagirite.ledger.data.WidgetStyle
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
    privateTitles: PrivateTitlesUi,
    onShowGuide: () -> Unit,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = plainTopBarColors(),
            )
        },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            Section("Theme and widget", group = "Look & feel") {
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
                Spacer(Modifier.height(20.dp))
                Text("Widget background", style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(8.dp))
                val styles = WidgetStyle.entries
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    styles.forEachIndexed { i, style ->
                        SegmentedButton(
                            selected = settings.widgetStyle == style,
                            onClick = { settings.updateWidgetStyle(style) },
                            shape = SegmentedButtonDefaults.itemShape(i, styles.size),
                            icon = {},
                            colors = SegmentedButtonDefaults.colors(
                                activeContainerColor = MaterialTheme.colorScheme.primary,
                                activeContentColor = MaterialTheme.colorScheme.onPrimary,
                            ),
                        ) { Text(style.label) }
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    "Clear drops the card and sets the text in white, for darker wallpapers.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Section("Personalise") { PersonaliseSection(settings) }

            Section("Lock and privacy", group = "Privacy & security") {
                LockSection(settings, lock)
                Spacer(Modifier.height(Space.l))
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
                    "No analytics, no accounts or servers of ours, and no ads unless you choose to watch one " +
                        "under Support. " + when {
                        sync.state == SyncState.Off ->
                            "Your ledger is kept only on this phone. Nothing leaves it unless you turn on sync."
                        sync.encrypted ->
                            "Your ledger is kept on this phone and synced to your Google Drive" +
                                (sync.email?.let { " ($it)" } ?: "") + ", encrypted with your passphrase."
                        else ->
                            "Your ledger is kept on this phone and synced to your Google Drive" +
                                (sync.email?.let { " ($it)" } ?: "") + ". Add a passphrase under Sync to encrypt it."
                    },
                )
            }

            Section("Private titles") { PrivateTitlesSection(privateTitles) }

            Section("Daily and weekly", group = "Reminders") {
                ReminderSection(settings, lock)
                Spacer(Modifier.height(16.dp))
                WeeklySection(settings, lock)
            }

            Section("Sync", group = "Your data") { SyncSection(sync, lock) }

            Section("Backup and export") { DataSection(store, settings, sync, lock) }

            LocalSupport.current?.let { support -> Section("Plus, tips and ads", group = "Support Stagira") { SupportSection(support) } }

            Section("Help & feedback", group = "Help & about") { FeedbackSection(lock) }

            Section("About") { AboutSection(onShowGuide) }

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
        val plus = LocalPlus.current
        OutlinedButton(
            onClick = { if (plus.has) connect() else plus.offer("Sync with your Google Drive is part of Plus.") },
            enabled = !sync.busy,
        ) { Text("Connect Google account") }
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
private fun DataSection(store: GoalStore, settings: Settings, sync: SyncManager, lock: AppLock) {
    val context = LocalContext.current
    val setUpBackup = rememberBackupSetup(store, settings, lock)
    val exporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val ok = runCatching {
            context.contentResolver.openOutputStream(uri)!!.use {
                it.write(AutoBackup.contents(store).toByteArray())
            }
        }.isSuccess
        if (ok) settings.markBackedUp()
        Toast.makeText(context, if (ok) "Exported" else "Couldn't export", Toast.LENGTH_SHORT).show()
    }
    val importer = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val goals = runCatching {
            context.contentResolver.openInputStream(uri)!!.use { input ->
                // A ledger is a few kilobytes; refuse anything absurd rather than run out of memory.
                val bytes = input.readNBytesCompat(MAX_IMPORT_BYTES + 1)
                require(bytes.size <= MAX_IMPORT_BYTES) { "Too large" }
                LedgerCodec.decode(bytes.toString(Charsets.UTF_8))
            }
        }.getOrNull()
        if (goals == null) {
            Toast.makeText(context, "That isn't a ledger export", Toast.LENGTH_SHORT).show()
        } else {
            store.mergeIn(goals)
            sync.requestSync()
            Toast.makeText(context, "Imported ${goals.size} goals", Toast.LENGTH_SHORT).show()
        }
    }

    if (settings.backupUri == null) {
        SettingRow(
            title = "Automatic backup",
            detail = if (settings.backupFailed) "The backup file couldn\u2019t be written any more, so backup stopped. Choose a place again."
            else "Keep a copy in a file you choose, such as in your Drive, updated after each change. On a new phone, restore it with Import.",
            action = "Set up",
            onClick = setUpBackup,
        )
    } else {
        SettingRow(
            title = "Automatic backup is on",
            detail = settings.lastBackupAt.takeIf { it > 0 }?.let {
                "Last saved " + DateUtils.getRelativeTimeSpanString(it, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS)
            } ?: "Saved after your next change.",
            action = "Turn off",
            onClick = { settings.updateBackupUri(null) },
        )
    }
    Spacer(Modifier.height(12.dp))
    SettingRow(
        title = "Export",
        detail = "Save a copy now. Like the backup, the file isn\u2019t encrypted, though private titles stay sealed.",
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
private fun Section(title: String, group: String? = null, content: @Composable ColumnScope.() -> Unit) {
    // A group opens with a spaced heading; the cards inside it follow closely.
    if (group != null) {
        Spacer(Modifier.height(Space.xxl))
        SectionHeading(group)
    } else {
        Spacer(Modifier.height(Space.m))
    }
    LedgerCard {
        Text(title, style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(Space.m))
        content()
    }
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

/** What the settings screen can do with private titles. */
class PrivateTitlesUi(
    val hasPin: Boolean,
    val unlocked: Boolean,
    val onSetUp: () -> Unit,
    val onChange: () -> Unit,
    val onForget: () -> Unit,
)

@Composable
private fun PrivateTitlesSection(ui: PrivateTitlesUi) {
    var forgetting by rememberSaveable { mutableStateOf(false) }
    Note(
        "Give a goal a cover name, like \u201CNo more corn\u201D, and keep its real name and note " +
            "behind a PIN. To show real names, long-press the name \u201CStagira\u201D at the top " +
            "of the Goals screen and enter the PIN; they hide again when you leave the app. " +
            "Real names are stored encrypted, including in sync.",
    )
    Spacer(Modifier.height(12.dp))
    if (!ui.hasPin) {
        OutlinedButton(onClick = ui.onSetUp) { Text("Set a PIN") }
    } else {
        SettingRow(
            title = "PIN",
            detail = if (ui.unlocked) "Private titles are showing." else "Set. Private titles are hidden.",
            action = "Change",
            onClick = ui.onChange,
        )
        TextButton(
            onClick = { forgetting = true },
            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
        ) { Text("Forget PIN") }
    }
    if (forgetting) {
        AlertDialog(
            onDismissRequest = { forgetting = false },
            title = { Text("Forget the PIN?") },
            text = {
                Text(
                    "Private goals keep their cover names and all their days, but their real names and notes " +
                        "are deleted, on every device you sync with. This can\u2019t be undone.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    forgetting = false
                    ui.onForget()
                }) { Text("Forget") }
            },
            dismissButton = { TextButton(onClick = { forgetting = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun AboutSection(onShowGuide: () -> Unit) {
    val context = LocalContext.current
    var showingLicence by rememberSaveable { mutableStateOf(false) }
    val version = remember {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: ""
    }

    Text("The name", style = MaterialTheme.typography.titleSmall)
    Spacer(Modifier.height(4.dp))
    Note(
        "Aristotle (384\u2013322 BC) was born in Stagira, in northern Greece, and so is often called " +
            "\u201Cthe Stagirite\u201D. The app takes its name from his home town. In the Nicomachean " +
            "Ethics he argues that good character isn\u2019t something we\u2019re born with but " +
            "something we build by habit, by doing. A ledger is a book of record: this one records " +
            "the days you keep.",
    )
    Spacer(Modifier.height(16.dp))
    Text("Goals", style = MaterialTheme.typography.titleSmall)
    Spacer(Modifier.height(4.dp))
    Note(
        "Each goal tackles a Vice, something to stop or cut down, or practises a Virtue, something good " +
            "to do each day. Good days count on their own; you only log a slip if one happens, and then begin again: the run " +
            "you had stays in your history, with a note if " +
            "you add one. Milestones mark the way, with a passage from Aristotle at each.",
    )
    Spacer(Modifier.height(12.dp))
    OutlinedButton(onClick = onShowGuide) { Text("Show the guide") }

    Spacer(Modifier.height(20.dp))
    Text("Sources and licences", style = MaterialTheme.typography.titleSmall)
    Spacer(Modifier.height(4.dp))
    Note(
        "Quotations are from Aristotle, Nicomachean Ethics, translated by H. Rackham (Loeb Classical " +
            "Library; London: William Heinemann, 1926), as published by the Perseus Digital Library " +
            "under CC BY-SA 4.0. References give book and chapter, then the standard Bekker page.",
    )
    Spacer(Modifier.height(8.dp))
    Note(
        "Text is set in Inter, \u00A9 2016 The Inter Project Authors, and EB Garamond, \u00A9 2017 The EB " +
            "Garamond Project Authors; icons are drawn with Noto Emoji, \u00A9 2013, 2022 Google. All three " +
            "are under the SIL Open Font License 1.1.",
    )
    TextButton(onClick = { showingLicence = true }) { Text("Read the font licence") }
    if (version.isNotEmpty()) Note("Version $version")

    if (showingLicence) {
        val text = remember {
            runCatching { context.assets.open("licenses/noto-emoji-OFL.txt").bufferedReader().use { it.readText() } }
                .getOrDefault("Licence text unavailable.")
        }
        AlertDialog(
            onDismissRequest = { showingLicence = false },
            title = { Text("SIL Open Font License 1.1") },
            text = {
                Text(
                    text,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                )
            },
            confirmButton = { TextButton(onClick = { showingLicence = false }) { Text("Close") } },
        )
    }
}

@Composable
private fun PersonaliseSection(settings: Settings) {
    val rows: List<Triple<String, String, Pair<Boolean, (Boolean) -> Unit>>> = listOf(
        Triple("Passages from Aristotle", "When you begin again, at milestones, on an empty page and in the guide.", settings.quotes to settings::updateQuotes),
        Triple("Milestone celebrations", "A moment to mark each milestone the day you reach it.", settings.celebrations to settings::updateCelebrations),
        Triple("Vibration", "A light buzz when you log a slip or reach a milestone.", settings.haptics to settings::updateHaptics),
    )
    rows.forEachIndexed { i, (title, detail, state) ->
        SwitchRow(title = title, detail = detail, checked = state.first, onChange = state.second)
        if (i < rows.lastIndex) Spacer(Modifier.height(12.dp))
    }
}

private const val MAX_IMPORT_BYTES = 5 * 1024 * 1024

/** Reads at most [limit] bytes (InputStream.readNBytes needs API 33). */
private fun java.io.InputStream.readNBytesCompat(limit: Int): ByteArray {
    val out = java.io.ByteArrayOutputStream()
    val buffer = ByteArray(16 * 1024)
    while (out.size() < limit) {
        val n = read(buffer, 0, minOf(buffer.size, limit - out.size()))
        if (n < 0) break
        out.write(buffer, 0, n)
    }
    return out.toByteArray()
}

@Composable
private fun FeedbackSection(lock: AppLock) {
    var writing by rememberSaveable { mutableStateOf<ReportKind?>(null) }
    SettingRow(
        title = "Report a bug",
        detail = "Something broken or behaving oddly.",
        action = "Report",
        onClick = { writing = ReportKind.Bug },
    )
    Spacer(Modifier.height(12.dp))
    SettingRow(
        title = "Request a feature",
        detail = "An idea that would make the ledger better.",
        action = "Suggest",
        onClick = { writing = ReportKind.Feature },
    )
    Spacer(Modifier.height(8.dp))
    Note("Sent through GitHub or email, your choice. Reports never include your goals, notes or dates.")
    writing?.let { FeedbackDialog(it, lock, onDismiss = { writing = null }) }
}

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun WeeklySection(settings: Settings, lock: AppLock) {
    val context = LocalContext.current
    val plus = LocalPlus.current
    var pickingTime by rememberSaveable { mutableStateOf(false) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        settings.updateWeekly(granted)
        Reminder.apply(context, settings)
    }
    val time = LocalTime.of(settings.weeklyMinutes / 60, settings.weeklyMinutes % 60)
    SwitchRow(
        title = "Weekly review",
        detail = "A note once a week that your review is ready. It never names your goals.",
        checked = settings.weekly,
        onChange = { on ->
            if (on && !plus.has) {
                plus.offer("The weekly review is part of Plus.")
            } else if (on && Build.VERSION.SDK_INT >= 33 &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
            ) {
                lock.expectReturn()
                permission.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                settings.updateWeekly(on)
                Reminder.apply(context, settings)
            }
        },
    )
    if (settings.weekly) {
        Spacer(Modifier.height(8.dp))
        androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            java.time.DayOfWeek.entries.forEach { d ->
                androidx.compose.material3.FilterChip(
                    selected = settings.weeklyDay == d.value,
                    onClick = {
                        settings.updateWeekly(true, day = d.value)
                        Reminder.apply(context, settings)
                    },
                    label = { Text(d.getDisplayName(java.time.format.TextStyle.SHORT, java.util.Locale.getDefault())) },
                )
            }
        }
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
            title = { Text("Weekly review time") },
            text = { TimePicker(state = state) },
            confirmButton = {
                TextButton(onClick = {
                    settings.updateWeekly(true, minutes = state.hour * 60 + state.minute)
                    Reminder.apply(context, settings)
                    pickingTime = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { pickingTime = false }) { Text("Cancel") } },
        )
    }
}
