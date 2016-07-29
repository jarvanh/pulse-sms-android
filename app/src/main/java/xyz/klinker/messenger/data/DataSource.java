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
import android.os.Build;
import android.support.annotation.VisibleForTesting;
import android.text.Html;
import android.text.Spannable;
import android.text.Spanned;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import xyz.klinker.messenger.data.model.Conversation;
import xyz.klinker.messenger.data.model.Draft;
import xyz.klinker.messenger.data.model.Message;
import xyz.klinker.messenger.util.ContactUtils;
import xyz.klinker.messenger.util.ImageUtils;
import xyz.klinker.messenger.util.SmsMmsUtils;
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
    public DataSource(SQLiteDatabase database) {
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
     * Checks if the database is open.
     */
    public boolean isOpen() {
        return database.isOpen();
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
     * @param conversations the list of conversations. See SmsMmsUtils.queryConversations().
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
            values.put(Conversation.COLUMN_MUTE, conversation.mute);

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
        values.put(Conversation.COLUMN_MUTE, conversation.mute);

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
     * Gets all pinned conversations in the database.
     *
     * @return a list of pinned conversations.
     */
    public Cursor getPinnedConversations() {
        return database.query(Conversation.TABLE, null, Conversation.COLUMN_PINNED + "=1", null,
                null, null, Conversation.COLUMN_TIMESTAMP + " desc");
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
     * Updates the settings for a conversation, such as ringtone and colors.
     */
    public void updateConversationSettings(Conversation conversation) {
        ContentValues values = new ContentValues(8);
        values.put(Conversation.COLUMN_PINNED, conversation.pinned);
        values.put(Conversation.COLUMN_TITLE, conversation.title);
        values.put(Conversation.COLUMN_RINGTONE, conversation.ringtoneUri);
        values.put(Conversation.COLUMN_COLOR, conversation.colors.color);
        values.put(Conversation.COLUMN_COLOR_DARK, conversation.colors.colorDark);
        values.put(Conversation.COLUMN_COLOR_LIGHT, conversation.colors.colorLight);
        values.put(Conversation.COLUMN_COLOR_ACCENT, conversation.colors.colorAccent);
        values.put(Conversation.COLUMN_MUTE, conversation.mute);

        database.update(Conversation.TABLE, values, Conversation.COLUMN_ID + "=?",
                new String[] { Long.toString(conversation.id) });
    }

    /**
     * Gets the number of conversations in the database.
     */
    public int getConversationCount() {
        Cursor cursor = getConversations();
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
     * Gets all messages in the database.
     */
    public Cursor getMessages() {
        return database.query(Message.TABLE, null, null, null, null, null,
                Message.COLUMN_TIMESTAMP + " asc");
    }

    /**
     * Gets all messages that contain the query text.
     *
     * @param query the text to look for.
     * @return a cursor with all messages matching that query.
     */
    public Cursor searchMessages(String query) {
        return database.query(Message.TABLE, null, Message.COLUMN_DATA + " LIKE '%" +
                query.replace("'", "''") + "%'", null, null, null,
                Message.COLUMN_TIMESTAMP + " desc");
    }

    /**
     * Gets all messages that are within 5 seconds of the given timestamp.
     *
     * @param timestamp the message timestamp.
     * @return the cursor of messages.
     */
    public Cursor searchMessages(long timestamp) {
        return database.query(Message.TABLE, null, Message.COLUMN_TIMESTAMP + " BETWEEN " +
                (timestamp - 5000) + " AND " + (timestamp + 5000), null, null, null,
                Message.COLUMN_TIMESTAMP + " desc");
    }

    /**
     * Updates the message with the given id to the given type.
     *
     * @param messageId the message to update.
     * @param type the type to change it to.
     */
    public void updateMessageType(long messageId, int type) {
        ContentValues values = new ContentValues(1);
        values.put(Message.COLUMN_TYPE, type);

        database.update(Message.TABLE, values, Message.COLUMN_ID + "=?",
                new String[] { Long.toString(messageId) });
    }

    /**
     * Updates the data field for a message.
     *
     * @param messageId the id of the message to update.
     * @param data the new data string.
     */
    public void updateMessageData(long messageId, String data) {
        ContentValues values = new ContentValues(1);
        values.put(Message.COLUMN_DATA, data);

        database.update(Message.TABLE, values, Message.COLUMN_ID + "=?",
                new String[] { Long.toString(messageId) });
    }

    /**
     * Inserts a new sent message after finding the conversation id.
     *
     * @param addresses the comma, space separated addresses.
     * @param data the message data.
     * @param mimeType the message mimeType.
     * @param context the application context.
     */
    public void insertSentMessage(String addresses, String data, String mimeType, Context context) {
        final Message m = new Message();
        m.type = Message.TYPE_SENDING;
        m.data = data;
        m.timestamp = System.currentTimeMillis();
        m.mimeType = mimeType;
        m.read = true;
        m.seen = true;
        m.from = null;
        m.color = null;

        insertMessage(m, addresses, context);
    }

    /**
     * Inserts a new message into the database without previously having a conversation id. This
     * will be slightly slower than if you were to have an id since we will need to find the
     * appropriate one in the database or create a new conversation entry.
     *
     * @param message the message to insert.
     * @param phoneNumbers the phone numbers to look up by conversation.id_matcher column.
     * @return the conversation id that the message was inserted into.
     */
    public long insertMessage(Message message, String phoneNumbers, Context context) {
        return insertMessage(message, updateOrCreateConversation(phoneNumbers, message, context));
    }

    /**
     * Checks whether or not a conversation exists for this string of phone numbers. If so, the
     * conversation id will be returned. If not, null will be returned.
     */
    public Long findConversationId(String phoneNumbers) {
        String matcher = SmsMmsUtils.createIdMatcher(phoneNumbers);
        Cursor cursor = database.query(Conversation.TABLE,
                new String[] {Conversation.COLUMN_ID, Conversation.COLUMN_ID_MATCHER},
                Conversation.COLUMN_ID_MATCHER + "=?", new String[] {matcher}, null, null, null);

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
     * @param message the message to use to initialize a conversation if needed.
     * @return the conversation id to use.
     */
    private long updateOrCreateConversation(String phoneNumbers, Message message, Context context) {
        String matcher = SmsMmsUtils.createIdMatcher(phoneNumbers);
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
            ImageUtils.fillConversationColors(conversation, context);

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
     * @return the conversation id that the message was inserted into.
     */
    public long insertMessage(Message message, long conversationId) {
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
        return conversationId;
    }

    /**
     * Deletes a message with the given id.
     */
    public void deleteMessage(long messageId) {
        database.delete(Message.TABLE, Message.COLUMN_ID + "=?",
                new String[] { Long.toString(messageId) });
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

        database.update(Message.TABLE, values, Message.COLUMN_CONVERSATION_ID + "=? AND (" +
                Message.COLUMN_READ + "=? OR " + Message.COLUMN_SEEN + "=?)",
                new String[] {Long.toString(conversationId), "0", "0"});

        values = new ContentValues(1);
        values.put(Conversation.COLUMN_READ, 1);

        database.update(Conversation.TABLE, values, Conversation.COLUMN_ID + "=?",
                new String[] { Long.toString(conversationId) });

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
                Message.COLUMN_SEEN + "=0", new String[] {Long.toString(conversationId)});
    }

    /**
     * Mark all messages as seen.
     */
    public void seenAllMessages() {
        ContentValues values = new ContentValues(1);
        values.put(Message.COLUMN_SEEN, 1);

        database.update(Message.TABLE, values, Message.COLUMN_SEEN + "=0", null);
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
    public void insertDraft(long conversationId, String data, String mimeType) {
        ContentValues values = new ContentValues(3);
        values.put(Draft.COLUMN_CONVERSATION_ID, conversationId);
        values.put(Draft.COLUMN_DATA, data);
        values.put(Draft.COLUMN_MIME_TYPE, mimeType);
        database.insert(Draft.TABLE, null, values);
    }

    /**
     * Gets all draft messages for a given conversation id. There may be multiple for each
     * conversation because there is the potential for different mime types. For example, a
     * conversation could have a text draft and an image draft, both of which should be displayed
     * when the conversation is loaded.
     */
    public List<Draft> getDrafts(long conversationId) {
        Cursor cursor = database.query(Draft.TABLE, null, Draft.COLUMN_CONVERSATION_ID + "=?",
                new String[] { Long.toString(conversationId) }, null, null, null);
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
                new String[] { Long.toString(conversationId) });
    }

}
