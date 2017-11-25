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
import xyz.klinker.messenger.shared.service.SmsReceivedService
import xyz.klinker.messenger.shared.service.notification.NotificationConstants
import xyz.klinker.messenger.shared.service.notification.NotificationService
import xyz.klinker.messenger.shared.util.*

class SmsReceivedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (Account.exists() && !Account.primary) {
            return
        }

        SmsReceivedReceiver.lastReceived = System.currentTimeMillis()
        SmsReceivedService.start(context, intent)
    }

    companion object {
        var lastReceived = 0L

        fun shouldSaveMessages(context: Context, source: DataSource, message: Message): Boolean {
            try {
                val search = source.searchMessagesAsList(context, message.data, 1, true)
                if (!search.isEmpty()) {
                    val inDatabase = search[0]
                    if (inDatabase.data == message.data && message.timestamp - inDatabase.timestamp < TimeUtils.MINUTE * 10) {
                        return false
                    }
                }
            } catch (e: Exception) {
            }

            return true
        }
    }

}
