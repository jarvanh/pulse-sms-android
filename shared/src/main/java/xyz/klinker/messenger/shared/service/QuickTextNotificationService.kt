package xyz.klinker.messenger.shared.service

import android.app.IntentService
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.support.v4.app.NotificationCompat
import android.support.v4.app.NotificationManagerCompat
import xyz.klinker.messenger.shared.R
import xyz.klinker.messenger.shared.data.ColorSet
import xyz.klinker.messenger.shared.data.FeatureFlags
import xyz.klinker.messenger.shared.util.ActivityUtils
import xyz.klinker.messenger.shared.util.NotificationUtils

class QuickTextNotificationService : IntentService("QuickTextNotificationService") {

    override fun onHandleIntent(intent: Intent?) {
        val foreground = NotificationCompat.Builder(this, NotificationUtils.GENERAL_CHANNEL_ID)
                .setContentTitle(getString(R.string.creating_channels_text))
                .setSmallIcon(R.drawable.ic_stat_notify_group)
                .setLocalOnly(true)
                .setColor(ColorSet.DEFAULT(this).color)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .build()
        startForeground(FOREGROUND_ID, foreground)

        val notification = NotificationCompat.Builder(this, NotificationUtils.QUICK_TEXT_CHANNEL_ID)
                .setContentTitle(getString(R.string.write_new_message))
                .setSmallIcon(R.drawable.ic_stat_notify_group)
                .setLocalOnly(true)
                .setOngoing(true)
                .setAutoCancel(false)
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .setWhen(0)

        addContentIntent(notification)
        addActionsToNotification(notification)

        NotificationManagerCompat.from(this).notify(QUICK_TEXT_ID, notification.build())
        stopForeground(true)
    }

    private fun addContentIntent(builder: NotificationCompat.Builder) {
        val compose = ActivityUtils.buildForComponent(ActivityUtils.QUICK_SHARE_ACTIVITY)
        val pendingCompose = PendingIntent.getActivity(this, QUICK_TEXT_ID,
                compose, PendingIntent.FLAG_UPDATE_CURRENT)

        builder.setContentIntent(pendingCompose)
    }

    private fun addActionsToNotification(builder: NotificationCompat.Builder) {
        // TODO: In the future, might want to add a list of favorites for notification buttons
        // would be a nice little addition
    }

    companion object {
        private val FOREGROUND_ID = 1225
        private val QUICK_TEXT_ID = 1226

        fun start(context: Context) {
            if (!FeatureFlags.QUICK_COMPOSE) {
                return
            }

            val intent = Intent(context, QuickTextNotificationService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            NotificationManagerCompat.from(context).cancel(QUICK_TEXT_ID)
        }
    }
}