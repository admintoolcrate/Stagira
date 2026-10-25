package com.stagirite.ledger.security

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.stagirite.ledger.data.Goal
import com.stagirite.ledger.data.LedgerCodec
import java.time.LocalDate
import com.stagirite.ledger.sync.Envelope
import com.stagirite.ledger.sync.SyncKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.util.Base64

/**
 * Private titles: a goal can show a cover name ("No More Corn") while its real
 * name and note are stored encrypted (AES-256-GCM, key derived from a PIN with
 * PBKDF2). They're shown only after the PIN is entered, and hidden again when
 * the app is left.
 *
 * This keeps the real names from anyone who picks up the phone or sees the
 * synced file. A short PIN can't stand up to a determined attacker who has the
 * file and time to guess; for that, also turn on sync encryption.
 */
sealed interface UnlockResult {
    data object Unlocked : UnlockResult
    data object Wrong : UnlockResult
    data class Wait(val millis: Long) : UnlockResult
}

/** What a private goal hides: its real title, its note, and notes on its slips. */
data class Opened(val title: String, val reason: String, val notes: Map<LocalDate, String> = emptyMap())

class PrivateTitles(context: Context) {
    private val prefs = context.getSharedPreferences("private", Context.MODE_PRIVATE)
    private val b64 = Base64.getEncoder()
    private val unb64 = Base64.getDecoder()
    private var key: SyncKey? = null
    private val opened = HashMap<String, Opened>()

    var unlocked by mutableStateOf(false)
        private set

    /** True once a PIN has been set on this device. */
    var hasPin by mutableStateOf(prefs.contains("check"))
        private set

    /** Milliseconds until another PIN attempt is allowed; 0 when one is allowed now. */
    fun waitMillis(): Long = maxOf(0, prefs.getLong("lockedUntil", 0) - System.currentTimeMillis())

    /**
     * Unlocks with [pin]. On a new device, the PIN is checked against [goals] synced
     * from elsewhere. After [FREE_ATTEMPTS] wrong PINs, each further one adds a wait
     * that doubles (30 s, 1 min, 2 min … up to an hour), so a PIN can't be guessed by
     * trying them all.
     */
    suspend fun unlock(pin: String, goals: List<Goal>): UnlockResult {
        if (waitMillis() > 0) return UnlockResult.Wait(waitMillis())
        val candidate = withContext(Dispatchers.Default) {
            // A damaged stored or synced value counts as a wrong PIN, never a crash.
            runCatching {
                if (hasPin) {
                    val k = Envelope.deriveKey(
                        pin.toCharArray(),
                        unb64.decode(prefs.getString("salt", "")),
                        prefs.getInt("iterations", Envelope.ITERATIONS),
                    )
                    k.takeIf { (Envelope.read(prefs.getString("check", "")!!) as Envelope.Sealed).open(it) == "ledger" }
                } else {
                    val sample = goals.firstNotNullOfOrNull { g -> g.sealed?.let { runCatching { Envelope.read(it) as? Envelope.Sealed }.getOrNull() } }
                        ?: return@runCatching null
                    Envelope.deriveKey(pin.toCharArray(), sample.salt, sample.iterations)
                        .takeIf { k -> runCatching { sample.open(k) }.isSuccess }
                }
            }.getOrNull()
        }
        if (candidate == null) {
            val failures = prefs.getInt("failures", 0) + 1
            val wait = pinBackoffMillis(failures, FREE_ATTEMPTS)
            prefs.edit().putInt("failures", failures).putLong("lockedUntil", System.currentTimeMillis() + wait).apply()
            return if (wait > 0) UnlockResult.Wait(wait) else UnlockResult.Wrong
        }
        if (!hasPin) remember(candidate)
        prefs.edit().putInt("failures", 0).putLong("lockedUntil", 0).apply()
        key = candidate
        unlocked = true
        return UnlockResult.Unlocked
    }

    /** Sets a new PIN and unlocks with it. Returns the previous key, for re-sealing. */
    suspend fun setPin(pin: String): SyncKey? {
        val fresh = withContext(Dispatchers.Default) { Envelope.deriveKey(pin.toCharArray()) }
        val old = key
        remember(fresh)
        key = fresh
        unlocked = true
        opened.clear()
        return old
    }

    fun lock() {
        key = null
        unlocked = false
        opened.clear()
    }

    /** Removes the PIN from this device. Sealed titles stay sealed until it's set again. */
    fun forget() {
        val failures = prefs.getInt("failures", 0)
        val until = prefs.getLong("lockedUntil", 0)
        prefs.edit().clear().putInt("failures", failures).putLong("lockedUntil", until).apply()
        hasPin = false
        lock()
    }

    fun seal(title: String, note: String, notes: Map<LocalDate, String> = emptyMap(), with: SyncKey? = key): String =
        Envelope.write(
            JSONObject()
                .put("t", title.trim())
                .put("r", note.trim())
                .put("n", LedgerCodec.encodeNotes(notes))
                .toString(),
            requireNotNull(with),
        )

    /** The real title, note and slip notes, if unlocked and they open with the current key. */
    fun open(goal: Goal, with: SyncKey? = key): Opened? {
        val sealed = goal.sealed ?: return null
        val k = with ?: return null
        if (k === key) opened[sealed]?.let { return it }
        val text = runCatching { (Envelope.read(sealed) as Envelope.Sealed).open(k) }.getOrNull() ?: return null
        val o = JSONObject(text)
        return Opened(o.optString("t"), o.optString("r"), LedgerCodec.decodeNotes(o.optJSONObject("n")))
            .also { if (k === key) opened[sealed] = it }
    }

    /** The goal as it should be shown: real title and note when unlocked, cover otherwise. */
    fun reveal(goal: Goal): Goal {
        if (!unlocked) return goal
        val o = open(goal) ?: return goal
        return goal.copy(title = o.title, reason = o.reason, notes = o.notes)
    }

    companion object {
        const val FREE_ATTEMPTS = 5
        /** For new PINs; older 4-digit ones still unlock. */
        const val MIN_NEW_PIN = 6
    }

    private fun remember(k: SyncKey) {
        prefs.edit()
            .putString("salt", b64.encodeToString(k.salt))
            .putInt("iterations", k.iterations)
            .putString("check", Envelope.write("ledger", k))
            .apply()
        hasPin = true
    }
}
