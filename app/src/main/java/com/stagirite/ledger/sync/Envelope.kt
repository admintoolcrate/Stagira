package com.stagirite.ledger.sync

import org.json.JSONObject
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/** A key derived from the user's sync passphrase, plus the salt it was derived with. */
class SyncKey(val bytes: ByteArray, val salt: ByteArray, val iterations: Int)

/**
 * The file stored in Google Drive. Either the plain ledger JSON, or that JSON
 * encrypted with AES-256-GCM under a key derived from a passphrase (PBKDF2-SHA256),
 * so that neither Google nor anyone else can read it without the passphrase.
 */
sealed interface Envelope {
    data class Plain(val ledger: String) : Envelope

    class Sealed(val salt: ByteArray, val iterations: Int, val iv: ByteArray, val data: ByteArray) : Envelope {
        fun matches(key: SyncKey) = key.salt.contentEquals(salt) && key.iterations == iterations

        /** Throws [javax.crypto.AEADBadTagException] if the key is wrong. */
        fun open(key: SyncKey): String {
            val cipher = Cipher.getInstance(TRANSFORM)
            cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key.bytes, "AES"), GCMParameterSpec(128, iv))
            return cipher.doFinal(data).toString(Charsets.UTF_8)
        }
    }

    companion object {
        private const val TRANSFORM = "AES/GCM/NoPadding"
        const val ITERATIONS = 210_000
        private val b64 = Base64.getEncoder()
        private val unb64 = Base64.getDecoder()
        private val random = SecureRandom()

        fun deriveKey(passphrase: CharArray, salt: ByteArray = randomBytes(16), iterations: Int = ITERATIONS): SyncKey {
            val spec = PBEKeySpec(passphrase, salt, iterations, 256)
            try {
                val bytes = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
                return SyncKey(bytes, salt, iterations)
            } finally {
                spec.clearPassword()
            }
        }

        /** Serializes [ledger] for upload, sealed with [key] when one is given. */
        fun write(ledger: String, key: SyncKey?): String {
            if (key == null) return ledger
            val iv = randomBytes(12)
            val cipher = Cipher.getInstance(TRANSFORM)
            cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key.bytes, "AES"), GCMParameterSpec(128, iv))
            val data = cipher.doFinal(ledger.toByteArray(Charsets.UTF_8))
            return JSONObject()
                .put("sealed", 1)
                .put("cipher", "AES-256-GCM")
                .put("kdf", "PBKDF2-HMAC-SHA256")
                .put("iterations", key.iterations)
                .put("salt", b64.encodeToString(key.salt))
                .put("iv", b64.encodeToString(iv))
                .put("data", b64.encodeToString(data))
                .toString()
        }

        /** Work factors outside this range are refused, so a damaged file can't hang or crash the app. */
        val ACCEPTED_ITERATIONS = 10_000..5_000_000

        /** Throws [IllegalArgumentException] or [org.json.JSONException] if [text] is malformed. */
        fun read(text: String): Envelope {
            val o = JSONObject(text)
            if (!o.has("sealed")) return Plain(text)
            val iterations = o.getInt("iterations")
            require(iterations in ACCEPTED_ITERATIONS) { "Unexpected key-derivation work factor: $iterations" }
            return Sealed(
                salt = unb64.decode(o.getString("salt")),
                iterations = iterations,
                iv = unb64.decode(o.getString("iv")),
                data = unb64.decode(o.getString("data")),
            )
        }

        private fun randomBytes(n: Int) = ByteArray(n).also { random.nextBytes(it) }
    }
}
