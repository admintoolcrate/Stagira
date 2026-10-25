package com.stagirite.ledger.security

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.stagirite.ledger.data.Goal
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
class PrivateTitles(context: Context) {
    private val prefs = context.getSharedPreferences("private", Context.MODE_PRIVATE)
    private val b64 = Base64.getEncoder()
    private val unb64 = Base64.getDecoder()
    private var key: SyncKey? = null
    private val opened = HashMap<String, Pair<String, String>>()

    var unlocked by mutableStateOf(false)
        private set

    /** True once a PIN has been set on this device. */
    var hasPin by mutableStateOf(prefs.contains("check"))
        private set

    /** Unlocks with [pin]. On a new device, the PIN is checked against [goals] synced from elsewhere. */
    suspend fun unlock(pin: String, goals: List<Goal>): Boolean = withContext(Dispatchers.Default) {
        val candidate = if (hasPin) {
            val k = Envelope.deriveKey(pin.toCharArray(), unb64.decode(prefs.getString("salt", "")), prefs.getInt("iterations", Envelope.ITERATIONS))
            k.takeIf { runCatching { (Envelope.read(prefs.getString("check", "")!!) as Envelope.Sealed).open(it) }.isSuccess }
        } else {
            val sample = goals.firstNotNullOfOrNull { it.sealed }?.let { Envelope.read(it) as? Envelope.Sealed }
                ?: return@withContext false
            Envelope.deriveKey(pin.toCharArray(), sample.salt, sample.iterations)
                .takeIf { k -> runCatching { sample.open(k) }.isSuccess }
                ?.also(::remember)
        }
        withContext(Dispatchers.Main) {
            if (candidate != null) {
                key = candidate
                unlocked = true
            }
        }
        candidate != null
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
        prefs.edit().clear().apply()
        hasPin = false
        lock()
    }

    fun seal(title: String, note: String, with: SyncKey? = key): String =
        Envelope.write(JSONObject().put("t", title.trim()).put("r", note.trim()).toString(), requireNotNull(with))

    /** The real title and note, if unlocked and they open with the current key. */
    fun open(goal: Goal, with: SyncKey? = key): Pair<String, String>? {
        val sealed = goal.sealed ?: return null
        val k = with ?: return null
        if (k === key) opened[sealed]?.let { return it }
        val text = runCatching { (Envelope.read(sealed) as Envelope.Sealed).open(k) }.getOrNull() ?: return null
        val o = JSONObject(text)
        return (o.optString("t") to o.optString("r")).also { if (k === key) opened[sealed] = it }
    }

    /** The goal as it should be shown: real title and note when unlocked, cover otherwise. */
    fun reveal(goal: Goal): Goal {
        if (!unlocked) return goal
        val (title, note) = open(goal) ?: return goal
        return goal.copy(title = title, reason = note)
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
