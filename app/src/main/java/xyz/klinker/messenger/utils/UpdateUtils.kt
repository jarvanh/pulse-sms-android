package xyz.klinker.messenger.utils

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.preference.PreferenceManager
import android.util.Log

import xyz.klinker.messenger.api.implementation.Account
import xyz.klinker.messenger.api.implementation.firebase.ScheduledTokenRefreshService
import xyz.klinker.messenger.shared.R
import xyz.klinker.messenger.shared.data.ColorSet
import xyz.klinker.messenger.shared.data.MmsSettings
import xyz.klinker.messenger.shared.data.Settings
import xyz.klinker.messenger.shared.service.ContactResyncService
import xyz.klinker.messenger.shared.service.jobs.CleanupOldMessagesJob
import xyz.klinker.messenger.shared.service.jobs.ContactSyncJob
import xyz.klinker.messenger.shared.service.jobs.ScheduledMessageJob
import xyz.klinker.messenger.shared.service.jobs.SignoutJob
import xyz.klinker.messenger.shared.service.jobs.SubscriptionExpirationCheckJob

class UpdateUtils(private val context: Activity) {

    private// should never happen
    val appVersion: Int
        get() = try {
            val packageInfo = context.packageManager
                    .getPackageInfo(context.packageName, 0)
            packageInfo.versionCode
        } catch (e: PackageManager.NameNotFoundException) {
            -1
        }

    fun checkForUpdate(): Boolean {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

        val storedAppVersion = sharedPreferences.getInt("app_version", 0)

        if (sharedPreferences.getBoolean("v2.5.0.1", true)) {
            val colorSetName = sharedPreferences.getString(context.getString(R.string.pref_global_color_theme), "default")
            val legacyGlobalTheme = ColorSet.getFromString(context, colorSetName!!)

            sharedPreferences.edit()
                    .putBoolean("v2.5.0.1", false)
                    .putInt(context.getString(R.string.pref_global_primary_color), legacyGlobalTheme.color)
                    .putInt(context.getString(R.string.pref_global_primary_dark_color), legacyGlobalTheme.colorDark)
                    .putInt(context.getString(R.string.pref_global_primary_light_color), legacyGlobalTheme.colorLight)
                    .putInt(context.getString(R.string.pref_global_accent_color), legacyGlobalTheme.colorAccent)
                    .putBoolean(context.getString(R.string.pref_apply_theme_globally), colorSetName != "default")
                    .commit()

            Settings.forceUpdate(context)
        }

        if (sharedPreferences.getBoolean("v2.5.4.2", true)) {
            if (storedAppVersion != 0) {
                context.startService(Intent(context, ContactResyncService::class.java))
            }

            sharedPreferences.edit()
                    .putBoolean("v2.5.4.2", false)
                    .commit()
        }

        val currentAppVersion = appVersion

        return if (storedAppVersion != currentAppVersion) {
            Log.v(TAG, "new app version")
            sharedPreferences.edit().putInt("app_version", currentAppVersion).apply()
            runEveryUpdate()
            true
        } else {
            false
        }
    }

    private fun runEveryUpdate() {
        CleanupOldMessagesJob.scheduleNextRun(context)
        ScheduledMessageJob.scheduleNextRun(context)
        ContactSyncJob.scheduleNextRun(context)
        SubscriptionExpirationCheckJob.scheduleNextRun(context)
        SignoutJob.scheduleNextRun(context)
        ScheduledTokenRefreshService.scheduleNextRun(context)
    }

    companion object {

        private val TAG = "UpdateUtil"
    }
}
