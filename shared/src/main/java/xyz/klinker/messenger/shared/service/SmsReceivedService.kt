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
import xyz.klinker.messenger.shared.R
import xyz.klinker.messenger.shared.data.ColorSet
import xyz.klinker.messenger.shared.data.DataSource
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

        if (intent != null) {
            try {
                handle(intent)
            } catch (e: Exception) {
                throw SmsSaveException(e)
            }
        }

        if (AndroidVersionUtil.isAndroidO) {
            stopForeground(true)
        }
    }

    private fun handle(intent: Intent) {
        val extras = intent.extras ?: return

        val simSlot = extras.getInt("slot", -1)
        var body = ""
        var address = ""
        var date = System.currentTimeMillis()
        val smsExtra = extras.get("pdus") as Array<*>? ?: return

        for (message in smsExtra) {
            val sms = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val format = extras.getString("format")
                SmsMessage.createFromPdu(message as ByteArray, format)
            } else {
                SmsMessage.createFromPdu(message as ByteArray)
            }

            body += sms.messageBody
            address = sms.originatingAddress
            date = sms.timestampMillis
        }

        if (BlacklistUtils.isBlacklisted(this, address)) {
            return
        }

        val conversationId = insertSms(this, address, body, simSlot)
        insertInternalSms(this, address, body, date)

        if (conversationId != -1L) {
            val foregroundNotificationService = Intent(this, NotificationService::class.java)

            if (AndroidVersionUtil.isAndroidO) {
                foregroundNotificationService.putExtra(NotificationConstants.EXTRA_FOREGROUND, true)
                startForegroundService(foregroundNotificationService)
            } else {
                startService(foregroundNotificationService)
            }

            if (MediaParserService.createParser(this, body.trim { it <= ' ' }) != null) {
                MediaParserService.start(this, conversationId, body)
            }
        }
    }

    private fun startForeground() {
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

    private fun insertInternalSms(context: Context, address: String, body: String, dateSent: Long) {
        val values = ContentValues(5)
        values.put(Telephony.Sms.ADDRESS, address)
        values.put(Telephony.Sms.BODY, body)
        values.put(Telephony.Sms.DATE, System.currentTimeMillis())
        values.put(Telephony.Sms.READ, "1")
        values.put(Telephony.Sms.DATE_SENT, dateSent)

        try {
            context.contentResolver.insert(Telephony.Sms.Inbox.CONTENT_URI, values)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun insertSms(context: Context, address: String, body: String, simSlot: Int): Long {
        val message = Message()
        message.type = Message.TYPE_RECEIVED
        message.data = body.trim { it <= ' ' }
        message.timestamp = System.currentTimeMillis()
        message.mimeType = MimeType.TEXT_PLAIN
        message.read = false
        message.seen = false
        message.simPhoneNumber = DualSimUtils.getNumberFromSimSlot(simSlot)
        message.sentDeviceId = -1L

        val source = DataSource

        if (SmsReceivedReceiver.shouldSaveMessages(context, source, message)) {
            val conversationId = try {
                source.insertMessage(message, PhoneNumberUtils.clearFormatting(address), context)
            } catch (e: Exception) {
                source.ensureActionable(context)
                source.insertMessage(message, PhoneNumberUtils.clearFormatting(address), context)
            }

            val conversation = source.getConversation(context, conversationId)
            ConversationListUpdatedReceiver.sendBroadcast(context, conversationId, body, NotificationConstants.CONVERSATION_ID_OPEN == conversationId)
            MessageListUpdatedReceiver.sendBroadcast(context, conversationId, message.data, message.type)

            if (conversation != null && conversation.mute) {
                source.seenConversation(context, conversationId)
                // don't run the notification service
                return -1
            }

            return conversationId
        } else {
            return -1
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