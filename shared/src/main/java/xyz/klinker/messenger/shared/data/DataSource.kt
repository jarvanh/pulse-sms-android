/*
 * Copyright (C) 2016 Jacob Klinker
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

package xyz.klinker.messenger.shared.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteConstraintException
import android.database.sqlite.SQLiteDatabase
import android.graphics.Color
import android.os.Build
import android.support.annotation.VisibleForTesting
import android.text.Html
import android.text.Spanned
import android.text.format.Formatter
import android.util.Log

import java.text.SimpleDateFormat
import java.util.ArrayList
import java.util.Date

import xyz.klinker.messenger.shared.R
import xyz.klinker.messenger.api.implementation.Account
import xyz.klinker.messenger.api.implementation.ApiUtils
import xyz.klinker.messenger.api.implementation.BinaryUtils
import xyz.klinker.messenger.shared.data.model.Blacklist
import xyz.klinker.messenger.shared.data.model.Contact
import xyz.klinker.messenger.shared.data.model.Conversation
import xyz.klinker.messenger.shared.data.model.Draft
import xyz.klinker.messenger.shared.data.model.Message
import xyz.klinker.messenger.shared.data.model.ScheduledMessage
import xyz.klinker.messenger.encryption.EncryptionUtils
import xyz.klinker.messenger.shared.service.NewMessagesCheckService
import xyz.klinker.messenger.shared.util.*
import xyz.klinker.messenger.shared.util.listener.ProgressUpdateListener

/**
 * Handles interactions with database models.
 */
object DataSource {

    private val TAG = "DataSource"

    /**
     * A max value for the id. With this value, there is a 1 in 200,000 chance of overlap when a
     * user uploads 100,000 messages, so we should be safe assuming that no user will be uploading
     * that many messages.
     *
     *
     * See https://github.com/klinker41/messenger-server/wiki/Generating-GUIDs.
     */
    private val MAX_ID = java.lang.Long.MAX_VALUE / 10000

    var _database: SQLiteDatabase? = null
    var _dbHelper: DatabaseSQLiteHelper? = null
    var _encryptor: EncryptionUtils? = null
    var _accountId: String? = null
    var _androidDeviceId: String? = null
    var apiUtils: ApiUtils = ApiUtils()

    @Synchronized
    private fun database(context: Context): SQLiteDatabase {
        if (_database == null) {
            _database = dbHelper(context).writableDatabase
        }

        return _database!!
    }

    @Synchronized
    private fun dbHelper(context: Context): DatabaseSQLiteHelper {
        if (_dbHelper == null) {
            _dbHelper = DatabaseSQLiteHelper(context)
        }

        return _dbHelper!!
    }

    @Synchronized
    private fun encryptor(context: Context): EncryptionUtils? {
        if (_encryptor == null) {
            _encryptor = Account.get(context).encryptor
        }

        return _encryptor
    }

    @Synchronized
    private fun accountId(context: Context): String? {
        if (_accountId == null) {
            _accountId = Account.get(context).accountId
        }
        
        return _accountId
    }

    @Synchronized
    private fun androidDeviceId(context: Context): String? {
        if (_androidDeviceId == null) {
            _androidDeviceId = Account.get(context).deviceId
        }

        return _androidDeviceId
    }

    @Synchronized
    fun ensureActionable(context: Context) {
        Log.v(TAG, "ensuring database actionable")

        try {
            Thread.sleep(500)
        } catch (e: Exception) {
        }

        // ensure we are closing everything and getting a brand new database connection
        close(context)
    }

    @Synchronized
    fun close(context: Context) {
        Log.v(TAG, "closing database")

        try {
            dbHelper(context).close()
        } catch (e: Exception) {
        }
        try {
            database(context).close()
        } catch (e: Exception) {
        }

        _dbHelper = null
        _database = null
    }

    private fun writeUnreadCount(context: Context) =
            try {
                UnreadBadger(context).writeCount(getUnreadConversationCount(context))
            } catch (e: Exception) {
            }

    /**
     * Deletes all data from the tables.
     */
    fun clearTables(context: Context) =
            try {
                database(context).delete(Message.TABLE, null, null)
                database(context).delete(Conversation.TABLE, null, null)
                database(context).delete(Blacklist.TABLE, null, null)
                database(context).delete(Draft.TABLE, null, null)
                database(context).delete(ScheduledMessage.TABLE, null, null)
                database(context).delete(Contact.TABLE, null, null)
            } catch (e: Exception) {
                ensureActionable(context)

                database(context).delete(Message.TABLE, null, null)
                database(context).delete(Conversation.TABLE, null, null)
                database(context).delete(Blacklist.TABLE, null, null)
                database(context).delete(Draft.TABLE, null, null)
                database(context).delete(ScheduledMessage.TABLE, null, null)
                database(context).delete(Contact.TABLE, null, null)
            }

    /**
     * Begins a bulk transaction on the database.
     */
    fun beginTransaction(context: Context) = database(context).beginTransaction()

    /**
     * Executes a raw sql statement on the database. Can be used in conjunction with
     * beginTransaction and endTransaction if bulk.
     *
     * @param sql the sql statement.
     */
    fun execSql(context: Context, sql: String) =
            try {
                database(context).execSQL(sql)
            } catch (e: Exception) {
                ensureActionable(context)
                database(context).execSQL(sql)
            }

    /**
     * Execute a raw sql query on the database.
     *
     * @param sql the sql statement
     * @return cursor for the data
     */
    fun rawQuery(context: Context, sql: String): Cursor =
            try {
                database(context).rawQuery(sql, null)
            } catch (e: Exception) {
                ensureActionable(context)
                database(context).rawQuery(sql, null)
            }

    /**
     * Sets the transaction into a successful state so that it can be committed to the database.
     * Should be used in conjunction with beginTransaction() and endTransaction().
     */
    fun setTransactionSuccessful(context: Context) = database(context).setTransactionSuccessful()

    /**
     * Ends a bulk transaction on the database.
     */
    fun endTransaction(context: Context) = database(context).endTransaction()

    /**
     * Bulk insert of contacts into the databse.
     *
     * @param contacts a list of all the contacts to insert
     * @param listener callback for the progress of the insert
     */
    fun insertContacts(context: Context, contacts: List<Contact>, listener: ProgressUpdateListener?) {
        beginTransaction(context)

        for (i in contacts.indices) {
            val contact = contacts[i]

            val values = ContentValues(7)

            // here we are loading the id from the internal database into the conversation object
            // but we don't want to use that so we'll just generate a new one.
            values.put(Contact.COLUMN_ID, generateId())
            values.put(Contact.COLUMN_PHONE_NUMBER, ContactUtils.getPlainNumber(contact.phoneNumber))
            values.put(Contact.COLUMN_NAME, contact.name)
            values.put(Contact.COLUMN_COLOR, contact.colors.color)
            values.put(Contact.COLUMN_COLOR_DARK, contact.colors.colorDark)
            values.put(Contact.COLUMN_COLOR_LIGHT, contact.colors.colorLight)
            values.put(Contact.COLUMN_COLOR_ACCENT, contact.colors.colorAccent)

            try {
                database(context).insert(Contact.TABLE, null, values)
            } catch (e: Exception) {
                ensureActionable(context)
                database(context).insert(Contact.TABLE, null, values)
            }

            listener?.onProgressUpdate(i + 1, contacts.size)
        }

        setTransactionSuccessful(context)
        endTransaction(context)
    }

    /**
     * Insert a new contact into the apps database.
     *
     * @param contact the new contact
     * @return id of the inserted contact or -1 if the insert failed
     */
    fun insertContact(context: Context, contact: Contact): Long {
        val values = ContentValues(7)

        if (contact.id <= 0) {
            contact.id = generateId()
        }

        values.put(Contact.COLUMN_ID, contact.id)
        values.put(Contact.COLUMN_PHONE_NUMBER, ContactUtils.getPlainNumber(contact.phoneNumber))
        values.put(Contact.COLUMN_NAME, contact.name)
        values.put(Contact.COLUMN_COLOR, contact.colors.color)
        values.put(Contact.COLUMN_COLOR_DARK, contact.colors.colorDark)
        values.put(Contact.COLUMN_COLOR_LIGHT, contact.colors.colorLight)
        values.put(Contact.COLUMN_COLOR_ACCENT, contact.colors.colorAccent)

        apiUtils.addContact(accountId(context), contact.phoneNumber, contact.name, contact.colors.color,
                contact.colors.colorDark, contact.colors.colorLight,
                contact.colors.colorAccent, encryptor(context))

        return try {
            database(context).insert(Contact.TABLE, null, values)
        } catch (e: Exception) {
            ensureActionable(context)
            database(context).insert(Contact.TABLE, null, values)
        }

    }

    /**
     * Get all the contacts in the database.
     *
     * @return a cursor of all the contacts stored in the app.
     */
    fun getContacts(context: Context): Cursor {
        return try {
            database(context).query(Contact.TABLE, null, null, null, null, null,
                    Contact.COLUMN_NAME + " ASC")
        } catch (e: Exception) {
            ensureActionable(context)
            database(context).query(Contact.TABLE, null, null, null, null, null,
                    Contact.COLUMN_NAME + " ASC")
        }

    }

    /**
     * Get a contact from the database.
     *
     * @param phoneNumber unique phone number to find
     * @return Contact from the database
     */
    fun getContact(context: Context, phoneNumber: String): Contact? {
        val cursor = try {
            database(context).query(Contact.TABLE, null, Contact.COLUMN_PHONE_NUMBER + "=?",
                    arrayOf(phoneNumber), null, null, null)
        } catch (e: Exception) {
            ensureActionable(context)
            database(context).query(Contact.TABLE, null, Contact.COLUMN_PHONE_NUMBER + "=?",
                    arrayOf(phoneNumber), null, null, null)
        }

        return if (cursor.moveToFirst()) {
            val contact = Contact()
            contact.fillFromCursor(cursor)
            cursor.closeSilent()
            contact
        } else {
            cursor.closeSilent()
            null
        }
    }

    /**
     * Get a list of contacts from a list of phone numbers
     *
     * @param numbers a comma separated list of phone numbers (Ex: 5154224558, 5159911493)
     * @return list of any contacts in the database for those phone numbers. Ignores numbers that are
     * not in the database.
     */
    fun getContacts(context: Context, numbers: String?): List<Contact> {
        if (numbers == null || numbers.isEmpty()) {
            return ArrayList()
        }

        // Could we miss matching a contact here? Sometimes phone numbers are resolved as
        // 5154224558 and other times it is +15154224558 depending on the carrier.
        val array = numbers.split(", ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        var where = ""
        when {
            array.isEmpty() -> return ArrayList()
            array.size == 1 -> {
                array[0] = "%" + ContactUtils.getPlainNumber(array[0]) + "%"
                where += Contact.COLUMN_PHONE_NUMBER + " LIKE ?"
            }
            else -> {
                array[0] = "%" + ContactUtils.getPlainNumber(array[0]) + "%"
                where = Contact.COLUMN_PHONE_NUMBER + " LIKE ?"
                for (i in 1 until array.size) {
                    array[i] = "%" + ContactUtils.getPlainNumber(array[i]) + "%"
                    where += " OR " + Contact.COLUMN_PHONE_NUMBER + " LIKE ?"
                }
            }
        }

        val contacts = ArrayList<Contact>()
        val cursor = try {
            database(context).query(Contact.TABLE, null, where, array, Contact.COLUMN_NAME, null, Contact.COLUMN_ID + " desc")
        } catch (e: Exception) {
            ensureActionable(context)
            database(context).query(Contact.TABLE, null, where, array, Contact.COLUMN_NAME, null, Contact.COLUMN_ID + " desc")
        }

        if (cursor.moveToFirst()) {
            do {
                val contact = Contact()
                contact.fillFromCursor(cursor)
                contacts.add(contact)
            } while (cursor.moveToNext())
        }

        cursor.closeSilent()
        return contacts
    }

    /**
     * Get a list of contacts from a list of phone numbers
     *
     * @param names a comma separated list of phone numbers (Ex: Luke Klinker, Jake Klinker)
     * @return list of any contacts in the database for those phone numbers. Ignores numbers that are
     * not in the database.
     */
    fun getContactsByNames(context: Context, names: String?): List<Contact> {
        if (names == null || names.isEmpty()) {
            return ArrayList()
        }

        val array = names.split(", ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        var where = ""
        when {
            array.isEmpty() -> return ArrayList()
            array.size == 1 -> where += Contact.COLUMN_NAME + "=?"
            else -> {
                where = Contact.COLUMN_NAME + "=?"
                for (i in 1 until array.size) {
                    where += " OR " + Contact.COLUMN_NAME + " LIKE ?"
                }
            }
        }

        val contacts = ArrayList<Contact>()
        val cursor = try {
            database(context).query(Contact.TABLE, null, where, array, Contact.COLUMN_NAME, null, Contact.COLUMN_ID + " desc")
        } catch (e: Exception) {
            ensureActionable(context)
            database(context).query(Contact.TABLE, null, where, array, Contact.COLUMN_NAME, null, Contact.COLUMN_ID + " desc")
        }

        if (cursor.moveToFirst()) {
            do {
                val contact = Contact()
                contact.fillFromCursor(cursor)
                contacts.add(contact)
            } while (cursor.moveToNext())
        }

        cursor.closeSilent()
        return contacts
    }

    /**
     * Deletes a contact from the database.
     *
     * @param phoneNumber the phone number to delete
     */
    fun deleteContact(context: Context, phoneNumber: String) {
        try {
            database(context).delete(Contact.TABLE, Contact.COLUMN_PHONE_NUMBER + "=?",
                    arrayOf(phoneNumber))
        } catch (e: Exception) {
            ensureActionable(context)
            database(context).delete(Contact.TABLE, Contact.COLUMN_PHONE_NUMBER + "=?",
                    arrayOf(phoneNumber))
        }

        apiUtils.deleteContact(accountId(context), phoneNumber, encryptor(context))
    }

    /**
     * Deletes a contact from the database.
     *
     * @param ids the phone number to delete
     */
    fun deleteContacts(context: Context, ids: Array<String>) {
        if (ids.isEmpty()) {
            return
        }

        var where = ""
        for (i in ids.indices) {
            if (i != 0) {
                where += " OR "
            }

            where += Contact.COLUMN_ID + "=?"
        }

        try {
            database(context).delete(Contact.TABLE, where, ids)
        } catch (e: Exception) {
            ensureActionable(context)
            database(context).delete(Contact.TABLE, where, ids)
        }
    }

    /**
     * Updates the conversation with given values.
     *
     * @param contact the contact with new values
     */
    fun updateContact(context: Context, contact: Contact) {
        updateContact(context, contact.phoneNumber, contact.name, contact.colors.color, contact.colors.colorDark,
                contact.colors.colorLight, contact.colors.colorAccent)
    }

    /**
     * Updates the conversation with given values.
     *
     * @param phoneNumber    the contact to update
     * @param name           the contacts new name (null if we don't want to update it)
     * @param color          the new main color (null if we don't want to update it)
     * @param colorDark      the new dark color (null if we don't want to update it)
     * @param colorLight     the new light color (null if we don't want to update it)
     * @param colorAccent    the new accent color (null if we don't want to update it)
     */
    fun updateContact(context: Context, phoneNumber: String, name: String?, color: Int?, colorDark: Int?,
                      colorLight: Int?, colorAccent: Int?) {
        val values = ContentValues()

        if (name != null) values.put(Contact.COLUMN_NAME, name)
        if (color != null) values.put(Contact.COLUMN_COLOR, color)
        if (colorDark != null) values.put(Contact.COLUMN_COLOR_DARK, colorDark)
        if (colorLight != null) values.put(Contact.COLUMN_COLOR_LIGHT, colorLight)
        if (colorAccent != null) values.put(Contact.COLUMN_COLOR_ACCENT, colorAccent)

        val updated = try {
            database(context).update(Contact.TABLE, values, Contact.COLUMN_PHONE_NUMBER + "=?",
                    arrayOf(phoneNumber))
        } catch (e: Exception) {
            ensureActionable(context)
            database(context).update(Contact.TABLE, values, Contact.COLUMN_PHONE_NUMBER + "=?",
                    arrayOf(phoneNumber))
        }

        if (updated > 0) {
            apiUtils.updateContact(accountId(context), phoneNumber, name, color, colorDark,
                    colorLight, colorAccent, encryptor(context))
        }
    }

    /**
     * Gets the number of contacts in the database.
     */
    fun getContactsCount(context: Context): Int {
        val cursor = getContacts(context)
        val count = cursor.count
        cursor.closeSilent()
        return count
    }

    /**
     * Writes the initial list of conversations to the database. These are the conversations that
     * will come from your phones internal SMS database. It will then find all messages in each
     * of these conversations and insert them as well, during the same transaction.
     *
     * @param conversations the list of conversations. See SmsMmsUtils.queryConversations().
     * @param context       the application context.
     */
    fun insertConversations(conversations: List<Conversation>, context: Context,
                            listener: ProgressUpdateListener?) {
        beginTransaction(context)

        for (i in conversations.indices) {
            val conversation = conversations[i]

            val values = ContentValues(16)

            // here we are loading the id from the internal database into the conversation object
            // but we don't want to use that so we'll just generate a new one.
            values.put(Conversation.COLUMN_ID, generateId())
            values.put(Conversation.COLUMN_COLOR, conversation.colors.color)
            values.put(Conversation.COLUMN_COLOR_DARK, conversation.colors.colorDark)
            values.put(Conversation.COLUMN_COLOR_LIGHT, conversation.colors.colorLight)
            values.put(Conversation.COLUMN_COLOR_ACCENT, conversation.colors.colorAccent)
            values.put(Conversation.COLUMN_LED_COLOR, conversation.ledColor)
            values.put(Conversation.COLUMN_PINNED, conversation.pinned)
            values.put(Conversation.COLUMN_READ, conversation.read)
            values.put(Conversation.COLUMN_TIMESTAMP, conversation.timestamp)
            values.put(Conversation.COLUMN_TITLE, conversation.title)
            values.put(Conversation.COLUMN_PHONE_NUMBERS, conversation.phoneNumbers)
            values.put(Conversation.COLUMN_SNIPPET, conversation.snippet)
            values.put(Conversation.COLUMN_RINGTONE, conversation.ringtoneUri)
            values.put(Conversation.COLUMN_IMAGE_URI, conversation.imageUri)
            values.put(Conversation.COLUMN_ID_MATCHER, conversation.idMatcher)
            values.put(Conversation.COLUMN_MUTE, conversation.mute)
            values.put(Conversation.COLUMN_ARCHIVED, conversation.archive)

            val conversationId = try {
                database(context).insert(Conversation.TABLE, null, values)
            } catch (e: Exception) {
                ensureActionable(context)
                database(context).insert(Conversation.TABLE, null, values)
            }

            if (conversationId != -1L) {
                val messages = SmsMmsUtils.queryConversation(conversation.id, context) ?: continue

                if (messages.count == 0) {
                    deleteConversation(context, conversationId)

                    try {
                        messages.close()
                    } catch (e: Exception) {
                    }

                    continue
                }

                if (messages.moveToFirst()) {
                    do {
                        val valuesList = SmsMmsUtils.processMessage(messages, conversationId, context)
                        if (valuesList != null) {
                            for (value in valuesList) {
                                database(context).insert(Message.TABLE, null, value)
                            }
                        }
                    } while (messages.moveToNext() && messages.position < SmsMmsUtils.INITIAL_MESSAGE_LIMIT)
                }

                try {
                    messages.close()
                } catch (e: Exception) {
                }

            }

            listener?.onProgressUpdate(i + 1, conversations.size)
        }

        setTransactionSuccessful(context)
        endTransaction(context)
    }

    /**
     * Inserts a conversation into the database.
     *
     * @param conversation the conversation to insert.
     * @return the conversation id after insertion.
     */
    fun insertConversation(context: Context, conversation: Conversation): Long {
        val values = ContentValues(16)

        if (conversation.id <= 0) {
            conversation.id = generateId()
        }

        values.put(Conversation.COLUMN_ID, conversation.id)
        values.put(Conversation.COLUMN_COLOR, conversation.colors.color)
        values.put(Conversation.COLUMN_COLOR_DARK, conversation.colors.colorDark)
        values.put(Conversation.COLUMN_COLOR_LIGHT, conversation.colors.colorLight)
        values.put(Conversation.COLUMN_COLOR_ACCENT, conversation.colors.colorAccent)
        values.put(Conversation.COLUMN_LED_COLOR, conversation.ledColor)
        values.put(Conversation.COLUMN_PINNED, conversation.pinned)
        values.put(Conversation.COLUMN_READ, conversation.read)
        values.put(Conversation.COLUMN_TIMESTAMP, conversation.timestamp)
        values.put(Conversation.COLUMN_TITLE, conversation.title)
        values.put(Conversation.COLUMN_PHONE_NUMBERS, conversation.phoneNumbers)
        values.put(Conversation.COLUMN_SNIPPET, conversation.snippet)
        values.put(Conversation.COLUMN_RINGTONE, conversation.ringtoneUri)
        values.put(Conversation.COLUMN_IMAGE_URI, conversation.imageUri)
        values.put(Conversation.COLUMN_ID_MATCHER, conversation.idMatcher)
        values.put(Conversation.COLUMN_MUTE, conversation.mute)
        values.put(Conversation.COLUMN_ARCHIVED, conversation.archive)
        values.put(Conversation.COLUMN_PRIVATE_NOTIFICATIONS, conversation.privateNotifications)

        apiUtils.addConversation(accountId(context), conversation.id, conversation.colors.color,
                conversation.colors.colorDark, conversation.colors.colorLight, conversation.colors.colorAccent,
                conversation.ledColor, conversation.pinned, conversation.read,
                conversation.timestamp, conversation.title, conversation.phoneNumbers,
                conversation.snippet, conversation.ringtoneUri, conversation.idMatcher,
                conversation.mute, conversation.archive, conversation.privateNotifications,
                encryptor(context))

        if (apiUtils.isActive) {
            writeUnreadCount(context)
        }

        return try {
            database(context).insert(Conversation.TABLE, null, values)
        } catch (e: Exception) {
            ensureActionable(context)
            try {
                database(context).insert(Conversation.TABLE, null, values)
            } catch (x: Exception) {
                -1L
            }

        }

    }

    private fun convertConversationCursorToList(cursor: Cursor): List<Conversation> {
        val conversations = ArrayList<Conversation>()

        try {
            if (cursor.moveToFirst()) {
                do {
                    val c = Conversation()
                    c.fillFromCursor(cursor)
                    conversations.add(c)
                } while (cursor.moveToNext())
            }

            cursor.close()
        } catch (e: Exception) {

        }

        return conversations
    }

    /**
     * Gets all conversations in the database that are not archived
     *
     * @return a list of conversations.
     */
    fun getUnarchivedConversations(context: Context): Cursor =
            try {
                database(context).query(Conversation.TABLE, null, Conversation.COLUMN_ARCHIVED + "=?", arrayOf("0"), null, null,
                        Conversation.COLUMN_PINNED + " desc, " + Conversation.COLUMN_TIMESTAMP + " desc"
                )
            } catch (e: Exception) {
                ensureActionable(context)
                database(context).query(Conversation.TABLE, null, Conversation.COLUMN_ARCHIVED + "=?", arrayOf("0"), null, null,
                        Conversation.COLUMN_PINNED + " desc, " + Conversation.COLUMN_TIMESTAMP + " desc"
                )
            }

    /**
     * Get a list of the unarchived conversations.
     * @return a list of the conversations in the cursor
     */
    fun getUnarchivedConversationsAsList(context: Context): List<Conversation> =
            convertConversationCursorToList(getUnarchivedConversations(context))

    /**
     * Get a list of all the conversations.
     * @return a list of the conversations in the cursor
     */
    fun getAllConversationsAsList(context: Context): List<Conversation> =
            convertConversationCursorToList(getAllConversations(context))

    /**
     * Gets all conversations in the database.
     *
     * @return a list of conversations.
     */
    fun getAllConversations(context: Context): Cursor =
            try {
                database(context).query(Conversation.TABLE, null, null, null, null, null,
                        Conversation.COLUMN_PINNED + " desc, " + Conversation.COLUMN_TIMESTAMP + " desc"
                )
            } catch (e: Exception) {
                ensureActionable(context)
                database(context).query(Conversation.TABLE, null, null, null, null, null,
                        Conversation.COLUMN_PINNED + " desc, " + Conversation.COLUMN_TIMESTAMP + " desc"
                )
            }

    /**
     * Gets all pinned conversations in the database.
     *
     * @return a list of pinned conversations.
     */
    fun getPinnedConversations(context: Context): Cursor =
            try {
                database(context).query(Conversation.TABLE, null, Conversation.COLUMN_PINNED + "=1", null, null, null, Conversation.COLUMN_TIMESTAMP + " desc")
            } catch (e: Exception) {
                ensureActionable(context)
                database(context).query(Conversation.TABLE, null, Conversation.COLUMN_PINNED + "=1", null, null, null, Conversation.COLUMN_TIMESTAMP + " desc")
            }

    /**
     * Get a list of all the pinned conversations.
     * @return a list of the conversations in the cursor
     */
    fun getPinnedConversationsAsList(context: Context): List<Conversation> =
            convertConversationCursorToList(getPinnedConversations(context))

    /**
     * Gets all archived conversations in the database.
     *
     * @return a list of pinned conversations.
     */
    fun getArchivedConversations(context: Context): Cursor =
            try {
                database(context).query(Conversation.TABLE, null, Conversation.COLUMN_ARCHIVED + "=1", null, null, null, Conversation.COLUMN_TIMESTAMP + " desc")
            } catch (e: Exception) {
                ensureActionable(context)
                database(context).query(Conversation.TABLE, null, Conversation.COLUMN_ARCHIVED + "=1", null, null, null, Conversation.COLUMN_TIMESTAMP + " desc")
            }


    /**
     * Get a list of all the archived conversations.
     * @return a list of the conversations in the cursor
     */
    fun getArchivedConversationsAsList(context: Context): List<Conversation> =
            convertConversationCursorToList(getArchivedConversations(context))

    /**
     * Gets all unread conversations in the database. Only those that are not archived
     *
     * @return a list of unread conversations that aren't archived
     */
    fun getUnreadConversations(context: Context): Cursor =
            try {
                database(context).query(Conversation.TABLE, null, Conversation.COLUMN_READ + "=0 and "
                        + Conversation.COLUMN_ARCHIVED + "=0", null, null, null,
                        Conversation.COLUMN_TIMESTAMP + " desc")
            } catch (e: Exception) {
                ensureActionable(context)
                database(context).query(Conversation.TABLE, null, Conversation.COLUMN_READ + "=0 and "
                        + Conversation.COLUMN_ARCHIVED + "=0", null, null, null,
                        Conversation.COLUMN_TIMESTAMP + " desc")
            }

    /**
     * Get the count of unread conversations in the database
     */
    fun getUnreadConversationCount(context: Context): Int {
        val cursor = getUnreadConversations(context)

        var count = 0
        if (cursor.moveToFirst()) {
            val muteIndex = cursor.getColumnIndex(Conversation.COLUMN_MUTE)

            do {
                val muted = cursor.getInt(muteIndex) == 1
                count += if (muted) 0 else 1
            } while (cursor.moveToNext())
        }

        cursor.closeSilent()
        return count
    }

    fun getUnreadConversationsAsList(context: Context): List<Conversation> {
        val cursor = getUnreadConversations(context)
        val conversations = ArrayList<Conversation>()

        if (cursor.moveToFirst()) {
            do {
                val conversation = Conversation()
                conversation.fillFromCursor(cursor)

                conversations.add(conversation)
            } while (cursor.moveToNext())
        }

        cursor.closeSilent()
        return conversations
    }

    /**
     * Searches for conversations that have a title that matches the given query.
     */
    fun searchConversations(context: Context, query: String?): Cursor? {
        return if (query == null || query.isEmpty()) {
            null
        } else {
            try {
                database(context).query(Conversation.TABLE, null, Conversation.COLUMN_TITLE + " LIKE '%" +
                        query.replace("'", "''") + "%'", null, null, null,
                        Conversation.COLUMN_TIMESTAMP + " desc")
            } catch (e: Exception) {
                ensureActionable(context)
                database(context).query(Conversation.TABLE, null, Conversation.COLUMN_TITLE + " LIKE '%" +
                        query.replace("'", "''") + "%'", null, null, null,
                        Conversation.COLUMN_TIMESTAMP + " desc")
            }
        }
    }

    /**
     * Searches the conversations that match the query, to return a List
     */
    fun searchConversationsAsList(context: Context, query: String?, count: Int): List<Conversation> {
        val cursor = searchConversations(context, query)
        val conversations = ArrayList<Conversation>()

        if (cursor != null && cursor.moveToFirst()) {
            do {
                val conversation = Conversation()
                conversation.fillFromCursor(cursor)

                conversations.add(conversation)
            } while (cursor.moveToNext() && conversations.size < count)
        }

        cursor?.closeSilent()
        return conversations
    }

    /**
     * Gets a conversation by its id.
     *
     * @param conversationId the conversation's id to find.
     * @return the conversation.
     */
    fun getConversation(context: Context, conversationId: Long): Conversation? {
        val cursor = try {
            database(context).query(Conversation.TABLE, null, Conversation.COLUMN_ID + "=?",
                    arrayOf(java.lang.Long.toString(conversationId)), null, null, null)
        } catch (e: Exception) {
            ensureActionable(context)
            database(context).query(Conversation.TABLE, null, Conversation.COLUMN_ID + "=?",
                    arrayOf(java.lang.Long.toString(conversationId)), null, null, null)
        }

        return if (cursor.moveToFirst()) {
            val conversation = Conversation()
            conversation.fillFromCursor(cursor)
            cursor.close()
            conversation
        } else {
            cursor.closeSilent()
            null
        }
    }

    /**
     * Deletes a conversation from the database.
     *
     * @param conversation the conversation to delete.
     */
    fun deleteConversation(context: Context, conversation: Conversation?) {
        if (conversation != null) {
            deleteConversation(context, conversation.id)
        } else {
            // more than likely already deleted
        }
    }

    /**
     * Deletes a conversation from the database.
     *
     * @param conversationId the conversation id to delete.
     */
    fun deleteConversation(context: Context, conversationId: Long) {
        try {
            database(context).delete(Message.TABLE, Message.COLUMN_CONVERSATION_ID + "=?",
                    arrayOf(java.lang.Long.toString(conversationId)))
        } catch (e: Exception) {
            ensureActionable(context)
            database(context).delete(Message.TABLE, Message.COLUMN_CONVERSATION_ID + "=?",
                    arrayOf(java.lang.Long.toString(conversationId)))
        }

        try {
            database(context).delete(Conversation.TABLE, Conversation.COLUMN_ID + "=?",
                    arrayOf(java.lang.Long.toString(conversationId)))
        } catch (e: Exception) {
            ensureActionable(context)
            database(context).delete(Conversation.TABLE, Conversation.COLUMN_ID + "=?",
                    arrayOf(java.lang.Long.toString(conversationId)))
        }

        apiUtils.deleteConversation(accountId(context), conversationId)
        NotificationUtils.deleteChannel(context, conversationId)

        writeUnreadCount(context)
        NewMessagesCheckService.writeLastRun(context)
    }

    /**
     * Archives a conversation from the database.
     *
     * @param conversationId the conversation to archive.
     */
    fun unarchiveConversation(context: Context, conversationId: Long) {
        archiveConversation(context, conversationId, false)
    }

    /**
     * Archives a conversation from the database.
     *
     * @param conversationId the conversation id to archive.
     * @param archive true if we want to archive, false if we want to have it not archived
     */
    @JvmOverloads
    fun archiveConversation(context: Context, conversationId: Long, archive: Boolean = true) {
        val values = ContentValues(1)
        values.put(Conversation.COLUMN_ARCHIVED, archive)
        values.put(Conversation.COLUMN_READ, true)

        val updated = try {
            database(context).update(Conversation.TABLE, values, Conversation.COLUMN_ID + "=?",
                    arrayOf(java.lang.Long.toString(conversationId)))
        } catch (e: Exception) {
            ensureActionable(context)
            database(context).update(Conversation.TABLE, values, Conversation.COLUMN_ID + "=?",
                    arrayOf(java.lang.Long.toString(conversationId)))
        }

        if (updated > 0) {
            if (archive) {
                apiUtils.archiveConversation(accountId(context), conversationId)
            } else {
                apiUtils.unarchiveConversation(accountId(context), conversationId)
            }

            writeUnreadCount(context)
        }
    }

    /**
     * Updates the conversation with given values.
     *
     * @param conversationId the conversation to update.
     * @param read           whether the conversation is read or not.
     * @param timestamp      the new timestamp for the conversation
     * @param snippet        the snippet to display for appropriate mime types.
     * @param snippetMime    the snippet's mime type.
     */
    fun updateConversation(context: Context, conversationId: Long, read: Boolean, timestamp: Long,
                           snippet: String, snippetMime: String?, archive: Boolean) {
        var snippet = snippet
        val values = ContentValues(4)
        values.put(Conversation.COLUMN_READ, read)

        if (snippetMime != null && snippetMime == MimeType.TEXT_PLAIN) {
            values.put(Conversation.COLUMN_SNIPPET, snippet)
        } else {
            snippet = ""
            values.put(Conversation.COLUMN_SNIPPET, "")
        }

        values.put(Conversation.COLUMN_TIMESTAMP, timestamp)
        values.put(Conversation.COLUMN_ARCHIVED, archive)

        val updated = try {
            database(context).update(Conversation.TABLE, values, Conversation.COLUMN_ID + "=?",
                    arrayOf(java.lang.Long.toString(conversationId)))
        } catch (e: Exception) {
            ensureActionable(context)
            database(context).update(Conversation.TABLE, values, Conversation.COLUMN_ID + "=?",
                    arrayOf(java.lang.Long.toString(conversationId)))
        }

        if (updated > 0) {
            apiUtils.updateConversationSnippet(accountId(context), conversationId,
                    read, archive, timestamp, snippet, encryptor(context))
            writeUnreadCount(context)
        }
    }

    /**
     * Updates the settings_global for a conversation, such as ringtone and colors.
     */
    fun updateConversationSettings(context: Context, conversation: Conversation) {
        val values = ContentValues(13)
        values.put(Conversation.COLUMN_PINNED, conversation.pinned)
        values.put(Conversation.COLUMN_TITLE, conversation.title)
        values.put(Conversation.COLUMN_RINGTONE, conversation.ringtoneUri)
        values.put(Conversation.COLUMN_COLOR, conversation.colors.color)
        values.put(Conversation.COLUMN_COLOR_DARK, conversation.colors.colorDark)
        values.put(Conversation.COLUMN_COLOR_LIGHT, conversation.colors.colorLight)
        values.put(Conversation.COLUMN_COLOR_ACCENT, conversation.colors.colorAccent)
        values.put(Conversation.COLUMN_LED_COLOR, conversation.ledColor)
        values.put(Conversation.COLUMN_MUTE, conversation.mute)
        values.put(Conversation.COLUMN_READ, conversation.read)
        values.put(Conversation.COLUMN_ARCHIVED, conversation.archive)
        values.put(Conversation.COLUMN_PRIVATE_NOTIFICATIONS, conversation.privateNotifications)

        if (conversation.simSubscriptionId != null) {
            values.put(Conversation.COLUMN_SIM_SUBSCRIPTION_ID, conversation.simSubscriptionId)
        }

        try {
            database(context).update(Conversation.TABLE, values, Conversation.COLUMN_ID + "=?",
                    arrayOf(java.lang.Long.toString(conversation.id)))
        } catch (e: Exception) {
            ensureActionable(context)
            database(context).update(Conversation.TABLE, values, Conversation.COLUMN_ID + "=?",
                    arrayOf(java.lang.Long.toString(conversation.id)))
        }

        apiUtils.updateConversation(accountId(context), conversation.id, conversation.colors.color,
                conversation.colors.colorDark, conversation.colors.colorLight, conversation.colors.colorAccent,
                conversation.ledColor, conversation.pinned, null, null,
                conversation.title, null, conversation.ringtoneUri, conversation.mute, conversation.archive,
                conversation.privateNotifications, encryptor(context))
    }

    /**
     * Updates the conversation title for a given conversation. Handy when the user has changed
     * the contact's name.
     */
    fun updateConversationTitle(context: Context, conversationId: Long, title: String) {
        val values = ContentValues(1)
        values.put(Conversation.COLUMN_TITLE, title)

        val updated = try {
            database(context).update(Conversation.TABLE, values, Conversation.COLUMN_ID + "=? AND " +
                    Conversation.COLUMN_TITLE + " <> ?",
                    arrayOf(java.lang.Long.toString(conversationId), title))
        } catch (e: Exception) {
            ensureActionable(context)
            database(context).update(Conversation.TABLE, values, Conversation.COLUMN_ID + "=? AND " +
                    Conversation.COLUMN_TITLE + " <> ?",
                    arrayOf(java.lang.Long.toString(conversationId), title))
        }

        if (updated > 0) {
            apiUtils.updateConversationTitle(accountId(context), conversationId, title, encryptor(context))
        }
    }

    /**
     * Updates the conversation image for a given conversation
     */
    fun updateConversationImage(context: Context, conversationId: Long, imageUri: String) {
        val values = ContentValues(1)
        values.put(Conversation.COLUMN_IMAGE_URI, imageUri)

        val updated = try {
            database(context).update(Conversation.TABLE, values, Conversation.COLUMN_ID + "=?",
                    arrayOf(java.lang.Long.toString(conversationId)))
        } catch (e: Exception) {
            ensureActionable(context)
            database(context).update(Conversation.TABLE, values, Conversation.COLUMN_ID + "=?",
                    arrayOf(java.lang.Long.toString(conversationId)))
        }

        // no need to update the API, since image uris are local
    }

    /**
     * Gets the number of conversations in the database.
     */
    fun getConversationCount(context: Context): Int {
        val cursor = getAllConversations(context)
        val count = cursor.count
        cursor.closeSilent()
        return count
    }

    /**
     * Gets the number of messages in the database.
     */
    fun getMessageCount(context: Context): Int {
        val cursor = getMessages(context)
        val count = cursor.count
        cursor.closeSilent()
        return count
    }

    /**
     * Gets details about a conversation that can be displayed to the user.
     */
    fun getConversationDetails(context: Context, conversation: Conversation): Spanned {
        val builder = StringBuilder()

        if (conversation.isGroup) {
            builder.append("<b>Title: </b>")
        } else {
            builder.append("<b>Name: </b>")
        }

        builder.append(conversation.title)
        builder.append("<br/>")

        if (conversation.isGroup) {
            builder.append("<b>Phone Numbers: </b>")
        } else {
            builder.append("<b>Phone Number: </b>")
        }

        builder.append(conversation.phoneNumbers)
        builder.append("<br/>")

        if (conversation.isGroup) {
            builder.append("<b>Number of Members: </b>")
            builder.append(conversation.phoneNumbers.split(", ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray().size)
            builder.append("<br/>")
        }

        builder.append("<b>Date: </b>")
        builder.append(SimpleDateFormat
                .getDateTimeInstance(SimpleDateFormat.SHORT, SimpleDateFormat.SHORT)
                .format(Date(conversation.timestamp)))
        builder.append("<br/>")

        val cursor = getMessages(context, conversation.id)
        if (cursor.moveToFirst()) {
            builder.append("<b>Message Count: </b>")
            builder.append(cursor.count)
            builder.append("<br/>")
        }

        cursor.closeSilent()

        // remove the last <br/>
        var description = builder.toString()
        description = description.substring(0, description.length - 5)

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Html.fromHtml(description, 0)
        } else {
            Html.fromHtml(description)
        }
    }

    /**
     * Gets the details for a message.
     */
    fun getMessageDetails(context: Context, messageId: Long): Spanned {
        val message = getMessage(context, messageId)
        val builder = StringBuilder()

        builder.append("<b>Date: </b>")
        builder.append(SimpleDateFormat
                .getDateTimeInstance(SimpleDateFormat.SHORT, SimpleDateFormat.SHORT)
                .format(Date(message!!.timestamp)))
        builder.append("<br/>")

        builder.append("<b>Status: </b>")
        when (message.type) {
            Message.TYPE_SENT -> builder.append("Sent")
            Message.TYPE_SENDING -> builder.append("Sending")
            Message.TYPE_ERROR -> builder.append("Failed")
            Message.TYPE_DELIVERED -> builder.append("Delivered")
            Message.TYPE_RECEIVED -> builder.append("Received")
            Message.TYPE_INFO -> builder.append("Info")
        }
        builder.append("<br/>")

        //        builder.append("<b>Read: </b>");
        //        builder.append(message.read);
        //        builder.append("<br/>");
        //
        //        builder.append("<b>Seen: </b>");
        //        builder.append(message.seen);
        //        builder.append("<br/>");

        if (message.from != null) {
            builder.append("<b>From: </b>")
            builder.append(message.from)
            builder.append("<br/>")
        }

        if (message.mimeType != MimeType.TEXT_PLAIN) {
            val bytes = BinaryUtils.getMediaBytes(context, message.data, message.mimeType)
            builder.append("<b>Size: </b>")
            builder.append(Formatter.formatShortFileSize(context, bytes.size.toLong()))
            builder.append("<br/>")

            builder.append("<b>Media Type: </b>")
            builder.append(message.mimeType)
            builder.append("<br/>")
        }

        // remove the last <br/>
        var description = builder.toString()
        description = description.substring(0, description.length - 5)

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Html.fromHtml(description, 0)
        } else {
            Html.fromHtml(description)
        }
    }

    /**
     * Gets all messages for a given conversation.
     *
     * @param conversationId the conversation id to find messages for.
     * @return a cursor with all messages.
     */
    fun getMessages(context: Context, conversationId: Long): Cursor =
            try {
                database(context).query(Message.TABLE, null, Message.COLUMN_CONVERSATION_ID + "=?",
                        arrayOf(java.lang.Long.toString(conversationId)), null, null,
                        Message.COLUMN_TIMESTAMP + " asc")
            } catch (e: Exception) {
                ensureActionable(context)
                database(context).query(Message.TABLE, null, Message.COLUMN_CONVERSATION_ID + "=?",
                        arrayOf(java.lang.Long.toString(conversationId)), null, null,
                        Message.COLUMN_TIMESTAMP + " asc")
            }

    /**
     * Gets a single message from the database.
     */
    fun getMessage(context: Context, messageId: Long): Message? {
        val cursor = try {
            database(context).query(Message.TABLE, null, Message.COLUMN_ID + "=?",
                    arrayOf(java.lang.Long.toString(messageId)), null, null, null)
        } catch (e: Exception) {
            ensureActionable(context)
            database(context).query(Message.TABLE, null, Message.COLUMN_ID + "=?",
                    arrayOf(java.lang.Long.toString(messageId)), null, null, null)
        }

        return if (cursor.moveToFirst()) {
            val message = Message()
            message.fillFromCursor(cursor)
            cursor.closeSilent()
            message
        } else {
            cursor.closeSilent()
            null
        }
    }

    /**
     * Gets the latest message in the database.
     */
    fun getLatestMessage(context: Context): Message? {
        val cursor = try {
            database(context).query(Message.TABLE, null, null, null, null, null, Message.COLUMN_TIMESTAMP + " desc", "1")
        } catch (e: Exception) {
            ensureActionable(context)
            database(context).query(Message.TABLE, null, null, null, null, null, Message.COLUMN_TIMESTAMP + " desc", "1")
        }

        return if (cursor.moveToFirst()) {
            val message = Message()
            message.fillFromCursor(cursor)
            cursor.closeSilent()
            message
        } else {
            cursor.closeSilent()
            null
        }
    }

    /**
     * Gets all messages in the database where mime type is not text/plain.
     */
    fun getAllMediaMessages(context: Context, limit: Int): Cursor =
            try {
                database(context).query(Message.TABLE, null, Message.COLUMN_MIME_TYPE + "!='text/plain'", null, null, null, Message.COLUMN_TIMESTAMP + " desc LIMIT " + limit)
            } catch (e: Exception) {
                ensureActionable(context)
                database(context).query(Message.TABLE, null, Message.COLUMN_MIME_TYPE + "!='text/plain'", null, null, null, Message.COLUMN_TIMESTAMP + " desc LIMIT " + limit)
            }

    /**
     * Gets all messages in the database that still need to be downloaded from firebase. When
     * inserted into the server database, instead of messages having the uri to the file they
     * will simply contain "firebase [num]" to indicate that they need to be downloaded still.
     * The num at the end is used for making the initial upload (20 will be done the first time)
     * and so if that num is < 20 on the downloading side it means that there won't actually be
     * an image for it and we shouldn't try to download. After the initial upload, we should use
     * "firebase -1" to indicate that the image will be available for download.
     */
    fun getFirebaseMediaMessages(context: Context): Cursor {
        return try {
            database(context).query(Message.TABLE, null, Message.COLUMN_MIME_TYPE + "!='text/plain' AND " +
                    Message.COLUMN_DATA + " LIKE 'firebase %'", null, null, null, null)
        } catch (e: Exception) {
            ensureActionable(context)
            database(context).query(Message.TABLE, null, Message.COLUMN_MIME_TYPE + "!='text/plain' AND " +
                    Message.COLUMN_DATA + " LIKE 'firebase %'", null, null, null, null)
        }
    }

    /**
     * Gets all messages for a conversation where the mime type is not text/plain.
     */
    fun getMediaMessages(context: Context, conversationId: Long): List<Message> {
        val cursor = try {
            database(context).query(Message.TABLE, null, Message.COLUMN_CONVERSATION_ID + "=? AND " +
                    Message.COLUMN_MIME_TYPE + "!='text/plain' AND " +
                    Message.COLUMN_MIME_TYPE + "!='text/x-vcard' AND " +
                    Message.COLUMN_MIME_TYPE + "!='text/vcard'",
                    arrayOf(java.lang.Long.toString(conversationId)), null, null,
                    Message.COLUMN_TIMESTAMP + " asc")
        } catch (e: Exception) {
            ensureActionable(context)
            database(context).query(Message.TABLE, null, Message.COLUMN_CONVERSATION_ID + "=? AND " +
                    Message.COLUMN_MIME_TYPE + "!='text/plain' AND " +
                    Message.COLUMN_MIME_TYPE + "!='text/x-vcard' AND " +
                    Message.COLUMN_MIME_TYPE + "!='text/vcard'",
                    arrayOf(java.lang.Long.toString(conversationId)), null, null,
                    Message.COLUMN_TIMESTAMP + " asc")
        }

        val messages = ArrayList<Message>()

        if (cursor.moveToFirst()) {
            do {
                val message = Message()
                message.fillFromCursor(cursor)
                messages.add(message)
            } while (cursor.moveToNext())
        }

        cursor.closeSilent()
        return messages
    }

    /**
     * Gets all messages in the database.
     */
    fun getMessages(context: Context): Cursor =
            try {
                database(context).query(Message.TABLE, null, null, null, null, null,
                        Message.COLUMN_TIMESTAMP + " asc")
            } catch (e: Exception) {
                ensureActionable(context)
                database(context).query(Message.TABLE, null, null, null, null, null,
                        Message.COLUMN_TIMESTAMP + " asc")
            }

    /**
     * Gets all messages in the database, never than the given time
     */
    fun getNewerMessages(context: Context, timestamp: Long): Cursor =
            try {
                database(context).query(Message.TABLE, null, Message.COLUMN_TIMESTAMP + ">?",
                        arrayOf(timestamp.toString()), null, null,
                        Message.COLUMN_TIMESTAMP + " desc")
            } catch (e: Exception) {
                ensureActionable(context)
                database(context).query(Message.TABLE, null, Message.COLUMN_TIMESTAMP + ">?",
                        arrayOf(timestamp.toString()), null, null,
                        Message.COLUMN_TIMESTAMP + " desc")
            }

    /**
     * Get the specified number of messages from the conversation.
     */
    fun getMessages(context: Context, conversationId: Long, count: Int): List<Message> {
        val cursor = getMessages(context, conversationId)
        val messages = ArrayList<Message>()

        if (cursor.moveToLast()) {
            do {
                val message = Message()
                message.fillFromCursor(cursor)
                messages.add(message)
            } while (cursor.moveToPrevious() && messages.size < count)
        }

        cursor.closeSilent()
        return messages
    }

    /**
     * Get the specified number of messages.
     */
    fun getNumberOfMessages(context: Context, count: Int): List<Message> {
        val cursor = getMessages(context)
        val messages = ArrayList<Message>()

        if (cursor.moveToLast()) {
            do {
                val message = Message()
                message.fillFromCursor(cursor)
                messages.add(message)
            } while (cursor.moveToPrevious() && messages.size < count)
        }

        cursor.closeSilent()
        return messages
    }

    /**
     * Gets all messages that contain the query text.
     *
     * @param query the text to look for.
     * @return a cursor with all messages matching that query.
     */
    fun searchMessages(context: Context, query: String?): Cursor? =
            if (query == null || query.isEmpty()) {
                null
            } else {
                try {
                    database(context).query(Message.TABLE + " m left outer join " + Conversation.TABLE + " c on m.conversation_id = c._id",
                            arrayOf("m._id as _id", "c._id as conversation_id", "m.type as type", "m.data as data", "m.timestamp as timestamp", "m.mime_type as mime_type", "m.read as read", "m.message_from as message_from", "m.color as color", "c.title as convo_title"),
                            Message.COLUMN_DATA + " LIKE '%" + query.replace("'", "''") + "%' AND " +
                                    Message.COLUMN_MIME_TYPE + "='" + MimeType.TEXT_PLAIN + "'", null, null, null, Message.COLUMN_TIMESTAMP + " desc")
                } catch (e: Exception) {
                    ensureActionable(context)
                    try {
                        database(context).query(Message.TABLE + " m left outer join " + Conversation.TABLE + " c on m.conversation_id = c._id",
                                arrayOf("m._id as _id", "c._id as conversation_id", "m.type as type", "m.data as data", "m.timestamp as timestamp", "m.mime_type as mime_type", "m.read as read", "m.message_from as message_from", "m.color as color", "c.title as convo_title"),
                                Message.COLUMN_DATA + " LIKE '%" + query.replace("'", "''") + "%' AND " +
                                        Message.COLUMN_MIME_TYPE + "='" + MimeType.TEXT_PLAIN + "'", null, null, null, Message.COLUMN_TIMESTAMP + " desc")
                    } catch (x: Exception) {
                        null
                    }

                }
            }

    fun searchMessagesAsList(context: Context, query: String, amount: Int): List<Message> {
        val cursor = searchMessages(context, query)
        val messages = ArrayList<Message>()

        if (cursor != null && cursor.moveToFirst()) {
            do {
                val message = Message()
                message.fillFromCursor(cursor)

                messages.add(message)
            } while (cursor.moveToNext() && messages.size < amount)
        }

        cursor?.closeSilent()
        return messages
    }

    /**
     * Gets all messages that are within 5 seconds of the given timestamp.
     *
     * @param timestamp the message timestamp.
     * @return the cursor of messages.
     */
    fun searchMessages(context: Context, timestamp: Long): Cursor =
            try {
                database(context).query(Message.TABLE, null, Message.COLUMN_TIMESTAMP + " BETWEEN " +
                        (timestamp - 10000) + " AND " + (timestamp + 10000), null, null, null,
                        Message.COLUMN_TIMESTAMP + " desc")
            } catch (e: Exception) {
                ensureActionable(context)
                database(context).query(Message.TABLE, null, Message.COLUMN_TIMESTAMP + " BETWEEN " +
                        (timestamp - 10000) + " AND " + (timestamp + 10000), null, null, null,
                        Message.COLUMN_TIMESTAMP + " desc")
            }

    /**
     * Updates the message with the given id to the given type.
     *
     * @param messageId the message to update.
     * @param type      the type to change it to.
     */
    fun updateMessageType(context: Context, messageId: Long, type: Int) {
        val values = ContentValues(1)
        values.put(Message.COLUMN_TYPE, type)

        try {
            database(context).update(Message.TABLE, values, Message.COLUMN_ID + "=? AND " + Message.COLUMN_TYPE + "<>?",
                    arrayOf(java.lang.Long.toString(messageId), Integer.toString(Message.TYPE_RECEIVED)))
        } catch (e: Exception) {
            ensureActionable(context)
            database(context).update(Message.TABLE, values, Message.COLUMN_ID + "=? AND " + Message.COLUMN_TYPE + "<>?",
                    arrayOf(java.lang.Long.toString(messageId), Integer.toString(Message.TYPE_RECEIVED)))
        }

        apiUtils.updateMessageType(accountId(context), messageId, type)
    }

    /**
     * Updates the data field for a message.
     *
     * @param messageId the id of the message to update.
     * @param data      the new data string.
     */
    fun updateMessageData(context: Context, messageId: Long, data: String) {
        val values = ContentValues(1)
        values.put(Message.COLUMN_DATA, data)

        try {
            database(context).update(Message.TABLE, values, Message.COLUMN_ID + "=?",
                    arrayOf(java.lang.Long.toString(messageId)))
        } catch (e: Exception) {
            ensureActionable(context)
            database(context).update(Message.TABLE, values, Message.COLUMN_ID + "=?",
                    arrayOf(java.lang.Long.toString(messageId)))
        }

        // NOTE: no changes to the server here. whenever we call this, it is only with messages
        //       that are multimedia, so this changes the uri which does no good on the server
        //       anyways.
    }

    /**
     * Inserts a new sent message after finding the conversation id.
     *
     * @param addresses the comma, space separated addresses.
     * @param data      the message data.
     * @param mimeType  the message mimeType.
     * @param context   the application context.
     */
    fun insertSentMessage(addresses: String, data: String, mimeType: String, context: Context): Long {
        val m = Message()
        m.type = Message.TYPE_SENDING
        m.data = data
        m.timestamp = System.currentTimeMillis()
        m.mimeType = mimeType
        m.read = true
        m.seen = true
        m.from = null
        m.color = null

        return insertMessage(m, addresses, context)
    }

    /**
     * Inserts a new message into the database without previously having a conversation id. This
     * will be slightly slower than if you were to have an id since we will need to find the
     * appropriate one in the database or create a new conversation entry.
     *
     * @param message      the message to insert.
     * @param phoneNumbers the phone numbers to look up by conversation.id_matcher column.
     * @return the conversation id that the message was inserted into.
     */
    fun insertMessage(message: Message, phoneNumbers: String, context: Context): Long {
        return insertMessage(context, message, updateOrCreateConversation(phoneNumbers, message, context))
    }

    /**
     * Checks whether or not a conversation exists for this string of phone numbers. If so, the
     * conversation id will be returned. If not, null will be returned.
     */
    fun findConversationId(context: Context, phoneNumbers: String): Long? {
        val matcher = SmsMmsUtils.createIdMatcher(phoneNumbers)
        val cursor = try {
            database(context).query(Conversation.TABLE,
                    arrayOf(Conversation.COLUMN_ID, Conversation.COLUMN_ID_MATCHER),
                    Conversation.COLUMN_ID_MATCHER + "=? OR " + Conversation.COLUMN_ID_MATCHER + "=? OR " + Conversation.COLUMN_ID_MATCHER + "=?",
                    arrayOf(matcher.fiveLetter, matcher.sevenLetter, matcher.tenLetter), null, null, null)
        } catch (e: Exception) {
            ensureActionable(context)
            database(context).query(Conversation.TABLE,
                    arrayOf(Conversation.COLUMN_ID, Conversation.COLUMN_ID_MATCHER),
                    Conversation.COLUMN_ID_MATCHER + "=? OR " + Conversation.COLUMN_ID_MATCHER + "=? OR " + Conversation.COLUMN_ID_MATCHER + "=?",
                    arrayOf(matcher.fiveLetter, matcher.sevenLetter, matcher.tenLetter), null, null, null)
        }

        return if (cursor.moveToFirst()) {
            val conversationId = cursor.getLong(0)
            cursor.closeSilent()
            conversationId
        } else {
            cursor.closeSilent()
            null
        }
    }

    /**
     * Checks whether or not a conversation exists for this title. If so, the
     * conversation id will be returned. If not, null will be returned.
     */
    fun findConversationIdByTitle(context: Context, title: String): Long? {
        val cursor = try {
            database(context).query(Conversation.TABLE,
                    arrayOf(Conversation.COLUMN_ID, Conversation.COLUMN_TITLE),
                    Conversation.COLUMN_TITLE + "=?", arrayOf(title), null, null, null)
        } catch (e: Exception) {
            ensureActionable(context)
            database(context).query(Conversation.TABLE,
                    arrayOf(Conversation.COLUMN_ID, Conversation.COLUMN_TITLE),
                    Conversation.COLUMN_TITLE + "=?", arrayOf(title), null, null, null)
        }

        return if (cursor.moveToFirst()) {
            val conversationId = cursor.getLong(0)
            cursor.closeSilent()
            conversationId
        } else {
            cursor.closeSilent()
            null
        }
    }

    /**
     * Gets a current conversation id if one exists for the phone number, or inserts a new
     * conversation and returns that id if one does not exist.
     *
     * @param phoneNumbers the phone number to match the conversation with.
     * @param message      the message to use to initialize a conversation if needed.
     * @return the conversation id to use.
     */
    private fun updateOrCreateConversation(phoneNumbers: String, message: Message, context: Context): Long {
        val matcher = SmsMmsUtils.createIdMatcher(phoneNumbers)
        val cursor = try {
            database(context).query(Conversation.TABLE,
                    arrayOf(Conversation.COLUMN_ID, Conversation.COLUMN_ID_MATCHER),
                    Conversation.COLUMN_ID_MATCHER + "=? OR " + Conversation.COLUMN_ID_MATCHER + "=? OR " + Conversation.COLUMN_ID_MATCHER + "=?",
                    arrayOf(matcher.fiveLetter, matcher.sevenLetter, matcher.tenLetter), null, null, null)
        } catch (e: Exception) {
            ensureActionable(context)
            database(context).query(Conversation.TABLE,
                    arrayOf(Conversation.COLUMN_ID, Conversation.COLUMN_ID_MATCHER),
                    Conversation.COLUMN_ID_MATCHER + "=? OR " + Conversation.COLUMN_ID_MATCHER + "=? OR " + Conversation.COLUMN_ID_MATCHER + "=?",
                    arrayOf(matcher.fiveLetter, matcher.sevenLetter, matcher.tenLetter), null, null, null)
        }

        val conversationId: Long

        if (cursor.moveToFirst()) {
            conversationId = cursor.getLong(0)
            updateConversation(context, conversationId, message.read, message.timestamp,
                    if (message.type == Message.TYPE_SENT || message.type == Message.TYPE_SENDING)
                        context.getString(R.string.you) + ": " + message.data
                    else
                        message.data,
                    message.mimeType, false)
            cursor.closeSilent()
        } else {
            cursor.closeSilent()

            val conversation = Conversation()
            conversation.pinned = false
            conversation.read = message.read
            conversation.timestamp = message.timestamp

            if (message.mimeType == MimeType.TEXT_PLAIN && message.type != Message.TYPE_INFO) {
                conversation.snippet = if (message.type == Message.TYPE_SENT || message.type == Message.TYPE_SENDING)
                    context.getString(R.string.you) + ": " + message.data
                else
                    message.data
            } else {
                conversation.snippet = ""
            }

            conversation.ringtoneUri = null
            conversation.phoneNumbers = phoneNumbers
            conversation.title = ContactUtils.findContactNames(phoneNumbers, context)
            conversation.imageUri = ContactUtils.findImageUri(phoneNumbers, context)
            conversation.idMatcher = matcher.tenLetter
            conversation.mute = false
            conversation.archive = false
            conversation.ledColor = Color.WHITE
            conversation.simSubscriptionId = -1

            ImageUtils.fillConversationColors(conversation, context)

            val contacts = getContacts(context, conversation.title)
            if (contacts.size == 1) {
                // just one user in this conversation, so lets set the conversation color to that user's color
                conversation.colors = contacts[0].colors
            }

            conversationId = insertConversation(context, conversation)
        }

        return conversationId
    }

    /**
     * Gets a current conversation id if one exists for the phone number, or inserts a new
     * conversation and returns that id if one does not exist.
     *
     * @param conversation the conversation with the parameters we are looking for
     * @return the conversation id to use.
     */
    private fun updateOrCreateConversation(context: Context, conversation: Conversation): Long {
        val cursor = try {
            database(context).query(Conversation.TABLE,
                    arrayOf(Conversation.COLUMN_ID, Conversation.COLUMN_ID_MATCHER),
                    Conversation.COLUMN_ID_MATCHER + "=?", arrayOf(conversation.idMatcher), null, null, null)
        } catch (e: Exception) {
            ensureActionable(context)
            database(context).query(Conversation.TABLE,
                    arrayOf(Conversation.COLUMN_ID, Conversation.COLUMN_ID_MATCHER),
                    Conversation.COLUMN_ID_MATCHER + "=?", arrayOf(conversation.idMatcher), null, null, null)
        }

        val conversationId: Long

        if (cursor.moveToFirst()) {
            conversationId = cursor.getLong(0)
            updateConversation(context, conversationId, conversation.read, conversation.timestamp,
                    conversation.snippet, MimeType.TEXT_PLAIN, false)
        } else {
            conversationId = insertConversation(context, conversation)
        }

        cursor.closeSilent()
        return conversationId
    }

    /**
     * Inserts a new message into the database. This also updates the conversation with the latest
     * data.
     *
     * @param message        the message to insert.
     * @param conversationId the conversation to insert the message into.
     * @return the conversation id that the message was inserted into.
     */
    @JvmOverloads
    fun insertMessage(context: Context, message: Message, conversationId: Long, returnMessageId: Boolean = false): Long {
        message.conversationId = conversationId

        val values = ContentValues(11)

        if (message.id <= 0) {
            message.id = generateId()
        }

        values.put(Message.COLUMN_ID, message.id)
        values.put(Message.COLUMN_CONVERSATION_ID, conversationId)
        values.put(Message.COLUMN_TYPE, message.type)
        values.put(Message.COLUMN_DATA, message.data)
        values.put(Message.COLUMN_TIMESTAMP, message.timestamp)
        values.put(Message.COLUMN_MIME_TYPE, message.mimeType)
        values.put(Message.COLUMN_READ, message.read)
        values.put(Message.COLUMN_SEEN, message.seen)
        values.put(Message.COLUMN_FROM, message.from)
        values.put(Message.COLUMN_COLOR, message.color)
        values.put(Message.COLUMN_SIM_NUMBER, message.simPhoneNumber)

        val id = try {
            database(context).insert(Message.TABLE, null, values)
        } catch (e: Exception) {
            ensureActionable(context)

            try {
                database(context).insert(Message.TABLE, null, values)
            } catch (x: Exception) {
                try {
                    Thread.sleep(2000)
                } catch (y: InterruptedException) {
                }

                ensureActionable(context)
                database(context).insert(Message.TABLE, null, values)
            }

        }

        apiUtils.addMessage(context, accountId(context), message.id, conversationId, message.type, message.data,
                message.timestamp, message.mimeType, message.read, message.seen, message.from,
                message.color, encryptor(context))

        if (message.type != Message.TYPE_MEDIA) {
            updateConversation(context, conversationId, message.read, message.timestamp,
                    if (message.type == Message.TYPE_SENT || message.type == Message.TYPE_SENDING)
                        context.getString(R.string.you) + ": " + message.data
                    else
                        message.data,
                    message.mimeType, false)
        }

        return if (returnMessageId) id else conversationId
    }

    /**
     * Inserts a new message list into the database. This also updates the conversation with the latest
     * data.
     *
     * @param messages        list of messages to batch insert
     */
    fun insertMessages(context: Context, messages: List<Message>) {
        beginTransaction(context)

        for (i in messages.indices) {
            val message = messages[i]

            val values = ContentValues(11)

            if (message.id <= 0) {
                message.id = generateId()
            }

            values.put(Message.COLUMN_ID, message.id)
            values.put(Message.COLUMN_CONVERSATION_ID, message.conversationId)
            values.put(Message.COLUMN_TYPE, message.type)
            values.put(Message.COLUMN_DATA, message.data)
            values.put(Message.COLUMN_TIMESTAMP, message.timestamp)
            values.put(Message.COLUMN_MIME_TYPE, message.mimeType)
            values.put(Message.COLUMN_READ, message.read)
            values.put(Message.COLUMN_SEEN, message.seen)
            values.put(Message.COLUMN_FROM, message.from)
            values.put(Message.COLUMN_COLOR, message.color)
            values.put(Message.COLUMN_SIM_NUMBER, message.simPhoneNumber)

            val id = try {
                database(context).insert(Message.TABLE, null, values)
            } catch (e: Exception) {
                ensureActionable(context)
                database(context).insert(Message.TABLE, null, values)
            }

            //            apiUtils.addMessage(context, accountId(context), message.id, message.conversationId, message.type, message.data,
            //                    message.timestamp, message.mimeType, message.read, message.seen, message.from,
            //                    message.color, getEncryptionUtils(context));

            updateConversation(context, message.conversationId, message.read, message.timestamp,
                    if (message.type == Message.TYPE_SENT || message.type == Message.TYPE_SENDING)
                        context.getString(R.string.you) + ": " + message.data
                    else
                        message.data,
                    message.mimeType, false)
        }

        setTransactionSuccessful(context)
        endTransaction(context)
    }

    /**
     * Deletes a message with the given id.
     */
    fun deleteMessage(context: Context, messageId: Long): Int {
        val deleted = try {
            database(context).delete(Message.TABLE, Message.COLUMN_ID + "=?",
                    arrayOf(java.lang.Long.toString(messageId)))
        } catch (e: Exception) {
            ensureActionable(context)
            database(context).delete(Message.TABLE, Message.COLUMN_ID + "=?",
                    arrayOf(java.lang.Long.toString(messageId)))
        }

        apiUtils.deleteMessage(accountId(context), messageId)
        return deleted
    }

    /**
     * Deletes messages and conversations older than the given timestamp
     */
    fun cleanupOldMessages(context: Context, timestamp: Long): Int {
        val deleted = try {
            database(context).delete(Message.TABLE, Message.COLUMN_TIMESTAMP + "<?",
                    arrayOf(java.lang.Long.toString(timestamp)))
        } catch (e: Exception) {
            ensureActionable(context)
            database(context).delete(Message.TABLE, Message.COLUMN_TIMESTAMP + "<?",
                    arrayOf(java.lang.Long.toString(timestamp)))
        }

        database(context).delete(Conversation.TABLE, Conversation.COLUMN_TIMESTAMP + "<?",
                arrayOf(java.lang.Long.toString(timestamp)))

        if (deleted > 0) {
            apiUtils.cleanupMessages(accountId(context), timestamp)
        }

        return deleted
    }

    /**
     * Marks a conversation and all messages inside of it as read and seen.
     *
     * @param conversationId the conversation id to mark.
     */
    fun readConversation(context: Context, conversationId: Long) {
        var values = ContentValues(2)
        values.put(Message.COLUMN_READ, true)
        values.put(Message.COLUMN_SEEN, true)

        var updated = try {
            database(context).update(Message.TABLE, values, Message.COLUMN_CONVERSATION_ID + "=?",
                    arrayOf(java.lang.Long.toString(conversationId)))
        } catch (e: Exception) {
            ensureActionable(context)
            database(context).update(Message.TABLE, values, Message.COLUMN_CONVERSATION_ID + "=?",
                    arrayOf(java.lang.Long.toString(conversationId)))
        }

        values = ContentValues(1)
        values.put(Conversation.COLUMN_READ, true)

        updated += try {
            database(context).update(Conversation.TABLE, values, Conversation.COLUMN_ID + "=?",
                    arrayOf(java.lang.Long.toString(conversationId)))
        } catch (e: Exception) {
            ensureActionable(context)
            database(context).update(Conversation.TABLE, values, Conversation.COLUMN_ID + "=?",
                    arrayOf(java.lang.Long.toString(conversationId)))
        }

        if (updated > 0) {
            apiUtils.readConversation(accountId(context), androidDeviceId(context), conversationId)
        }

        writeUnreadCount(context)

        try {
            SmsMmsUtils.markConversationRead(context, getConversation(context, conversationId)!!.phoneNumbers)
        } catch (e: NullPointerException) {
            // thrown in robolectric tests
        }

    }

    /**
     * Marks a conversation and all messages inside of it as read and seen.
     *
     * @param conversations the conversation ids to mark.
     */
    fun readConversations(context: Context, conversations: List<Conversation>) {
        val conversationIds = conversations.mapTo(ArrayList()) { it.id }

        var values = ContentValues(2)
        values.put(Message.COLUMN_READ, 1)
        values.put(Message.COLUMN_SEEN, 1)

        var updated = try {
            database(context).update(Message.TABLE, values, StringUtils.buildSqlOrStatement(Message.COLUMN_CONVERSATION_ID, conversationIds),
                    arrayOf())
        } catch (e: Exception) {
            ensureActionable(context)
            database(context).update(Message.TABLE, values, StringUtils.buildSqlOrStatement(Message.COLUMN_CONVERSATION_ID, conversationIds),
                    arrayOf())
        }

        values = ContentValues(1)
        values.put(Conversation.COLUMN_READ, 1)

        updated += try {
            database(context).update(Conversation.TABLE, values, StringUtils.buildSqlOrStatement(Conversation.COLUMN_ID, conversationIds),
                    arrayOf())
        } catch (e: Exception) {
            ensureActionable(context)
            database(context).update(Conversation.TABLE, values, StringUtils.buildSqlOrStatement(Conversation.COLUMN_ID, conversationIds),
                    arrayOf())
        }

        Log.v("Data Source", "updated: " + updated)
        if (updated > 0) {
            for (id in conversationIds) {
                apiUtils.readConversation(accountId(context), androidDeviceId(context), id)
            }

            writeUnreadCount(context)
        }

        try {
            for (conversation in conversations) {
                SmsMmsUtils.markConversationRead(context, conversation.phoneNumbers)
            }
        } catch (e: NullPointerException) {
            // thrown in robolectric tests
        }

    }

    /**
     * Marks all messages in a conversation as seen.
     */
    fun seenConversation(context: Context, conversationId: Long) {
        val values = ContentValues(1)
        values.put(Message.COLUMN_SEEN, 1)

        try {
            database(context).update(Message.TABLE, values, Message.COLUMN_CONVERSATION_ID + "=? AND " +
                    Message.COLUMN_SEEN + "=0", arrayOf(java.lang.Long.toString(conversationId)))
        } catch (e: Exception) {
            ensureActionable(context)
            database(context).update(Message.TABLE, values, Message.COLUMN_CONVERSATION_ID + "=? AND " +
                    Message.COLUMN_SEEN + "=0", arrayOf(java.lang.Long.toString(conversationId)))
        }

        apiUtils.seenConversation(accountId(context), conversationId)
    }

    /**
     * Mark all messages as seen.
     */
    fun seenConversations(context: Context) {
        val values = ContentValues(1)
        values.put(Message.COLUMN_SEEN, 1)

        try {
            database(context).update(Message.TABLE, values, Message.COLUMN_SEEN + "=0", null)
        } catch (e: Exception) {
            ensureActionable(context)
        }

        apiUtils.seenConversations(accountId(context))
    }

    /**
     * Mark all messages as seen.
     */
    fun seenAllMessages(context: Context) {
        val values = ContentValues(1)
        values.put(Message.COLUMN_SEEN, 1)

        try {
            database(context).update(Message.TABLE, values, Message.COLUMN_SEEN + "=0", null)
        } catch (e: Exception) {
            ensureActionable(context)
            database(context).update(Message.TABLE, values, Message.COLUMN_SEEN + "=0", null)
        }

        apiUtils.seenConversations(accountId(context))
    }

    /**
     * Gets all messages in the database not marked as read.
     *
     * @return a cursor of all unread messages.
     */
    fun getUnreadMessages(context: Context): Cursor =
            try {
                database(context).query(Message.TABLE, null, Message.COLUMN_READ + "=0", null, null, null,
                        Message.COLUMN_TIMESTAMP + " desc")
            } catch (e: Exception) {
                ensureActionable(context)
                database(context).query(Message.TABLE, null, Message.COLUMN_READ + "=0", null, null, null,
                        Message.COLUMN_TIMESTAMP + " desc")
            }

    /**
     * Gets all message in the database not marked as seen.
     *
     * @return a cursor of all unseen messages.
     */
    fun getUnseenMessages(context: Context): Cursor =
            try {
                database(context).query(Message.TABLE, null, Message.COLUMN_SEEN + "=0", null, null, null,
                        Message.COLUMN_TIMESTAMP + " asc")
            } catch (e: Exception) {
                ensureActionable(context)
                database(context).query(Message.TABLE, null, Message.COLUMN_SEEN + "=0", null, null, null,
                        Message.COLUMN_TIMESTAMP + " asc")
            }

    /**
     * Inserts a draft into the database with the given parameters.
     */
    fun insertDraft(context: Context, conversationId: Long, data: String, mimeType: String): Long {
        val values = ContentValues(4)
        val id = generateId()
        values.put(Draft.COLUMN_ID, id)
        values.put(Draft.COLUMN_CONVERSATION_ID, conversationId)
        values.put(Draft.COLUMN_DATA, data)
        values.put(Draft.COLUMN_MIME_TYPE, mimeType)

        apiUtils.addDraft(accountId(context), id, conversationId, data, mimeType, encryptor(context))

        return try {
            database(context).insert(Draft.TABLE, null, values)
        } catch (e: Exception) {
            ensureActionable(context)
            database(context).insert(Draft.TABLE, null, values)
        }
    }

    /**
     * Inserts a draft into the database.
     */
    fun insertDraft(context: Context, draft: Draft): Long {
        val values = ContentValues(4)

        if (draft.id > 0) {
            values.put(Draft.COLUMN_ID, draft.id)
        } else {
            values.put(Draft.COLUMN_ID, generateId())
        }

        values.put(Draft.COLUMN_CONVERSATION_ID, draft.conversationId)
        values.put(Draft.COLUMN_DATA, draft.data)
        values.put(Draft.COLUMN_MIME_TYPE, draft.mimeType)

        return try {
            database(context).insert(Draft.TABLE, null, values)
        } catch (e: SQLiteConstraintException) {
            e.printStackTrace()
            -1
        }

        // NOTE: no api interaction here because this is only called when we insert a draft
        //       in the api download service.
    }

    /**
     * Gets all drafts in the database.
     */
    fun getDrafts(context: Context): Cursor =
            try {
                database(context).query(Draft.TABLE, null, null, null, null, null, null)
            } catch (e: Exception) {
                ensureActionable(context)
                database(context).query(Draft.TABLE, null, null, null, null, null, null)
            }

    /**
     * Gets all draft messages for a given conversation id. There may be multiple for each
     * conversation because there is the potential for different mime types. For example, a
     * conversation could have a text draft and an image draft, both of which should be displayed
     * when the conversation is loaded.
     */
    fun getDrafts(context: Context, conversationId: Long): List<Draft> {
        val cursor = try {
            database(context).query(Draft.TABLE, null, Draft.COLUMN_CONVERSATION_ID + "=?",
                    arrayOf(java.lang.Long.toString(conversationId)), null, null, null)
        } catch (e: Exception) {
            ensureActionable(context)
            database(context).query(Draft.TABLE, null, Draft.COLUMN_CONVERSATION_ID + "=?",
                    arrayOf(java.lang.Long.toString(conversationId)), null, null, null)
        }

        val drafts = ArrayList<Draft>()

        if (cursor.moveToFirst()) {
            do {
                val draft = Draft()
                draft.fillFromCursor(cursor)
                drafts.add(draft)
            } while (cursor.moveToNext())
        }

        cursor.closeSilent()
        return drafts
    }

    /**
     * Deletes all drafts for a given conversation. This should be used after a message has been
     * sent to the conversation.
     */
    fun deleteDrafts(context: Context, conversationId: Long) {
        try {
            database(context).delete(Draft.TABLE, Draft.COLUMN_CONVERSATION_ID + "=?",
                    arrayOf(java.lang.Long.toString(conversationId)))
        } catch (e: Exception) {
            ensureActionable(context)
            database(context).delete(Draft.TABLE, Draft.COLUMN_CONVERSATION_ID + "=?",
                    arrayOf(java.lang.Long.toString(conversationId)))
        }

        apiUtils.deleteDrafts(accountId(context), androidDeviceId(context), conversationId)
    }

    /**
     * Gets all blacklists in the database.
     */
    fun getBlacklists(context: Context): Cursor =
            try {
                database(context).query(Blacklist.TABLE, null, null, null, null, null, null)
            } catch (e: Exception) {
                ensureActionable(context)
                database(context).query(Blacklist.TABLE, null, null, null, null, null, null)
            }

    fun getBlacklistsAsList(context: Context): List<Blacklist> {
        val cursor = getBlacklists(context)
        val blacklists = ArrayList<Blacklist>()

        if (cursor.moveToFirst()) {
            do {
                val blacklist = Blacklist()
                blacklist.fillFromCursor(cursor)

                blacklists.add(blacklist)
            } while (cursor.moveToNext())
        }

        cursor.closeSilent()
        return blacklists
    }

    /**
     * Inserts a blacklist into the database.
     */
    fun insertBlacklist(context: Context, blacklist: Blacklist) {
        val values = ContentValues(2)

        if (blacklist.id <= 0) {
            blacklist.id = generateId()
        }

        values.put(Blacklist.COLUMN_ID, blacklist.id)
        values.put(Blacklist.COLUMN_PHONE_NUMBER, blacklist.phoneNumber)

        try {
            database(context).insert(Blacklist.TABLE, null, values)
        } catch (e: Exception) {
            ensureActionable(context)
            database(context).insert(Blacklist.TABLE, null, values)
        }

        apiUtils.addBlacklist(accountId(context), blacklist.id, blacklist.phoneNumber, encryptor(context))
    }

    /**
     * Deletes a blacklist from the database.
     */
    fun deleteBlacklist(context: Context, id: Long) {
        try {
            database(context).delete(Blacklist.TABLE, Blacklist.COLUMN_ID + "=?",
                    arrayOf(java.lang.Long.toString(id)))
        } catch (e: Exception) {
            ensureActionable(context)
            database(context).delete(Blacklist.TABLE, Blacklist.COLUMN_ID + "=?",
                    arrayOf(java.lang.Long.toString(id)))
        }

        apiUtils.deleteBlacklist(accountId(context), id)
    }

    /**
     * Gets all scheduled messages in the database.
     */
    fun getScheduledMessages(context: Context): Cursor =
        try {
            database(context).query(ScheduledMessage.TABLE, null, null, null, null, null,
                    ScheduledMessage.COLUMN_TIMESTAMP + " asc")
        } catch (e: Exception) {
            ensureActionable(context)
            database(context).query(ScheduledMessage.TABLE, null, null, null, null, null,
                    ScheduledMessage.COLUMN_TIMESTAMP + " asc")
        }

    /**
     * Get all scheduled messages as a list
     */
    fun getScheduledMessagesAsList(context: Context): List<ScheduledMessage> {
        val cursor = getScheduledMessages(context)
        val scheduledMessages = ArrayList<ScheduledMessage>()

        if (cursor.moveToFirst()) {
            do {
                val message = ScheduledMessage()
                message.fillFromCursor(cursor)

                scheduledMessages.add(message)
            } while (cursor.moveToNext())
        }

        cursor.closeSilent()
        return scheduledMessages
    }

    /**
     * Inserts a scheduled message into the database.
     */
    fun insertScheduledMessage(context: Context, message: ScheduledMessage): Long {
        val values = ContentValues(6)

        if (message.id <= 0) {
            message.id = generateId()
        }

        values.put(ScheduledMessage.COLUMN_ID, message.id)
        values.put(ScheduledMessage.COLUMN_TITLE, message.title)
        values.put(ScheduledMessage.COLUMN_TO, message.to)
        values.put(ScheduledMessage.COLUMN_DATA, message.data)
        values.put(ScheduledMessage.COLUMN_MIME_TYPE, message.mimeType)
        values.put(ScheduledMessage.COLUMN_TIMESTAMP, message.timestamp)

        try {
            apiUtils.addScheduledMessage(accountId(context), message.id, message.title, message.to, message.data,
                    message.mimeType, message.timestamp, encryptor(context))
        } catch (e: Exception) {
            ensureActionable(context)
            apiUtils.addScheduledMessage(accountId(context), message.id, message.title, message.to, message.data,
                    message.mimeType, message.timestamp, encryptor(context))
        }

        return database(context).insert(ScheduledMessage.TABLE, null, values)
    }

    /**
     * Updates the values on the scheduled message
     *
     * @param message the message to upate
     */
    fun updateScheduledMessage(context: Context, message: ScheduledMessage) {
        val values = ContentValues(6)

        values.put(ScheduledMessage.COLUMN_ID, message.id)
        values.put(ScheduledMessage.COLUMN_TITLE, message.title)
        values.put(ScheduledMessage.COLUMN_TO, message.to)
        values.put(ScheduledMessage.COLUMN_DATA, message.data)
        values.put(ScheduledMessage.COLUMN_MIME_TYPE, message.mimeType)
        values.put(ScheduledMessage.COLUMN_TIMESTAMP, message.timestamp)

        try {
            database(context).update(ScheduledMessage.TABLE, values, ScheduledMessage.COLUMN_ID + "=?",
                    arrayOf(java.lang.Long.toString(message.id)))
        } catch (e: Exception) {
            ensureActionable(context)
            database(context).update(ScheduledMessage.TABLE, values, ScheduledMessage.COLUMN_ID + "=?",
                    arrayOf(java.lang.Long.toString(message.id)))
        }

        apiUtils.updateScheduledMessage(accountId(context), message.id, message.title, message.to, message.data,
                message.mimeType, message.timestamp, encryptor(context))
    }

    /**
     * Deletes a scheduled message from the database.
     */
    fun deleteScheduledMessage(context: Context, id: Long) {
        try {
            database(context).delete(ScheduledMessage.TABLE, ScheduledMessage.COLUMN_ID + "=?",
                    arrayOf(java.lang.Long.toString(id)))
        } catch (e: Exception) {
            ensureActionable(context)
            database(context).delete(ScheduledMessage.TABLE, ScheduledMessage.COLUMN_ID + "=?",
                    arrayOf(java.lang.Long.toString(id)))
        }

        apiUtils.deleteScheduledMessage(accountId(context), id)
    }

    /**
     * Sets whether or not to upload data changes to the server. If there is no account id, then
     * this value will always be false.
     */
    fun setUpload(upload: Boolean) {
        this.apiUtils.isActive = upload
    }

    /**
     * Available to close the database after tests have finished running. Don't call
     * in the production application outside of test code.
     */
    @VisibleForTesting
    fun forceCloseImmediate() {
        _dbHelper?.close()
    }


    /**
     * Generates a random id for the row.
     */
    fun generateId(): Long {
        val leftLimit = 1L
        val rightLimit = MAX_ID
        return leftLimit + (Math.random() * (rightLimit - leftLimit)).toLong()
    }

}