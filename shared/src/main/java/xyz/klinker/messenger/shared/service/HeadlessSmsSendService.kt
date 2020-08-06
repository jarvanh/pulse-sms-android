/*
 * Copyright (C) 2020 Luke Klinker
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

package xyz.klinker.messenger.shared.service

import android.app.Service
import android.content.Intent
import android.net.Uri
import android.os.IBinder

import xyz.klinker.messenger.shared.data.DataSource
import xyz.klinker.messenger.shared.data.MimeType
import xyz.klinker.messenger.shared.data.model.Conversation
import xyz.klinker.messenger.shared.util.PhoneNumberUtils
import xyz.klinker.messenger.shared.util.SendUtils

/**
 * Service for sending messages to a conversation without a UI present. These messages could come
 * from something like Phone.
 */
class HeadlessSmsSendService : Service() {

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        try {
            if (intent == null) {
                return Service.START_NOT_STICKY
            }

            val addresses = PhoneNumberUtils.parseAddress(Uri.decode(intent.dataString))
            val text = getText(intent)

            val phoneNumbers = StringBuilder()
            for (i in addresses.indices) {
                phoneNumbers.append(addresses[i])
                if (i != addresses.size - 1) {
                    phoneNumbers.append(", ")
                }
            }

            val source = DataSource
            val conversationId = source.insertSentMessage(phoneNumbers.toString(), text, MimeType.TEXT_PLAIN, this)
            val conversation = source.getConversation(this, conversationId)

            SendUtils(conversation?.simSubscriptionId)
                    .send(this, text, addresses)
        } catch (e: Exception) {

        }

        return super.onStartCommand(intent, flags, startId)
    }

    private fun getText(intent: Intent): String {
        val text = intent.getCharSequenceExtra(Intent.EXTRA_TEXT)
        return text?.toString() ?: intent.getStringExtra(Intent.EXTRA_TEXT)!!
    }

}
