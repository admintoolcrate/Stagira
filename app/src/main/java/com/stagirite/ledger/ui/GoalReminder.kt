package com.stagirite.ledger.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.text.format.DateFormat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.stagirite.ledger.LedgerApplication
import com.stagirite.ledger.reminders.Reminder
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * "Remind me at 8:00 pm" on a goal's page: a daily nudge for this goal alone,
 * on this phone. It skips rest days and days already logged as a slip.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalReminderButton(goalId: String) {
    val context = LocalContext.current
    val app = context.applicationContext as LedgerApplication
    val settings = app.settings
    val minutes = settings.goalReminders[goalId]
    var picking by rememberSaveable { mutableStateOf(false) }
    // The time chosen while the notification permission is being asked for.
    var pendingMinutes by rememberSaveable { mutableStateOf<Int?>(null) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        val chosen = pendingMinutes
        if (granted && chosen != null) Reminder.setGoal(context, settings, goalId, chosen)
        pendingMinutes = null
    }
    val formatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)

    TextButton(onClick = { picking = true }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Outlined.Notifications,
                contentDescription = null,
                modifier = Modifier.padding(end = 6.dp).size(14.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                if (minutes == null) "Add a reminder"
                else "Reminder at ${LocalTime.of(minutes / 60, minutes % 60).format(formatter)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    if (picking) {
        val start = LocalTime.of((minutes ?: settings.reminderMinutes) / 60, (minutes ?: settings.reminderMinutes) % 60)
        val state = rememberTimePickerState(
            initialHour = start.hour,
            initialMinute = start.minute,
            is24Hour = DateFormat.is24HourFormat(context),
        )
        AlertDialog(
            onDismissRequest = { picking = false },
            title = { Text("Remind me about this goal") },
            text = { TimePicker(state = state) },
            confirmButton = {
                TextButton(onClick = {
                    picking = false
                    val chosen = state.hour * 60 + state.minute
                    if (Build.VERSION.SDK_INT >= 33 &&
                        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                    ) {
                        pendingMinutes = chosen
                        app.lock.expectReturn()
                        permission.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        Reminder.setGoal(context, settings, goalId, chosen)
                    }
                }) { Text("Save") }
            },
            dismissButton = {
                Row {
                    if (minutes != null) {
                        TextButton(onClick = {
                            picking = false
                            Reminder.setGoal(context, settings, goalId, null)
                        }) { Text("Remove") }
                    }
                    TextButton(onClick = { picking = false }) { Text("Cancel") }
                }
            },
        )
    }
}
