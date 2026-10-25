package com.stagirite.ledger.sync

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Scope
import com.stagirite.ledger.data.GoalStore
import com.stagirite.ledger.data.LedgerCodec
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.IOException
import java.security.GeneralSecurityException

sealed interface SyncState {
    /** Not linked to a Google account. Everything stays on this device. */
    data object Off : SyncState
    data object Idle : SyncState
    /** Google needs the user to sign in again before we can sync. */
    data object NeedsSignIn : SyncState
    /** First connection and nothing in Drive yet: ask whether to encrypt. */
    data object ChoosePassphrase : SyncState
    /** The copy in Drive is encrypted and this device doesn't have the key. */
    data class EnterPassphrase(val wrongAttempt: Boolean) : SyncState
    data class Failed(val message: String) : SyncState
}

/**
 * Keeps the ledger in step with a copy in the user's Google Drive app data folder.
 *
 * Each sync downloads the remote copy, merges it with the local one goal by goal,
 * and uploads the result if it differs. If the user set a passphrase, only an
 * encrypted copy ever leaves the device.
 */
class SyncManager(
    private val context: Context,
    private val store: GoalStore,
    private val scope: CoroutineScope,
) {
    private val prefs = context.getSharedPreferences("sync", Context.MODE_PRIVATE)
    private val vault = KeyVault(context)
    private val mutex = Mutex()
    private var debounce: Job? = null
    private var sealedRemote: Envelope.Sealed? = null

    private val request = AuthorizationRequest.builder()
        .setRequestedScopes(listOf(Scope(DriveClient.SCOPE)))
        .build()

    private val connected get() = prefs.getBoolean("connected", false)

    var state by mutableStateOf<SyncState>(if (connected) SyncState.Idle else SyncState.Off)
        private set
    var busy by mutableStateOf(false)
        private set
    var email by mutableStateOf(prefs.getString("email", null))
        private set
    var lastSynced by mutableStateOf(prefs.getLong("lastSynced", 0).takeIf { it > 0 })
        private set
    var encrypted by mutableStateOf(vault.load() != null)
        private set

    /** Why the last attempt to connect failed, while still not connected. */
    var connectError by mutableStateOf<String?>(null)
        private set

    /**
     * Asks Google for access to the app data folder. Returns an [IntentSender] when
     * Google needs to show its account picker / consent screen; pass the result
     * of that to [onAuthorizationResult].
     */
    suspend fun connect(activity: Activity): IntentSender? = try {
        connectError = null
        val result = Identity.getAuthorizationClient(activity).authorize(request).await()
        if (result.hasResolution()) {
            result.pendingIntent?.intentSender
        } else {
            onAuthorized(result.accessToken)
            null
        }
    } catch (e: Exception) {
        connectFailed(e)
        null
    }

    fun onAuthorizationResult(activity: Activity, data: Intent?) {
        try {
            val result = Identity.getAuthorizationClient(activity).getAuthorizationResultFromIntent(data)
            onAuthorized(result.accessToken)
        } catch (e: ApiException) {
            if (e.statusCode != CommonStatusCodes.CANCELED) connectFailed(e)
        }
    }

    /** Syncs after [delayMs], replacing any sync already waiting. */
    fun requestSync(delayMs: Long = 0) {
        if (!connected) return
        if (state is SyncState.ChoosePassphrase || state is SyncState.EnterPassphrase) return
        debounce?.cancel()
        debounce = scope.launch {
            delay(delayMs)
            scope.launch { sync() }
        }
    }

    /** Answer to [SyncState.ChoosePassphrase]: null means don't encrypt. */
    fun choosePassphrase(passphrase: CharArray?) {
        scope.launch {
            busy = true
            if (passphrase != null) {
                vault.save(withContext(Dispatchers.Default) { Envelope.deriveKey(passphrase) })
            } else {
                vault.clear()
            }
            prefs.edit().putBoolean("setupPending", false).apply()
            state = SyncState.Idle
            sync()
        }
    }

    /** Answer to [SyncState.EnterPassphrase]. */
    fun submitPassphrase(passphrase: CharArray) {
        val remote = sealedRemote ?: return
        scope.launch {
            busy = true
            val key = withContext(Dispatchers.Default) {
                Envelope.deriveKey(passphrase, remote.salt, remote.iterations)
                    .takeIf { k -> runCatching { remote.open(k) }.isSuccess }
            }
            if (key == null) {
                busy = false
                state = SyncState.EnterPassphrase(wrongAttempt = true)
                return@launch
            }
            vault.save(key)
            sealedRemote = null
            state = SyncState.Idle
            sync()
        }
    }

    /** Sets, changes (non-null) or removes (null) encryption of the synced copy. */
    fun changePassphrase(passphrase: CharArray?) {
        scope.launch {
            sync() // Bring in anything new under the old key first.
            if (state != SyncState.Idle) return@launch
            busy = true
            if (passphrase != null) {
                vault.save(withContext(Dispatchers.Default) { Envelope.deriveKey(passphrase) })
            } else {
                vault.clear()
            }
            mutex.withLock { guarded { token -> upload(DriveClient(token), overwrite = true) } }
        }
    }

    /** Unlinks the Google account, optionally deleting the copy in Drive first. */
    fun disconnect(deleteCloudCopy: Boolean) {
        debounce?.cancel()
        scope.launch {
            mutex.withLock {
                busy = true
                val token = runCatching { silentToken() }.getOrNull()
                if (token != null) {
                    withContext(Dispatchers.IO) {
                        if (deleteCloudCopy) runCatching {
                            val drive = DriveClient(token)
                            drive.findFile()?.let(drive::delete)
                        }
                        DriveClient.revoke(token)
                    }
                }
                prefs.edit().clear().apply()
                vault.clear()
                sealedRemote = null
                email = null
                lastSynced = null
                encrypted = false
                state = SyncState.Off
                busy = false
            }
        }
    }

    private fun onAuthorized(token: String?) {
        if (token == null) {
            connectFailed(null)
            return
        }
        connectError = null
        if (!connected) prefs.edit().putBoolean("connected", true).putBoolean("setupPending", true).apply()
        state = SyncState.Idle
        scope.launch { sync(token) }
    }

    /** Before the first connection, a failure leaves sync off and says why. */
    private fun connectFailed(e: Exception?) {
        val message = e?.let(::describe) ?: "Google didn't grant access."
        if (connected) state = SyncState.Failed(message) else connectError = message
    }

    private suspend fun sync(givenToken: String? = null) = mutex.withLock {
        if (!connected) {
            busy = false
            return@withLock
        }
        guarded(givenToken) { token ->
            val drive = DriveClient(token)
            if (email == null) {
                email = withContext(Dispatchers.IO) { drive.email() }
                prefs.edit().putString("email", email).apply()
            }
            val fileId = withContext(Dispatchers.IO) { drive.findFile() }
            if (fileId == null) {
                if (prefs.getBoolean("setupPending", false)) {
                    state = SyncState.ChoosePassphrase
                    return@guarded
                }
                upload(drive, overwrite = false)
                return@guarded
            }
            prefs.edit().putBoolean("setupPending", false).apply()

            val remote = Envelope.read(withContext(Dispatchers.IO) { drive.download(fileId) })
            var key = vault.load()
            val remoteText = when (remote) {
                // Another device turned encryption off: follow it.
                is Envelope.Plain -> remote.ledger.also { vault.clear(); key = null }
                is Envelope.Sealed -> {
                    val opened = key?.takeIf { remote.matches(it) }?.let { k ->
                        try {
                            withContext(Dispatchers.Default) { remote.open(k) }
                        } catch (_: GeneralSecurityException) {
                            null
                        }
                    }
                    if (opened == null) {
                        // New device, or the passphrase was changed elsewhere.
                        vault.clear()
                        encrypted = true
                        sealedRemote = remote
                        state = SyncState.EnterPassphrase(wrongAttempt = false)
                        return@guarded
                    }
                    opened
                }
            }
            val remoteGoals = LedgerCodec.decode(remoteText)
            store.mergeIn(remoteGoals)
            if (store.snapshot != remoteGoals) {
                val body = Envelope.write(LedgerCodec.encode(store.snapshot), key)
                withContext(Dispatchers.IO) { drive.update(fileId, body) }
            }
            synced(encryptedNow = key != null)
        }
    }

    /** Writes the local ledger to Drive, creating the file if needed. */
    private suspend fun upload(drive: DriveClient, overwrite: Boolean) {
        val key = vault.load()
        val body = Envelope.write(LedgerCodec.encode(store.snapshot), key)
        withContext(Dispatchers.IO) {
            val id = if (overwrite) drive.findFile() else null
            if (id != null) drive.update(id, body) else drive.create(body)
        }
        synced(encryptedNow = key != null)
    }

    private fun synced(encryptedNow: Boolean) {
        encrypted = encryptedNow
        lastSynced = System.currentTimeMillis()
        prefs.edit().putLong("lastSynced", lastSynced!!).apply()
        state = SyncState.Idle
    }

    /** Runs [block] with an access token, turning failures into [state]. */
    private suspend fun guarded(givenToken: String? = null, block: suspend (String) -> Unit) {
        busy = true
        try {
            val token = givenToken ?: silentToken()
            if (token == null) {
                state = SyncState.NeedsSignIn
                return
            }
            block(token)
        } catch (e: Exception) {
            state = SyncState.Failed(describe(e))
        } finally {
            busy = false
        }
    }

    /** A fresh access token without showing any UI, or null if the user must sign in again. */
    private suspend fun silentToken(): String? {
        val result = Identity.getAuthorizationClient(context).authorize(request).await()
        return if (result.hasResolution()) null else result.accessToken
    }

    private fun describe(e: Exception): String = when {
        e is ApiException && e.statusCode == CommonStatusCodes.DEVELOPER_ERROR ->
            "Google doesn't recognise this app yet. It has to be registered with Google once " +
                "before sync can work (see \u201CSetting up Google sign-in\u201D in the README)."
        e is ApiException && e.statusCode == CommonStatusCodes.NETWORK_ERROR ->
            "No connection. Will try again later."
        e is DriveException && (e.code == 401 || e.code == 403) ->
            "Google Drive refused access. Try reconnecting."
        e is IOException -> "Couldn't reach Google Drive. Will try again later."
        else -> e.message ?: "Sync failed."
    }
}
