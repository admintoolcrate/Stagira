package com.stagirite.ledger.support

import android.app.Activity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import com.stagirite.ledger.BuildConfig

/**
 * "Watch an ad to support us". Google's ad service is only started when the
 * user taps the button: nothing is loaded, initialised or tracked before that.
 * In the EEA and UK, Google's consent form is shown first, as Google requires.
 * The ad pays the developer a little; it unlocks nothing.
 */
class SupportAd {
    enum class State { Idle, Loading, Showing }

    var state by mutableStateOf(State.Idle)
        private set

    private var started = false

    /** Shows one rewarded ad. [onDone] gets true if it was watched to the end. */
    fun watch(activity: Activity, onDone: (watched: Boolean, problem: String?) -> Unit) {
        if (state != State.Idle) return
        state = State.Loading
        val consent = UserMessagingPlatform.getConsentInformation(activity)
        consent.requestConsentInfoUpdate(
            activity,
            ConsentRequestParameters.Builder().build(),
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { _ ->
                    if (consent.canRequestAds()) load(activity, onDone) else finish(onDone, false, "Ads weren’t allowed.")
                }
            },
            { _ ->
                // Consent couldn't be checked (often no connection); only go on if it was settled earlier.
                if (consent.canRequestAds()) load(activity, onDone) else finish(onDone, false, "Couldn’t reach Google. Try again later.")
            },
        )
    }

    private fun load(activity: Activity, onDone: (Boolean, String?) -> Unit) {
        if (!started) {
            MobileAds.initialize(activity) {}
            started = true
        }
        RewardedAd.load(
            activity,
            BuildConfig.ADMOB_REWARDED_ID,
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    var watched = false
                    ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                        override fun onAdDismissedFullScreenContent() = finish(onDone, watched, null)
                        override fun onAdFailedToShowFullScreenContent(error: com.google.android.gms.ads.AdError) =
                            finish(onDone, false, "The ad couldn’t be shown.")
                    }
                    state = State.Showing
                    ad.show(activity) { watched = true }
                }

                override fun onAdFailedToLoad(error: LoadAdError) =
                    finish(onDone, false, "No ad is available right now. Thank you for trying.")
            },
        )
    }

    private fun finish(onDone: (Boolean, String?) -> Unit, watched: Boolean, problem: String?) {
        state = State.Idle
        onDone(watched, problem)
    }
}
