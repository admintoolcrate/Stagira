package com.stagirite.ledger.backup

import android.content.Context
import android.net.Uri
import com.stagirite.ledger.data.GoalStore
import com.stagirite.ledger.data.LedgerCodec
import com.stagirite.ledger.data.Settings

/**
 * A copy of the ledger kept in a file the user chose (on this phone, in their
 * Drive, wherever their file picker reaches), rewritten whenever they leave the
 * app after a change. It's the same format as Export, so restoring on a new
 * phone is Settings → Import. Free, and independent of sync.
 */
object AutoBackup {
    /** Everything worth keeping: active and archived goals, not deleted ones. */
    fun contents(store: GoalStore): String = LedgerCodec.encode(store.goals + store.archived)

    /** Writes the backup if one is set up and the ledger changed since the last. */
    @Synchronized
    fun runIfNeeded(context: Context, store: GoalStore, settings: Settings) {
        val uri = settings.backupUri?.let(Uri::parse) ?: return
        val text = contents(store)
        val hash = text.hashCode()
        if (hash == settings.backupHash) return
        val ok = runCatching {
            context.contentResolver.openOutputStream(uri, "wt")!!.use { it.write(text.toByteArray()) }
        }.isSuccess
        // A file that's gone (deleted, or its permission revoked) turns the backup off rather than failing quietly forever.
        if (ok) settings.markBackedUp(hash) else settings.updateBackupUri(null, failed = true)
    }
}
