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

import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.support.annotation.VisibleForTesting
import android.util.Log

import xyz.klinker.messenger.api.implementation.Account
import xyz.klinker.messenger.shared.data.DataSource
import xyz.klinker.messenger.shared.data.MimeType
import xyz.klinker.messenger.shared.data.MmsSettings
import xyz.klinker.messenger.shared.data.model.Message
import xyz.klinker.messenger.shared.service.AutoReplyParserService
import xyz.klinker.messenger.shared.service.MediaParserService
import xyz.klinker.messenger.shared.service.notification.NotificationConstants
import xyz.klinker.messenger.shared.service.notification.NotificationService
import xyz.klinker.messenger.shared.util.*

/**
 * Receiver for notifying us when a new MMS has been received by the device. By default it will
 * persist the message to the internal database. We also need to add functionality for
 * persisting it to our own database and giving a notification that it has been received.
 */
class MmsReceivedReceiver : com.klinker.android.send_message.MmsReceivedReceiver() {

    private var context: Context? = null
    private var conversationId: Long? = null
    private var ignoreNotification = false

    override fun onReceive(context: Context, intent: Intent) {
        this.context = context
        super.onReceive(context, intent)
    }

    override fun onMessageReceived(messageUri: Uri) {
        Log.v("MmsReceivedReceiver", "message received: $messageUri")
        val lastMessage = SmsMmsUtils.getMmsMessage(context, messageUri, null)
        if (lastMessage != null && lastMessage.moveToFirst()) {
            handleMms(context!!, messageUri, lastMessage)
        } else {
            try {
                CursorUtil.closeSilent(lastMessage)
            } catch (e: Exception) {
            }
        }
    }

    override fun onError(error: String) {
        Log.v("MmsReceivedReceiver", "message save error: $error")
        val lastMessage = SmsMmsUtils.getLastMmsMessage(context)
        if (lastMessage != null && lastMessage.moveToFirst()) {
            val uri = Uri.parse("content://mms/" + lastMessage.getLong(0))
            handleMms(context!!, uri, lastMessage)
        } else {
            try {
                CursorUtil.closeSilent(lastMessage)
            } catch (e: Exception) {
            }
        }
    }

    private fun handleMms(context: Context, uri: Uri, lastMessage: Cursor) {
        val nullableOrBlankBodyText = insertMms(context, uri, lastMessage)

        if (!ignoreNotification) {
            try {
                context.startService(Intent(context, NotificationService::class.java))
            } catch (e: Exception) {
                if (AndroidVersionUtil.isAndroidO) {
                    val foregroundNotificationService = Intent(context, NotificationService::class.java)
                    foregroundNotificationService.putExtra(NotificationConstants.EXTRA_FOREGROUND, true)
                    context.startForegroundService(foregroundNotificationService)
                }
            }
        }
    }

    private fun insertMms(context: Context, uri: Uri, lastMessage: Cursor): String? {
        var snippet: String? = ""
        val from = SmsMmsUtils.getMmsFrom(uri, context)

        if (BlacklistUtils.isBlacklisted(context, from)) {
            return null
        }

        val to = SmsMmsUtils.getMmsTo(uri, context)
        val phoneNumbers = getPhoneNumbers(from, to,
                PhoneNumberUtils.getMyPossiblePhoneNumbers(context), context)
        val values = SmsMmsUtils.processMessage(lastMessage, -1L, context)

        if (isReceivingMessageFromThemself(context, from) && phoneNumbers.contains(",")) {
            // a group message, coming from themselves, should not be saved
            return null
        }

        val source = DataSource

        for (value in values) {
            val message = Message()
            message.type = value.getAsInteger(Message.COLUMN_TYPE)
            message.data = value.getAsString(Message.COLUMN_DATA).trim { it <= ' ' }
            message.timestamp = System.currentTimeMillis()
            message.mimeType = value.getAsString(Message.COLUMN_MIME_TYPE)
            message.read = false
            message.seen = false
            message.from = ContactUtils.findContactNames(from, context)
            message.simPhoneNumber = if (DualSimUtils.availableSims.isEmpty()) null else to
            message.sentDeviceId = -1L

            if (message.mimeType == MimeType.TEXT_PLAIN) {
                snippet = message.data
            }

            if (!phoneNumbers.contains(",")) {
                message.from = null
            }

            if (SmsReceivedReceiver.shouldSaveMessage(context, message, phoneNumbers)) {
                conversationId = source.insertMessage(message, phoneNumbers, context)

                val conversation = source.getConversation(context, conversationId!!)
                if (conversation != null && conversation.mute) {
                    source.seenConversation(context, conversationId!!)
                    ignoreNotification = true
                }

                if (MmsSettings.autoSaveMedia && MimeType.TEXT_PLAIN != message.mimeType) {
                    try {
                        MediaSaver(context).saveMedia(message)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                }
            }
        }

        if (conversationId != null) {
            ConversationListUpdatedReceiver.sendBroadcast(context, conversationId!!,
                    snippet, false)
            MessageListUpdatedReceiver.sendBroadcast(context, conversationId!!)
        }

        try {
            CursorUtil.closeSilent(lastMessage)
        } catch (e: Exception) {
        }

        return snippet
    }

    fun getPhoneNumbers(from: String, to: String, myPossiblePhoneNumbers: List<String>, context: Context): String {
        val toNumbers = to.split(", ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        val fromMatcher = SmsMmsUtils.createIdMatcher(from).sevenLetterNoFormatting

        val builder = StringBuilder()

        for (number in toNumbers) {
            val contactName = ContactUtils.findContactNames(number, context)
            val idMatcher = SmsMmsUtils.createIdMatcher(number).sevenLetterNoFormatting

            val matchesFromNumber = idMatcher == fromMatcher
            val matchesMyNumber = myPossiblePhoneNumbers
                    .map { SmsMmsUtils.createIdMatcher(it).sevenLetterNoFormatting }
                    .contains(idMatcher)

            if (!matchesFromNumber && !matchesMyNumber && contactName.toLowerCase() != "me" && !number.isEmpty()) {
                builder.append(number)
                builder.append(", ")
            }
        }

        builder.append(from)
        return builder.toString().replace(",".toRegex(), ", ").replace("  ".toRegex(), " ")
    }

    fun getMyName(): String? {
        return Account.myName
    }

    @VisibleForTesting
    fun getContactName(context: Context, number: String): String {
        return ContactUtils.findContactNames(number, context)
    }

    private fun isReceivingMessageFromThemself(context: Context, from: String): Boolean {
        val myPossiblePhoneNumbers = PhoneNumberUtils.getMyPossiblePhoneNumbers(context)
        val fromMatcher = SmsMmsUtils.createIdMatcher(from).sevenLetter

        return myPossiblePhoneNumbers
                .map { SmsMmsUtils.createIdMatcher(it).sevenLetter }
                .contains(fromMatcher)
    }

    override fun getMmscInfoForReceptionAck(): com.klinker.android.send_message.MmsReceivedReceiver.MmscInformation? {
        return if (MmsSettings.mmscUrl != null && !MmsSettings.mmscUrl!!.isEmpty() &&
                MmsSettings.mmsProxy != null && !MmsSettings.mmsProxy!!.isEmpty() &&
                MmsSettings.mmsPort != null && !MmsSettings.mmsPort!!.isEmpty()) {
            com.klinker.android.send_message.MmsReceivedReceiver.MmscInformation(MmsSettings.mmscUrl, MmsSettings.mmsProxy, Integer.parseInt(MmsSettings.mmsPort))
        } else {
            null
        }
    }
}
