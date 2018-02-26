package xyz.klinker.messenger.utils

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.preference.PreferenceManager
import android.util.Log
import xyz.klinker.messenger.api.implementation.firebase.ScheduledTokenRefreshService
import xyz.klinker.messenger.shared.R
import xyz.klinker.messenger.shared.data.ColorSet
import xyz.klinker.messenger.shared.data.Settings
import xyz.klinker.messenger.shared.service.ContactResyncService
import xyz.klinker.messenger.shared.service.jobs.*

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
        ContactResyncService.runIfApplicable(context, sharedPreferences, storedAppVersion);

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
        SyncRetryableRequestsJob.scheduleNextRun(context)
    }

    companion object {

        private const val TAG = "UpdateUtil"
    }
}
