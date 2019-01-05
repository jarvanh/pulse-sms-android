package xyz.klinker.messenger.shared.util

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Handler
import xyz.klinker.messenger.api.implementation.Account
import xyz.klinker.messenger.shared.activity.RateItDialog
import xyz.klinker.messenger.shared.data.Settings

class PromotionUtils(private val context: Context) {
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

    private fun shouldAskForRating(): Boolean {
        val pref = "install_time"
        val currentTime = TimeUtils.now
        val installTime = sharedPreferences.getLong(pref, -1L)

        if (installTime == -1L) {
            // write the install time to now
            sharedPreferences.edit().putLong(pref, currentTime).apply()
        } else {
            if (currentTime - installTime > TimeUtils.TWO_WEEKS) {
                return sharedPreferences.getBoolean("show_rate_it", true)
            }
        }

        return false
    }

    private fun askForRating() {
        sharedPreferences.edit().putBoolean("show_rate_it", false).apply()
        Handler().postDelayed({ context.startActivity(Intent(context, RateItDialog::class.java)) },
                500)
    }
}
