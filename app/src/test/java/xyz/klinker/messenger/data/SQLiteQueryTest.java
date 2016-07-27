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

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.robolectric.RuntimeEnvironment;

import java.util.ArrayList;
import java.util.List;

import xyz.klinker.messenger.MessengerRobolectricSuite;
import xyz.klinker.messenger.activity.InitialLoadActivity;
import xyz.klinker.messenger.data.model.Conversation;
import xyz.klinker.messenger.data.model.Draft;
import xyz.klinker.messenger.data.model.Message;
import xyz.klinker.messenger.util.ContactUtil;
import xyz.klinker.messenger.util.FixtureLoader;
import xyz.klinker.messenger.util.ImageUtil;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNotSame;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;

public class SQLiteQueryTest extends MessengerRobolectricSuite {

    private DataSource source;

    @Before
    public void setUp() throws Exception {
        SQLiteDatabase database = SQLiteDatabase.create(null);
        DatabaseSQLiteHelper helper = new DatabaseSQLiteHelper(RuntimeEnvironment.application);
        helper.onCreate(database);

        source = new DataSource(database);
        insertData();
    }

    @Test
    public void databaseCreated() {
        assertNotNull(source.getDatabase());

        int numTables = 0;
        Cursor cursor = source.getDatabase().rawQuery("SELECT count(*) FROM sqlite_master " +
                "WHERE type = 'table' AND name != 'android_metadata' AND name != " +
                "'sqlite_sequence';", null);
        if (cursor != null && cursor.moveToFirst()) {
            numTables = cursor.getInt(0);
            cursor.close();
        }

        assertTrue(numTables > 0);
    }

    @Test
    public void insertConversations() {
        int initialSize = source.getConversations().getCount();
        source.insertConversations(InitialLoadActivity
                .getFakeConversations(RuntimeEnvironment.application.getResources()),
                RuntimeEnvironment.application, null);
        int newSize = source.getConversations().getCount();

        assertEquals(7, newSize - initialSize);
    }

    @Test
    public void insertConversation() {
        Conversation conversation = new Conversation();
        conversation.pinned = false;
        conversation.read = true;
        conversation.timestamp = System.currentTimeMillis();
        conversation.snippet = "test conversation";
        conversation.ringtoneUri = null;
        conversation.phoneNumbers = "5154224558";
        conversation.title = "test";
        conversation.imageUri = null;
        conversation.idMatcher = "24558";

        int initialSize = source.getConversations().getCount();
        source.insertConversation(conversation);
        int newSize = source.getConversations().getCount();

        Assert.assertEquals(1, newSize - initialSize);
    }

    @Test
    public void getConversations() {
        List<String> titles = new ArrayList<>();
        Cursor cursor = source.getConversations();

        if (cursor.moveToFirst()) {
            do {
                titles.add(cursor.getString(cursor.getColumnIndex(Conversation.COLUMN_TITLE)));
            } while (cursor.moveToNext());

            cursor.close();
        }

        assertEquals("Andrew Klinker", titles.get(0));
        assertEquals("Luke Klinker", titles.get(1));
        assertEquals("Aaron Klinker", titles.get(2));
    }

    @Test
    public void getConversation() {
        Conversation conversation = source.getConversation(1L);
        assertEquals("Luke Klinker", conversation.title);
    }

    @Test
    public void getConversationNull() {
        Conversation conversation = source.getConversation(10L);
        assertNull(conversation);
    }

    @Test
    public void deleteConversation() {
        assertNotSame(0, source.getMessages(1).getCount());
        int initialConversationSize = source.getConversations().getCount();
        source.deleteConversation(1);
        int newConversationSize = source.getConversations().getCount();
        int newMessageSize = source.getMessages(1).getCount();

        assertEquals(-1, newConversationSize - initialConversationSize);
        assertEquals(0, newMessageSize);
    }

    @Test
    public void updateConversation() {
        source.updateConversation(1, false, System.currentTimeMillis(), "test updated message",
                MimeType.TEXT_PLAIN);
        Conversation conversation = source.getConversation(1);
        assertEquals("test updated message", conversation.snippet);
    }

    @Test
    public void updateConversationImage() {
        source.updateConversation(1, false, System.currentTimeMillis(), "test updated message",
                MimeType.IMAGE_PNG);
        Conversation conversation = source.getConversation(1);
        assertEquals("", conversation.snippet);
    }

    @Test
    public void getMessages() {
        assertNotSame(0, source.getMessages(1).getCount());
        assertNotSame(0, source.getMessages(2).getCount());
        assertNotSame(0, source.getMessages(3).getCount());
    }

    @Test
    public void searchMessages() {
        Cursor messages = source.searchMessages("How is");
        assertEquals(2, messages.getCount());
    }

    @Test
    public void searchMessagesWithApostrophe() {
        Cursor messages = source.searchMessages("How's");
        assertEquals(0, messages.getCount());
    }

    @Test
    public void searchMessagesTimestamp() {
        Cursor messages = source.searchMessages(1000);
        assertEquals(5, messages.getCount());
    }

    @Test
    public void updateMessageType() {
        source.updateMessageType(1, Message.TYPE_SENT);
        Cursor messages = source.getMessages(1);
        messages.moveToLast();
        assertEquals(Message.TYPE_SENT, messages.getInt(messages.getColumnIndex(Message.COLUMN_TYPE)));
    }

    @Test
    public void insertSentMessage() {
        int initialMessageSize = source.getMessages(1).getCount();
        source.insertSentMessage("1111111", "test", MimeType.TEXT_PLAIN, RuntimeEnvironment.application);
        int newMessageSize = source.getMessages(1).getCount();

        assertEquals(1, newMessageSize - initialMessageSize);
    }

    @Test
    public void insertMessageExistingConversation() {
        int initialMessageSize = source.getMessages(1).getCount();
        int initialConversationSize = source.getConversations().getCount();
        source.insertMessage(getFakeMessage(), "1111111", RuntimeEnvironment.application);
        int newMessageSize = source.getMessages(1).getCount();
        int newConversationSize = source.getConversations().getCount();

        assertEquals(initialConversationSize, newConversationSize);
        assertEquals(1, newMessageSize - initialMessageSize);
    }

    @Test
    public void insertMessageExistingGroupConversation() {
        int initialMessageSize = source.getMessages(4).getCount();
        int initialConversationSize = source.getConversations().getCount();
        source.insertMessage(getFakeMessage(), "1111111, 3333333", RuntimeEnvironment.application);
        int newMessageSize = source.getMessages(4).getCount();
        int newConversationSize = source.getConversations().getCount();

        assertEquals(initialConversationSize, newConversationSize);
        assertEquals(1, newMessageSize - initialMessageSize);
    }

    @Test
    public void insertMessageNewConversation() {
        int initialMessageSize = source.getMessages(5).getCount();
        int initialConversationSize = source.getConversations().getCount();
        source.insertMessage(getFakeMessage(), "4444444", RuntimeEnvironment.application);
        int newMessageSize = source.getMessages(5).getCount();
        int newConversationSize = source.getConversations().getCount();

        assertEquals(1, newConversationSize - initialConversationSize);
        assertEquals(1, newMessageSize - initialMessageSize);
    }

    @Test
    public void insertMessageNewGroupConversation() {
        int initialMessageSize = source.getMessages(5).getCount();
        int initialConversationSize = source.getConversations().getCount();
        source.insertMessage(getFakeMessage(), "1111111, 2222222", RuntimeEnvironment.application);
        int newMessageSize = source.getMessages(5).getCount();
        int newConversationSize = source.getConversations().getCount();

        assertEquals(1, newConversationSize - initialConversationSize);
        assertEquals(1, newMessageSize - initialMessageSize);
    }

    @Test
    public void insertMessage() {
        int initialSize = source.getMessages(2).getCount();
        source.insertMessage(getFakeMessage(), 2);
        int newSize = source.getMessages(2).getCount();

        assertEquals(1, newSize - initialSize);

        Cursor conversation = source.getConversations();
        conversation.moveToFirst();
        assertEquals("test message", conversation
                .getString(conversation.getColumnIndex(Conversation.COLUMN_SNIPPET)));
    }

    private Message getFakeMessage() {
        Message m = new Message();
        m.conversationId = 2;
        m.type = Message.TYPE_SENT;
        m.data = "test message";
        m.timestamp = System.currentTimeMillis();
        m.mimeType = "text/plain";
        m.read = true;
        m.seen = true;
        m.from = null;
        m.color = null;
        return m;
    }

    @Test
    public void readConversation() {
        assertEquals(1, source.getUnseenMessages().getCount());
        assertEquals(2, source.getUnreadMessages().getCount());
        source.readConversation(RuntimeEnvironment.application, 3);
        assertEquals(1, source.getUnseenMessages().getCount());
        assertEquals(1, source.getUnreadMessages().getCount());
    }

    @Test
    public void seenConversation() {
        assertEquals(1, source.getUnseenMessages().getCount());
        assertEquals(2, source.getUnreadMessages().getCount());
        source.seenConversation(1);
        assertEquals(0, source.getUnseenMessages().getCount());
        assertEquals(2, source.getUnreadMessages().getCount());
    }

    @Test
    public void seenAllMessages() {
        assertEquals(1, source.getUnseenMessages().getCount());
        assertEquals(2, source.getUnreadMessages().getCount());
        source.seenAllMessages();
        assertEquals(0, source.getUnseenMessages().getCount());
        assertEquals(2, source.getUnreadMessages().getCount());
    }

    @Test
    public void insertDraft() {
        int initialSize = source.getDrafts(3).size();
        source.insertDraft(3, "test", "text/plain");
        int finalSize = source.getDrafts(3).size();

        assertEquals(1, finalSize - initialSize);
    }

    @Test
    public void getDrafts() {
        List<Draft> drafts = source.getDrafts(1);
        assertEquals(2, drafts.size());
    }

    @Test
    public void deleteDrafts() {
        assertEquals(1, source.getDrafts(2).size());
        source.deleteDrafts(2);
        assertEquals(0, source.getDrafts(2).size());
    }

    private void insertData() throws Exception {
        SQLiteDatabase database = source.getDatabase();
        FixtureLoader loader = new FixtureLoader();
        loader.loadFixturesToDatabase(database);
    }

}