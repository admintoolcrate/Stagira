package com.stagirite.ledger.ui

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.stagirite.ledger.feedback.Feedback
import com.stagirite.ledger.feedback.Report
import com.stagirite.ledger.feedback.ReportKind
import com.stagirite.ledger.security.AppLock

/**
 * Writes a bug report or feature request and hands it to GitHub or the
 * reporter's email app. Nothing about their goals is ever included.
 */
@Composable
fun FeedbackDialog(
    kind: ReportKind,
    lock: AppLock,
    onDismiss: () -> Unit,
    /** Filled in already, e.g. after a crash; the person can still edit or remove it. */
    initialTitle: String = "",
    initialDetails: String = "",
) {
    val context = LocalContext.current
    val device = remember { Feedback.deviceSummary(context) }
    var title by rememberSaveable { mutableStateOf(initialTitle) }
    var details by rememberSaveable { mutableStateOf(initialDetails) }
    var steps by rememberSaveable { mutableStateOf("") }
    var expected by rememberSaveable { mutableStateOf("") }
    var includeDevice by rememberSaveable { mutableStateOf(kind == ReportKind.Bug) }
    val bug = kind == ReportKind.Bug

    val report = Report(
        kind = kind,
        title = title,
        details = details,
        steps = if (bug) steps else "",
        expected = if (bug) expected else "",
        device = device.takeIf { includeDevice },
    )
    val ready = title.isNotBlank() && details.isNotBlank()

    fun send(open: (android.content.Context, Report) -> Boolean, failure: String) {
        lock.expectReturn()
        if (open(context, report)) onDismiss() else Toast.makeText(context, failure, Toast.LENGTH_LONG).show()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(kind.label) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    if (bug) "Tell us what went wrong. Your goals, notes and dates are never included."
                    else "What would make the ledger better for you? Your goals are never included.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                Field(title, { title = it }, if (bug) "Short summary" else "Idea in a few words", singleLine = true)
                Field(details, { details = it }, if (bug) "What happened?" else "What would you like, and why?", lines = 3)
                if (bug) {
                    Field(steps, { steps = it }, "Steps to make it happen (optional)", lines = 2)
                    Field(expected, { expected = it }, "What you expected instead (optional)", lines = 2)
                }
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { includeDevice = !includeDevice },
                    verticalAlignment = Alignment.Top,
                ) {
                    Checkbox(checked = includeDevice, onCheckedChange = { includeDevice = it })
                    Column(Modifier.padding(top = 12.dp)) {
                        Text("Include device details", style = MaterialTheme.typography.bodyMedium)
                        Text(device, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Spacer(Modifier.height(16.dp))
                Text("Send with", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { send(Feedback::openGitHub, "Couldn’t open a browser.") },
                        enabled = ready,
                    ) { Text("GitHub") }
                    OutlinedButton(
                        onClick = { send(Feedback::openEmail, "No email app found.") },
                        enabled = ready,
                    ) { Text("Email") }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    "GitHub opens a new issue in your browser (you’ll need a GitHub account). Email opens your " +
                        "email app. Either way you can check the text before it’s sent.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun Field(value: String, onChange: (String) -> Unit, label: String, singleLine: Boolean = false, lines: Int = 1) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = singleLine,
        minLines = if (singleLine) 1 else lines,
        maxLines = if (singleLine) 1 else 8,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
    )
}
