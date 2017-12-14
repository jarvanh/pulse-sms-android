package xyz.klinker.messenger.shared.service

import android.app.IntentService
import android.content.ComponentName
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Telephony
import android.support.v4.app.NotificationCompat
import android.telephony.SmsMessage
import xyz.klinker.messenger.api.implementation.firebase.AnalyticsHelper
import xyz.klinker.messenger.shared.R
import xyz.klinker.messenger.shared.data.ColorSet
import xyz.klinker.messenger.shared.data.DataSource
import xyz.klinker.messenger.shared.data.FeatureFlags
import xyz.klinker.messenger.shared.data.MimeType
import xyz.klinker.messenger.shared.data.model.Message
import xyz.klinker.messenger.shared.exception.SmsSaveException
import xyz.klinker.messenger.shared.receiver.ConversationListUpdatedReceiver
import xyz.klinker.messenger.shared.receiver.MessageListUpdatedReceiver
import xyz.klinker.messenger.shared.receiver.SmsReceivedReceiver
import xyz.klinker.messenger.shared.service.notification.NotificationConstants
import xyz.klinker.messenger.shared.service.notification.NotificationService
import xyz.klinker.messenger.shared.util.*

class SmsReceivedService : IntentService("SmsReceivedService") {

    override fun onHandleIntent(intent: Intent?) {
        if (AndroidVersionUtil.isAndroidO) {
            startForeground()
        }

        SmsReceivedHandler(this).newSmsRecieved(intent)

        if (AndroidVersionUtil.isAndroidO) {
            stopForeground(true)
        }
    }

    private fun startForeground() {
        try {
            val notification = NotificationCompat.Builder(this,
                    NotificationUtils.BACKGROUND_SERVICE_CHANNEL_ID)
                    .setContentTitle(getString(R.string.receiving_a_message))
                    .setSmallIcon(R.drawable.ic_stat_notify_group)
                    .setLocalOnly(true)
                    .setColor(ColorSet.DEFAULT(this).color)
                    .setOngoing(true)
                    .build()

            startForeground(NotificationConstants.FOREGROUND_NOTIFICATION_ID, notification)
        } catch (e: Exception) {
            NotificationUtils.createBackgroundServiceChannel(this)

            val notification = NotificationCompat.Builder(this,
                    NotificationUtils.BACKGROUND_SERVICE_CHANNEL_ID)
                    .setContentTitle(getString(R.string.receiving_a_message))
                    .setSmallIcon(R.drawable.ic_stat_notify_group)
                    .setLocalOnly(true)
                    .setColor(ColorSet.DEFAULT(this).color)
                    .setOngoing(true)
                    .build()

            startForeground(NotificationConstants.FOREGROUND_NOTIFICATION_ID, notification)
        }
    }

    companion object {
        fun start(context: Context, intent: Intent) {
            intent.component = ComponentName("xyz.klinker.messenger",
                    "xyz.klinker.messenger.shared" + ".service.SmsReceivedService")

            if (AndroidVersionUtil.isAndroidO) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }
}