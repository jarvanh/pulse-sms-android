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

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.provider.Telephony

import com.klinker.android.send_message.DeliveredReceiver
import xyz.klinker.messenger.api.implementation.Account

import xyz.klinker.messenger.shared.data.DataSource
import xyz.klinker.messenger.shared.data.Settings
import xyz.klinker.messenger.shared.data.model.Message
import xyz.klinker.messenger.shared.util.CursorUtil
import xyz.klinker.messenger.shared.util.SmsMmsUtils
import xyz.klinker.messenger.shared.util.TimeUtils

/**
 * Receiver for getting notifications of when an SMS has been delivered. By default it's super
 * class will mark the internal message as delivered, we need to also mark our database as delivered.
 */
class SmsDeliveredReceiver : DeliveredReceiver() {

    override fun updateInInternalDatabase(context: Context, intent: Intent, resultCode: Int) {
        Thread { super.updateInInternalDatabase(context, intent, resultCode) }.start()
    }

    override fun onMessageStatusUpdated(context: Context, intent: Intent, receiverResultCode: Int) {
        if (Account.exists() && !Account.primary) {
            return
        }

        val uri = Uri.parse(intent.getStringExtra("message_uri"))
        when (receiverResultCode) {
            Activity.RESULT_OK -> markMessageDelivered(context, uri)
            Activity.RESULT_CANCELED -> markMessageError(context, uri)
        }
    }


    private fun markMessageDelivered(context: Context, uri: Uri) {
        markMessage(context, uri, false)
    }

    private fun markMessageError(context: Context, uri: Uri) {
        markMessage(context, uri, true)
    }

    private fun markMessage(context: Context, uri: Uri, error: Boolean) {
        val message = SmsMmsUtils.getSmsMessage(context, uri, null)

        if (message != null && message.moveToFirst()) {
            val time = message.getLong(message.getColumnIndex(Telephony.Sms.DATE))
            val address = message.getString(message.getColumnIndex(Telephony.Sms.ADDRESS))
            var body = message.getString(message.getColumnIndex(Telephony.Sms.BODY))
            message.close()

            if (Settings.signature != null && !Settings.signature!!.isEmpty()) {
                body = body.replace("\n" + Settings.signature!!, "")
            }

            val source = DataSource
            val messages = source.searchMessages(context, body)

            if (messages != null && messages.moveToFirst()) {
                val id = messages.getLong(0)
                source.updateMessageType(context, id, if (error) Message.TYPE_ERROR else Message.TYPE_DELIVERED)

                val conversationId = messages
                        .getLong(messages.getColumnIndex(Message.COLUMN_CONVERSATION_ID))
                MessageListUpdatedReceiver.sendBroadcast(context, conversationId)
            } else {
                val conversationId = source.findConversationId(context, address)
                if (conversationId != null) {
                    val conversationMessages = source.getMessages(context, conversationId, 20)
                    for (m in conversationMessages) {
                        if ((m.type == Message.TYPE_SENT || m.type == Message.TYPE_SENDING) && timestampsMatch(m.timestamp, time)) {
                            source.updateMessageType(context, m.id, if (error) Message.TYPE_ERROR else Message.TYPE_DELIVERED)
                            MessageListUpdatedReceiver.sendBroadcast(context, conversationId)
                        }
                    }
                }
            }

            CursorUtil.closeSilent(messages)
        }
    }

    private fun timestampsMatch(realTime: Long, internalSmsTime: Long) =
            Math.abs(realTime - internalSmsTime) < 10 * TimeUtils.SECOND

}
