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

package xyz.klinker.messenger.shared.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

import xyz.klinker.messenger.api.implementation.Account
import xyz.klinker.messenger.api.implementation.ApiUtils
import xyz.klinker.messenger.shared.data.DataSource
import xyz.klinker.messenger.shared.service.jobs.RepeatNotificationJob

/**
 * A service to get run when a notification is dismissed.
 */
class NotificationDismissedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) {
            return
        }

        Thread {
            handle(intent, context)
        }.start()
    }

    companion object {
        const val EXTRA_CONVERSATION_ID = "conversation_id"

        fun handle(intent: Intent?, context: Context) {
            val conversationId = intent?.getLongExtra(EXTRA_CONVERSATION_ID, 0) ?: return

            if (conversationId == 0L) {
                DataSource.seenAllMessages(context)
            } else {
                DataSource.seenConversation(context, conversationId)
            }

            Log.v("dismissed_notification", "id: $conversationId")
            ApiUtils.dismissNotification(Account.accountId, Account.deviceId, conversationId)
            RepeatNotificationJob.cancel(context)
        }
    }
}
