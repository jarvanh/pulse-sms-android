package xyz.klinker.messenger.shared.service.notification

import android.app.Notification
import android.app.Service
import android.content.Context
import android.content.Intent
import android.support.v4.app.NotificationCompat
import android.support.v4.app.NotificationManagerCompat
import xyz.klinker.messenger.shared.R
import xyz.klinker.messenger.shared.data.ColorSet
import xyz.klinker.messenger.shared.util.AndroidVersionUtil
import xyz.klinker.messenger.shared.util.NotificationUtils

class NotificationForegroundController(private val service: NotificationService) {

    private var gaveDismissableForegroundNotification = false
    private var gaveForegroundNotification = false

    fun show(intent: Intent?) {
        if (intent != null && intent.getBooleanExtra(NotificationConstants.EXTRA_FOREGROUND, false) && AndroidVersionUtil.isAndroidO) {
            gaveForegroundNotification = true
            gaveDismissableForegroundNotification = true
            val notification = NotificationCompat.Builder(service,
                    NotificationUtils.SILENT_BACKGROUND_CHANNEL_ID)
                    .setContentTitle(service.getString(R.string.repeat_interval))
                    .setSmallIcon(R.drawable.ic_stat_notify_group)
                    .setLocalOnly(true)
                    .setColor(ColorSet.DEFAULT(service).color)
                    .setOngoing(false)
                    .build()

            service.startForeground(NotificationConstants.FOREGROUND_NOTIFICATION_ID, notification)
        }
    }

    fun hide() {
        if (gaveForegroundNotification) {
            service.stopForeground(true)
        }
    }
}