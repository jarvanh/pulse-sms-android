package xyz.klinker.messenger.shared.service

import android.app.IntentService
import android.app.Notification
import android.content.Context
import android.content.Intent
import android.preference.PreferenceManager
import android.support.v4.app.NotificationCompat

import xyz.klinker.messenger.shared.R
import xyz.klinker.messenger.shared.data.ColorSet
import xyz.klinker.messenger.shared.data.DataSource
import xyz.klinker.messenger.shared.util.AndroidVersionUtil
import xyz.klinker.messenger.shared.util.NotificationUtils

class CreateNotificationChannelService : IntentService("CreateNotificationChannelService") {

    override fun onHandleIntent(intent: Intent?) {
        val notification = NotificationCompat.Builder(this,
                NotificationUtils.GENERAL_CHANNEL_ID)
                .setContentTitle(getString(R.string.downloading_and_decrypting))
                .setSmallIcon(R.drawable.ic_stat_notify_group)
                .setProgress(0, 0, true)
                .setLocalOnly(true)
                .setColor(ColorSet.DEFAULT(this).color)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .build()
        startForeground(FOREGROUND_ID, notification)

        NotificationUtils.createNotificationChannels(this)

        stopForeground(true)
    }

    companion object {
        private val FOREGROUND_ID = 1224
        fun shouldRun(context: Context): Boolean {
            if (!AndroidVersionUtil.isAndroidO) {
                return false
            }

            val prefs = PreferenceManager.getDefaultSharedPreferences(context)
            val shouldRun = prefs.getBoolean("needs_to_create_notification_channels", true)

            if (shouldRun) prefs.edit().putBoolean("needs_to_create_notification_channels", false).apply()
            return shouldRun
        }
    }
}
