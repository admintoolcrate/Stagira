package com.stagirite.ledger.security

import android.content.Context
import android.os.SystemClock
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.stagirite.ledger.data.Settings

/**
 * Hides the ledger behind fingerprint, face or the phone's screen lock.
 * Locks on launch, and again when the app has been in the background longer
 * than the chosen delay. Trips to our own pickers and Google sign-in don't count.
 */
class AppLock(private val settings: Settings) {
    var locked by mutableStateOf(settings.appLock)
        private set

    private var backgroundedAt: Long? = null
    private var expectingReturnUntil = 0L

    /** Call before opening a file picker, sign-in screen or similar. */
    fun expectReturn() {
        expectingReturnUntil = SystemClock.elapsedRealtime() + 10 * 60_000
    }

    fun onBackground() {
        backgroundedAt = SystemClock.elapsedRealtime()
    }

    fun onForeground() {
        val now = SystemClock.elapsedRealtime()
        val away = backgroundedAt?.let { now - it }
        backgroundedAt = null
        if (!settings.appLock || away == null) return
        if (now < expectingReturnUntil) {
            expectingReturnUntil = 0
            return
        }
        if (away >= settings.lockDelay.millis) locked = true
    }

    /** Leaves a few seconds for the return from a PIN screen, then locking works normally again. */
    private fun promptFinished() {
        expectingReturnUntil = minOf(expectingReturnUntil, SystemClock.elapsedRealtime() + 3_000)
    }

    fun unlock() {
        locked = false
    }

    fun prompt(activity: FragmentActivity, title: String, onSuccess: () -> Unit) {
        // Unlocking with a PIN can open a separate system screen; coming back from it isn't a new visit.
        expectReturn()
        val prompt = BiometricPrompt(
            activity,
            ContextCompat.getMainExecutor(activity),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    promptFinished()
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) = promptFinished()
            },
        )
        prompt.authenticate(
            BiometricPrompt.PromptInfo.Builder()
                .setTitle(title)
                .setAllowedAuthenticators(AUTHENTICATORS)
                .build()
        )
    }

    companion object {
        private const val AUTHENTICATORS = BIOMETRIC_WEAK or DEVICE_CREDENTIAL

        /** False if the phone has no fingerprint, face or screen lock set up. */
        fun available(context: Context): Boolean =
            BiometricManager.from(context).canAuthenticate(AUTHENTICATORS) == BiometricManager.BIOMETRIC_SUCCESS
    }
}
