package xyz.klinker.messenger.shared.util

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Handler
import xyz.klinker.messenger.shared.activity.RateItDialog
import xyz.klinker.messenger.shared.data.Settings

class PromotionUtils(private val context: Context) {
    private val sharedPreferences: SharedPreferences = Settings.getSharedPrefs(context)

    fun checkPromotions() {
        if (shouldAskForRating()) {
            askForRating()
        }
    }

    private fun shouldAskForRating(): Boolean {
        val pref = "install_time"
        val currentTime = System.currentTimeMillis()
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
        sharedPreferences.edit().putBoolean("show_rate_it", false)
                .apply()

        Handler().postDelayed({ context.startActivity(Intent(context, RateItDialog::class.java)) },
                500)
    }
}
