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
import xyz.klinker.messenger.api.implementation.Account
import xyz.klinker.messenger.shared.data.DataSource
import xyz.klinker.messenger.shared.data.model.Message
import xyz.klinker.messenger.shared.service.SmsReceivedService
import xyz.klinker.messenger.shared.util.*

class SmsReceivedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (Account.exists() && !Account.primary) {
            return
        }

        Thread {
            SmsReceivedReceiver.lastReceived = System.currentTimeMillis()
            SmsReceivedHandler(context).newSmsRecieved(intent)
        }.start()
    }

    companion object {
        var lastReceived = 0L

        fun shouldSaveMessage(context: Context, message: Message, phoneNumbers: String): Boolean {
            val conversationId = DataSource.findConversationId(context, phoneNumbers) ?: return true
            val databaseMessage = DataSource.getMessages(context, conversationId, 1).firstOrNull() ?: return true
            val isSameMessage = databaseMessage.data == message.data
            val isSameType = databaseMessage.type == message.type
            val areTimestampsClose = Math.abs(message.timestamp - databaseMessage.timestamp) < TimeUtils.MINUTE * 1

            return !isSameMessage || !isSameType || !areTimestampsClose
        }
    }

}
