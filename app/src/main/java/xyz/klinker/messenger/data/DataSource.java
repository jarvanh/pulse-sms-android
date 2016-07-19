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
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.VisibleForTesting;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import xyz.klinker.messenger.activity.InitialLoadActivity;
import xyz.klinker.messenger.data.model.Conversation;
import xyz.klinker.messenger.data.model.Message;
import xyz.klinker.messenger.util.ContactUtil;
import xyz.klinker.messenger.util.ImageUtil;
import xyz.klinker.messenger.util.SmsMmsUtil;
import xyz.klinker.messenger.util.listener.ProgressUpdateListener;

/**
 * Handles interactions with database models.
 */
public class DataSource {

    private static final String TAG = "DataSource";
    private static volatile DataSource instance;

    private SQLiteDatabase database;
    private DatabaseSQLiteHelper dbHelper;
    private AtomicInteger openCounter = new AtomicInteger();

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

        return instance;
    }

    /**
     * Private constructor to force a singleton.
     *
     * @param context Current calling context
     */
    private DataSource(Context context) {
        this.dbHelper = new DatabaseSQLiteHelper(context);
    }

    /**
     * Contructor to help with testing.
     *
     * @param helper Mock of the database helper
     */
    @VisibleForTesting
    protected DataSource(DatabaseSQLiteHelper helper) {
        this.dbHelper = helper;
    }

    /**
     * Constructor to help with testing.
     *
     * @param database Mock of the sqlite database
     */
    @VisibleForTesting
    protected DataSource(SQLiteDatabase database) {
        this.database = database;
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
    protected SQLiteDatabase getDatabase() {
        return database;
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
     * Writes the initial list of conversations to the database. These are the conversations that
     * will come from your phones internal SMS database. It will then find all messages in each
     * of these conversations and insert them as well, during the same transaction.
     *
     * @param conversations the list of conversations. See SmsMmsUtil.queryConversations().
     * @param context the application context.
     */
    public void insertConversations(List<Conversation> conversations, Context context,
                                    ProgressUpdateListener listener) {
        beginTransaction();

        for (int i = 0; i < conversations.size(); i++) {
            Conversation conversation = conversations.get(i);

            ContentValues values = new ContentValues(12);
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

            long conversationId = database.insert(Conversation.TABLE, null, values);

            if (conversationId != -1) {
                Cursor messages = SmsMmsUtil.queryConversation(conversation.id, context);

                if (messages == null) {
                    continue;
                }

                if (messages.getCount() == 0) {
                    messages = InitialLoadActivity.getFakeMessages();
                }

                if (messages.moveToFirst()) {
                    do {
                        List<ContentValues> valuesList =
                                SmsMmsUtil.processMessage(messages, conversationId, context);
                        if (valuesList != null) {
                            for (ContentValues value : valuesList) {
                                database.insert(Message.TABLE, null, value);
                            }
                        }
                    } while (messages.moveToNext());

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
        ContentValues values = new ContentValues(12);
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

        return database.insert(Conversation.TABLE, null, values);
    }

    /**
     * Gets all conversations in the database.
     *
     * @return a list of conversations.
     */
    public Cursor getConversations() {
        return database.query(Conversation.TABLE, null, null, null, null, null,
                Conversation.COLUMN_PINNED + " desc, " + Conversation.COLUMN_TIMESTAMP + " desc"
        );
    }

    /**
     * Gets a conversation by its id.
     *
     * @param conversationId the conversation's id to find.
     * @return the conversation.
     */
    public Conversation getConversation(long conversationId) {
        Cursor cursor = database.query(Conversation.TABLE, null, Conversation.COLUMN_ID + "=?",
                new String[] { Long.toString(conversationId) }, null, null, null);
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
        deleteConversation(conversation.id);
    }

    /**
     * Deletes a conversation from the database.
     *
     * @param conversationId the conversation id to delete.
     */
    public void deleteConversation(long conversationId) {
        database.delete(Conversation.TABLE, Conversation.COLUMN_ID + "=?",
                new String[] { Long.toString(conversationId) });

        database.delete(Message.TABLE, Message.COLUMN_CONVERSATION_ID + "=?",
                new String[] { Long.toString(conversationId) });
    }

    /**
     * Updates the conversation with given values.
     *
     * @param conversationId the conversation to update.
     * @param read whether the conversation is read or not.
     * @param timestamp the new timestamp for the conversation
     * @param snippet the snippet to display for appropriate mime types.
     * @param snippetMime the snippet's mime type.
     */
    public void updateConversation(long conversationId, boolean read, long timestamp,
                                   String snippet, String snippetMime) {
        ContentValues values = new ContentValues(3);
        values.put(Conversation.COLUMN_READ, read);

        if (snippetMime != null && snippetMime.equals(MimeType.TEXT_PLAIN)) {
            values.put(Conversation.COLUMN_SNIPPET, snippet);
        } else {
            values.put(Conversation.COLUMN_SNIPPET, "");
        }

        values.put(Conversation.COLUMN_TIMESTAMP, timestamp);

        database.update(Conversation.TABLE, values, Conversation.COLUMN_ID + "=?",
                new String[] { Long.toString(conversationId) });
    }

    /**
     * Gets all messages for a given conversation.
     *
     * @param conversationId the conversation id to find messages for.
     * @return a cursor with all messages.
     */
    public Cursor getMessages(long conversationId) {
        return database.query(Message.TABLE, null, Message.COLUMN_CONVERSATION_ID + "=?",
                new String[] { Long.toString(conversationId) }, null, null,
                Message.COLUMN_TIMESTAMP + " asc");
    }

    /**
     * Inserts a new message into the database without previously having a conversation id. This
     * will be slightly slower than if you were to have an id since we will need to find the
     * appropriate one in the database or create a new conversation entry.
     *
     * @param message the message to insert.
     * @param phoneNumbers the phone numbers to look up by conversation.id_matcher column.
     */
    public void insertMessage(Message message, String phoneNumbers, Context context) {
        insertMessage(message, updateOrCreateConversation(phoneNumbers, message, context));
    }

    /**
     * Gets a current conversation id if one exists for the phone number, or inserts a new
     * conversation and returns that id if one does not exist.
     *
     * @param phoneNumbers the phone number to match the conversation with.
     * @param message the message to use to initialize a conversation if needed.
     * @return the conversation id to use.
     */
    private long updateOrCreateConversation(String phoneNumbers, Message message, Context context) {
        String matcher = SmsMmsUtil.createIdMatcher(phoneNumbers);
        Cursor cursor = database.query(Conversation.TABLE,
                new String[] {Conversation.COLUMN_ID, Conversation.COLUMN_ID_MATCHER},
                Conversation.COLUMN_ID_MATCHER + "=?", new String[] {matcher}, null, null, null);

        long conversationId;

        if (cursor != null && cursor.moveToFirst()) {
            conversationId = cursor.getLong(0);
            updateConversation(conversationId, message.read, message.timestamp, message.data,
                    message.mimeType);
            cursor.close();
        } else {
            Conversation conversation = new Conversation();
            conversation.pinned = false;
            conversation.read = message.read;
            conversation.timestamp = message.timestamp;

            if (message.mimeType.equals(MimeType.TEXT_PLAIN)) {
                conversation.snippet = message.data;
            } else {
                conversation.snippet = "";
            }

            conversation.ringtoneUri = null;
            conversation.phoneNumbers = phoneNumbers;
            conversation.title = ContactUtil.findContactNames(phoneNumbers, context);
            conversation.imageUri = ContactUtil.findImageUri(phoneNumbers, context);
            conversation.idMatcher = matcher;
            ImageUtil.fillConversationColors(conversation, context);

            conversationId = insertConversation(conversation);
        }

        return conversationId;
    }

    /**
     * Inserts a new message into the database. This also updates the conversation with the latest
     * data.
     *
     * @param message the message to insert.
     * @param conversationId the conversation to insert the message into.
     */
    public void insertMessage(Message message, long conversationId) {
        ContentValues values = new ContentValues(9);
        values.put(Message.COLUMN_CONVERSATION_ID, conversationId);
        values.put(Message.COLUMN_TYPE, message.type);
        values.put(Message.COLUMN_DATA, message.data);
        values.put(Message.COLUMN_TIMESTAMP, message.timestamp);
        values.put(Message.COLUMN_MIME_TYPE, message.mimeType);
        values.put(Message.COLUMN_READ, message.read);
        values.put(Message.COLUMN_SEEN, message.seen);
        values.put(Message.COLUMN_FROM, message.from);
        values.put(Message.COLUMN_COLOR, message.color);

        database.insert(Message.TABLE, null, values);

        updateConversation(conversationId, message.read, message.timestamp, message.data,
                message.mimeType);
    }

    /**
     * Marks a conversation and all messages inside of it as read and seen.
     *
     * @param conversationId the conversation id to mark.
     */
    public void readConversation(Context context, long conversationId) {
        ContentValues values = new ContentValues(1);
        values.put(Message.COLUMN_READ, 1);
        values.put(Message.COLUMN_SEEN, 1);

        database.update(Message.TABLE, values, Message.COLUMN_CONVERSATION_ID + "=? AND (" +
                Message.COLUMN_READ + "=? OR " + Message.COLUMN_SEEN + "=?)",
                new String[] {Long.toString(conversationId), "0", "0"});

        values = new ContentValues(1);
        values.put(Conversation.COLUMN_READ, 1);

        database.update(Conversation.TABLE, values, Conversation.COLUMN_ID + "=?",
                new String[] { Long.toString(conversationId) });

        try {
            SmsMmsUtil.markConversationRead(context, getConversation(conversationId).phoneNumbers);
        } catch (NullPointerException e) {
            // thrown in robolectric tests
        }
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
                Message.COLUMN_TIMESTAMP + " desc");
    }

}
