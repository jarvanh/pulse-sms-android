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

package xyz.klinker.messenger.shared.data;

import android.content.ContentValues;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.robolectric.RuntimeEnvironment;

import java.util.ArrayList;
import java.util.List;

import xyz.klinker.messenger.MessengerRobolectricSuite;
import xyz.klinker.messenger.R;
import xyz.klinker.messenger.shared.data.model.Blacklist;
import xyz.klinker.messenger.shared.data.model.Contact;
import xyz.klinker.messenger.shared.data.model.Conversation;
import xyz.klinker.messenger.shared.data.model.Draft;
import xyz.klinker.messenger.shared.data.model.Message;
import xyz.klinker.messenger.shared.data.model.ScheduledMessage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class DataSourceTest extends MessengerRobolectricSuite {

    private DataSource source = DataSource.INSTANCE;
    private Context context = RuntimeEnvironment.application;

    @Mock
    private SQLiteDatabase database;
    @Mock
    private DatabaseSQLiteHelper helper;
    @Mock
    private Cursor cursor;

    @Before
    public void setUp() {
        source.set_dbHelper(helper);
        source.set_database(database);
        when(database.isOpen()).thenReturn(true);
        when(helper.getWritableDatabase()).thenReturn(database);
    }

    @After
    public void tearDown() {
        source.close(context);
        verify(helper).close();
    }

    @Test
    public void getDatabase() {
        assertEquals(database, source.get_database());
    }

    @Test
    public void clearTables() {
        source.clearTables(context);
        verify(database).delete("message", null, null);
        verify(database).delete("conversation", null, null);
        verify(database).delete("blacklist", null, null);
        verify(database).delete("draft", null, null);
        verify(database).delete("scheduled_message", null, null);
        verify(database).delete("contact", null, null);

        verifyNoMoreInteractions(database);
    }

    @Test
    public void beginTransaction() {
        source.beginTransaction(context);
        verify(database).beginTransaction();
    }

    @Test
    public void setTransactionSuccessful() {
        source.setTransactionSuccessful(context);
        verify(database).setTransactionSuccessful();
    }

    @Test
    public void endTransaction() {
        source.endTransaction(context);
        verify(database).endTransaction();
    }

    @Test
    public void execSql() {
        source.execSql(context, "test sql");
        verify(database).execSQL("test sql");
    }

    @Test
    public void rawQuery() {
        source.rawQuery(context, "test sql");
        verify(database).rawQuery("test sql", null);
    }

    @Test
    public void insertContacts() {
        source.insertContacts(context, getFakeContacts(RuntimeEnvironment.application.getResources()), null);
        verify(database, times(7)).insert(eq("contact"), eq((String) null),
                any(ContentValues.class));
    }

    @Test
    public void insertContact() {
        source.insertContact(context, new Contact());
        verify(database).insert(eq("contact"), eq((String) null), any(ContentValues.class));
    }

    @Test
    public void getContacts() {
        when(database.query("contact", null, null, null, null, null,
                "name ASC")).thenReturn(cursor);

        assertEquals(cursor, source.getContacts(context));
    }

    @Test
    public void insertConversations() {
        source.insertConversations(
                getFakeConversations(RuntimeEnvironment.application.getResources()),
                context, null);

        verify(database, times(7)).insert(eq("conversation"), eq((String) null),
                any(ContentValues.class));
    }

    @Test
    public void insertConversation() {
        source.insertConversation(context, new Conversation());
        verify(database).insert(eq("conversation"), eq((String) null), any(ContentValues.class));
    }

    @Test
    public void getConversations() {
        when(database.query("conversation", null, "archive=?", new String[]{"0"}, null, null,
                "pinned desc, timestamp desc")).thenReturn(cursor);

        assertEquals(cursor, source.getUnarchivedConversations(context));
    }

    @Test
    public void getUnreadConversations() {
        when(database.query("conversation", null, "read=0 and archive=0", null, null, null,
                "timestamp desc")).thenReturn(cursor);

        assertEquals(cursor, source.getUnreadConversations(context));
    }

    @Test
    public void getUnreadConversationsCount() {
        when(cursor.getCount()).thenReturn(10);
        when(database.query("conversation", null, "read=0 and archive=0", null, null, null,
                "timestamp desc")).thenReturn(cursor);

        //assertEquals(10, source.getUnreadConversationsCount(context));
    }

    @Test
    public void findConversationByNumbers() {
        source.findConversationId(context, "515");
        verify(database).query("conversation", new String[]{"_id", "id_matcher"},
                "id_matcher=? OR id_matcher=? OR id_matcher=?", new String[]{"515", "515", "515"}, null, null, null);
    }

    @Test
    public void findConversationByTitle() {
        source.findConversationIdByTitle(context, "test");
        verify(database).query("conversation", new String[]{"_id", "title"},
                "title=?", new String[]{"test"}, null, null, null);
    }

    @Test
    public void updateContact() {
        source.updateContact(context, "515", "Test", 1, 2, 3, 4);
        verify(database).update(eq("contact"), any(ContentValues.class), eq("phone_number=?"),
                eq(new String[]{"515"}));
    }

    @Test
    public void deleteContact() {
        source.deleteContact(context, "515");
        verify(database).delete("contact", "phone_number=?", new String[]{"515"});
    }

    @Test
    public void deleteMultipleContactsById() {
        source.deleteContacts(context, new String[] { "1", "2" });
        verify(database).delete("contact", "_id=? OR _id=?", new String[]{ "1", "2" });
    }

    @Test
    public void deleteSingleContactById() {
        source.deleteContacts(context, new String[] { "1" });
        verify(database).delete("contact", "_id=?", new String[]{"1"});
    }

    @Test
    public void getPinnedConversations() {
        when(database.query("conversation", null, "pinned=1", null, null, null, "timestamp desc"))
                .thenReturn(cursor);
        assertEquals(cursor, source.getPinnedConversations(context));
    }

    @Test
    public void getArchivedConversations() {
        when(database.query("conversation", null, "archive=1", null, null, null, "timestamp desc"))
                .thenReturn(cursor);
        assertEquals(cursor, source.getArchivedConversations(context));
    }

    @Test
    public void searchConversations() {
        when(database.query("conversation", null, "title LIKE '%swimmer''s%'", null, null, null,
                "timestamp desc")).thenReturn(cursor);
        assertEquals(cursor, source.searchConversations(context,"swimmer's"));
    }

    @Test
    public void searchConversationNull() {
        assertEquals(null, source.searchConversations(context, null));
    }

    @Test
    public void searchConversationBlank() {
        assertEquals(null, source.searchConversations(context, null));
    }

    @Test
    public void deleteConversation() {
        Conversation conversation = new Conversation();
        conversation.id = 1;
        source.deleteConversation(context, conversation);

        verify(database).delete("conversation", "_id=?", new String[]{"1"});
        verify(database).delete("message", "conversation_id=?", new String[]{"1"});
    }

    @Test
    public void archiveConversation() {
        source.archiveConversation(context, 1);
        verify(database).update(eq("conversation"), any(ContentValues.class), eq("_id=?"),
                eq(new String[]{"1"}));
    }

    @Test
    public void updateConversation() {
        source.updateConversation(context, 1, true, System.currentTimeMillis(), "test", "text/plain", false);
        verify(database).update(eq("conversation"), any(ContentValues.class), eq("_id=?"),
                eq(new String[]{"1"}));
    }

    @Test
    public void updateConversationSettings() {
        source.updateConversationSettings(context, new Conversation());
        verify(database).update(eq("conversation"), any(ContentValues.class), eq("_id=?"),
                eq(new String[]{"0"}));
    }

    @Test
    public void updateConversationTitle() {
        source.updateConversationTitle(context, 0L, "test");
        verify(database).update(eq("conversation"), any(ContentValues.class), eq("_id=? AND title <> ?"),
                eq(new String[]{"0", "test"}));
    }

    @Test
    public void getConversationCount() {
        when(database.query("conversation", null, null, null, null, null,
                "pinned desc, timestamp desc")).thenReturn(cursor);
        when(cursor.getCount()).thenReturn(20);

        assertEquals(20, source.getConversationCount(context));
    }

    @Test
    public void getMessageCount() {
        when(database.query("message", null, null, null, null, null,
                "timestamp asc")).thenReturn(cursor);
        when(cursor.getCount()).thenReturn(20);

        assertEquals(20, source.getMessageCount(context));
    }

    @Test
    public void getMessages() {
        when(database.query("message", null, "conversation_id=?", new String[]{"1"}, null, null,
                "timestamp asc")).thenReturn(cursor);

        assertEquals(cursor, source.getMessages(context, 1L));
    }

    @Test
    public void getMessage() {
        when(database.query("message", null, "_id=?", new String[]{"1"}, null, null, null))
                .thenReturn(cursor);
        when(cursor.moveToFirst()).thenReturn(true);
        assertNotNull(source.getMessage(context, 1));
    }

    @Test
    public void getLatestMessage() {
        when(database.query("message", null, null, null, null, null, "timestamp desc", "1"))
                .thenReturn(cursor);
        when(cursor.moveToFirst()).thenReturn(true);
        assertNotNull(source.getLatestMessage(context));
    }

    @Test
    public void getMediaMessages() {
        when(database.query("message", null, "conversation_id=? AND mime_type!='text/plain'",
                new String[]{"1"}, null, null, "timestamp asc")).thenReturn(cursor);
        assertNotNull(source.getMediaMessages(context,1));
    }

    @Test
    public void getAllMediaMessages() {
        when(database.query("message", null, "mime_type!='text/plain'", null, null, null,
                "timestamp desc LIMIT 20")).thenReturn(cursor);
        assertNotNull(source.getAllMediaMessages(context,20));
    }

    @Test
    public void getFirebaseMediaMessages() {
        when(database.query("message", null, "mime_type!='text/plain' AND data LIKE 'firebase %'", null,
                null, null, null)).thenReturn(cursor);
        assertEquals(cursor, source.getFirebaseMediaMessages(context));
    }

    @Test
    public void getAllMessages() {
        when(database.query("message", null, null, null, null, null, "timestamp asc"))
                .thenReturn(cursor);

        assertEquals(cursor, source.getMessages(context));
    }

    @Test
    public void searchMessages() {
        when(database.query("message m left outer join conversation c on m.conversation_id = c._id",
                new String[] { "m._id as _id", "c._id as conversation_id", "m.type as type", "m.data as data", "m.timestamp as timestamp", "m.mime_type as mime_type", "m.read as read", "m.message_from as message_from", "m.color as color", "c.title as convo_title" },
                "data LIKE '%test%' AND mime_type='text/plain'",
                null, null, null, "timestamp desc")).thenReturn(cursor);

        assertEquals(cursor, source.searchMessages(context, "test"));
    }

    @Test
    public void searchMessagesNullQuery() {
        assertEquals(null, source.searchMessages(context, null));
    }

    @Test
    public void searchMessagesBlankQuery() {
        assertEquals(null, source.searchMessages(context, ""));
    }

    @Test
    public void searchMessagesTimestamp() {
        when(database.query("message", null, "timestamp BETWEEN 0 AND 20000", null, null, null,
                "timestamp desc")).thenReturn(cursor);

        assertEquals(cursor, source.searchMessages(context, 10000L));
    }

    @Test
    public void updateMessageType() {
        source.updateMessageType(context, 1, Message.TYPE_SENT);
        verify(database).update(eq("message"), any(ContentValues.class), eq("_id=? AND type<>?"),
                eq(new String[]{"1", "0"}));
    }

    @Test
    public void insertMessage() {
        source.insertMessage(context, new Message(), 1);

        verify(database).insert(eq("message"), eq((String) null), any(ContentValues.class));
        verify(database).update(eq("conversation"), any(ContentValues.class), eq("_id=?"),
                any(String[].class));
    }

    @Test
    public void deleteMessage() {
        source.deleteMessage(context, 1);

        verify(database).delete("message", "_id=?", new String[]{"1"});
    }

    @Test
    public void cleanupMessages() {
        source.cleanupOldMessages(context, 1);

        verify(database).delete("message", "timestamp<?", new String[]{"1"});
        verify(database).delete("conversation", "timestamp<?", new String[]{"1"});
    }

    @Test
    public void readConversation() {
        source.readConversation(context, 3);

        verify(database).update(eq("message"), any(ContentValues.class),
                eq("conversation_id=?"), eq(new String[]{"3"}));
        verify(database).update(eq("conversation"), any(ContentValues.class), eq("_id=?"),
                eq(new String[]{"3"}));
    }

    @Test
    public void seenConversation() {
        source.seenConversation(context, 1);

        verify(database).update(eq("message"), any(ContentValues.class),
                eq("conversation_id=? AND seen=0"), eq(new String[]{"1"}));
    }

    @Test
    public void seenConversations() {
        source.seenConversations(context);

        verify(database).update(eq("message"), any(ContentValues.class),
                eq("seen=0"), eq((String[]) null));
    }

    @Test
    public void seenAllMessages() {
        source.seenAllMessages(context);

        verify(database).update(eq("message"), any(ContentValues.class),
                eq("seen=0"), eq((String[]) null));
    }

    @Test
    public void getUnreadMessages() {
        when(database.query("message", null, "read=0", null, null, null, "timestamp desc"))
                .thenReturn(cursor);

        assertEquals(cursor, source.getUnreadMessages(context));
    }

    @Test
    public void getUnseenMessages() {
        when(database.query("message", null, "seen=0", null, null, null, "timestamp asc"))
                .thenReturn(cursor);

        assertEquals(cursor, source.getUnseenMessages(context));
    }

    @Test
    public void getAllDrafts() {
        when(database.query("draft", null, null, null, null, null, null)).thenReturn(cursor);
        assertNotNull(source.getDrafts(context));
    }

    @Test
    public void getDrafts() {
        when(database.query("draft", null, "conversation_id=?", new String[]{"1"}, null, null,
                null)).thenReturn(cursor);
        assertNotNull(source.getDrafts(context, 1));
    }

    @Test
    public void insertDraft() {
        source.insertDraft(context, 1, "test", "text/plain");
        verify(database).insert(eq("draft"), eq((String) null), any(ContentValues.class));
    }

    @Test
    public void insertDraftObject() {
        Draft draft = new Draft();
        draft.id = 1;
        draft.conversationId = 1;
        draft.data = "test";
        draft.mimeType = "text/plain";

        source.insertDraft(context, draft);

        ContentValues values = new ContentValues(4);
        values.put("_id", 1L);
        values.put("conversation_id", 1L);
        values.put("data", "test");
        values.put("mime_type", "text/plain");

        verify(database).insert("draft", null, values);
    }

    @Test
    public void deleteDrafts() {
        source.deleteDrafts(context, 1);
        verify(database).delete("draft", "conversation_id=?", new String[]{"1"});
    }

    @Test
    public void getBlacklists() {
        when(database.query("blacklist", null, null, null, null, null, null)).thenReturn(cursor);
        assertEquals(cursor, source.getBlacklists(context));
    }

    @Test
    public void insertBlacklist() {
        source.insertBlacklist(context, new Blacklist());
        verify(database).insert(eq("blacklist"), eq((String) null), any(ContentValues.class));
    }

    @Test
    public void deleteBlacklist() {
        source.deleteBlacklist(context, 1);
        verify(database).delete("blacklist", "_id=?", new String[]{"1"});
    }

    @Test
    public void getScheduledMessages() {
        when(database.query("scheduled_message", null, null, null, null, null, "timestamp asc"))
                .thenReturn(cursor);
        assertEquals(cursor, source.getScheduledMessages(context));
    }

    @Test
    public void insertScheduledMessage() {
        source.insertScheduledMessage(context, new ScheduledMessage());
        verify(database).insert(eq("scheduled_message"), eq((String) null),
                any(ContentValues.class));
    }

    @Test
    public void deleteScheduledMessage() {
        source.deleteScheduledMessage(context, 1);
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
        conversation.privateNotifications = false;
        conversation.ledColor = Color.WHITE;
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
        conversation.privateNotifications = false;
        conversation.ledColor = Color.WHITE;
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
        conversation.privateNotifications = false;
        conversation.ledColor = Color.WHITE;
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
        conversation.privateNotifications = false;
        conversation.ledColor = Color.WHITE;
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
        conversation.privateNotifications = false;
        conversation.ledColor = Color.WHITE;
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
        conversation.privateNotifications = false;
        conversation.ledColor = Color.WHITE;
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
        conversation.privateNotifications = false;
        conversation.ledColor = Color.WHITE;
        conversations.add(conversation);

        return conversations;
    }

    public static List<Contact> getFakeContacts(Resources resources) {
        List<Contact> contacts = new ArrayList<>();

        Contact contact = new Contact();
        contact.name = "Luke Klinker";
        contact.phoneNumber = "(515) 991-1493";
        contact.colors.color = resources.getColor(R.color.materialIndigo);
        contact.colors.colorDark = resources.getColor(R.color.materialIndigoDark);
        contact.colors.colorLight = resources.getColor(R.color.materialIndigoLight);
        contact.colors.colorAccent = resources.getColor(R.color.materialGreenAccent);
        contacts.add(contact);

        contact = new Contact();
        contact.name = "Matt Swiontek";
        contact.phoneNumber = "(708) 928-0846";
        contact.colors.color = resources.getColor(R.color.materialRed);
        contact.colors.colorDark = resources.getColor(R.color.materialRedDark);
        contact.colors.colorLight = resources.getColor(R.color.materialRedLight);
        contact.colors.colorAccent = resources.getColor(R.color.materialBlueAccent);
        contacts.add(contact);

        contact = new Contact();
        contact.name = "Kris Klinker";
        contact.phoneNumber = "(515) 419-6726";
        contact.colors.color = resources.getColor(R.color.materialPink);
        contact.colors.colorDark = resources.getColor(R.color.materialPinkDark);
        contact.colors.colorLight = resources.getColor(R.color.materialPinkLight);
        contact.colors.colorAccent = resources.getColor(R.color.materialOrangeAccent);
        contacts.add(contact);

        contact = new Contact();
        contact.name = "Andrew Klinker";
        contact.phoneNumber = "(515) 991-8235";
        contact.colors.color = resources.getColor(R.color.materialBlue);
        contact.colors.colorDark = resources.getColor(R.color.materialBlueDark);
        contact.colors.colorLight = resources.getColor(R.color.materialBlueLight);
        contact.colors.colorAccent = resources.getColor(R.color.materialRedAccent);
        contacts.add(contact);

        contact = new Contact();
        contact.name = "Aaron Klinker";
        contact.phoneNumber = "(515) 556-7749";
        contact.colors.color = resources.getColor(R.color.materialGreen);
        contact.colors.colorDark = resources.getColor(R.color.materialGreenDark);
        contact.colors.colorLight = resources.getColor(R.color.materialGreenLight);
        contact.colors.colorAccent = resources.getColor(R.color.materialIndigoAccent);
        contacts.add(contact);

        contact = new Contact();
        contact.name = "Mike Klinker";
        contact.phoneNumber = "(515) 480-8532";
        contact.colors.color = resources.getColor(R.color.materialBrown);
        contact.colors.colorDark = resources.getColor(R.color.materialBrownDark);
        contact.colors.colorLight = resources.getColor(R.color.materialBrownLight);
        contact.colors.colorAccent = resources.getColor(R.color.materialDeepOrangeAccent);
        contacts.add(contact);

        contact = new Contact();
        contact.name = "Ben Madden";
        contact.phoneNumber = "(847) 609-0939";
        contact.colors.color = resources.getColor(R.color.materialPurple);
        contact.colors.colorDark = resources.getColor(R.color.materialPurpleDark);
        contact.colors.colorLight = resources.getColor(R.color.materialPurpleLight);
        contact.colors.colorAccent = resources.getColor(R.color.materialTealAccent);
        contacts.add(contact);

        return contacts;
    }

}