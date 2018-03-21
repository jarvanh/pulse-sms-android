package xyz.klinker.messenger.shared.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Telephony
import android.telephony.SmsMessage
import xyz.klinker.messenger.api.implementation.firebase.AnalyticsHelper
import xyz.klinker.messenger.shared.data.DataSource
import xyz.klinker.messenger.shared.data.FeatureFlags
import xyz.klinker.messenger.shared.data.MimeType
import xyz.klinker.messenger.shared.data.model.Message
import xyz.klinker.messenger.shared.exception.SmsSaveException
import xyz.klinker.messenger.shared.receiver.ConversationListUpdatedReceiver
import xyz.klinker.messenger.shared.receiver.MessageListUpdatedReceiver
import xyz.klinker.messenger.shared.receiver.SmsReceivedReceiver
import xyz.klinker.messenger.shared.service.NewMessagesCheckService
import xyz.klinker.messenger.shared.service.notification.NotificationConstants
import xyz.klinker.messenger.shared.service.notification.NotificationService
import xyz.klinker.messenger.shared.service.notification.Notifier

class SmsReceivedHandler(private val context: Context) {

    fun newSmsRecieved(intent: Intent?) {
        if (intent != null) {
            val latestMessageOne = DataSource.getLatestMessage(context)

            val wasBlacklisted = try {
                handle(intent)
            } catch (e: Exception) {
                throw SmsSaveException(e)
            }

            val latestMessageTwo = DataSource.getLatestMessage(context)
        }
    }

    private fun handle(intent: Intent): Boolean {
        val extras = intent.extras ?: return false

        val simSlot = extras.getInt("slot", -1)
        var body = ""
        var address = ""
        var date = TimeUtils.now
        val smsExtra = extras.get("pdus") as Array<*>? ?: return false

        for (message in smsExtra) {
            if (message == null) {
                continue
            }

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

        if (BlacklistUtils.isBlacklisted(context, address)) {
            return true
        }

        val conversationId = insertSms(context, address, body, simSlot)
        if (conversationId != -2L) {
            insertInternalSms(context, address, body, date)
        }

        if (conversationId != -1L && conversationId != -2L) {
            Thread { Notifier(context).notify() }.start()
        }

        return false
    }



    private fun insertInternalSms(context: Context, address: String, body: String, dateSent: Long) {
        Thread {
            val values = ContentValues(5)
            values.put(Telephony.Sms.ADDRESS, address)
            values.put(Telephony.Sms.BODY, body)
            values.put(Telephony.Sms.DATE, TimeUtils.now)
            values.put(Telephony.Sms.READ, "1")
            values.put(Telephony.Sms.DATE_SENT, dateSent)

            try {
                context.contentResolver.insert(Telephony.Sms.Inbox.CONTENT_URI, values)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }

    private fun insertSms(context: Context, address: String, body: String, simSlot: Int): Long {
        var body = body
        var address = address

        if (address.length <= 5 && body.split(" ".toRegex())[0].contains("@")) {
            // this is a text from an email address.
            address = body.split(" ".toRegex())[0]
            body = body.split(" ".toRegex()).drop(1).joinToString(" ")
        }

        address = PhoneNumberUtils.clearFormatting(address)

        val message = Message()
        message.type = Message.TYPE_RECEIVED
        message.data = body.trim { it <= ' ' }
        message.timestamp = TimeUtils.now
        message.mimeType = MimeType.TEXT_PLAIN
        message.read = false
        message.seen = false
        message.simPhoneNumber = DualSimUtils.getNumberFromSimSlot(simSlot)
        message.sentDeviceId = -1L

        val source = DataSource

        if (SmsReceivedReceiver.shouldSaveMessage(context, message, address)) {
            val conversationId = try {
                source.insertMessage(message, address, context)
            } catch (e: Exception) {
                source.ensureActionable(context)
                source.insertMessage(message, address, context)
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
            AnalyticsHelper.receivedDuplicateSms(context)
            return -2
        }
    }

}