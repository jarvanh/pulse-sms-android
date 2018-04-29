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

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import xyz.klinker.messenger.api.implementation.Account

import xyz.klinker.messenger.shared.data.DataSource
import xyz.klinker.messenger.shared.data.model.Message
import xyz.klinker.messenger.shared.util.SmsMmsUtils
import xyz.klinker.messenger.shared.util.closeSilent

/**
 * Receiver which gets a notification when an MMS message has finished sending. It will mark the
 * message as sent in the database by default. We also need to add functionality for marking it
 * as sent in our own database.
 */
class MmsSentReceiver : com.klinker.android.send_message.MmsSentReceiver() {

    override fun onMessageStatusUpdated(context: Context, intent: Intent, receiverResultCode: Int) {
        if (Account.exists() && !Account.primary) {
            return
        }

        handle(context, intent)
    }

    private fun handle(context: Context, intent: Intent) {
        val uri = Uri.parse(intent.getStringExtra(com.klinker.android.send_message.MmsSentReceiver.EXTRA_CONTENT_URI).replace("/outbox", ""))
        val message = SmsMmsUtils.getMmsMessage(context, uri, null)

        if (message != null && message.moveToFirst()) {
            val mmsParts = SmsMmsUtils.processMessage(message, -1, context)
            message.closeSilent()

            for (values in mmsParts) {
                val messages = DataSource.searchMessages(context, values.getAsLong(Message.COLUMN_TIMESTAMP)!!)
                if (messages.moveToFirst()) {
                    do {
                        val m = Message()
                        m.fillFromCursor(messages)

                        if (m.type == Message.TYPE_SENDING) {
                            DataSource.updateMessageType(context, m.id, Message.TYPE_SENT)
                            MessageListUpdatedReceiver.sendBroadcast(context, m.conversationId)
                        }
                    } while (messages.moveToNext())
                }

                message.closeSilent()
            }
        }

        message?.closeSilent()
    }

}
