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

import android.app.IntentService
import android.content.Intent
import android.support.v4.app.NotificationManagerCompat
import android.support.v4.app.RemoteInput
import android.util.Log
import xyz.klinker.messenger.api.implementation.Account
import xyz.klinker.messenger.api.implementation.ApiUtils
import xyz.klinker.messenger.shared.R
import xyz.klinker.messenger.shared.data.DataSource
import xyz.klinker.messenger.shared.data.MimeType
import xyz.klinker.messenger.shared.data.Settings
import xyz.klinker.messenger.shared.data.model.Message
import xyz.klinker.messenger.shared.receiver.ConversationListUpdatedReceiver
import xyz.klinker.messenger.shared.receiver.MessageListUpdatedReceiver
import xyz.klinker.messenger.shared.service.jobs.MarkAsSentJob
import xyz.klinker.messenger.shared.util.DualSimUtils
import xyz.klinker.messenger.shared.util.SendUtils
import xyz.klinker.messenger.shared.util.TimeUtils
import xyz.klinker.messenger.shared.util.closeSilent
import xyz.klinker.messenger.shared.widget.MessengerAppWidgetProvider

/**
 * Service for getting back voice replies from Android Wear and sending them out.
 */
class ReplyService : IntentService("Reply Service") {

    override fun onHandleIntent(intent: Intent?) {
        val remoteInput = RemoteInput.getResultsFromIntent(intent)
        var reply: String? = null
        if (remoteInput != null) {
            reply = remoteInput.getCharSequence(EXTRA_REPLY)!!.toString()
        }

        if (reply == null) {
            Log.e(TAG, "could not find attached reply")
            return
        }

        val conversationId = intent?.getLongExtra(EXTRA_CONVERSATION_ID, -1) ?: return

        if (conversationId == -1L) {
            Log.e(TAG, "could not find attached conversation id")
            return
        }

        val conversation = DataSource.getConversation(this, conversationId) ?: return

        val m = Message()
        m.conversationId = conversationId
        m.type = Message.TYPE_SENDING
        m.data = reply
        m.timestamp = TimeUtils.now
        m.mimeType = MimeType.TEXT_PLAIN
        m.read = true
        m.seen = true
        m.from = null
        m.color = null
        m.simPhoneNumber = if (conversation.simSubscriptionId != null)
            DualSimUtils
                    .getPhoneNumberFromSimSubscription(conversation.simSubscriptionId!!)
        else
            null
        m.sentDeviceId = if (Account.exists()) java.lang.Long.parseLong(Account.deviceId) else -1L

        val messageId = DataSource.insertMessage(this, m, conversationId, true)
        DataSource.readConversation(this, conversationId)

        Log.v(TAG, "sending message \"" + reply + "\" to \"" + conversation.phoneNumbers + "\"")

        SendUtils(conversation.simSubscriptionId)
                .send(this, reply, conversation.phoneNumbers!!)
        MarkAsSentJob.scheduleNextRun(this, messageId)

        // cancel the notification we just replied to or
        // if there are no more notifications, cancel the summary as well
        val unseenMessages = DataSource.getUnseenMessages(this)
        if (unseenMessages.count <= 0) {
            try {
                NotificationManagerCompat.from(this).cancelAll()
            } catch (e: SecurityException) {
            }
        } else {
            NotificationManagerCompat.from(this).cancel(conversationId.toInt())
        }

//        if (Settings.dismissNotificationAfterReply) {
//            val conversationIdInt = conversationId.toInt()
//            val manager = NotificationManagerCompat.from(this)
//
//            Thread {
//                try {
//                    Thread.sleep(1000)
//                } catch (e: Exception) {
//                }
//
//                manager.cancel(conversationIdInt)
//            }.start()
//        }

        ApiUtils.dismissNotification(Account.accountId,
                Account.deviceId,
                conversationId)

        unseenMessages.closeSilent()

        ConversationListUpdatedReceiver.sendBroadcast(this, conversationId, getString(R.string.you) + ": " + reply, true)
        MessageListUpdatedReceiver.sendBroadcast(this, conversationId)
        MessengerAppWidgetProvider.refreshWidget(this)
    }

    companion object {
        private val TAG = "ReplyService"
        val EXTRA_REPLY = "reply_text"
        val EXTRA_CONVERSATION_ID = "conversation_id"
    }
}
