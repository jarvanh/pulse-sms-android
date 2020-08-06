package xyz.klinker.messenger.shared.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Handler
import com.sensortower.rating.RatingPrompt
import xyz.klinker.messenger.api.implementation.Account
import xyz.klinker.messenger.shared.data.Settings

class PromotionUtils(private val context: Activity) {
    private val sharedPreferences: SharedPreferences = Settings.getSharedPrefs(context)

    fun checkPromotions(onTrialExpired: () -> Unit) {
        if (trialExpired()) {
            onTrialExpired()
        } else if (shouldAskForRating()) {
            askForRating()
        }
    }

    private fun trialExpired(): Boolean {
        return Account.exists() && Account.subscriptionType == Account.SubscriptionType.FREE_TRIAL && Account.getDaysLeftInTrial() <= 0
    }

    // The "rate-it" library will manage whether or not it has been shown or whether it needs to be
    // shown. This shared preference key is so that we are not re-prompting users that have seen
    // the dialog in the past, through the legacy system.

    private fun shouldAskForRating(): Boolean {
        return sharedPreferences.getBoolean("show_rate_it", true)
    }

    private fun askForRating() {
        Handler().postDelayed({
            RatingPrompt.show(context)
        }, 500)
    }
}
