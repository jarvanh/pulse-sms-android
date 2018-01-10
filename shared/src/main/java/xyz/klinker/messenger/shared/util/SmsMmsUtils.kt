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

package xyz.klinker.messenger.shared.util

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.SQLException
import android.database.sqlite.SqliteWrapper
import android.graphics.Color
import android.net.Uri
import android.provider.Telephony
import android.text.TextUtils
import android.util.Log

import com.android.mms.transaction.MmsMessageSender
import com.google.android.mms.pdu_alt.EncodedStringValue
import com.google.android.mms.pdu_alt.MultimediaMessagePdu
import com.google.android.mms.pdu_alt.PduHeaders
import com.google.android.mms.pdu_alt.PduPersister
import com.google.android.mms.pdu_alt.RetrieveConf
import com.klinker.android.send_message.Utils

import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.util.ArrayList
import java.util.Collections
import java.util.HashSet

import xyz.klinker.messenger.shared.data.DataSource
import xyz.klinker.messenger.shared.data.IdMatcher
import xyz.klinker.messenger.shared.data.MimeType
import xyz.klinker.messenger.shared.data.model.Conversation
import xyz.klinker.messenger.shared.data.model.Message

object SmsMmsUtils {

    private val TAG = "SmsMmsUtils"

    val INITIAL_CONVERSATION_LIMIT = 250
    val INITIAL_MESSAGE_LIMIT = 500

    /**
     * Gets a list of conversations from the internal sms database that is ready to be inserted
     * into our database.
     *
     * @param context the current application context.
     * @return a list of conversations that is filled and ready to be inserted into our database.
     */
    fun queryConversations(context: Context?): List<Conversation> {
        if (context == null) {
            return emptyList()
        }

        val conversations = ArrayList<Conversation>()

        val projection = arrayOf(Telephony.ThreadsColumns._ID, Telephony.ThreadsColumns.DATE, Telephony.ThreadsColumns.MESSAGE_COUNT, Telephony.ThreadsColumns.RECIPIENT_IDS, Telephony.ThreadsColumns.SNIPPET, Telephony.ThreadsColumns.READ)

        val uri = Uri.parse(Telephony.Threads.CONTENT_URI.toString() + "?simple=true")

        val cursor = try {
            context.contentResolver
                    .query(uri, projection, null, null, Telephony.ThreadsColumns.DATE + " desc")
        } catch (e: SQLException) {
            null
        } catch (e: IllegalArgumentException) {
            null
        } catch (e: SecurityException) {
            null
        }

        if (cursor != null && cursor.moveToFirst()) {
            do {
                val conversation = Conversation()
                conversation.id = cursor.getLong(0)
                conversation.pinned = false
                conversation.read = cursor.getInt(5) == 1
                conversation.timestamp = cursor.getLong(1)
                conversation.snippet = cursor.getString(4)
                conversation.ringtoneUri = null
                conversation.phoneNumbers = ContactUtils.findContactNumbers(cursor.getString(3), context)
                conversation.title = ContactUtils.findContactNames(conversation.phoneNumbers, context)
                conversation.imageUri = ContactUtils.findImageUri(conversation.phoneNumbers, context)
                conversation.idMatcher = createIdMatcher(conversation.phoneNumbers!!).default
                conversation.mute = false
                conversation.privateNotifications = false
                conversation.ledColor = Color.WHITE
                ImageUtils.fillConversationColors(conversation, context!!)
                conversation.simSubscriptionId = -1
                conversation.folderId = -1

                conversations.add(conversation)
            } while (cursor.moveToNext() && conversations.size < INITIAL_CONVERSATION_LIMIT)
        }

        cursor?.closeSilent()
        return conversations
    }

    /**
     * Creates a column that we can use later on for a findOrCreateConversationId method on my
     * database. It will take all of the comma, space separated numbers and combine them together
     * by taking the last 5  (and 7) digits of each number, sorting them and then recombining them into a
     * single string. We can then do the same process for any string of phone numbers later on
     * and search for that string in the data source to see if it exists yet.
     *
     * I added the seven digit finder after some issues that people ran into with conversations not
     * being able to be saved correctly. It is now being used throughout the app, but I needed
     * to continue supporting the legacy version (5 digits) as well.
     *
     * @param phoneNumbers the phone numbers to look for.
     * @return the combined string.
     */
    fun createIdMatcher(phoneNumbers: String): IdMatcher {
        val numbers = phoneNumbers.split(", ".toRegex())
                .dropLastWhile { it.isEmpty() }
                .map { it.replace("-".toRegex(), "").replace(" ".toRegex(), "").replace("/+".toRegex(), "") }

        val fiveMatchers = ArrayList<String>()
        val sevenMatchers = ArrayList<String>()
        val sevenMatchersNoFormatting = ArrayList<String>()
        val eightMatchers = ArrayList<String>()
        val eightMatchersNoFormatting = ArrayList<String>()
        val tenMatchers = ArrayList<String>()

        numbers.forEach {
            when {
                it.contains("@") -> fiveMatchers.add(it)
                it.length >= 5 -> fiveMatchers.add(it.substring(it.length - 5))
                else -> fiveMatchers.add(it)
            }
        }

        numbers.forEach {
            when {
                it.contains("@") -> sevenMatchers.add(it)
                it.length >= 7 -> sevenMatchers.add(it.substring(it.length - 7))
                else -> sevenMatchers.add(it)
            }
        }

        numbers.map { PhoneNumberUtils.clearFormatting(it) }
                .forEach {
                    when {
                        it.contains("@") -> sevenMatchersNoFormatting.add(it)
                        it.length >= 7 -> sevenMatchersNoFormatting.add(it.substring(it.length - 7))
                        else -> sevenMatchersNoFormatting.add(it)
                    }
                }

        numbers.forEach {
            when {
                it.contains("@") -> eightMatchers.add(it)
                it.length >= 8 -> eightMatchers.add(it.substring(it.length - 8))
                else -> eightMatchers.add(it)
            }
        }

        numbers.map { PhoneNumberUtils.clearFormatting(it) }
                .forEach {
                    when {
                        it.contains("@") -> eightMatchersNoFormatting.add(it)
                        it.length >= 8 -> eightMatchersNoFormatting.add(it.substring(it.length - 8))
                        else -> eightMatchersNoFormatting.add(it)
                    }
                }

        numbers.forEach {
            when {
                it.contains("@") -> tenMatchers.add(it)
                it.length >= 10 -> tenMatchers.add(it.substring(it.length - 10))
                else -> tenMatchers.add(it)
            }
        }

        Collections.sort(fiveMatchers)
        Collections.sort(sevenMatchers)
        Collections.sort(sevenMatchersNoFormatting)
        Collections.sort(eightMatchers)
        Collections.sort(eightMatchersNoFormatting)
        Collections.sort(tenMatchers)

        val sevenBuilder = StringBuilder()
        for (m in sevenMatchers) {
            sevenBuilder.append(m)
        }

        val sevenNoFormattingBuilder = StringBuilder()
        for (m in sevenMatchersNoFormatting) {
            sevenNoFormattingBuilder.append(m)
        }

        val eightBuilder = StringBuilder()
        for (m in eightMatchers) {
            eightBuilder.append(m)
        }

        val eightNoFormattingBuilder = StringBuilder()
        for (m in eightMatchersNoFormatting) {
            eightNoFormattingBuilder.append(m)
        }

        val fiveBuilder = StringBuilder()
        for (m in fiveMatchers) {
            fiveBuilder.append(m)
        }

        val tenBuilder = StringBuilder()
        for (m in tenMatchers) {
            tenBuilder.append(m)
        }

        return IdMatcher(fiveBuilder.toString(), sevenBuilder.toString(), sevenNoFormattingBuilder.toString(),
                eightBuilder.toString(), eightNoFormattingBuilder.toString(), tenBuilder.toString())
    }

    /**
     * Queries a conversation that is currently in the database and returns a cursor with all of the
     * data.
     *
     * @param conversationId the internal sms db conversation id.
     * @return the conversation as a cursor.
     */
    fun queryConversation(conversationId: Long, context: Context?): Cursor? {
        if (conversationId == -1L || context == null) {
            return null
        }

        val projection = arrayOf(Telephony.MmsSms._ID, Telephony.Sms.BODY, Telephony.Sms.DATE, Telephony.Sms.READ, Telephony.Sms.TYPE, Telephony.Mms.MESSAGE_BOX, Telephony.Mms.MESSAGE_TYPE, Telephony.Sms.STATUS)

        val uri = Uri.parse("content://mms-sms/conversations/$conversationId/")
        val sortOrder = "normalized_date desc"

        return context.contentResolver.query(uri, projection, null, null, sortOrder)
    }

    /**
     * Gets content values that can be inserted into our own database from a cursor with data from
     * the internal database. See queryConversation(). For an mms message, there could be multiple
     * messages that need to be inserted, so the method returns a list.
     *
     * @param messages       the cursor holding the message.
     * @param conversationId the conversation id from our own internal database.
     * @return the content values to insert into our database.
     */
    fun processMessage(messages: Cursor, conversationId: Long, context: Context?): List<ContentValues> {
        if (context == null) {
            return emptyList()
        }

        val values = ArrayList<ContentValues>()

        if (isSms(messages)) {
            if (messages.getString(1) != null) {
                val message = ContentValues(9)
                message.put(Message.COLUMN_ID, DataSource.generateId())
                message.put(Message.COLUMN_CONVERSATION_ID, conversationId)
                message.put(Message.COLUMN_TYPE, getSmsMessageType(messages))
                message.put(Message.COLUMN_DATA, messages.getString(1).trim { it <= ' ' })
                message.put(Message.COLUMN_TIMESTAMP, messages.getLong(2))
                message.put(Message.COLUMN_MIME_TYPE, MimeType.TEXT_PLAIN)
                message.put(Message.COLUMN_READ, messages.getInt(3))
                message.put(Message.COLUMN_SEEN, true)
                message.put(Message.COLUMN_FROM, null as String?)
//                message.put(Message.COLUMN_COLOR, null)

                values.add(message)
            }
        } else {
            val uri = Uri.parse("content://mms/" + messages.getLong(0))
            val number = getMmsFrom(uri, context)
            val from = ContactUtils.findContactNames(number, context)
            val mId = "mid=" + messages.getString(0)
            val type = getMmsMessageType(messages)

            val query = context.contentResolver.query(Uri.parse("content://mms/part"),
                    arrayOf(Telephony.Mms.Part._ID, Telephony.Mms.Part.CONTENT_TYPE, Telephony.Mms.Part._DATA, Telephony.Mms.Part.TEXT),
                    mId, null, null)

            if (query != null && query.moveToFirst()) {
                do {
                    val partId = query.getString(0)
                    val mimeType = query.getString(1)

                    if (mimeType != null && MimeType.isSupported(mimeType)) {
                        val message = ContentValues(9)
                        message.put(Message.COLUMN_CONVERSATION_ID, conversationId)
                        message.put(Message.COLUMN_TYPE, type)
                        message.put(Message.COLUMN_MIME_TYPE, mimeType)
                        message.put(Message.COLUMN_TIMESTAMP, messages
                                .getLong(messages.getColumnIndex(Telephony.Sms.DATE)) * 1000)
                        message.put(Message.COLUMN_READ, messages
                                .getInt(messages.getColumnIndex(Telephony.Sms.READ)))
                        message.put(Message.COLUMN_SEEN, true)
                        message.put(Message.COLUMN_FROM, from)
//                        message.put(Message.COLUMN_COLOR, null)

                        if (mimeType == MimeType.TEXT_PLAIN) {
                            val data = query.getString(2)
                            var text = if (data != null) {
                                getMmsText(partId, context)
                            } else {
                                query.getString(3)
                            }

                            if (text == null) {
                                text = ""
                            }

                            if (text.trim { it <= ' ' }.isNotEmpty()) {
                                message.put(Message.COLUMN_DATA, text.trim { it <= ' ' })
                                values.add(message)
                            }
                        } else {
                            message.put(Message.COLUMN_DATA, "content://mms/part/" + partId)
                            values.add(message)
                        }
                    }
                } while (query.moveToNext())
            }

            query.closeSilent()
        }

        return values
    }

    /**
     * Checks whether or not the msg_box column has data in it. If it doesn't, then the message is
     * SMS. If it does, the message is MMS.
     *
     * @param message the message to try.
     * @return true for sms, false for mms.
     */
    private fun isSms(message: Cursor): Boolean {
        return message.getString(message.getColumnIndex(Telephony.Mms.MESSAGE_BOX)) == null
    }

    /**
     * Gets the message type of the internal sms. It will be one of the constants defined in Message,
     * eg TYPE_RECEIVED, TYPE_SENT, etc.
     *
     * @param message the message to inspect.
     * @return the Message.TYPE_ value.
     */
    fun getSmsMessageType(message: Cursor): Int {
        val internalType = message.getInt(message.getColumnIndex(Telephony.Sms.TYPE))
        val status = message.getInt(message.getColumnIndex(Telephony.Sms.STATUS))

        return if (status == Telephony.Sms.STATUS_NONE || internalType == Telephony.Sms.MESSAGE_TYPE_INBOX) {
            when (internalType) {
                Telephony.Sms.MESSAGE_TYPE_INBOX -> Message.TYPE_RECEIVED
                Telephony.Sms.MESSAGE_TYPE_FAILED -> Message.TYPE_ERROR
                Telephony.Sms.MESSAGE_TYPE_OUTBOX -> Message.TYPE_SENDING
                Telephony.Sms.MESSAGE_TYPE_SENT -> Message.TYPE_SENT
                else -> Message.TYPE_SENT
            }
        } else {
            when (status) {
                Telephony.Sms.STATUS_COMPLETE -> Message.TYPE_DELIVERED
                Telephony.Sms.STATUS_PENDING -> Message.TYPE_SENT
                Telephony.Sms.STATUS_FAILED -> Message.TYPE_ERROR
                else -> Message.TYPE_SENT
            }
        }
    }

    /**
     * Gets the message type of the internal mms. It will be one of the constants defined in Message,
     * eg TYPE_RECEIVED, TYPE_SENT, etc.
     *
     * @param message the message to inspect.
     * @return the Message.TYPE_ value.
     */
    private fun getMmsMessageType(message: Cursor): Int {
        val internalType = message.getInt(message.getColumnIndex(Telephony.Mms.MESSAGE_BOX))

        return when (internalType) {
            Telephony.Mms.MESSAGE_BOX_INBOX -> Message.TYPE_RECEIVED
            Telephony.Mms.MESSAGE_BOX_FAILED -> Message.TYPE_ERROR
            Telephony.Mms.MESSAGE_BOX_OUTBOX -> Message.TYPE_SENDING
            Telephony.Mms.MESSAGE_BOX_SENT -> Message.TYPE_SENT
            else -> Message.TYPE_SENT
        }
    }

    fun getMmsFrom(uri: Uri, context: Context?): String {
        if (context == null) {
            return ""
        }

        val msgId = uri.lastPathSegment
        val builder = Telephony.Mms.CONTENT_URI.buildUpon()

        builder.appendPath(msgId).appendPath("addr")

        val cursor = SqliteWrapper.query(context, context.contentResolver,
                builder.build(), arrayOf(Telephony.Mms.Addr.ADDRESS, Telephony.Mms.Addr.CHARSET),
                Telephony.Mms.Addr.TYPE + "=" + PduHeaders.FROM, null, null)

        if (cursor != null) {
            if (cursor.moveToFirst()) {
                val from = cursor.getString(0)

                if (!TextUtils.isEmpty(from)) {
                    val bytes = PduPersister.getBytes(from)
                    val charset = cursor.getInt(1)
                    cursor.closeSilent()
                    return EncodedStringValue(charset, bytes).string
                }
            }
        }

        cursor?.closeSilent()
        return ""
    }

    fun getMmsTo(uri: Uri, context: Context?): String {
        val msg: MultimediaMessagePdu

        try {
            msg = PduPersister.getPduPersister(
                    context).load(uri) as MultimediaMessagePdu
        } catch (e: Exception) {
            return ""
        }

        val toBuilder = StringBuilder()
        val to = msg.to

        if (to != null) {
            toBuilder.append(EncodedStringValue.concat(to))
        }

        if (msg is RetrieveConf) {
            val cc = msg.cc
            if (cc != null && cc.isNotEmpty()) {
                toBuilder.append(";")
                toBuilder.append(EncodedStringValue.concat(cc))
            }
        }

        var built = toBuilder.toString().replace(";", ", ")
        if (built.startsWith(", ")) {
            built = built.substring(2)
        }

        return stripDuplicatePhoneNumbers(built)
    }

    /**
     * Expects the conversation formatted list of phone numbers and returns the same list,
     * stripped of duplicates.
     *
     * @param phoneNumbers comma and space separated list of numbers.
     * @return the same list, with any duplicates stripped out.
     */
    fun stripDuplicatePhoneNumbers(phoneNumbers: String?): String {
        if (phoneNumbers == null) {
            return ""
        }

        val split = phoneNumbers.split(", ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        val numbers = split.toSet()

        val builder = StringBuilder()
        for (s in numbers) {
            builder.append(s)
            builder.append(", ")
        }

        var result = builder.toString()
        if (result.contains(", ")) {
            result = result.substring(0, result.length - 2)
        }

        return result
    }

    private fun getMmsText(id: String, context: Context?): String {
        if (context == null) {
            return ""
        }

        val partURI = Uri.parse("content://mms/part/" + id)
        var `is`: InputStream? = null
        val sb = StringBuilder()
        try {
            `is` = context.contentResolver.openInputStream(partURI)
            if (`is` != null) {
                val isr = InputStreamReader(`is`, "UTF-8")
                val reader = BufferedReader(isr)
                var temp: String? = reader.readLine()
                while (temp != null) {
                    sb.append(temp)
                    temp = reader.readLine()
                }
            }
        } catch (e: IOException) {
        } finally {
            `is`?.closeSilent()
        }
        return sb.toString()
    }

    /**
     * Gets the last sms message that was inserted into the database.
     *
     * @param context the context to get the content provider with.
     * @return the cursor for a single mms message.
     */
    fun getLatestSmsMessages(context: Context?, limit: Int): Cursor? {
        val uri = Uri.parse("content://sms")
        val sortOrder = "date desc limit " + limit
        return getSmsMessage(context, uri, sortOrder)
    }

    /**
     * Get an SMS message(s) from the provided URI.
     *
     * @param context   the context for the content provider.
     * @param uri       the sms message uri.
     * @param sortOrder the sort order to apply.
     * @return the cursor for the messages that match.
     */
    fun getSmsMessage(context: Context?, uri: Uri, sortOrder: String?): Cursor? {
        if (context == null) {
            return null
        }

        val projection = arrayOf(Telephony.MmsSms._ID, Telephony.Sms.BODY, Telephony.Sms.DATE, Telephony.Sms.READ, Telephony.Sms.TYPE, Telephony.Sms.STATUS, Telephony.Sms.ADDRESS)

        return try {
            context.contentResolver.query(uri, projection, null, null, sortOrder)
        } catch (e: Exception) {
            // they probably aren't using our app as the default?
            null
        }

    }

    /**
     * Gets the last mms message that was inserted into the database.
     *
     * @param context the context to get the content provider with.
     * @return the cursor for a single mms message.
     */
    fun getLastMmsMessage(context: Context?): Cursor? {
        val uri = Uri.parse("content://mms")
        val sortOrder = "date desc limit 1"
        return getMmsMessage(context, uri, sortOrder)
    }

    /**
     * Get an MMS message(s) from the provided URI.
     *
     * @param context   the context for the content provider.
     * @param uri       the mms message uri.
     * @param sortOrder the sort order to apply.
     * @return the cursor for the messages that match.
     */
    fun getMmsMessage(context: Context?, uri: Uri, sortOrder: String?): Cursor? {
        if (context == null) {
            return null
        }

        val projection = arrayOf(Telephony.MmsSms._ID, Telephony.Sms.DATE, Telephony.Sms.READ, Telephony.Mms.MESSAGE_BOX, Telephony.Mms.MESSAGE_TYPE)

        return context.contentResolver.query(uri, projection, null, null, sortOrder)
    }

    /**
     * Marks a conversation as read in the internal database.
     *
     * @param context      the context to get the content provider with.
     * @param phoneNumbers the phone numbers to find the conversation with.
     */
    fun markConversationRead(context: Context?, phoneNumbers: String) {
        Thread {
            try {
                val recipients = HashSet<String>()
                Collections.addAll(recipients, *phoneNumbers.split(", ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray())
                val threadId = Utils.getOrCreateThreadId(context, recipients)
                markConversationRead(context,
                        ContentUris.withAppendedId(Telephony.Threads.CONTENT_URI, threadId), threadId)
            } catch (e: IllegalStateException) {
                // the conversation doesn't exist
                e.printStackTrace()
            } catch (e: IllegalArgumentException) {
                e.printStackTrace()
            } catch (e: SQLException) {
                e.printStackTrace()
            } catch (e: SecurityException) {
                e.printStackTrace()
            } catch (e: NullPointerException) {
                e.printStackTrace()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }

    private fun markConversationRead(context: Context?, threadUri: Uri?, threadId: Long) {
        Log.v(TAG, "marking thread as read. Thread Id: $threadId, Thread Uri: $threadUri")

        // If we have no Uri to mark (as in the case of a conversation that
        // has not yet made its way to disk), there's nothing to do.
        if (threadUri != null && context != null) {
            // Check the read flag first. It's much faster to do a query than
            // to do an update. Timing this function show it's about 10x faster to
            // do the query compared to the update, even when there's nothing to
            // update.
            var needUpdate = true

            context.contentResolver.query(threadUri,
                    arrayOf("_id", "read", "seen"), "(read=0 OR seen=0)", null, null)?.use { c ->
                needUpdate = c.count > 0
            }

            if (needUpdate) {
                Log.v(TAG, "MMS need to be marked as read")

                val values = ContentValues(2)
                values.put("read", 1)
                values.put("seen", 1)

                sendReadReport(context, threadId, PduHeaders.READ_STATUS_READ)
                context.contentResolver.update(threadUri, values,
                        "(read=0 OR seen=0)", null)
            }
        }
    }

    private fun sendReadReport(context: Context?, threadId: Long, status: Int) {
        if (context == null) {
            return
        }

        var selection = Telephony.Mms.READ + " = 0"

        if (threadId != -1L) {
            selection = selection + " AND " + Telephony.Mms.THREAD_ID + " = " + threadId
        }

        try {
            val c = context.contentResolver.query(Telephony.Mms.Inbox.CONTENT_URI,
                    arrayOf(Telephony.Mms._ID, Telephony.Mms.MESSAGE_ID),
                    selection, null, null)

            if (c != null && c.moveToFirst()) {
                do {
                    Log.v("SmsMmsUtils", "marking MMS as seen. ID:" + c.getString(1))
                    val uri = ContentUris.withAppendedId(Telephony.Mms.CONTENT_URI, c.getLong(0))
                    MmsMessageSender.sendReadRec(context, getMmsFrom(uri, context),
                            c.getString(1), status)
                } while (c.moveToNext())
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Deletes a conversation from the internal sms database.
     */
    fun deleteConversation(context: Context?, phoneNumbers: String) {
        if (context == null) {
            return
        }

        try {
            val recipients = HashSet<String>()
            Collections.addAll(recipients, *phoneNumbers.split(", ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray())
            val threadId = Utils.getOrCreateThreadId(context, recipients)
            context.contentResolver.delete(Uri.parse("content://mms-sms/conversations/" +
                    threadId + "/"), null, null)
            context.contentResolver.delete(Uri.parse("content://mms-sms/conversations/"),
                    "_id=?", arrayOf(java.lang.Long.toString(threadId)))
        } catch (e: Exception) {
            Log.e("delete conversation", "error deleting", e)
        }

    }

}
