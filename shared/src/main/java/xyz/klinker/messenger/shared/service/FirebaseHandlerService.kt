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
import android.app.Notification
import android.content.Context
import android.content.Intent
import android.database.sqlite.SQLiteConstraintException
import android.net.Uri
import android.support.v4.app.NotificationCompat
import android.support.v4.app.NotificationManagerCompat
import android.util.Log
import com.klinker.android.send_message.Utils
import org.json.JSONException
import org.json.JSONObject
import xyz.klinker.messenger.api.implementation.Account
import xyz.klinker.messenger.api.implementation.ApiUtils
import xyz.klinker.messenger.api.implementation.LoginActivity
import xyz.klinker.messenger.api.implementation.firebase.FirebaseDownloadCallback
import xyz.klinker.messenger.api.implementation.firebase.MessengerFirebaseMessagingService
import xyz.klinker.messenger.encryption.EncryptionUtils
import xyz.klinker.messenger.shared.R
import xyz.klinker.messenger.shared.data.*
import xyz.klinker.messenger.shared.data.model.*
import xyz.klinker.messenger.shared.receiver.ConversationListUpdatedReceiver
import xyz.klinker.messenger.shared.receiver.MessageListUpdatedReceiver
import xyz.klinker.messenger.shared.service.jobs.MarkAsSentJob
import xyz.klinker.messenger.shared.service.jobs.ScheduledMessageJob
import xyz.klinker.messenger.shared.service.jobs.SignoutJob
import xyz.klinker.messenger.shared.service.jobs.SubscriptionExpirationCheckJob
import xyz.klinker.messenger.shared.service.notification.Notifier
import xyz.klinker.messenger.shared.util.*
import xyz.klinker.messenger.shared.widget.MessengerAppWidgetProvider
import java.io.File
import java.util.*

/**
 * Receiver responsible for processing firebase data messages and persisting to the database.
 */
class FirebaseHandlerService : IntentService("FirebaseHandlerService") {

    //    override fun doWakefulWork(intent: Intent?) {
    override fun onHandleIntent(intent: Intent?) {
        if (intent != null && intent.action != null && intent.action == MessengerFirebaseMessagingService.ACTION_FIREBASE_MESSAGE_RECEIVED) {
            val operation = intent.getStringExtra(MessengerFirebaseMessagingService.EXTRA_OPERATION)
            val data = intent.getStringExtra(MessengerFirebaseMessagingService.EXTRA_DATA)

            process(this, operation, data)
        }
    }

    companion object {

        private val TAG = "FirebaseHandlerService"
        private val INFORMATION_NOTIFICATION_ID = 13

        fun process(context: Context, operation: String, data: String) {
            val account = Account

            // received a message without having initialized an account yet
            // could happen if their subscription ends
            if (account.key == null) {
                return
            }

            val encryptionUtils = account.encryptor

            if (encryptionUtils == null && account.exists()) {
                context.startActivity(Intent(context, LoginActivity::class.java))
                return
            }


            Log.v(TAG, "operation: $operation, contents: $data")

            try {
                val json = JSONObject(data)
                when (operation) {
                    "removed_account" -> removeAccount(json, context)
                    "updated_account" -> updatedAccount(json, context)
                    "cleaned_account" -> cleanAccount(json, context)
                    "added_message" -> addMessage(json, context, encryptionUtils)
                    "update_message_type" -> updateMessageType(json, context)
                    "updated_message" -> updateMessage(json, context)
                    "removed_message" -> removeMessage(json, context)
                    "cleanup_messages" -> cleanupMessages(json, context)
                    "cleanup_conversation_messages" -> cleanupConversationMessages(json, context)
                    "added_contact" -> addContact(json, context, encryptionUtils)
                    "updated_contact" -> updateContact(json, context, encryptionUtils)
                    "removed_contact" -> removeContact(json, context)
                    "removed_contact_by_id" -> removeContactById(json, context)
                    "added_conversation" -> addConversation(json, context, encryptionUtils)
                    "update_conversation_snippet" -> updateConversationSnippet(json, context, encryptionUtils)
                    "update_conversation_title" -> updateConversationTitle(json, context, encryptionUtils)
                    "updated_conversation" -> updateConversation(json, context, encryptionUtils)
                    "removed_conversation" -> removeConversation(json, context)
                    "read_conversation" -> readConversation(json, context)
                    "seen_conversation" -> seenConversation(json, context)
                    "archive_conversation" -> archiveConversation(json, context)
                    "seen_conversations" -> seenConversations(context)
                    "added_draft" -> addDraft(json, context, encryptionUtils)
                    "removed_drafts" -> removeDrafts(json, context)
                    "added_blacklist" -> addBlacklist(json, context, encryptionUtils)
                    "removed_blacklist" -> removeBlacklist(json, context)
                    "added_scheduled_message" -> addScheduledMessage(json, context, encryptionUtils)
                    "updated_scheduled_message" -> updatedScheduledMessage(json, context, encryptionUtils)
                    "removed_scheduled_message" -> removeScheduledMessage(json, context)
                    "added_folder" -> addFolder(json, context, encryptionUtils)
                    "add_conversation_to_folder" -> addConversationToFolder(json, context)
                    "remove_conversation_from_folder" -> removeConversationFromFolder(json, context)
                    "updated_folder" -> updateFolder(json, context, encryptionUtils)
                    "removed_folder" -> removeFolder(json, context)
                    "added_template" -> addTemplate(json, context, encryptionUtils)
                    "updated_template" -> updateTemplate(json, context, encryptionUtils)
                    "removed_template" -> removeTemplate(json, context)
                    "added_auto_reply" -> addAutoReply(json, context, encryptionUtils)
                    "updated_auto_reply" -> updateAutoReply(json, context, encryptionUtils)
                    "removed_auto_reply" -> removeAutoReply(json, context)
                    "update_setting" -> updateSetting(json, context)
                    "dismissed_notification" -> dismissNotification(json, context)
                    "update_subscription" -> updateSubscription(json, context)
                    "update_primary_device" -> updatePrimaryDevice(json, context)
                    "feature_flag" -> writeFeatureFlag(json, context)
                    "forward_to_phone" -> forwardToPhone(json, context, encryptionUtils)
                    else -> Log.e(TAG, "unsupported operation: $operation")
                }
            } catch (e: JSONException) {
                Log.e(TAG, "error parsing data json", e)
            }

        }

        @Throws(JSONException::class)
        private fun removeAccount(json: JSONObject, context: Context) {
            val account = Account

            if (json.getString("id") == account.accountId) {
                Log.v(TAG, "clearing account")
                DataSource.clearTables(context)
                account.clearAccount(context)
            } else {
                Log.v(TAG, "ids do not match, did not clear account")
            }
        }

        @Throws(JSONException::class)
        private fun updatedAccount(json: JSONObject, context: Context) {
            val account = Account
            val name = json.getString("real_name")
            val number = json.getString("phone_number")

            if (json.getString("id") == account.accountId) {
                account.setName(context, name)
                account.setPhoneNumber(context, number)
                Log.v(TAG, "updated account name and number")
            } else {
                Log.v(TAG, "ids do not match, did not clear account")
            }
        }

        @Throws(JSONException::class)
        private fun cleanAccount(json: JSONObject, context: Context) {
            val account = Account

            if (json.getString("id") == account.accountId) {
                Log.v(TAG, "clearing account")
                DataSource.clearTables(context)
            } else {
                Log.v(TAG, "ids do not match, did not clear account")
            }
        }

        @Throws(JSONException::class)
        private fun addMessage(json: JSONObject, context: Context, encryptionUtils: EncryptionUtils?) {
            val id = getLong(json, "id")
            if (DataSource.getMessage(context, id) == null) {
                var conversation = DataSource.getConversation(context, getLong(json, "conversation_id"))

                val message = Message()
                message.id = id
                message.conversationId = if (conversation == null) getLong(json, "conversation_id") else conversation.id
                message.type = json.getInt("type")
                message.timestamp = getLong(json, "timestamp")
                message.read = json.getBoolean("read")
                message.seen = json.getBoolean("seen")
                message.simPhoneNumber = if (conversation == null || conversation.simSubscriptionId == null)
                    null
                else DualSimUtils.getPhoneNumberFromSimSubscription(conversation.simSubscriptionId!!)

                if (json.has("sent_device")) {
                    try {
                        message.sentDeviceId = json.getLong("sent_device")
                    } catch (e: Exception) {
                        message.sentDeviceId = -1L
                    }
                } else {
                    message.sentDeviceId = -1L
                }

                if (Account.primary && message.type == Message.TYPE_MEDIA) {
                    return
                }

                if (Account.deviceId != null && message.sentDeviceId == Account.deviceId!!.toLong()) {
                    return
                }

                try {
                    message.data = encryptionUtils!!.decrypt(json.getString("data"))
                    message.mimeType = encryptionUtils.decrypt(json.getString("mime_type"))
                    message.from = encryptionUtils.decrypt(if (json.has("from")) json.getString("from") else null)
                } catch (e: Exception) {
                    Log.v(TAG, "error adding message, from decyrption.")
                    message.data = context.getString(R.string.error_decrypting)
                    message.mimeType = MimeType.TEXT_PLAIN
                    message.from = null
                }

                if (json.has("color") && json.getString("color") != "null") {
                    message.color = json.getInt("color")
                }

                if (message.data == "firebase -1" && message.mimeType != MimeType.TEXT_PLAIN) {
                    Log.v(TAG, "downloading binary from firebase")

                    addMessageAfterFirebaseDownload(context, encryptionUtils!!, message)
                    return
                }

                val messageId = DataSource.insertMessage(context, message, message.conversationId, true, false)
                Log.v(TAG, "added message")

                if (!Utils.isDefaultSmsApp(context) && Account.primary && message.type == Message.TYPE_SENDING) {
                    Thread {
                        try {
                            Thread.sleep(500)
                        } catch (e: Exception) {
                        }

                        DataSource.updateMessageType(context, id, Message.TYPE_SENT, true)
                    }.start()
                }

                val isSending = message.type == Message.TYPE_SENDING

                if (!Utils.isDefaultSmsApp(context) && isSending) {
                    message.type = Message.TYPE_SENT
                }

                if (Account.primary && isSending) {
                    conversation = DataSource.getConversation(context, message.conversationId)

                    if ((message.timestamp > TimeUtils.now || message.timestamp < (TimeUtils.now - TimeUtils.MINUTE * 5))) {

                        // if the phone receives a message and the timestamp doesn't seem in-line with the phones, we should update the timestamp:
                        // if the timestamp of the message going in to the database is in the future, then update it.
                        // if the timestamp is in the past (5 mins or more), then update it, since this is when it is actually getting sent.
                        message.timestamp = TimeUtils.now
                        DataSource.updateMessageTimestamp(context, id, message.timestamp, true)

                    }

                    if (conversation != null) {
                        if (message.mimeType == MimeType.TEXT_PLAIN) {
                            SendUtils(conversation.simSubscriptionId)
                                    .send(context, message.data!!, conversation.phoneNumbers!!)
                            MarkAsSentJob.scheduleNextRun(context, messageId)
                        } else {
                            SendUtils(conversation.simSubscriptionId)
                                    .send(context, "", conversation.phoneNumbers!!,
                                            Uri.parse(message.data), message.mimeType)
                            MarkAsSentJob.scheduleNextRun(context, messageId)
                        }
                    } else {
                        Log.e(TAG, "trying to send message without the conversation, so can't find phone numbers")
                    }

                    Log.v(TAG, "sent message")
                }

                MessageListUpdatedReceiver.sendBroadcast(context, message)
                ConversationListUpdatedReceiver.sendBroadcast(context, message.conversationId,
                        if (message.mimeType == MimeType.TEXT_PLAIN) message.data else MimeType.getTextDescription(context, message.mimeType!!),
                        message.type != Message.TYPE_RECEIVED)

                if (message.type == Message.TYPE_RECEIVED) {
                    Notifier(context).notify()
                } else if (isSending) {
                    DataSource.readConversation(context, message.conversationId, false)
                    NotificationManagerCompat.from(context).cancel(message.conversationId.toInt())
                    NotificationUtils.cancelGroupedNotificationWithNoContent(context)
                }
            } else {
                Log.v(TAG, "message already exists, not doing anything with it")
            }
        }

        private fun addMessageAfterFirebaseDownload(context: Context, encryptionUtils: EncryptionUtils, message: Message, to: String? = null) {
            val apiUtils = ApiUtils
            apiUtils.saveFirebaseFolderRef(Account.accountId)
            val file = File(context.filesDir,
                    message.id.toString() + MimeType.getExtension(message.mimeType!!))

            if (to == null) {
                DataSource.insertMessage(context, message, message.conversationId, false, false)
            } else {
                message.conversationId = DataSource.insertMessage(message, to, context, true)
            }

            Log.v(TAG, "added message")

            val isSending = message.type == Message.TYPE_SENDING

            if (!Utils.isDefaultSmsApp(context) && isSending) {
                message.type = Message.TYPE_SENT
            }

            val callback = FirebaseDownloadCallback {
                message.data = Uri.fromFile(file).toString()
                DataSource.updateMessageData(context, message.id, message.data!!)
                MessageListUpdatedReceiver.sendBroadcast(context, message.conversationId)

                if (Account.primary && isSending) {
                    val conversation = DataSource.getConversation(context, message.conversationId)

                    if (conversation != null) {
                        if (message.mimeType == MimeType.TEXT_PLAIN) {
                            SendUtils(conversation.simSubscriptionId)
                                    .send(context, message.data!!, conversation.phoneNumbers!!)
                        } else {
                            SendUtils(conversation.simSubscriptionId)
                                    .send(context, "", conversation.phoneNumbers!!,
                                            Uri.parse(message.data), message.mimeType)
                        }
                    } else {
                        Log.e(TAG, "trying to send message without the conversation, so can't find phone numbers")
                    }

                    Log.v(TAG, "sent message")
                }

                if (!Utils.isDefaultSmsApp(context) && Account.primary && message.type == Message.TYPE_SENDING) {
                    DataSource.updateMessageType(context, message.id, Message.TYPE_SENT, false)
                }

                MessageListUpdatedReceiver.sendBroadcast(context, message)
                ConversationListUpdatedReceiver.sendBroadcast(context, message.conversationId,
                        if (message.mimeType == MimeType.TEXT_PLAIN) message.data else MimeType.getTextDescription(context, message.mimeType),
                        message.type != Message.TYPE_RECEIVED)

                when (message.type) {
                    Message.TYPE_RECEIVED -> {
                        Notifier(context).notify()
                    }
                    Message.TYPE_SENDING -> {
                        DataSource.readConversation(context, message.conversationId, false)
                        NotificationManagerCompat.from(context).cancel(message.conversationId.toInt())
                        NotificationUtils.cancelGroupedNotificationWithNoContent(context)
                    }
                    else -> { }
                }
            }

            apiUtils.downloadFileFromFirebase(Account.accountId, file, message.id, encryptionUtils, callback, 0)

        }

        @Throws(JSONException::class)
        private fun updateMessage(json: JSONObject, context: Context) {
            val id = getLong(json, "id")
            val type = json.getInt("type")
            DataSource.updateMessageType(context, id, type, false)

            val timestamp = json.getString("timestamp")
            if (timestamp != null) {
                DataSource.updateMessageTimestamp(context, id, timestamp.toLong(), false)
            }

            val message = DataSource.getMessage(context, id)
            if (message != null) {
                MessageListUpdatedReceiver.sendBroadcast(context, message)
            }

            Log.v(TAG, "updated message type")
        }

        @Throws(JSONException::class)
        private fun updateMessageType(json: JSONObject, context: Context) {
            val id = getLong(json, "id")
            val type = json.getInt("message_type")
            DataSource.updateMessageType(context, id, type, false)

            val message = DataSource.getMessage(context, id)
            if (message != null) {
                MessageListUpdatedReceiver.sendBroadcast(context, message)
            }

            Log.v(TAG, "updated message type")
        }

        @Throws(JSONException::class)
        private fun removeMessage(json: JSONObject, context: Context) {
            val id = getLong(json, "id")
            DataSource.deleteMessage(context, id, false)
            Log.v(TAG, "removed message")
        }

        @Throws(JSONException::class)
        private fun cleanupMessages(json: JSONObject, context: Context) {
            val timestamp = getLong(json, "timestamp")
            DataSource.cleanupOldMessages(context, timestamp, false)
            Log.v(TAG, "cleaned up old messages")
        }

        @Throws(JSONException::class)
        private fun cleanupConversationMessages(json: JSONObject, context: Context) {
            val timestamp = getLong(json, "timestamp")
            val conversationId = getLong(json, "conversation_id")

            DataSource.cleanupOldMessagesInConversation(context, conversationId, timestamp, false)
            Log.v(TAG, "cleaned up old messages in conversation")
        }

        @Throws(JSONException::class)
        private fun addConversation(json: JSONObject, context: Context, encryptionUtils: EncryptionUtils?) {
            val conversation = Conversation()
            conversation.id = getLong(json, "id")
            conversation.colors.color = json.getInt("color")
            conversation.colors.colorDark = json.getInt("color_dark")
            conversation.colors.colorLight = json.getInt("color_light")
            conversation.colors.colorAccent = json.getInt("color_accent")
            conversation.ledColor = json.getInt("led_color")
            conversation.pinned = json.getBoolean("pinned")
            conversation.read = json.getBoolean("read")
            conversation.timestamp = getLong(json, "timestamp")
            conversation.title = encryptionUtils!!.decrypt(json.getString("title"))
            conversation.phoneNumbers = encryptionUtils.decrypt(json.getString("phone_numbers"))
            conversation.snippet = encryptionUtils.decrypt(json.getString("snippet"))
            conversation.ringtoneUri = encryptionUtils.decrypt(if (json.has("ringtone"))
                json.getString("ringtone") else null)
            conversation.imageUri = ContactUtils.findImageUri(conversation.phoneNumbers, context)
            conversation.idMatcher = encryptionUtils.decrypt(json.getString("id_matcher"))
            conversation.mute = json.getBoolean("mute")
            conversation.archive = json.getBoolean("archive")
            conversation.simSubscriptionId = -1
            conversation.folderId = if (json.has("folder_id")) json.getLong("folder_id") else null

            val image = ImageUtils.getContactImage(conversation.imageUri, context)
            if (conversation.imageUri != null && image == null) {
                conversation.imageUri = null
            } else if (conversation.imageUri != null) {
                conversation.imageUri = conversation.imageUri!! + "/photo"
            }

            image?.recycle()

            try {
                DataSource.insertConversation(context, conversation, false)
            } catch (e: SQLiteConstraintException) {
                // conversation already exists
            }

        }

        @Throws(JSONException::class)
        private fun updateContact(json: JSONObject, context: Context, encryptionUtils: EncryptionUtils?) {
            try {
                val contact = Contact()
                contact.id = json.getLong("device_id")
                contact.phoneNumber = encryptionUtils!!.decrypt(json.getString("phone_number"))
                contact.name = encryptionUtils.decrypt(json.getString("name"))
                contact.colors.color = json.getInt("color")
                contact.colors.colorDark = json.getInt("color_dark")
                contact.colors.colorLight = json.getInt("color_light")
                contact.colors.colorAccent = json.getInt("color_accent")

                DataSource.updateContact(context, contact, false)
                Log.v(TAG, "updated contact")
            } catch (e: RuntimeException) {
                Log.e(TAG, "failed to update contact b/c of decrypting data")
            }

        }

        @Throws(JSONException::class)
        private fun removeContact(json: JSONObject, context: Context) {
            val phoneNumber = json.getString("phone_number")
            val deviceId = json.getLong("device_id")
            DataSource.deleteContact(context, deviceId, phoneNumber, false)
            Log.v(TAG, "removed contact")
        }

        @Throws(JSONException::class)
        private fun removeContactById(json: JSONObject, context: Context) {
            val ids = json.getString("id").split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            DataSource.deleteContacts(context, ids, false)
            Log.v(TAG, "removed contacts by id")
        }

        @Throws(JSONException::class)
        private fun addContact(json: JSONObject, context: Context, encryptionUtils: EncryptionUtils?) {

            try {
                val contact = Contact()
                contact.phoneNumber = encryptionUtils!!.decrypt(json.getString("phone_number"))
                contact.name = encryptionUtils.decrypt(json.getString("name"))
                contact.colors.color = json.getInt("color")
                contact.colors.colorDark = json.getInt("color_dark")
                contact.colors.colorLight = json.getInt("color_light")
                contact.colors.colorAccent = json.getInt("color_accent")

                DataSource.insertContact(context, contact, false)
                Log.v(TAG, "added contact")
            } catch (e: SQLiteConstraintException) {
                // contact already exists
                Log.e(TAG, "error adding contact", e)
            } catch (e: Exception) {
                // error decrypting
            }

        }

        @Throws(JSONException::class)
        private fun updateConversation(json: JSONObject, context: Context, encryptionUtils: EncryptionUtils?) {
            try {
                val conversation = Conversation()
                conversation.id = getLong(json, "id")
                conversation.title = encryptionUtils!!.decrypt(json.getString("title"))
                conversation.colors.color = json.getInt("color")
                conversation.colors.colorDark = json.getInt("color_dark")
                conversation.colors.colorLight = json.getInt("color_light")
                conversation.colors.colorAccent = json.getInt("color_accent")
                conversation.ledColor = json.getInt("led_color")
                conversation.pinned = json.getBoolean("pinned")
                conversation.ringtoneUri = encryptionUtils.decrypt(if (json.has("ringtone"))
                    json.getString("ringtone") else null)
                conversation.mute = json.getBoolean("mute")
                conversation.read = json.getBoolean("read")
                conversation.read = json.getBoolean("read")
                conversation.archive = json.getBoolean("archive")
                conversation.private = json.getBoolean("private_notifications")

                DataSource.updateConversationSettings(context, conversation, false)

                if (conversation.read) {
                    DataSource.readConversation(context, conversation.id, false)
                }
                Log.v(TAG, "updated conversation")
            } catch (e: RuntimeException) {
                Log.e(TAG, "failed to update conversation b/c of decrypting data")
            }

        }

        @Throws(JSONException::class)
        private fun updateConversationTitle(json: JSONObject, context: Context, encryptionUtils: EncryptionUtils?) {
            try {
                DataSource.updateConversationTitle(context, getLong(json, "id"),
                        encryptionUtils!!.decrypt(json.getString("title"))!!, false
                )

                Log.v(TAG, "updated conversation title")
            } catch (e: RuntimeException) {
                Log.e(TAG, "failed to update conversation title b/c of decrypting data")
            }

        }

        @Throws(JSONException::class)
        private fun updateConversationSnippet(json: JSONObject, context: Context, encryptionUtils: EncryptionUtils?) {
            try {
                DataSource.updateConversation(context,
                        getLong(json, "id"),
                        json.getBoolean("read"),
                        getLong(json, "timestamp"),
                        encryptionUtils!!.decrypt(json.getString("snippet")),
                        MimeType.TEXT_PLAIN,
                        json.getBoolean("archive"),
                        false
                )

                Log.v(TAG, "updated conversation snippet")
            } catch (e: RuntimeException) {
                Log.e(TAG, "failed to update conversation snippet b/c of decrypting data")
            }

        }

        @Throws(JSONException::class)
        private fun removeConversation(json: JSONObject, context: Context) {
            val id = getLong(json, "id")
            DataSource.deleteConversation(context, id, false)
            Log.v(TAG, "removed conversation")
        }

        @Throws(JSONException::class)
        private fun readConversation(json: JSONObject, context: Context) {
            val id = getLong(json, "id")
            val deviceId = json.getString("android_device")

            if (deviceId == null || deviceId != Account.deviceId) {
                val conversation = DataSource.getConversation(context, id)
                DataSource.readConversation(context, id, false)

                if (conversation != null && !conversation.read) {
                    ConversationListUpdatedReceiver.sendBroadcast(context, id, conversation.snippet, true)
                }

                Log.v(TAG, "read conversation")
            }
        }

        @Throws(JSONException::class)
        private fun seenConversation(json: JSONObject, context: Context) {
            val id = getLong(json, "id")
            DataSource.seenConversation(context, id, false)
            Log.v(TAG, "seen conversation")
        }

        @Throws(JSONException::class)
        private fun archiveConversation(json: JSONObject, context: Context) {
            val id = getLong(json, "id")
            val archive = json.getBoolean("archive")
            DataSource.archiveConversation(context, id, archive, false)
            Log.v(TAG, "archive conversation: " + archive)
        }

        @Throws(JSONException::class)
        private fun seenConversations(context: Context) {
            DataSource.seenConversations(context, false)
            Log.v(TAG, "seen all conversations")
        }

        @Throws(JSONException::class)
        private fun addDraft(json: JSONObject, context: Context, encryptionUtils: EncryptionUtils?) {
            val draft = Draft()
            draft.id = getLong(json, "id")
            draft.conversationId = getLong(json, "conversation_id")
            draft.data = encryptionUtils!!.decrypt(json.getString("data"))
            draft.mimeType = encryptionUtils.decrypt(json.getString("mime_type"))

            DataSource.insertDraft(context, draft, false)
            Log.v(TAG, "added draft")
        }

        @Throws(JSONException::class)
        private fun removeDrafts(json: JSONObject, context: Context) {
            val id = getLong(json, "id")
            val deviceId = json.getString("android_device")

            if (deviceId == null || deviceId != Account.deviceId) {
                DataSource.deleteDrafts(context, id, false)
                Log.v(TAG, "removed drafts")
            }
        }

        @Throws(JSONException::class)
        private fun addBlacklist(json: JSONObject, context: Context, encryptionUtils: EncryptionUtils?) {
            val id = getLong(json, "id")
            var phoneNumber: String? = json.getString("phone_number")
            phoneNumber = encryptionUtils!!.decrypt(phoneNumber)

            val blacklist = Blacklist()
            blacklist.id = id
            blacklist.phoneNumber = phoneNumber
            DataSource.insertBlacklist(context, blacklist, false)
            Log.v(TAG, "added blacklist")
        }

        @Throws(JSONException::class)
        private fun removeBlacklist(json: JSONObject, context: Context) {
            val id = getLong(json, "id")
            DataSource.deleteBlacklist(context, id, false)
            Log.v(TAG, "removed blacklist")
        }

        @Throws(JSONException::class)
        private fun addScheduledMessage(json: JSONObject, context: Context, encryptionUtils: EncryptionUtils?) {
            val message = ScheduledMessage()
            message.id = getLong(json, "id")
            message.to = encryptionUtils!!.decrypt(json.getString("to"))
            message.data = encryptionUtils.decrypt(json.getString("data"))
            message.mimeType = encryptionUtils.decrypt(json.getString("mime_type"))
            message.timestamp = getLong(json, "timestamp")
            message.title = encryptionUtils.decrypt(json.getString("title"))

            DataSource.insertScheduledMessage(context, message, false)
            ScheduledMessageJob.scheduleNextRun(context, DataSource)
            Log.v(TAG, "added scheduled message")
        }

        @Throws(JSONException::class)
        private fun updatedScheduledMessage(json: JSONObject, context: Context, encryptionUtils: EncryptionUtils?) {
            val message = ScheduledMessage()
            message.id = getLong(json, "id")
            message.to = encryptionUtils!!.decrypt(json.getString("to"))
            message.data = encryptionUtils.decrypt(json.getString("data"))
            message.mimeType = encryptionUtils.decrypt(json.getString("mime_type"))
            message.timestamp = getLong(json, "timestamp")
            message.title = encryptionUtils.decrypt(json.getString("title"))

            DataSource.updateScheduledMessage(context, message, false)
            ScheduledMessageJob.scheduleNextRun(context, DataSource)
            Log.v(TAG, "updated scheduled message")
        }

        @Throws(JSONException::class)
        private fun removeScheduledMessage(json: JSONObject, context: Context) {
            val id = getLong(json, "id")
            DataSource.deleteScheduledMessage(context, id, false)
            ScheduledMessageJob.scheduleNextRun(context, DataSource)
            Log.v(TAG, "removed scheduled message")
        }

        @Throws(JSONException::class)
        private fun addTemplate(json: JSONObject, context: Context, encryptionUtils: EncryptionUtils?) {
            val template = Template()
            template.id = getLong(json, "device_id")
            template.text = encryptionUtils!!.decrypt(json.getString("text"))

            DataSource.insertTemplate(context, template, false)
            Log.v(TAG, "added template")
        }

        @Throws(JSONException::class)
        private fun updateTemplate(json: JSONObject, context: Context, encryptionUtils: EncryptionUtils?) {
            val template = Template()
            template.id = getLong(json, "device_id")
            template.text = encryptionUtils!!.decrypt(json.getString("text"))

            DataSource.updateTemplate(context, template, false)
            Log.v(TAG, "updated template")
        }

        @Throws(JSONException::class)
        private fun removeTemplate(json: JSONObject, context: Context) {
            val id = getLong(json, "id")
            DataSource.deleteTemplate(context, id, false)
            Log.v(TAG, "removed template")
        }

        @Throws(JSONException::class)
        private fun addAutoReply(json: JSONObject, context: Context, encryptionUtils: EncryptionUtils?) {
            val reply = AutoReply()
            reply.id = getLong(json, "device_id")
            reply.type = json.getString("type")
            reply.pattern = encryptionUtils!!.decrypt(json.getString("pattern"))
            reply.response = encryptionUtils.decrypt(json.getString("response"))

            DataSource.insertAutoReply(context, reply, false)
            Log.v(TAG, "added auto reply")
        }

        @Throws(JSONException::class)
        private fun updateAutoReply(json: JSONObject, context: Context, encryptionUtils: EncryptionUtils?) {
            val reply = AutoReply()
            reply.id = getLong(json, "device_id")
            reply.type = json.getString("type")
            reply.pattern = encryptionUtils!!.decrypt(json.getString("pattern"))
            reply.response = encryptionUtils.decrypt(json.getString("response"))

            DataSource.updateAutoReply(context, reply, false)
            Log.v(TAG, "updated auto reply")
        }

        @Throws(JSONException::class)
        private fun removeAutoReply(json: JSONObject, context: Context) {
            val id = getLong(json, "id")
            val reply = DataSource.getAutoRepliesAsList(context).firstOrNull { it.id == id }
            DataSource.deleteAutoReply(context, id, false)

            Log.v(TAG, "removed auto reply")

            if (reply == null) {
                return
            }

            when (reply.type) {
                AutoReply.TYPE_VACATION -> Settings.getSharedPrefs(context).edit()
                        .putBoolean(context.getString(R.string.pref_vacation_mode), false)
                        .putString(context.getString(R.string.pref_vacation_mode_editable), "")
                        .apply()
                AutoReply.TYPE_DRIVING -> Settings.getSharedPrefs(context).edit()
                        .putBoolean(context.getString(R.string.pref_driving_mode), false)
                        .putString(context.getString(R.string.pref_driving_mode_editable), "")
                        .apply()
            }
        }

        @Throws(JSONException::class)
        private fun addConversationToFolder(json: JSONObject, context: Context) {
            val conversationId = getLong(json, "id")
            val folderId = getLong(json, "folder_id")

            DataSource.addConversationToFolder(context, conversationId, folderId, false)
            Log.v(TAG, "added conversation to folder")
        }

        @Throws(JSONException::class)
        private fun removeConversationFromFolder(json: JSONObject, context: Context) {
            val conversationId = getLong(json, "id")

            DataSource.removeConversationFromFolder(context, conversationId, false)
            Log.v(TAG, "removed conversation from folder")
        }

        @Throws(JSONException::class)
        private fun addFolder(json: JSONObject, context: Context, encryptionUtils: EncryptionUtils?) {
            val folder = Folder()
            folder.id = getLong(json, "device_id")
            folder.name = encryptionUtils!!.decrypt(json.getString("name"))
            folder.colors.color = json.getInt("color")
            folder.colors.colorDark = json.getInt("color_dark")
            folder.colors.colorLight = json.getInt("color_light")
            folder.colors.colorAccent = json.getInt("color_accent")

            DrawerItemHelper.folders = null

            DataSource.insertFolder(context, folder, false)
            Log.v(TAG, "added folder")
        }

        @Throws(JSONException::class)
        private fun updateFolder(json: JSONObject, context: Context, encryptionUtils: EncryptionUtils?) {
            val folder = Folder()
            folder.id = getLong(json, "device_id")
            folder.name = encryptionUtils!!.decrypt(json.getString("name"))
            folder.colors.color = json.getInt("color")
            folder.colors.colorDark = json.getInt("color_dark")
            folder.colors.colorLight = json.getInt("color_light")
            folder.colors.colorAccent = json.getInt("color_accent")

            DrawerItemHelper.folders = null

            DataSource.updateFolder(context, folder, false)
            Log.v(TAG, "updated folder")
        }

        @Throws(JSONException::class)
        private fun removeFolder(json: JSONObject, context: Context) {
            val id = getLong(json, "id")
            DataSource.deleteFolder(context, id, false)

            DrawerItemHelper.folders = null

            Log.v(TAG, "removed folder")
        }

        @Throws(JSONException::class)
        private fun dismissNotification(json: JSONObject, context: Context) {
            val conversationId = getLong(json, "id")
            val deviceId = json.getString("device_id")

            if (deviceId == null || deviceId != Account.deviceId) {
                val conversation = DataSource.getConversation(context, conversationId)

                // don't want to mark as read if this device was the one that sent the dismissal fcm message
                DataSource.readConversation(context, conversationId, false)
                if (conversation != null && !conversation.read) {
                    ConversationListUpdatedReceiver.sendBroadcast(context, conversationId, conversation.snippet, true)
                    MessengerAppWidgetProvider.refreshWidget(context)
                }

                NotificationManagerCompat.from(context).cancel(conversationId.toInt())
                NotificationUtils.cancelGroupedNotificationWithNoContent(context)
                Log.v(TAG, "dismissed notification for " + conversationId)
            }
        }

        @Throws(JSONException::class)
        private fun updateSetting(json: JSONObject, context: Context) {
            val pref = json.getString("pref")
            val type = json.getString("type")

            if (pref != null && type != null && json.has("value")) {
                val settings = Settings
                when (type.toLowerCase(Locale.getDefault())) {
                    "boolean" -> settings.setValue(context, pref, json.getBoolean("value"))
                    "long" -> settings.setValue(context, pref, getLong(json, "value"))
                    "int" -> settings.setValue(context, pref, json.getInt("value"))
                    "string" -> settings.setValue(context, pref, json.getString("value"))
                    "set" -> settings.setValue(context, pref, SetUtils.createSet(json.getString("value")))
                }

                if (type.toLowerCase() == "string" && pref == context.getString(R.string.pref_secure_private_conversations)) {
                    settings.setValue(context, pref, Account.encryptor?.decrypt(json.getString("value")) ?: "")
                } else if (type.toLowerCase() == "boolean" && pref == context.getString(R.string.pref_quick_compose)) {
                    val turnOn = json.getBoolean("value")
                    if (turnOn) {
                        QuickComposeNotificationService.start(context)
                    } else {
                        QuickComposeNotificationService.stop(context)
                    }
                } else if (type.toLowerCase() == "string" && pref == context.getString(R.string.pref_quick_compose_favorites)) {
                    QuickComposeNotificationService.stop(context)
                    QuickComposeNotificationService.start(context)
                }
            }
        }

        @Throws(JSONException::class)
        private fun updateSubscription(json: JSONObject, context: Context) {
            val type = if (json.has("type")) json.getInt("type") else 0
            val expiration = if (json.has("expiration")) json.getLong("expiration") else 0L
            val fromAdmin = if (json.has("from_admin")) json.getBoolean("from_admin") else false

            val account = Account

            if (account.primary) {
                account.updateSubscription(context,
                        Account.SubscriptionType.findByTypeCode(type), expiration, false
                )

                SubscriptionExpirationCheckJob.scheduleNextRun(context)
                SignoutJob.writeSignoutTime(context, 0)

                if (fromAdmin) {
                    var content = "Enjoy the app!"

                    if (account.subscriptionType != Account.SubscriptionType.LIFETIME) {
                        content = "Expiration: " + Date(expiration).toString()
                    }

                    notifyUser(context, "Subscription Updated: " + StringUtils.titleize(account.subscriptionType!!.name), content)
                }
            }
        }

        @Throws(JSONException::class)
        private fun updatePrimaryDevice(json: JSONObject, context: Context) {
            val newPrimaryDeviceId = json.getString("new_primary_device_id")

            val account = Account
            if (newPrimaryDeviceId != null && newPrimaryDeviceId != account.deviceId) {
                account.setPrimary(context, false)
            }
        }

        @Throws(JSONException::class)
        private fun writeFeatureFlag(json: JSONObject, context: Context) {

            val identifier = json.getString("id")
            val value = json.getBoolean("value")
            val rolloutPercent = json.getInt("rollout") // 1 - 100

            if (!value) {
                // if we are turning the flag off, we want to do it for everyone immediately
                FeatureFlags.updateFlag(context, identifier, false)
            } else {
                val rand = Random()
                val random = rand.nextInt(100) + 1 // between 1 - 100

                if (random <= rolloutPercent) {
                    // they made it in the staged rollout!
                    FeatureFlags.updateFlag(context, identifier, true)
                }

                // otherwise, don't do anything. We don't want to turn the flag off for those
                // that had gotten it turned on in the past.
            }
        }

        @Throws(JSONException::class)
        private fun forwardToPhone(json: JSONObject, context: Context, encryptionUtils: EncryptionUtils?) {

            if (!Account.primary) {
                return
            }

            val mimeType = json.getString("mime_type")
            val text = json.getString("message")
            val toFromWeb = json.getString("to")
            val split = toFromWeb.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()

            var to = ""
            for (i in split.indices) {
                if (i != 0) {
                    to += ", "
                }

                to += PhoneNumberUtils.clearFormatting(split[i])
            }

            val message = Message()
            message.type = Message.TYPE_SENDING
            message.data = text
            message.timestamp = TimeUtils.now
            message.mimeType = mimeType ?: MimeType.TEXT_PLAIN
            message.read = true
            message.seen = true
            message.simPhoneNumber = if (DualSimUtils.availableSims.size > 1) DualSimUtils.defaultPhoneNumber else null

            if (json.has("sent_device")) {
                message.sentDeviceId = json.getLong("sent_device")
            } else {
                message.sentDeviceId = 0
            }

            if (message.data == "firebase -1" && message.mimeType != MimeType.TEXT_PLAIN) {
                Log.v(TAG, "downloading binary from firebase")

                addMessageAfterFirebaseDownload(context, encryptionUtils!!, message, to)
                return
            } else {
                val conversationId = DataSource.insertMessage(message, to, context, true)
                val conversation = DataSource.getConversation(context, conversationId)

                SendUtils(conversation?.simSubscriptionId).send(context, message.data!!, to)
            }
        }

        private fun getLong(json: JSONObject, identifier: String) = try {
            val str = json.getString(identifier)
            java.lang.Long.parseLong(str)
        } catch (e: Exception) {
            0L
        }

        private fun notifyUser(context: Context, title: String, content: String) {
            val builder = NotificationCompat.Builder(context, NotificationUtils.SILENT_BACKGROUND_CHANNEL_ID)
                    .setContentTitle(title)
                    .setContentText(content)
                    .setStyle(NotificationCompat.BigTextStyle().setBigContentTitle(title).setSummaryText(content))
                    .setSmallIcon(R.drawable.ic_stat_notify_group)
                    .setPriority(Notification.PRIORITY_HIGH)
                    .setColor(Settings.mainColorSet.color)

            NotificationManagerCompat.from(context).notify(INFORMATION_NOTIFICATION_ID, builder.build())
        }
    }
}
