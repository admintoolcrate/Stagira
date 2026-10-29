package com.stagirite.ledger.support

import android.app.Activity
import android.content.Context
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.android.play.core.review.ReviewManagerFactory
import com.stagirite.ledger.BuildConfig

/**
 * Google Play's own review prompt and in-app updates. Copies installed from
 * GitHub skip both: Play doesn't know them, and they have their own beta check.
 */
class PlayServices(context: Context) {
    private val prefs = context.getSharedPreferences("play", Context.MODE_PRIVATE)
    private val updates = AppUpdateManagerFactory.create(context)

    /** True for copies from Google Play. */
    private val fromPlay = !BuildConfig.BETA_UPDATES

    /** An update has downloaded in the background and needs a restart to install. */
    var updateReady by mutableStateOf(false)
        private set

    private val listener = InstallStateUpdatedListener { state ->
        if (state.installStatus() == InstallStatus.DOWNLOADED) updateReady = true
    }

    init {
        if (fromPlay) updates.registerListener(listener)
    }

    /**
     * Asks Play, at most once a day, whether a newer version exists, and if so offers it as a
     * background download (the app keeps working meanwhile). Also notices one that finished
     * downloading while the app was closed.
     */
    fun checkForUpdate(launcher: ActivityResultLauncher<IntentSenderRequest>, beforeLeaving: () -> Unit) {
        if (!fromPlay) return
        updates.appUpdateInfo.addOnSuccessListener { info ->
            if (info.installStatus() == InstallStatus.DOWNLOADED) {
                updateReady = true
                return@addOnSuccessListener
            }
            val now = System.currentTimeMillis()
            if (now - prefs.getLong("lastUpdateOffer", 0) < 86_400_000L) return@addOnSuccessListener
            if (info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE &&
                info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)
            ) {
                prefs.edit().putLong("lastUpdateOffer", now).apply()
                beforeLeaving()
                updates.startUpdateFlowForResult(info, launcher, AppUpdateOptions.newBuilder(AppUpdateType.FLEXIBLE).build())
            }
        }
    }

    /** Restarts into the downloaded update. */
    fun installUpdate() {
        updates.completeUpdate()
    }

    /**
     * Play's review card, shown once ever, on a good day: right after someone has marked a
     * milestone of a week or more. Play itself decides whether to actually show it.
     */
    fun maybeAskForReview(activity: Activity, days: Long) {
        if (!fromPlay || days < 7 || prefs.getBoolean("reviewAsked", false)) return
        val reviews = ReviewManagerFactory.create(activity)
        reviews.requestReviewFlow().addOnCompleteListener { request ->
            if (!request.isSuccessful) return@addOnCompleteListener
            reviews.launchReviewFlow(activity, request.result).addOnCompleteListener {
                prefs.edit().putBoolean("reviewAsked", true).apply()
            }
        }
    }
}
