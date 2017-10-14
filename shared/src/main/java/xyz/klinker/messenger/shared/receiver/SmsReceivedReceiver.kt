/*
 * Copyright (C) 2017 Luke Klinker
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package xyz.klinker.messenger.shared.receiver

import android.content.BroadcastReceiver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.provider.Telephony
import android.telephony.SmsMessage
import xyz.klinker.messenger.api.implementation.Account
import xyz.klinker.messenger.api.implementation.firebase.AnalyticsHelper
import xyz.klinker.messenger.shared.data.DataSource
import xyz.klinker.messenger.shared.data.MimeType
import xyz.klinker.messenger.shared.data.model.Message
import xyz.klinker.messenger.shared.service.MediaParserService
import xyz.klinker.messenger.shared.service.NotificationService
import xyz.klinker.messenger.shared.util.BlacklistUtils
import xyz.klinker.messenger.shared.util.DualSimUtils
import xyz.klinker.messenger.shared.util.PhoneNumberUtils
import xyz.klinker.messenger.shared.util.TimeUtils

class SmsReceivedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (Account.exists() && !Account.primary) {
            return
        }

        val handler = Handler()
        Thread {
            try {
                handleReceiver(context, intent, handler)
            } catch (e: Exception) {
                AnalyticsHelper.failedToSaveSms(context, e.message)
                e.printStackTrace()
            }
        }.start()
    }

    private fun handleReceiver(context: Context, intent: Intent, handler: Handler) {
        val extras = intent.extras

        val simSlot = extras!!.getInt("slot", -1)
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

        if (BlacklistUtils.isBlacklisted(context, address)) {
            return
        }

        insertInternalSms(context, address, body, date)

        val conversationId = insertSms(context, handler, address, body, simSlot)
        if (conversationId != -1L) {
            context.startService(Intent(context, NotificationService::class.java))

            if (MediaParserService.createParser(context, body.trim { it <= ' ' }) != null) {
                MediaParserService.start(context, conversationId, body)
            }
        }
    }

    private fun insertInternalSms(context: Context, address: String, body: String, dateSent: Long) {
        val values = ContentValues(5)
        values.put(Telephony.Sms.ADDRESS, address)
        values.put(Telephony.Sms.BODY, body)
        values.put(Telephony.Sms.DATE, System.currentTimeMillis())
        values.put(Telephony.Sms.READ, "1")
        values.put(Telephony.Sms.DATE_SENT, dateSent)

        Thread {
            try {
                context.contentResolver.insert(Telephony.Sms.Inbox.CONTENT_URI, values)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }

    private fun insertSms(context: Context, handler: Handler, address: String, body: String, simSlot: Int): Long {
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

        if (shouldSaveMessages(context, source, message)) {
            val conversationId = try {
                source.insertMessage(message, PhoneNumberUtils.clearFormatting(address), context)
            } catch (e: Exception) {
                source.ensureActionable(context)
                source.insertMessage(message, PhoneNumberUtils.clearFormatting(address), context)
            }

            val conversation = source.getConversation(context, conversationId)
            handler.post {
                ConversationListUpdatedReceiver.sendBroadcast(context, conversationId, body, NotificationService.CONVERSATION_ID_OPEN == conversationId)
                MessageListUpdatedReceiver.sendBroadcast(context, conversationId, message.data, message.type)
            }

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

        fun shouldSaveMessages(context: Context, source: DataSource, message: Message): Boolean {
            try {
                val search = source.searchMessagesAsList(context, message.data, 1)
                if (!search.isEmpty()) {
                    val inDatabase = search[0]
                    if (inDatabase.data == message.data && inDatabase.type == Message.TYPE_RECEIVED &&
                            message.timestamp - inDatabase.timestamp < TimeUtils.MINUTE * 10) {
                        return false
                    }
                }
            } catch (e: Exception) {
            }

            return true
        }
    }

}
