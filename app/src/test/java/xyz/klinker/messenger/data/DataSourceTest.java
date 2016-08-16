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
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.robolectric.RuntimeEnvironment;

import java.util.ArrayList;
import java.util.List;

import xyz.klinker.messenger.MessengerRobolectricSuite;
import xyz.klinker.messenger.R;
import xyz.klinker.messenger.data.model.Blacklist;
import xyz.klinker.messenger.data.model.Conversation;
import xyz.klinker.messenger.data.model.Draft;
import xyz.klinker.messenger.data.model.Message;
import xyz.klinker.messenger.data.model.ScheduledMessage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DataSourceTest extends MessengerRobolectricSuite {

    private DataSource source;

    @Mock
    private SQLiteDatabase database;
    @Mock
    private DatabaseSQLiteHelper helper;
    @Mock
    private Cursor cursor;

    @Before
    public void setUp() {
        source = new DataSource(helper);
        when(helper.getWritableDatabase()).thenReturn(database);
        source.open();
    }

    @After
    public void tearDown() {
        source.close();
        verify(helper).close();
    }

    @Test
    public void realConstructor() {
        DataSource dataSource = DataSource.getInstance(RuntimeEnvironment.application);
        dataSource.open();
        dataSource.close();
    }

    @Test
    public void getDatabase() {
        assertEquals(database, source.getDatabase());
    }

    @Test
    public void clearTables() {
        source.clearTables();
        verify(database).delete("message", null, null);
        verify(database).delete("conversation", null, null);
        verify(database).delete("blacklist", null, null);
        verify(database).delete("draft", null, null);
        verify(database).delete("scheduled_message", null, null);
    }

    @Test
    public void beginTransaction() {
        source.beginTransaction();
        verify(database).beginTransaction();
    }

    @Test
    public void setTransactionSuccessful() {
        source.setTransactionSuccessful();
        verify(database).setTransactionSuccessful();
    }

    @Test
    public void endTransaction() {
        source.endTransaction();
        verify(database).endTransaction();
    }

    @Test
    public void execSql() {
        source.execSql("test sql");
        verify(database).execSQL("test sql");
    }

    @Test
    public void rawQuery() {
        source.rawQuery("test sql");
        verify(database).rawQuery("test sql", null);
    }

    @Test
    public void insertConversations() {
        source.insertConversations(
                getFakeConversations(RuntimeEnvironment.application.getResources()),
                RuntimeEnvironment.application, null);

        verify(database, times(7)).insert(eq("conversation"), eq((String) null),
                any(ContentValues.class));
    }

    @Test
    public void insertConversation() {
        source.insertConversation(new Conversation());
        verify(database).insert(eq("conversation"), eq((String) null), any(ContentValues.class));
    }

    @Test
    public void getConversations() {
        when(database.query("conversation", null, null, null, null, null,
                "pinned desc, timestamp desc")).thenReturn(cursor);

        assertEquals(cursor, source.getConversations());
    }

    @Test
    public void getPinnedConversations() {
        when(database.query("conversation", null, "pinned=1", null, null, null, "timestamp desc"))
                .thenReturn(cursor);
        assertEquals(cursor, source.getPinnedConversations());
    }

    @Test
    public void searchConversations() {
        when(database.query("conversation", null, "title LIKE '%swimmer''s%'", null, null, null,
                "timestamp desc")).thenReturn(cursor);
        assertEquals(cursor, source.searchConversations("swimmer's"));
    }

    @Test
    public void searchConversationNull() {
        assertEquals(null, source.searchConversations(null));
    }

    @Test
    public void searchConversationBlank() {
        assertEquals(null, source.searchConversations(null));
    }

    @Test
    public void deleteConversation() {
        Conversation conversation = new Conversation();
        conversation.id = 1;
        source.deleteConversation(conversation);

        verify(database).delete("conversation", "_id=?", new String[]{"1"});
        verify(database).delete("message", "conversation_id=?", new String[]{"1"});
    }

    @Test
    public void updateConversation() {
        source.updateConversation(1, true, System.currentTimeMillis(), "test", "text/plain");
        verify(database).update(eq("conversation"), any(ContentValues.class), eq("_id=?"),
                eq(new String[]{"1"}));
    }

    @Test
    public void updateConversationSettings() {
        source.updateConversationSettings(new Conversation());
        verify(database).update(eq("conversation"), any(ContentValues.class), eq("_id=?"),
                eq(new String[]{"0"}));
    }

    @Test
    public void getConversationCount() {
        when(database.query("conversation", null, null, null, null, null,
                "pinned desc, timestamp desc")).thenReturn(cursor);
        when(cursor.getCount()).thenReturn(20);

        assertEquals(20, source.getConversationCount());
    }

    @Test
    public void getMessageCount() {
        when(database.query("message", null, null, null, null, null,
                "timestamp asc")).thenReturn(cursor);
        when(cursor.getCount()).thenReturn(20);

        assertEquals(20, source.getMessageCount());
    }

    @Test
    public void getMessages() {
        when(database.query("message", null, "conversation_id=?", new String[]{"1"}, null, null,
                "timestamp asc")).thenReturn(cursor);

        assertEquals(cursor, source.getMessages(1));
    }

    @Test
    public void getMessage() {
        when(database.query("message", null, "_id=?", new String[]{"1"}, null, null, null))
                .thenReturn(cursor);
        when(cursor.moveToFirst()).thenReturn(true);
        assertNotNull(source.getMessage(1));
    }

    @Test
    public void getMediaMessages() {
        when(database.query("message", null, "conversation_id=? AND mime_type!='text/plain'",
                new String[]{"1"}, null, null, "timestamp asc")).thenReturn(cursor);
        assertNotNull(source.getMediaMessages(1));
    }

    @Test
    public void getAllMediaMessages() {
        when(database.query("message", null, "mime_type!='text/plain'", null, null, null,
                "timestamp desc LIMIT 20")).thenReturn(cursor);
        assertNotNull(source.getAllMediaMessages(20));
    }

    @Test
    public void getFirebaseMediaMessages() {
        when(database.query("message", null, "mime_type!='text/plain' AND data LIKE 'firebase %'", null,
                null, null, null)).thenReturn(cursor);
        assertEquals(cursor, source.getFirebaseMediaMessages());
    }

    @Test
    public void getAllMessages() {
        when(database.query("message", null, null, null, null, null, "timestamp asc"))
                .thenReturn(cursor);

        assertEquals(cursor, source.getMessages());
    }

    @Test
    public void searchMessages() {
        when(database.query("message", null, "data LIKE '%test%' AND mime_type='text/plain'",
                null, null, null, "timestamp desc")).thenReturn(cursor);

        assertEquals(cursor, source.searchMessages("test"));
    }

    @Test
    public void searchMessagesNullQuery() {
        assertEquals(null, source.searchMessages(null));
    }

    @Test
    public void searchMessagesBlankQuery() {
        assertEquals(null, source.searchMessages(""));
    }

    @Test
    public void searchMessagesTimestamp() {
        when(database.query("message", null, "timestamp BETWEEN 0 AND 10000", null, null, null,
                "timestamp desc")).thenReturn(cursor);

        assertEquals(cursor, source.searchMessages(5000L));
    }

    @Test
    public void updateMessageType() {
        source.updateMessageType(1, Message.TYPE_SENT);
        verify(database).update(eq("message"), any(ContentValues.class), eq("_id=?"),
                eq(new String[]{"1"}));
    }

    @Test
    public void insertMessage() {
        source.insertMessage(RuntimeEnvironment.application, new Message(), 1);

        verify(database).insert(eq("message"), eq((String) null), any(ContentValues.class));
        verify(database).update(eq("conversation"), any(ContentValues.class), eq("_id=?"),
                any(String[].class));
    }

    @Test
    public void deleteMessage() {
        source.deleteMessage(1);

        verify(database).delete("message", "_id=?", new String[]{"1"});
    }

    @Test
    public void readConversation() {
        source.readConversation(RuntimeEnvironment.application, 3);

        verify(database).update(eq("message"), any(ContentValues.class),
                eq("conversation_id=? AND (read=? OR seen=?)"), eq(new String[]{"3", "0", "0"}));
        verify(database).update(eq("conversation"), any(ContentValues.class), eq("_id=?"),
                eq(new String[]{"3"}));
    }

    @Test
    public void seenConversation() {
        source.seenConversation(1);

        verify(database).update(eq("message"), any(ContentValues.class),
                eq("conversation_id=? AND seen=0"), eq(new String[]{"1"}));
    }

    @Test
    public void seenAllMessages() {
        source.seenAllMessages();

        verify(database).update(eq("message"), any(ContentValues.class),
                eq("seen=0"), eq((String[]) null));
    }

    @Test
    public void getUnreadMessages() {
        when(database.query("message", null, "read=0", null, null, null, "timestamp desc"))
                .thenReturn(cursor);

        assertEquals(cursor, source.getUnreadMessages());
    }

    @Test
    public void getUnseenMessages() {
        when(database.query("message", null, "seen=0", null, null, null, "timestamp asc"))
                .thenReturn(cursor);

        assertEquals(cursor, source.getUnseenMessages());
    }

    @Test
    public void getAllDrafts() {
        when(database.query("draft", null, null, null, null, null, null)).thenReturn(cursor);
        assertNotNull(source.getDrafts());
    }

    @Test
    public void getDrafts() {
        when(database.query("draft", null, "conversation_id=?", new String[]{"1"}, null, null,
                null)).thenReturn(cursor);
        assertNotNull(source.getDrafts(1));
    }

    @Test
    public void insertDraft() {
        source.insertDraft(1, "test", "text/plain");
        verify(database).insert(eq("draft"), eq((String) null), any(ContentValues.class));
    }

    @Test
    public void insertDraftObject() {
        Draft draft = new Draft();
        draft.id = 1;
        draft.conversationId = 1;
        draft.data = "test";
        draft.mimeType = "text/plain";

        source.insertDraft(draft);

        ContentValues values = new ContentValues(4);
        values.put("_id", 1L);
        values.put("conversation_id", 1L);
        values.put("data", "test");
        values.put("mime_type", "text/plain");

        verify(database).insert("draft", null, values);
    }

    @Test
    public void deleteDrafts() {
        source.deleteDrafts(1);
        verify(database).delete("draft", "conversation_id=?", new String[]{"1"});
    }

    @Test
    public void getBlacklists() {
        when(database.query("blacklist", null, null, null, null, null, null)).thenReturn(cursor);
        assertEquals(cursor, source.getBlacklists());
    }

    @Test
    public void insertBlacklist() {
        source.insertBlacklist(new Blacklist());
        verify(database).insert(eq("blacklist"), eq((String) null), any(ContentValues.class));
    }

    @Test
    public void deleteBlacklist() {
        source.deleteBlacklist(1);
        verify(database).delete("blacklist", "_id=?", new String[]{"1"});
    }

    @Test
    public void getScheduledMessages() {
        when(database.query("scheduled_message", null, null, null, null, null, "timestamp asc"))
                .thenReturn(cursor);
        assertEquals(cursor, source.getScheduledMessages());
    }

    @Test
    public void insertScheduledMessage() {
        source.insertScheduledMessage(new ScheduledMessage());
        verify(database).insert(eq("scheduled_message"), eq((String) null),
                any(ContentValues.class));
    }

    @Test
    public void deleteScheduledMessage() {
        source.deleteScheduledMessage(1);
        verify(database).delete("scheduled_message", "_id=?", new String[]{"1"});
    }

    public static List<Conversation> getFakeConversations(Resources resources) {
        List<Conversation> conversations = new ArrayList<>();

        Conversation conversation = new Conversation();
        conversation.title = "Luke Klinker";
        conversation.phoneNumbers = "(515) 991-1493";
        conversation.colors.color = resources.getColor(R.color.materialIndigo);
        conversation.colors.colorDark = resources.getColor(R.color.materialIndigoDark);
        conversation.colors.colorAccent = resources.getColor(R.color.materialGreenAccent);
        conversation.pinned = true;
        conversation.read = true;
        conversation.timestamp = System.currentTimeMillis() - (1000 * 60 * 60);
        conversation.snippet = "So maybe not going to be able to get platinum huh?";
        conversation.idMatcher = "11493";
        conversation.mute = false;
        conversations.add(conversation);

        conversation = new Conversation();
        conversation.title = "Matt Swiontek";
        conversation.phoneNumbers = "(708) 928-0846";
        conversation.colors.color = resources.getColor(R.color.materialRed);
        conversation.colors.colorDark = resources.getColor(R.color.materialRedDark);
        conversation.colors.colorAccent = resources.getColor(R.color.materialBlueAccent);
        conversation.pinned = true;
        conversation.read = true;
        conversation.timestamp = System.currentTimeMillis() - (1000 * 60 * 60 * 12);
        conversation.snippet = "Whoops ya idk what happened but anysho drive safe";
        conversation.idMatcher = "80846";
        conversation.mute = false;
        conversations.add(conversation);

        conversation = new Conversation();
        conversation.title = "Kris Klinker";
        conversation.phoneNumbers = "(515) 419-6726";
        conversation.colors.color = resources.getColor(R.color.materialPink);
        conversation.colors.colorDark = resources.getColor(R.color.materialPinkDark);
        conversation.colors.colorAccent = resources.getColor(R.color.materialOrangeAccent);
        conversation.pinned = false;
        conversation.read = false;
        conversation.timestamp = System.currentTimeMillis() - (1000 * 60 * 20);
        conversation.snippet = "Will probably be there from 6:30-9, just stop by when you can!";
        conversation.idMatcher = "96726";
        conversation.mute = false;
        conversations.add(conversation);

        conversation = new Conversation();
        conversation.title = "Andrew Klinker";
        conversation.phoneNumbers = "(515) 991-8235";
        conversation.colors.color = resources.getColor(R.color.materialBlue);
        conversation.colors.colorDark = resources.getColor(R.color.materialBlueDark);
        conversation.colors.colorAccent = resources.getColor(R.color.materialRedAccent);
        conversation.pinned = false;
        conversation.read = true;
        conversation.timestamp = System.currentTimeMillis() - (1000 * 60 * 60 * 26);
        conversation.snippet = "Just finished, it was a lot of fun";
        conversation.idMatcher = "18235";
        conversation.mute = false;
        conversations.add(conversation);

        conversation = new Conversation();
        conversation.title = "Aaron Klinker";
        conversation.phoneNumbers = "(515) 556-7749";
        conversation.colors.color = resources.getColor(R.color.materialGreen);
        conversation.colors.colorDark = resources.getColor(R.color.materialGreenDark);
        conversation.colors.colorAccent = resources.getColor(R.color.materialIndigoAccent);
        conversation.pinned = false;
        conversation.read = true;
        conversation.timestamp = System.currentTimeMillis() - (1000 * 60 * 60 * 32);
        conversation.snippet = "Yeah I'll do it when I get home";
        conversation.idMatcher = "67749";
        conversation.mute = false;
        conversations.add(conversation);

        conversation = new Conversation();
        conversation.title = "Mike Klinker";
        conversation.phoneNumbers = "(515) 480-8532";
        conversation.colors.color = resources.getColor(R.color.materialBrown);
        conversation.colors.colorDark = resources.getColor(R.color.materialBrownDark);
        conversation.colors.colorAccent = resources.getColor(R.color.materialDeepOrangeAccent);
        conversation.pinned = false;
        conversation.read = true;
        conversation.timestamp = System.currentTimeMillis() - (1000 * 60 * 60 * 55);
        conversation.snippet = "Yeah so hiking around in some place called beaver meadows now.";
        conversation.idMatcher = "08532";
        conversation.mute = false;
        conversations.add(conversation);

        conversation = new Conversation();
        conversation.title = "Ben Madden";
        conversation.phoneNumbers = "(847) 609-0939";
        conversation.colors.color = resources.getColor(R.color.materialPurple);
        conversation.colors.colorDark = resources.getColor(R.color.materialPurpleDark);
        conversation.colors.colorAccent = resources.getColor(R.color.materialTealAccent);
        conversation.pinned = false;
        conversation.read = true;
        conversation.timestamp = System.currentTimeMillis() - (1000 * 60 * 60 * 78);
        conversation.snippet = "Maybe they'll run into each other on the way back... idk";
        conversation.idMatcher = "90939";
        conversation.mute = false;
        conversations.add(conversation);

        return conversations;
    }

}