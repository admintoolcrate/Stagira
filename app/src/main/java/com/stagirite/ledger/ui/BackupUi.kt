package com.stagirite.ledger.ui

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.stagirite.ledger.backup.AutoBackup
import com.stagirite.ledger.data.GoalStore
import com.stagirite.ledger.data.Settings
import com.stagirite.ledger.security.AppLock

/**
 * Asks where to keep the automatic backup, remembers the file, and writes the
 * first copy straight away. Returns the action that starts it.
 */
@Composable
fun rememberBackupSetup(store: GoalStore, settings: Settings, lock: AppLock): () -> Unit {
    val context = LocalContext.current
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val kept = runCatching {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
            )
        }.isSuccess
        if (!kept) {
            Toast.makeText(context, "That place can’t be written to later. Try another folder.", Toast.LENGTH_LONG).show()
            return@rememberLauncherForActivityResult
        }
        settings.updateBackupUri(uri.toString())
        AutoBackup.runIfNeeded(context, store, settings)
        Toast.makeText(
            context,
            if (settings.backupUri != null) "Backup is on" else "Couldn’t write the backup there",
            Toast.LENGTH_SHORT,
        ).show()
    }
    return {
        lock.expectReturn()
        picker.launch("stagira-backup.json")
    }
}
