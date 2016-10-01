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

package xyz.klinker.messenger.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.support.annotation.VisibleForTesting;
import android.text.Html;
import android.text.Spanned;
import android.text.format.Formatter;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import xyz.klinker.messenger.api.implementation.Account;
import xyz.klinker.messenger.api.implementation.ApiUtils;
import xyz.klinker.messenger.api.implementation.BinaryUtils;
import xyz.klinker.messenger.data.model.Blacklist;
import xyz.klinker.messenger.data.model.Contact;
import xyz.klinker.messenger.data.model.Conversation;
import xyz.klinker.messenger.data.model.Draft;
import xyz.klinker.messenger.data.model.Message;
import xyz.klinker.messenger.data.model.ScheduledMessage;
import xyz.klinker.messenger.encryption.EncryptionUtils;
import xyz.klinker.messenger.util.ContactUtils;
import xyz.klinker.messenger.util.ImageUtils;
import xyz.klinker.messenger.util.SmsMmsUtils;
import xyz.klinker.messenger.util.listener.ProgressUpdateListener;

/**
 * Handles interactions with database models.
 */
public class DataSource {

    private static final String TAG = "DataSource";

    /**
     * A max value for the id. With this value, there is a 1 in 200,000 chance of overlap when a
     * user uploads 100,000 messages, so we should be safe assuming that no user will be uploading
     * that many messages.
     * <p>
     * See https://github.com/klinker41/messenger-server/wiki/Generating-GUIDs.
     */
    private static final long MAX_ID = Long.MAX_VALUE / 10000;
    private static volatile DataSource instance;

    protected Context context;
    private SQLiteDatabase database;
    private DatabaseSQLiteHelper dbHelper;
    private AtomicInteger openCounter = new AtomicInteger();
    private String accountId = null;
    private ApiUtils apiUtils;

    /**
     * Gets a new instance of the DataSource.
     *
     * @param context the current application instance.
     * @return the data source.
     */
    public static DataSource getInstance(Context context) {
        if (instance == null) {
            instance = new DataSource(context);
        }

        instance.accountId = Account.get(context).accountId;
        return instance;
    }

    /**
     * Private constructor to force a singleton.
     *
     * @param context Current calling context
     */
    private DataSource(Context context) {
        this.context = context;
        this.dbHelper = new DatabaseSQLiteHelper(context);
        this.apiUtils = new ApiUtils();
    }

    public EncryptionUtils getEncryptionUtils(final Context context) {
        return Account.get(context).getEncryptor();
    }

    /**
     * Contructor to help with testing.
     *
     * @param helper Mock of the database helper
     */
    @VisibleForTesting
    protected DataSource(DatabaseSQLiteHelper helper) {
        this.dbHelper = helper;
        this.apiUtils = new ApiUtils();
    }

    /**
     * Constructor to help with testing.
     *
     * @param database Mock of the sqlite database
     */
    @VisibleForTesting
    public DataSource(SQLiteDatabase database) {
        this.database = database;
        this.apiUtils = new ApiUtils();
    }

    /**
     * Opens the database.
     */
    public synchronized void open() {
        if (openCounter.incrementAndGet() == 1) {
            database = dbHelper.getWritableDatabase();
        }
    }

    /**
     * Checks if the database is open.
     */
    public boolean isOpen() {
        return database != null && database.isOpen();
    }

    /**
     * Closes the database.
     */
    public synchronized void close() {
        if (openCounter.decrementAndGet() == 0) {
            dbHelper.close();
        }
    }

    /**
     * Available to close the database after tests have finished running. Don't call
     * in the production application outside of test code.
     */
    @VisibleForTesting
    public synchronized static void forceCloseImmediate() {
        if (instance != null && instance.openCounter.get() > 0) {
            instance.openCounter.set(0);
            instance.dbHelper.close();
            instance = null;
        }
    }

    /**
     * Get the currently open database
     *
     * @return sqlite database
     */
    @VisibleForTesting
    public SQLiteDatabase getDatabase() {
        return database;
    }

    /**
     * Deletes all data from the tables.
     */
    public void clearTables() {
        database.delete(Message.TABLE, null, null);
        database.delete(Conversation.TABLE, null, null);
        database.delete(Blacklist.TABLE, null, null);
        database.delete(Draft.TABLE, null, null);
        database.delete(ScheduledMessage.TABLE, null, null);
    }

    /**
     * Begins a bulk transaction on the database.
     */
    public void beginTransaction() {
        database.beginTransaction();
    }

    /**
     * Executes a raw sql statement on the database. Can be used in conjunction with
     * beginTransaction and endTransaction if bulk.
     *
     * @param sql the sql statement.
     */
    public void execSql(String sql) {
        database.execSQL(sql);
    }

    /**
     * Execute a raw sql query on the database.
     *
     * @param sql the sql statement
     * @return cursor for the data
     */
    public Cursor rawQuery(String sql) {
        return database.rawQuery(sql, null);
    }

    /**
     * Sets the transaction into a successful state so that it can be committed to the database.
     * Should be used in conjunction with beginTransaction() and endTransaction().
     */
    public void setTransactionSuccessful() {
        database.setTransactionSuccessful();
    }

    /**
     * Ends a bulk transaction on the database.
     */
    public void endTransaction() {
        database.endTransaction();
    }

    /**
     * Bulk insert of contacts into the databse.
     *
     * @param contacts a list of all the contacts to insert
     * @param listener callback for the progress of the insert
     */
    public void insertContacts(List<Contact> contacts, ProgressUpdateListener listener) {
        beginTransaction();

        for (int i = 0; i < contacts.size(); i++) {
            Contact contact = contacts.get(i);

            ContentValues values = new ContentValues(7);

            // here we are loading the id from the internal database into the conversation object
            // but we don't want to use that so we'll just generate a new one.
            values.put(Contact.COLUMN_ID, generateId());
            values.put(Contact.COLUMN_PHONE_NUMBER, contact.phoneNumber);
            values.put(Contact.COLUMN_NAME, contact.name);
            values.put(Contact.COLUMN_COLOR, contact.colors.color);
            values.put(Contact.COLUMN_COLOR_DARK, contact.colors.colorDark);
            values.put(Contact.COLUMN_COLOR_LIGHT, contact.colors.colorLight);
            values.put(Contact.COLUMN_COLOR_ACCENT, contact.colors.colorAccent);

            database.insert(Contact.TABLE, null, values);

            if (listener != null) {
                listener.onProgressUpdate(i + 1, contacts.size());
            }
        }

        setTransactionSuccessful();
        endTransaction();
    }

    /**
     * Insert a new contact into the apps database.
     *
     * @param contact the new contact
     * @return id of the inserted contact or -1 if the insert failed
     */
    public long insertContact(Contact contact) {
        ContentValues values = new ContentValues(7);

        if (contact.id <= 0) {
            contact.id = generateId();
        }

        values.put(Contact.COLUMN_ID, contact.id);
        values.put(Contact.COLUMN_PHONE_NUMBER, contact.phoneNumber);
        values.put(Contact.COLUMN_NAME, contact.name);
        values.put(Contact.COLUMN_COLOR, contact.colors.color);
        values.put(Contact.COLUMN_COLOR_DARK, contact.colors.colorDark);
        values.put(Contact.COLUMN_COLOR_LIGHT, contact.colors.colorLight);
        values.put(Contact.COLUMN_COLOR_ACCENT, contact.colors.colorAccent);

        apiUtils.addContact(accountId,contact.phoneNumber, contact.name, contact.colors.color,
                contact.colors.colorDark, contact.colors.colorLight,
                contact.colors.colorAccent, getEncryptionUtils(context));

        try {
            return database.insert(Contact.TABLE, null, values);
        } catch (Exception e) {
            Log.e(TAG, "failed to insert contact", e);
            return -1L;
        }
    }

    /**
     * Get all the contacts in the database.
     *
     * @return a cursor of all the contacts stored in the app.
     */
    public Cursor getContacts() {
        return database.query(Contact.TABLE, null, null, null, null, null,
                Contact.COLUMN_NAME + " ASC");
    }

    /**
     * Get a contact from the database.
     *
     * @param phoneNumber unique phone number to find
     * @return Contact from the database
     */
    public Contact getContact(String phoneNumber) {
        Cursor cursor = database.query(Contact.TABLE, null, Contact.COLUMN_PHONE_NUMBER+ "=?",
                new String[]{phoneNumber}, null, null, null);
        if (cursor.moveToFirst()) {
            Contact contact = new Contact();
            contact.fillFromCursor(cursor);
            cursor.close();
            return contact;
        }

        return null;
    }

    /**
     * Get a list of contacts from a list of phone numbers
     *
     * @param numbers a comma separated list of phone numbers (Ex: 5154224558, 5159911493)
     * @return list of any contacts in the database for those phone numbers. Ignores numbers that are
     *          not in the database.
     */
    public List<Contact> getContacts(String numbers) {
        if (numbers == null || numbers.isEmpty()) {
            return new ArrayList<>();
        }

        // Could we miss matching a contact here? Sometimes phone numbers are resolved as
        // 5154224558 and other times it is +15154224558 depending on the carrier.
        String[] array = numbers.split(", ");
        String where = "";
        if (array.length <= 0) {
            return new ArrayList<>();
        } else if (array.length == 1) {
            array[0] = "%" + ContactUtils.getPlainNumber(array[0]) + "%";
            where += Contact.COLUMN_PHONE_NUMBER + " LIKE ?";
        } else {
            array[0] = "%" + ContactUtils.getPlainNumber(array[0]) + "%";
            where = Contact.COLUMN_PHONE_NUMBER + " LIKE ?";
            for (int i = 1; i < array.length; i++) {
                array[i] = "%" + ContactUtils.getPlainNumber(array[i]) + "%";
                where += " OR " + Contact.COLUMN_PHONE_NUMBER + " LIKE ?";
            }
        }

        List<Contact> contacts = new ArrayList<>();
        Cursor cursor = database.query(Contact.TABLE, null, where, array, null, null, null);
        if (cursor.moveToFirst()) {
            do {
                Contact contact = new Contact();
                contact.fillFromCursor(cursor);
                contacts.add(contact);
            } while (cursor.moveToNext());
        }

        return contacts;
    }

    /**
     * Get a list of contacts from a list of phone numbers
     *
     * @param names a comma separated list of phone numbers (Ex: Luke Klinker, Jake Klinker)
     * @return list of any contacts in the database for those phone numbers. Ignores numbers that are
     *          not in the database.
     */
    public List<Contact> getContactsByNames(String names) {
        if (names == null || names.isEmpty()) {
            return new ArrayList<>();
        }

        String[] array = names.split(", ");
        String where = "";
        if (array.length <= 0) {
            return new ArrayList<>();
        } else if (array.length == 1) {
            where += Contact.COLUMN_NAME + "=?";
        } else {
            where = Contact.COLUMN_NAME + "=?";
            for (int i = 1; i < array.length; i++) {
                where += " OR " + Contact.COLUMN_NAME + " LIKE ?";
            }
        }

        List<Contact> contacts = new ArrayList<>();
        Cursor cursor = database.query(Contact.TABLE, null, where, array, null, null, null);
        if (cursor.moveToFirst()) {
            do {
                Contact contact = new Contact();
                contact.fillFromCursor(cursor);
                contacts.add(contact);
            } while (cursor.moveToNext());
        }

        return contacts;
    }

    /**
     * Deletes a contact from the database.
     *
     * @param phoneNumber the phone number to delete
     */
    public void deleteContact(String phoneNumber) {
        database.delete(Contact.TABLE, Contact.COLUMN_PHONE_NUMBER + "=?",
                new String[]{phoneNumber});

        apiUtils.deleteContact(accountId, phoneNumber, getEncryptionUtils(context));
    }

    /**
     * Updates the conversation with given values.
     *
     * @param contact the contact with new values
     */
    public void updateContact(Contact contact) {
        updateContact(contact.phoneNumber, contact.name, contact.colors.color, contact.colors.colorDark,
                contact.colors.colorLight, contact.colors.colorAccent);
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
    public void updateContact(String phoneNumber, String name, Integer color, Integer colorDark,
                                   Integer colorLight, Integer colorAccent) {
        ContentValues values = new ContentValues();

        if (name != null) values.put(Contact.COLUMN_NAME, name);
        if (color != null) values.put(Contact.COLUMN_COLOR, color);
        if (colorDark != null) values.put(Contact.COLUMN_COLOR_DARK, colorDark);
        if (colorLight != null) values.put(Contact.COLUMN_COLOR_LIGHT, colorLight);
        if (colorAccent != null) values.put(Contact.COLUMN_COLOR_ACCENT, colorAccent);

        int updated = database.update(Contact.TABLE, values, Contact.COLUMN_PHONE_NUMBER + "=?",
                new String[]{phoneNumber});

        if (updated > 0) {
            apiUtils.updateContact(accountId, phoneNumber, name, color, colorDark,
                    colorLight, colorAccent, getEncryptionUtils(context));
        }
    }

    /**
     * Gets the number of contacts in the database.
     */
    public int getContactsCount() {
        Cursor cursor = getContacts();
        int count = cursor.getCount();
        cursor.close();
        return count;
    }

    /**
     * Writes the initial list of conversations to the database. These are the conversations that
     * will come from your phones internal SMS database. It will then find all messages in each
     * of these conversations and insert them as well, during the same transaction.
     *
     * @param conversations the list of conversations. See SmsMmsUtils.queryConversations().
     * @param context       the application context.
     */
    public void insertConversations(List<Conversation> conversations, Context context,
                                    ProgressUpdateListener listener) {
        beginTransaction();

        for (int i = 0; i < conversations.size(); i++) {
            Conversation conversation = conversations.get(i);

            ContentValues values = new ContentValues(16);

            // here we are loading the id from the internal database into the conversation object
            // but we don't want to use that so we'll just generate a new one.
            values.put(Conversation.COLUMN_ID, generateId());
            values.put(Conversation.COLUMN_COLOR, conversation.colors.color);
            values.put(Conversation.COLUMN_COLOR_DARK, conversation.colors.colorDark);
            values.put(Conversation.COLUMN_COLOR_LIGHT, conversation.colors.colorLight);
            values.put(Conversation.COLUMN_COLOR_ACCENT, conversation.colors.colorAccent);
            values.put(Conversation.COLUMN_PINNED, conversation.pinned);
            values.put(Conversation.COLUMN_READ, conversation.read);
            values.put(Conversation.COLUMN_TIMESTAMP, conversation.timestamp);
            values.put(Conversation.COLUMN_TITLE, conversation.title);
            values.put(Conversation.COLUMN_PHONE_NUMBERS, conversation.phoneNumbers);
            values.put(Conversation.COLUMN_SNIPPET, conversation.snippet);
            values.put(Conversation.COLUMN_RINGTONE, conversation.ringtoneUri);
            values.put(Conversation.COLUMN_IMAGE_URI, conversation.imageUri);
            values.put(Conversation.COLUMN_ID_MATCHER, conversation.idMatcher);
            values.put(Conversation.COLUMN_MUTE, conversation.mute);
            values.put(Conversation.COLUMN_ARCHIVED, conversation.archive);

            long conversationId = database.insert(Conversation.TABLE, null, values);

            if (conversationId != -1) {
                Cursor messages = SmsMmsUtils.queryConversation(conversation.id, context);

                if (messages == null) {
                    continue;
                }

                if (messages.getCount() == 0) {
                    deleteConversation(conversationId);
                    continue;
                }

                if (messages.moveToFirst()) {
                    do {
                        List<ContentValues> valuesList =
                                SmsMmsUtils.processMessage(messages, conversationId, context);
                        if (valuesList != null) {
                            for (ContentValues value : valuesList) {
                                database.insert(Message.TABLE, null, value);
                            }
                        }
                    } while (messages.moveToNext() && messages.getPosition() < SmsMmsUtils.INITIAL_MESSAGE_LIMIT);

                    messages.close();
                }
            }

            if (listener != null) {
                listener.onProgressUpdate(i + 1, conversations.size());
            }
        }

        setTransactionSuccessful();
        endTransaction();
    }

    /**
     * Inserts a conversation into the database.
     *
     * @param conversation the conversation to insert.
     * @return the conversation id after insertion.
     */
    public long insertConversation(Conversation conversation) {
        ContentValues values = new ContentValues(16);

        if (conversation.id <= 0) {
            conversation.id = generateId();
        }

        values.put(Conversation.COLUMN_ID, conversation.id);
        values.put(Conversation.COLUMN_COLOR, conversation.colors.color);
        values.put(Conversation.COLUMN_COLOR_DARK, conversation.colors.colorDark);
        values.put(Conversation.COLUMN_COLOR_LIGHT, conversation.colors.colorLight);
        values.put(Conversation.COLUMN_COLOR_ACCENT, conversation.colors.colorAccent);
        values.put(Conversation.COLUMN_PINNED, conversation.pinned);
        values.put(Conversation.COLUMN_READ, conversation.read);
        values.put(Conversation.COLUMN_TIMESTAMP, conversation.timestamp);
        values.put(Conversation.COLUMN_TITLE, conversation.title);
        values.put(Conversation.COLUMN_PHONE_NUMBERS, conversation.phoneNumbers);
        values.put(Conversation.COLUMN_SNIPPET, conversation.snippet);
        values.put(Conversation.COLUMN_RINGTONE, conversation.ringtoneUri);
        values.put(Conversation.COLUMN_IMAGE_URI, conversation.imageUri);
        values.put(Conversation.COLUMN_ID_MATCHER, conversation.idMatcher);
        values.put(Conversation.COLUMN_MUTE, conversation.mute);
        values.put(Conversation.COLUMN_ARCHIVED, conversation.archive);
        values.put(Conversation.COLUMN_PRIVATE_NOTIFICATIONS, conversation.privateNotifications);

        apiUtils.addConversation(accountId,conversation.id, conversation.colors.color,
                conversation.colors.colorDark, conversation.colors.colorLight,
                conversation.colors.colorAccent, conversation.pinned, conversation.read,
                conversation.timestamp, conversation.title, conversation.phoneNumbers,
                conversation.snippet, conversation.ringtoneUri, conversation.idMatcher,
                conversation.mute, conversation.archive, conversation.privateNotifications,
                getEncryptionUtils(context));

        try {
            return database.insert(Conversation.TABLE, null, values);
        } catch (Exception e) {
            Log.e(TAG, "failed to insert conversation", e);
            return -1L;
        }
    }

    /**
     * Gets all conversations in the database that are not archived
     *
     * @return a list of conversations.
     */
    public Cursor getUnarchivedConversations() {
        return database.query(Conversation.TABLE, null, Conversation.COLUMN_ARCHIVED + "=?", new String[] { "0" }, null, null,
                Conversation.COLUMN_PINNED + " desc, " + Conversation.COLUMN_TIMESTAMP + " desc"
        );
    }

    /**
     * Gets all conversations in the database.
     *
     * @return a list of conversations.
     */
    public Cursor getAllConversations() {
        return database.query(Conversation.TABLE, null, null, null, null, null,
                Conversation.COLUMN_PINNED + " desc, " + Conversation.COLUMN_TIMESTAMP + " desc"
        );
    }

    /**
     * Gets all pinned conversations in the database.
     *
     * @return a list of pinned conversations.
     */
    public Cursor getPinnedConversations() {
        return database.query(Conversation.TABLE, null, Conversation.COLUMN_PINNED + "=1", null,
                null, null, Conversation.COLUMN_TIMESTAMP + " desc");
    }

    /**
     * Gets all archived conversations in the database.
     *
     * @return a list of pinned conversations.
     */
    public Cursor getArchivedConversations() {
        return database.query(Conversation.TABLE, null, Conversation.COLUMN_ARCHIVED + "=1", null,
                null, null, Conversation.COLUMN_TIMESTAMP + " desc");
    }

    /**
     * Searches for conversations that have a title that matches the given query.
     */
    public Cursor searchConversations(String query) {
        if (query == null || query.length() == 0) {
            return null;
        } else {
            return database.query(Conversation.TABLE, null, Conversation.COLUMN_TITLE + " LIKE '%" +
                            query.replace("'", "''") + "%'", null, null, null,
                    Conversation.COLUMN_TIMESTAMP + " desc");
        }
    }

    /**
     * Gets a conversation by its id.
     *
     * @param conversationId the conversation's id to find.
     * @return the conversation.
     */
    public Conversation getConversation(long conversationId) {
        Cursor cursor = database.query(Conversation.TABLE, null, Conversation.COLUMN_ID + "=?",
                new String[]{Long.toString(conversationId)}, null, null, null);
        if (cursor.moveToFirst()) {
            Conversation conversation = new Conversation();
            conversation.fillFromCursor(cursor);
            cursor.close();
            return conversation;
        }

        return null;
    }

    /**
     * Deletes a conversation from the database.
     *
     * @param conversation the conversation to delete.
     */
    public void deleteConversation(Conversation conversation) {
        if (conversation != null) {
            deleteConversation(conversation.id);
        } else {
            // more than likely already deleted
        }
    }

    /**
     * Deletes a conversation from the database.
     *
     * @param conversationId the conversation id to delete.
     */
    public void deleteConversation(long conversationId) {
        database.delete(Message.TABLE, Message.COLUMN_CONVERSATION_ID + "=?",
                new String[]{Long.toString(conversationId)});

        database.delete(Conversation.TABLE, Conversation.COLUMN_ID + "=?",
                new String[]{Long.toString(conversationId)});

        apiUtils.deleteConversation(accountId, conversationId);
    }

    /**
     * Archives a conversation from the database.
     *
     * @param conversationId the conversation to archive.
     */
    public void archiveConversation(long conversationId) {
        archiveConversation(conversationId, true);
    }

    /**
     * Archives a conversation from the database.
     *
     * @param conversationId the conversation to archive.
     */
    public void unarchiveConversation(long conversationId) {
        archiveConversation(conversationId, false);
    }

    /**
     * Archives a conversation from the database.
     *
     * @param conversationId the conversation id to archive.
     * @param archive true if we want to archive, false if we want to have it not archived
     */
    public void archiveConversation(long conversationId, boolean archive) {
        ContentValues values = new ContentValues(1);
        values.put(Conversation.COLUMN_ARCHIVED, archive);

        int updated = database.update(Conversation.TABLE, values, Conversation.COLUMN_ID + "=?",
                new String[]{Long.toString(conversationId)});

        if (updated > 0) {
            if (archive) {
                apiUtils.archiveConversation(accountId, conversationId);
            } else {
                apiUtils.unarchiveConversation(accountId, conversationId);
            }
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
    public void updateConversation(long conversationId, boolean read, long timestamp,
                                   String snippet, String snippetMime, boolean archive) {
        ContentValues values = new ContentValues(4);
        values.put(Conversation.COLUMN_READ, read);

        if (snippetMime != null && snippetMime.equals(MimeType.TEXT_PLAIN)) {
            values.put(Conversation.COLUMN_SNIPPET, snippet);
        } else {
            snippet = "";
            values.put(Conversation.COLUMN_SNIPPET, "");
        }

        values.put(Conversation.COLUMN_TIMESTAMP, timestamp);
        values.put(Conversation.COLUMN_ARCHIVED, archive);

        int updated = database.update(Conversation.TABLE, values, Conversation.COLUMN_ID + "=?",
                new String[]{Long.toString(conversationId)});

        if (updated > 0) {
            apiUtils.updateConversationSnippet(accountId, conversationId,
                    read, archive, timestamp, snippet, getEncryptionUtils(context));
        }
    }

    /**
     * Updates the settings for a conversation, such as ringtone and colors.
     */
    public void updateConversationSettings(Conversation conversation) {
        ContentValues values = new ContentValues(10);
        values.put(Conversation.COLUMN_PINNED, conversation.pinned);
        values.put(Conversation.COLUMN_TITLE, conversation.title);
        values.put(Conversation.COLUMN_RINGTONE, conversation.ringtoneUri);
        values.put(Conversation.COLUMN_COLOR, conversation.colors.color);
        values.put(Conversation.COLUMN_COLOR_DARK, conversation.colors.colorDark);
        values.put(Conversation.COLUMN_COLOR_LIGHT, conversation.colors.colorLight);
        values.put(Conversation.COLUMN_COLOR_ACCENT, conversation.colors.colorAccent);
        values.put(Conversation.COLUMN_MUTE, conversation.mute);
        values.put(Conversation.COLUMN_READ, conversation.read);
        values.put(Conversation.COLUMN_ARCHIVED, conversation.archive);
        values.put(Conversation.COLUMN_PRIVATE_NOTIFICATIONS, conversation.privateNotifications);

        database.update(Conversation.TABLE, values, Conversation.COLUMN_ID + "=?",
                new String[]{Long.toString(conversation.id)});

        apiUtils.updateConversation(accountId, conversation.id, conversation.colors.color,
                conversation.colors.colorDark, conversation.colors.colorLight,
                conversation.colors.colorAccent, conversation.pinned, null, null,
                conversation.title, null, conversation.ringtoneUri, conversation.mute, conversation.archive,
                conversation.privateNotifications, getEncryptionUtils(context));
    }

    /**
     * Updates the conversation title for a given conversation. Handy when the user has changed
     * the contact's name.
     */
    public void updateConversationTitle(long conversationId, String title) {
        ContentValues values = new ContentValues(1);
        values.put(Conversation.COLUMN_TITLE, title);

        int updated = database.update(Conversation.TABLE, values, Conversation.COLUMN_ID + "=? AND " +
                    Conversation.COLUMN_TITLE + " <> ?",
                new String[] {Long.toString(conversationId), title});

        if (updated > 0) {
            apiUtils.updateConversationTitle(accountId, conversationId, title, getEncryptionUtils(context));
        }
    }

    /**
     * Gets the number of conversations in the database.
     */
    public int getConversationCount() {
        Cursor cursor = getAllConversations();
        int count = cursor.getCount();
        cursor.close();
        return count;
    }

    /**
     * Gets the number of messages in the database.
     */
    public int getMessageCount() {
        Cursor cursor = getMessages();
        int count = cursor.getCount();
        cursor.close();
        return count;
    }

    /**
     * Gets details about a conversation that can be displayed to the user.
     */
    public Spanned getConversationDetails(Conversation conversation) {
        StringBuilder builder = new StringBuilder();

        if (conversation.isGroup()) {
            builder.append("<b>Title: </b>");
        } else {
            builder.append("<b>Name: </b>");
        }

        builder.append(conversation.title);
        builder.append("<br/>");

        if (conversation.isGroup()) {
            builder.append("<b>Phone Numbers: </b>");
        } else {
            builder.append("<b>Phone Number: </b>");
        }

        builder.append(conversation.phoneNumbers);
        builder.append("<br/>");

        if (conversation.isGroup()) {
            builder.append("<b>Number of Members: </b>");
            builder.append(conversation.phoneNumbers.split(", ").length);
            builder.append("<br/>");
        }

        builder.append("<b>Date: </b>");
        builder.append(SimpleDateFormat
                .getDateTimeInstance(SimpleDateFormat.SHORT, SimpleDateFormat.SHORT)
                .format(new Date(conversation.timestamp)));
        builder.append("<br/>");

        Cursor cursor = getMessages(conversation.id);
        if (cursor != null && cursor.moveToFirst()) {
            builder.append("<b>Message Count: </b>");
            builder.append(cursor.getCount());
            builder.append("<br/>");
            cursor.close();
        }

        // remove the last <br/>
        String description = builder.toString();
        description = description.substring(0, description.length() - 5);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return Html.fromHtml(description, 0);
        } else {
            return Html.fromHtml(description);
        }
    }

    /**
     * Gets the details for a message.
     */
    public Spanned getMessageDetails(Context context, long messageId) {
        Message message = getMessage(messageId);
        StringBuilder builder = new StringBuilder();

        builder.append("<b>Date: </b>");
        builder.append(SimpleDateFormat
                .getDateTimeInstance(SimpleDateFormat.SHORT, SimpleDateFormat.SHORT)
                .format(new Date(message.timestamp)));
        builder.append("<br/>");

        builder.append("<b>Status: </b>");
        switch(message.type) {
            case Message.TYPE_SENT:
                builder.append("Sent");
                break;
            case Message.TYPE_SENDING:
                builder.append("Sending");
                break;
            case Message.TYPE_ERROR:
                builder.append("Failed");
                break;
            case Message.TYPE_DELIVERED:
                builder.append("Delivered");
                break;
            case Message.TYPE_RECEIVED:
                builder.append("Received");
                break;
            case Message.TYPE_INFO:
                builder.append("Info");
                break;
        }
        builder.append("<br/>");

//        builder.append("<b>Read: </b>");
//        builder.append(message.read);
//        builder.append("<br/>");
//
//        builder.append("<b>Seen: </b>");
//        builder.append(message.seen);
//        builder.append("<br/>");

        if (message.from != null) {
            builder.append("<b>From: </b>");
            builder.append(message.from);
            builder.append("<br/>");
        }

        if (!message.mimeType.equals(MimeType.TEXT_PLAIN)) {
            byte[] bytes = BinaryUtils.getMediaBytes(context, message.data, message.mimeType);
            builder.append("<b>Size: </b>");
            builder.append(Formatter.formatShortFileSize(context, bytes.length));
            builder.append("<br/>");

            builder.append("<b>Media Type: </b>");
            builder.append(message.mimeType);
            builder.append("<br/>");
        }

        // remove the last <br/>
        String description = builder.toString();
        description = description.substring(0, description.length() - 5);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return Html.fromHtml(description, 0);
        } else {
            return Html.fromHtml(description);
        }
    }

    /**
     * Gets all messages for a given conversation.
     *
     * @param conversationId the conversation id to find messages for.
     * @return a cursor with all messages.
     */
    public Cursor getMessages(long conversationId) {
        return database.query(Message.TABLE, null, Message.COLUMN_CONVERSATION_ID + "=?",
                new String[]{Long.toString(conversationId)}, null, null,
                Message.COLUMN_TIMESTAMP + " asc");
    }

    /**
     * Gets a single message from the database.
     */
    public Message getMessage(long messageId) {
        Cursor cursor = database.query(Message.TABLE, null, Message.COLUMN_ID + "=?",
                new String[]{Long.toString(messageId)}, null, null, null);
        if (cursor.moveToFirst()) {
            Message message = new Message();
            message.fillFromCursor(cursor);
            return message;
        } else {
            return null;
        }
    }

    /**
     * Gets all messages in the database where mime type is not text/plain.
     */
    public Cursor getAllMediaMessages(int limit) {
        return database.query(Message.TABLE, null, Message.COLUMN_MIME_TYPE + "!='text/plain'",
                null, null, null, Message.COLUMN_TIMESTAMP + " desc LIMIT " + limit);
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
    public Cursor getFirebaseMediaMessages() {
        return database.query(Message.TABLE, null, Message.COLUMN_MIME_TYPE + "!='text/plain' AND " +
                Message.COLUMN_DATA + " LIKE 'firebase %'", null, null, null, null);
    }

    /**
     * Gets all messages for a conversation where the mime type is not text/plain.
     */
    public List<Message> getMediaMessages(long conversationId) {
        Cursor c = database.query(Message.TABLE, null, Message.COLUMN_CONVERSATION_ID + "=? AND " +
                        Message.COLUMN_MIME_TYPE + "!='text/plain' AND " +
                        Message.COLUMN_MIME_TYPE + "!='text/x-vcard' AND " +
                        Message.COLUMN_MIME_TYPE + "!='text/vcard'",
                new String[]{Long.toString(conversationId)}, null, null,
                Message.COLUMN_TIMESTAMP + " asc");

        List<Message> messages = new ArrayList<>();

        if (c != null && c.moveToFirst()) {
            do {
                Message message = new Message();
                message.fillFromCursor(c);
                messages.add(message);
            } while (c.moveToNext());

            c.close();
        }

        return messages;
    }

    /**
     * Gets all messages in the database.
     */
    public Cursor getMessages() {
        return database.query(Message.TABLE, null, null, null, null, null,
                Message.COLUMN_TIMESTAMP + " asc");
    }

    /**
     * Get the specified number of messages from the conversation.
     */
    public List<Message> getMessages(long conversationId, int count) {
        Cursor c = getMessages(conversationId);
        List<Message> messages = new ArrayList<>();

        if (c.moveToLast()) {
            do {
                Message message = new Message();
                message.fillFromCursor(c);
                messages.add(message);
            } while (c.moveToPrevious() && messages.size() < count);

            c.close();
        }

        return messages;
    }

    /**
     * Gets all messages that contain the query text.
     *
     * @param query the text to look for.
     * @return a cursor with all messages matching that query.
     */
    public Cursor searchMessages(String query) {
        if (query == null || query.length() == 0) {
            return null;
        } else {
            return database.query(Message.TABLE + " m left outer join " + Conversation.TABLE + " c on m.conversation_id = c._id",
                    new String[] { "m._id as _id", "c._id as conversation_id", "m.type as type", "m.data as data", "m.timestamp as timestamp", "m.mime_type as mime_type", "m.read as read", "m.message_from as message_from", "m.color as color", "c.title as convo_title" },
                    Message.COLUMN_DATA + " LIKE '%" + query.replace("'", "''") + "%' AND " +
                            Message.COLUMN_MIME_TYPE + "='" + MimeType.TEXT_PLAIN + "'",
                    null, null, null, Message.COLUMN_TIMESTAMP + " desc");
        }
    }

    /**
     * Gets all messages that are within 5 seconds of the given timestamp.
     *
     * @param timestamp the message timestamp.
     * @return the cursor of messages.
     */
    public Cursor searchMessages(long timestamp) {
        return database.query(Message.TABLE, null, Message.COLUMN_TIMESTAMP + " BETWEEN " +
                        (timestamp - 10000) + " AND " + (timestamp + 10000), null, null, null,
                Message.COLUMN_TIMESTAMP + " desc");
    }

    /**
     * Updates the message with the given id to the given type.
     *
     * @param messageId the message to update.
     * @param type      the type to change it to.
     */
    public void updateMessageType(long messageId, int type) {
        ContentValues values = new ContentValues(1);
        values.put(Message.COLUMN_TYPE, type);

        database.update(Message.TABLE, values, Message.COLUMN_ID + "=?",
                new String[]{Long.toString(messageId)});

        apiUtils.updateMessageType(accountId, messageId, type);
    }

    /**
     * Updates the data field for a message.
     *
     * @param messageId the id of the message to update.
     * @param data      the new data string.
     */
    public void updateMessageData(long messageId, String data) {
        ContentValues values = new ContentValues(1);
        values.put(Message.COLUMN_DATA, data);

        database.update(Message.TABLE, values, Message.COLUMN_ID + "=?",
                new String[]{Long.toString(messageId)});

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
    public long insertSentMessage(String addresses, String data, String mimeType, Context context) {
        final Message m = new Message();
        m.type = Message.TYPE_SENDING;
        m.data = data;
        m.timestamp = System.currentTimeMillis();
        m.mimeType = mimeType;
        m.read = true;
        m.seen = true;
        m.from = null;
        m.color = null;

        return insertMessage(m, addresses, context);
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
    public long insertMessage(Message message, String phoneNumbers, Context context) {
        return insertMessage(context, message, updateOrCreateConversation(phoneNumbers, message, context));
    }

    /**
     * Checks whether or not a conversation exists for this string of phone numbers. If so, the
     * conversation id will be returned. If not, null will be returned.
     */
    public Long findConversationId(String phoneNumbers) {
        String matcher = SmsMmsUtils.createIdMatcher(phoneNumbers);
        Cursor cursor = database.query(Conversation.TABLE,
                new String[]{Conversation.COLUMN_ID, Conversation.COLUMN_ID_MATCHER},
                Conversation.COLUMN_ID_MATCHER + "=?", new String[]{matcher}, null, null, null);

        if (cursor != null && cursor.moveToFirst()) {
            long conversationId = cursor.getLong(0);
            cursor.close();
            return conversationId;
        } else {
            return null;
        }
    }

    /**
     * Checks whether or not a conversation exists for this title. If so, the
     * conversation id will be returned. If not, null will be returned.
     */
    public Long findConversationIdByTitle(String title) {
        Cursor cursor = database.query(Conversation.TABLE,
                new String[]{Conversation.COLUMN_ID, Conversation.COLUMN_TITLE},
                Conversation.COLUMN_TITLE + "=?", new String[]{title}, null, null, null);

        if (cursor != null && cursor.moveToFirst()) {
            long conversationId = cursor.getLong(0);
            cursor.close();
            return conversationId;
        } else {
            return null;
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
    private long updateOrCreateConversation(String phoneNumbers, Message message, Context context) {
        String matcher = SmsMmsUtils.createIdMatcher(phoneNumbers);
        Cursor cursor = database.query(Conversation.TABLE,
                new String[]{Conversation.COLUMN_ID, Conversation.COLUMN_ID_MATCHER},
                Conversation.COLUMN_ID_MATCHER + "=?", new String[]{matcher}, null, null, null);

        long conversationId;

        if (cursor != null && cursor.moveToFirst()) {
            conversationId = cursor.getLong(0);
            updateConversation(conversationId, message.read, message.timestamp, message.data,
                    message.mimeType, false);
            cursor.close();
        } else {
            Conversation conversation = new Conversation();
            conversation.pinned = false;
            conversation.read = message.read;
            conversation.timestamp = message.timestamp;

            if (message.mimeType.equals(MimeType.TEXT_PLAIN) && message.type != Message.TYPE_INFO) {
                conversation.snippet = message.data;
            } else {
                conversation.snippet = "";
            }

            conversation.ringtoneUri = null;
            conversation.phoneNumbers = phoneNumbers;
            conversation.title = ContactUtils.findContactNames(phoneNumbers, context);
            conversation.imageUri = ContactUtils.findImageUri(phoneNumbers, context);
            conversation.idMatcher = matcher;
            conversation.mute = false;
            conversation.archive = false;

            ImageUtils.fillConversationColors(conversation, context);

            List<Contact> contacts = getContacts(conversation.title);
            if (contacts.size() == 1) {
                // just one user in this conversation, so lets set the conversation color to that user's color
                conversation.colors = contacts.get(0).colors;
            }

            conversationId = insertConversation(conversation);
        }

        return conversationId;
    }

    /**
     * Inserts a new message into the database. This also updates the conversation with the latest
     * data.
     *
     * @param message        the message to insert.
     * @param conversationId the conversation to insert the message into.
     * @return the conversation id that the message was inserted into.
     */
    public long insertMessage(Context context, Message message, long conversationId) {
        message.conversationId = conversationId;

        ContentValues values = new ContentValues(10);

        if (message.id <= 0) {
            message.id = generateId();
        }

        values.put(Message.COLUMN_ID, message.id);
        values.put(Message.COLUMN_CONVERSATION_ID, conversationId);
        values.put(Message.COLUMN_TYPE, message.type);
        values.put(Message.COLUMN_DATA, message.data);
        values.put(Message.COLUMN_TIMESTAMP, message.timestamp);
        values.put(Message.COLUMN_MIME_TYPE, message.mimeType);
        values.put(Message.COLUMN_READ, message.read);
        values.put(Message.COLUMN_SEEN, message.seen);
        values.put(Message.COLUMN_FROM, message.from);
        values.put(Message.COLUMN_COLOR, message.color);

        long id = database.insert(Message.TABLE, null, values);

        apiUtils.addMessage(context, accountId, message.id, conversationId, message.type, message.data,
                message.timestamp, message.mimeType, message.read, message.seen, message.from,
                message.color, getEncryptionUtils(context));

        updateConversation(conversationId, message.read, message.timestamp, message.data,
                message.mimeType, false);

        return conversationId;
    }

    /**
     * Inserts a new message list into the database. This also updates the conversation with the latest
     * data.
     *
     * @param messages        list of messages to batch insert
     */
    public void insertMessages(Context context, List<Message> messages) {
        beginTransaction();

        for (int i = 0; i < messages.size(); i++) {
            Message message = messages.get(i);

            ContentValues values = new ContentValues(10);

            if (message.id <= 0) {
                message.id = generateId();
            }

            values.put(Message.COLUMN_ID, message.id);
            values.put(Message.COLUMN_CONVERSATION_ID, message.conversationId);
            values.put(Message.COLUMN_TYPE, message.type);
            values.put(Message.COLUMN_DATA, message.data);
            values.put(Message.COLUMN_TIMESTAMP, message.timestamp);
            values.put(Message.COLUMN_MIME_TYPE, message.mimeType);
            values.put(Message.COLUMN_READ, message.read);
            values.put(Message.COLUMN_SEEN, message.seen);
            values.put(Message.COLUMN_FROM, message.from);
            values.put(Message.COLUMN_COLOR, message.color);

            long id = database.insert(Message.TABLE, null, values);

            apiUtils.addMessage(context, accountId, message.id, message.conversationId, message.type, message.data,
                    message.timestamp, message.mimeType, message.read, message.seen, message.from,
                    message.color, getEncryptionUtils(context));

            updateConversation(message.conversationId, message.read, message.timestamp, message.data,
                    message.mimeType, false);
        }

        setTransactionSuccessful();
        endTransaction();
    }

    /**
     * Deletes a message with the given id.
     */
    public int deleteMessage(long messageId) {
        int deleted = database.delete(Message.TABLE, Message.COLUMN_ID + "=?",
                new String[]{Long.toString(messageId)});

        apiUtils.deleteMessage(accountId, messageId);
        return deleted;
    }

    /**
     * Marks a conversation and all messages inside of it as read and seen.
     *
     * @param conversationId the conversation id to mark.
     */
    public void readConversation(Context context, long conversationId) {
        ContentValues values = new ContentValues(2);
        values.put(Message.COLUMN_READ, 1);
        values.put(Message.COLUMN_SEEN, 1);

        int updated = database.update(Message.TABLE, values, Message.COLUMN_CONVERSATION_ID + "=? AND (" +
                        Message.COLUMN_READ + "=? OR " + Message.COLUMN_SEEN + "=?)",
                new String[]{Long.toString(conversationId), "0", "0"});

        values = new ContentValues(1);
        values.put(Conversation.COLUMN_READ, 1);

        updated += database.update(Conversation.TABLE, values, Conversation.COLUMN_ID + "=? AND " +
                        Conversation.COLUMN_READ + "=?",
                new String[]{Long.toString(conversationId), "0"});

        if (updated > 0) {
            apiUtils.readConversation(accountId, conversationId);
        }

        try {
            SmsMmsUtils.markConversationRead(context, getConversation(conversationId).phoneNumbers);
        } catch (NullPointerException e) {
            // thrown in robolectric tests
        }
    }

    /**
     * Marks all messages in a conversation as seen.
     */
    public void seenConversation(long conversationId) {
        ContentValues values = new ContentValues(1);
        values.put(Message.COLUMN_SEEN, 1);

        database.update(Message.TABLE, values, Message.COLUMN_CONVERSATION_ID + "=? AND " +
                Message.COLUMN_SEEN + "=0", new String[]{Long.toString(conversationId)});

        apiUtils.seenConversation(accountId, conversationId);
    }

    /**
     * Mark all messages as seen.
     */
    public void seenConversations() {
        ContentValues values = new ContentValues(1);
        values.put(Message.COLUMN_SEEN, 1);

        database.update(Message.TABLE, values, Message.COLUMN_SEEN + "=0", null);

        apiUtils.seenConversations(accountId);
    }

    /**
     * Mark all messages as seen.
     */
    public void seenAllMessages() {
        ContentValues values = new ContentValues(1);
        values.put(Message.COLUMN_SEEN, 1);

        database.update(Message.TABLE, values, Message.COLUMN_SEEN + "=0", null);
        apiUtils.seenConversations(accountId);
    }

    /**
     * Gets all messages in the database not marked as read.
     *
     * @return a cursor of all unread messages.
     */
    public Cursor getUnreadMessages() {
        return database.query(Message.TABLE, null, Message.COLUMN_READ + "=0", null, null, null,
                Message.COLUMN_TIMESTAMP + " desc");
    }

    /**
     * Gets all message in the database not marked as seen.
     *
     * @return a cursor of all unseen messages.
     */
    public Cursor getUnseenMessages() {
        return database.query(Message.TABLE, null, Message.COLUMN_SEEN + "=0", null, null, null,
                Message.COLUMN_TIMESTAMP + " asc");
    }

    /**
     * Inserts a draft into the database with the given parameters.
     */
    public long insertDraft(long conversationId, String data, String mimeType) {
        ContentValues values = new ContentValues(4);
        long id = generateId();
        values.put(Draft.COLUMN_ID, id);
        values.put(Draft.COLUMN_CONVERSATION_ID, conversationId);
        values.put(Draft.COLUMN_DATA, data);
        values.put(Draft.COLUMN_MIME_TYPE, mimeType);

        apiUtils.addDraft(accountId, id, conversationId, data, mimeType, getEncryptionUtils(context));
        return database.insert(Draft.TABLE, null, values);
    }

    /**
     * Inserts a draft into the database.
     */
    public long insertDraft(Draft draft) {
        ContentValues values = new ContentValues(4);

        if (draft.id > 0) {
            values.put(Draft.COLUMN_ID, draft.id);
        } else {
            values.put(Draft.COLUMN_ID, generateId());
        }

        values.put(Draft.COLUMN_CONVERSATION_ID, draft.conversationId);
        values.put(Draft.COLUMN_DATA, draft.data);
        values.put(Draft.COLUMN_MIME_TYPE, draft.mimeType);

        try {
            return database.insert(Draft.TABLE, null, values);
        } catch (SQLiteConstraintException e) {
            e.printStackTrace();
            return -1;
        }

        // NOTE: no api interaction here because this is only called when we insert a draft
        //       in the api download service.
    }

    /**
     * Gets all drafts in the database.
     */
    public Cursor getDrafts() {
        return database.query(Draft.TABLE, null, null, null, null, null, null);
    }

    /**
     * Gets all draft messages for a given conversation id. There may be multiple for each
     * conversation because there is the potential for different mime types. For example, a
     * conversation could have a text draft and an image draft, both of which should be displayed
     * when the conversation is loaded.
     */
    public List<Draft> getDrafts(long conversationId) {
        Cursor cursor = database.query(Draft.TABLE, null, Draft.COLUMN_CONVERSATION_ID + "=?",
                new String[]{Long.toString(conversationId)}, null, null, null);
        List<Draft> drafts = new ArrayList<>();

        if (cursor != null && cursor.moveToFirst()) {
            do {
                Draft draft = new Draft();
                draft.fillFromCursor(cursor);
                drafts.add(draft);
            } while (cursor.moveToNext());
            cursor.close();
        }

        return drafts;
    }

    /**
     * Deletes all drafts for a given conversation. This should be used after a message has been
     * sent to the conversation.
     */
    public void deleteDrafts(long conversationId) {
        database.delete(Draft.TABLE, Draft.COLUMN_CONVERSATION_ID + "=?",
                new String[]{Long.toString(conversationId)});

        apiUtils.deleteDrafts(accountId, conversationId);
    }

    /**
     * Gets all blacklists in the database.
     */
    public Cursor getBlacklists() {
        return database.query(Blacklist.TABLE, null, null, null, null, null, null);
    }

    /**
     * Inserts a blacklist into the database.
     */
    public void insertBlacklist(Blacklist blacklist) {
        ContentValues values = new ContentValues(2);

        if (blacklist.id <= 0) {
            blacklist.id = generateId();
        }

        values.put(Blacklist.COLUMN_ID, blacklist.id);
        values.put(Blacklist.COLUMN_PHONE_NUMBER, blacklist.phoneNumber);
        database.insert(Blacklist.TABLE, null, values);
        apiUtils.addBlacklist(accountId, blacklist.id, blacklist.phoneNumber, getEncryptionUtils(context));
    }

    /**
     * Deletes a blacklist from the database.
     */
    public void deleteBlacklist(long id) {
        database.delete(Blacklist.TABLE, Blacklist.COLUMN_ID + "=?",
                new String[]{Long.toString(id)});

        apiUtils.deleteBlacklist(accountId, id);
    }

    /**
     * Gets all scheduled messages in the database.
     */
    public Cursor getScheduledMessages() {
        return database.query(ScheduledMessage.TABLE, null, null, null, null, null,
                ScheduledMessage.COLUMN_TIMESTAMP + " asc");
    }

    /**
     * Inserts a scheduled message into the database.
     */
    public long insertScheduledMessage(ScheduledMessage message) {
        ContentValues values = new ContentValues(6);

        if (message.id <= 0) {
            message.id = generateId();
        }

        values.put(ScheduledMessage.COLUMN_ID, message.id);
        values.put(ScheduledMessage.COLUMN_TITLE, message.title);
        values.put(ScheduledMessage.COLUMN_TO, message.to);
        values.put(ScheduledMessage.COLUMN_DATA, message.data);
        values.put(ScheduledMessage.COLUMN_MIME_TYPE, message.mimeType);
        values.put(ScheduledMessage.COLUMN_TIMESTAMP, message.timestamp);

        apiUtils.addScheduledMessage(accountId, message.id, message.title, message.to, message.data,
                message.mimeType, message.timestamp, getEncryptionUtils(context));

        return database.insert(ScheduledMessage.TABLE, null, values);
    }

    /**
     * Deletes a scheduled message from the database.
     */
    public void deleteScheduledMessage(long id) {
        database.delete(ScheduledMessage.TABLE, ScheduledMessage.COLUMN_ID + "=?",
                new String[]{Long.toString(id)});
        apiUtils.deleteScheduledMessage(accountId, id);
    }

    /**
     * Sets whether or not to upload data changes to the server. If there is no account id, then
     * this value will always be false.
     */
    public void setUpload(boolean upload) {
        this.apiUtils.setActive(upload);
    }

    /**
     * Generates a random id for the row.
     */
    public static long generateId() {
        long leftLimit = 1L;
        long rightLimit = MAX_ID;
        return leftLimit + (long) (Math.random() * (rightLimit - leftLimit));
    }

}
