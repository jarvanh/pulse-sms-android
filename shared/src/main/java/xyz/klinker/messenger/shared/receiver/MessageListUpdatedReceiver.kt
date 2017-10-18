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
import android.content.IntentFilter

import xyz.klinker.messenger.shared.R
import xyz.klinker.messenger.shared.data.MimeType
import xyz.klinker.messenger.shared.data.Settings
import xyz.klinker.messenger.shared.data.model.Message
import xyz.klinker.messenger.shared.service.notification.NotificationConstants
import xyz.klinker.messenger.shared.service.notification.NotificationService
import xyz.klinker.messenger.shared.shared_interfaces.IMessageListFragment
import xyz.klinker.messenger.shared.util.AudioWrapper

/**
 * Receiver that handles updating the message list when a new message is received for the
 * conversation being displayed or the sent/delivered status is updated.
 */
class MessageListUpdatedReceiver(private val fragment: IMessageListFragment) : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        try {
            handleReceiver(context, intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @Throws(Exception::class)
    private fun handleReceiver(context: Context, intent: Intent) {
        val conversationId = intent.getLongExtra(ARG_CONVERSATION_ID, -1)
        val newMessageText = intent.getStringExtra(ARG_NEW_MESSAGE_TEXT)
        val messageType = intent.getIntExtra(ARG_MESSAGE_TYPE, -1)

        if (conversationId == -1L) {
            return
        }

        if (conversationId == fragment.conversationId) {
            if (messageType == Message.TYPE_RECEIVED) {
                fragment.setShouldPullDrafts(false)
                fragment.loadMessages(true)
            } else {
                fragment.loadMessages(false)
            }

            fragment.setDismissOnStartup()

            if (Settings.soundEffects && messageType == Message.TYPE_RECEIVED &&
                    NotificationConstants.CONVERSATION_ID_OPEN == conversationId) {
                Thread {
                    try {
                        Thread.sleep(500)
                    } catch (e: Exception) {
                    }

                    val wrapper = AudioWrapper(context, conversationId)
                    wrapper.play()
                }.start()
            }

            if (newMessageText != null) {
                if (messageType == Message.TYPE_SENDING || messageType == Message.TYPE_SENT) {
                    fragment.setConversationUpdateInfo(context.getString(R.string.you) + ": " + newMessageText)
                } else {
                    fragment.setConversationUpdateInfo(newMessageText)
                }
            }
        }
    }

    companion object {

        private val ACTION_UPDATED = "xyz.klinker.messenger.MESSAGE_UPDATED"
        private val ARG_CONVERSATION_ID = "conversation_id"
        private val ARG_NEW_MESSAGE_TEXT = "new_message_text"
        private val ARG_MESSAGE_TYPE = "message_type"

        /**
         * Sends a broadcast to anywhere that has registered this receiver to let it know to update.
         */
        fun sendBroadcast(context: Context, message: Message) {
            if (message.mimeType == MimeType.TEXT_PLAIN) {
                sendBroadcast(context, message.conversationId, message.data, message.type)
            } else {
                sendBroadcast(context, message.conversationId)
            }
        }

        /**
         * Sends a broadcast to anywhere that has registered this receiver to let it know to update.
         */
        fun sendBroadcast(context: Context, conversationId: Long, newMessageText: String? = null, messageType: Int = Message.TYPE_SENT) {
            val intent = Intent(ACTION_UPDATED)
            intent.putExtra(ARG_CONVERSATION_ID, conversationId)
            intent.putExtra(ARG_NEW_MESSAGE_TEXT, newMessageText)
            intent.putExtra(ARG_MESSAGE_TYPE, messageType)
            context.sendBroadcast(intent)
        }

        /**
         * Gets an intent filter that will pick up these broadcasts.
         */
        val intentFilter: IntentFilter
            get() = IntentFilter(ACTION_UPDATED)
    }

}