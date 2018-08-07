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
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.telephony.SmsMessage
import xyz.klinker.messenger.api.implementation.Account
import xyz.klinker.messenger.shared.data.DataSource
import xyz.klinker.messenger.shared.data.MimeType
import xyz.klinker.messenger.shared.data.model.Message
import xyz.klinker.messenger.shared.service.notification.NotificationConstants
import xyz.klinker.messenger.shared.service.notification.Notifier
import xyz.klinker.messenger.shared.util.BlacklistUtils
import xyz.klinker.messenger.shared.util.PermissionsUtils
import xyz.klinker.messenger.shared.util.PhoneNumberUtils
import xyz.klinker.messenger.shared.util.TimeUtils

class SmsReceivedNonDefaultReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (Account.exists() && !Account.primary) {
            return
        }

        val handler = Handler()
        Thread {
            try {
                Thread.sleep(TimeUtils.SECOND * 4)
                if (TimeUtils.now - SmsReceivedReceiver.lastReceived < TimeUtils.SECOND * 15) {
                    // the main receiver should have handled this just fine.
                    return@Thread
                }

                handleReceiver(context, intent, handler)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }

    @Throws(Exception::class)
    private fun handleReceiver(context: Context, intent: Intent, handler: Handler) {
        val extras = intent.extras

        var body = ""
        var address = ""
        val smsExtra = extras!!.get("pdus") as Array<*>? ?: return

        for (message in smsExtra) {
            val sms = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val format = extras.getString("format")
                SmsMessage.createFromPdu(message as ByteArray, format)
            } else {
                SmsMessage.createFromPdu(message as ByteArray)
            }

            body += sms.messageBody
            address = sms.originatingAddress
        }

        if (BlacklistUtils.isBlacklisted(context, address)) {
            return
        }

        val conversationId = insertSms(context, handler, address, body)

        if (conversationId != -1L && PermissionsUtils.isDefaultSmsApp(context)) {
            Thread { Notifier(context).notify() }.start()
        }
    }

    private fun insertSms(context: Context, handler: Handler, address: String, body: String): Long {
        val message = Message()
        message.type = Message.TYPE_RECEIVED
        message.data = body.trim { it <= ' ' }
        message.timestamp = TimeUtils.now
        message.mimeType = MimeType.TEXT_PLAIN
        message.read = false
        message.seen = false
        message.simPhoneNumber = null
        message.sentDeviceId = -1L

        if (shouldSaveMessages(context, DataSource, message)) {
            val conversationId = DataSource
                    .insertMessage(message, PhoneNumberUtils.clearFormatting(address), context)
            val conversation = DataSource.getConversation(context, conversationId)

            handler.post {
                ConversationListUpdatedReceiver.sendBroadcast(context, conversationId, body, NotificationConstants.CONVERSATION_ID_OPEN == conversationId)
                MessageListUpdatedReceiver.sendBroadcast(context, conversationId, message.data, message.type)
            }

            if (conversation!!.mute) {
                DataSource.seenConversation(context, conversationId)

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
                val search = source.searchMessagesAsList(context, message.data, 1, true)
                if (search.isEmpty()) {
                    return true
                }
            } catch (e: Exception) {
            }

            return false
        }
    }

}
