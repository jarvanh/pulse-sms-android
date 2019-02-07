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

package xyz.klinker.messenger.shared.data;

import android.content.ContentValues;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mock;
import org.robolectric.RuntimeEnvironment;

import java.util.ArrayList;
import java.util.List;

import xyz.klinker.messenger.MessengerRobolectricSuite;
import xyz.klinker.messenger.encryption.EncryptionUtils;
import xyz.klinker.messenger.shared.data.model.AutoReply;
import xyz.klinker.messenger.shared.data.model.Blacklist;
import xyz.klinker.messenger.shared.data.model.Contact;
import xyz.klinker.messenger.shared.data.model.Conversation;
import xyz.klinker.messenger.shared.data.model.Draft;
import xyz.klinker.messenger.shared.data.model.Folder;
import xyz.klinker.messenger.shared.data.model.Message;
import xyz.klinker.messenger.shared.data.model.RetryableRequest;
import xyz.klinker.messenger.shared.data.model.ScheduledMessage;
import xyz.klinker.messenger.shared.data.model.Template;
import xyz.klinker.messenger.shared.util.TimeUtils;

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
    private EncryptionUtils encryption;
    @Mock
    private Cursor cursor;

    @Before
    public void setUp() {
        source.set_dbHelper(helper);
        source.set_database(database);
        source.set_encryptor(encryption);
        source.set_accountId("1234");
        source.set_androidDeviceId("1234");

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
        source.rawQuery(context, "select * from conversation");
        verify(database).rawQuery("select * from conversation", null);
    }

    @Test
    public void insertContacts() {
        source.insertContacts(context, getFakeContacts(context.getResources()), null, false);
        verify(database, times(7)).insert(eq("contact"), eq((String) null),
                any(ContentValues.class));
    }

    @Test
    public void insertContact() {
        Contact contact = new Contact();
        contact.setPhoneNumber("1234");

        source.insertContact(context, contact, false);
        verify(database).insert(eq("contact"), eq((String) null), any(ContentValues.class));
    }

    @Test
    public void getContacts() {
        when(database.query("contact", null, null, null, null, null,
                "name ASC")).thenReturn(cursor);

        assertEquals(cursor, source.getContacts(context));
    }

    @Test
    public void deleteAllContacts() {
        when(database.delete("contact", null, null)).thenReturn(10);
        assertEquals(10, source.deleteAllContacts(context));
    }

    @Test
    @Ignore
    // TODO: this fails after some changes to the import process because it isn't returning any messages from the conversation
    public void insertConversations() {
        source.insertConversations(getFakeConversations(context.getResources()), context, null);
        verify(database, times(7)).insert(eq("conversation"), eq((String) null),
                any(ContentValues.class));
    }

    @Test
    public void insertConversation() {
        source.insertConversation(context, new Conversation(), false);
        verify(database).insert(eq("conversation"), eq((String) null), any(ContentValues.class));
    }

    @Test
    public void getConversations() {
        when(database.query("conversation", null, "archive=? AND private_notifications=?", new String[]{"0", "0"}, null, null,
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
        when(database.query("conversation", new String[]{"_id", "id_matcher"},
                "id_matcher=? OR id_matcher=? OR id_matcher=? OR id_matcher=? OR id_matcher=? OR id_matcher=?", new String[]{"515", "515", "515", "515", "515", "515"}, null, null, null))
                .thenReturn(cursor);
        when(cursor.moveToFirst()).thenReturn(true);
        when(cursor.getLong(0)).thenReturn(1001L);

        long convoId = source.findConversationId(context, "515");
        assertEquals(1001L, convoId);
    }

    @Test
    public void findConversationByTitle() {
        when(database.query("conversation", new String[]{"_id", "title"},
                "title=?", new String[]{"test"}, null, null, null))
                .thenReturn(cursor);
        when(cursor.moveToFirst()).thenReturn(true);
        when(cursor.getLong(0)).thenReturn(1001L);

        long convoId = source.findConversationIdByTitle(context, "test");
        assertEquals(1001L, convoId);
    }

    @Test
    public void updateContact() {
        source.updateContact(context, 1, "515", "Test", 1, 1, 2, 3, 4, false);
        verify(database).update(eq("contact"), any(ContentValues.class), eq("phone_number=?"),
                eq(new String[]{"515"}));
    }

    @Test
    public void deleteContact() {
        source.deleteContact(context, 1, "515", false);
        verify(database).delete("contact", "phone_number=?", new String[]{"515"});
    }

    @Test
    public void deleteMultipleContactsById() {
        source.deleteContacts(context, new String[]{"1", "2"}, false);
        verify(database).delete("contact", "_id=? OR _id=?", new String[]{"1", "2"});
    }

    @Test
    public void deleteSingleContactById() {
        source.deleteContacts(context, new String[]{"1"});
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
        when(database.query("conversation", null, "archive=1 AND private_notifications=0", null, null, null, "timestamp desc"))
                .thenReturn(cursor);
        assertEquals(cursor, source.getArchivedConversations(context));
    }

    @Test
    public void getPrivateConversations() {
        when(database.query("conversation", null, "private_notifications=1", null, null, null, "timestamp desc"))
                .thenReturn(cursor);
        assertEquals(cursor, source.getPrivateConversations(context));
    }

    @Test
    public void searchConversations() {
        when(database.query("conversation", null, "(title LIKE '%swimmer''s%' OR phone_numbers LIKE '%swimmer''s%') AND private_notifications=0", null, null, null,
                "timestamp desc")).thenReturn(cursor);
        assertEquals(cursor, source.searchConversations(context, "swimmer's"));
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
        conversation.setId(1);
        source.deleteConversation(context, conversation, false);

        verify(database).delete("conversation", "_id=?", new String[]{"1"});
        verify(database).delete("message", "conversation_id=?", new String[]{"1"});
    }

    @Test
    public void archiveConversation() {
        source.archiveConversation(context, 1, false);
        verify(database).update(eq("conversation"), any(ContentValues.class), eq("_id=?"),
                eq(new String[]{"1"}));
    }

    @Test
    public void addConversationToFolder() {
        source.addConversationToFolder(context, 1, 2, false);
        verify(database).update(eq("conversation"), any(ContentValues.class), eq("_id=?"),
                eq(new String[]{"1"}));
    }

    @Test
    public void removeConversationFromFolder() {
        source.removeConversationFromFolder(context, 1, false);
        verify(database).update(eq("conversation"), any(ContentValues.class), eq("_id=?"),
                eq(new String[]{"1"}));
    }

    @Test
    public void updateConversation() {
        source.updateConversation(context, 1, true, TimeUtils.INSTANCE.getNow(), "test", "text/plain", false, false);
        verify(database).update(eq("conversation"), any(ContentValues.class), eq("_id=?"),
                eq(new String[]{"1"}));
    }

    @Test
    public void updateConversationSettings() {
        source.updateConversationSettings(context, new Conversation(), false);
        verify(database).update(eq("conversation"), any(ContentValues.class), eq("_id=?"),
                eq(new String[]{"0"}));
    }

    @Test
    public void updateConversationTitle() {
        source.updateConversationTitle(context, 0L, "test", false);
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
        when(database.query("message", null, "conversation_id=? AND (mime_type LIKE 'image/%' OR mime_type LIKE 'video/%' OR mime_type LIKE 'audio/%') AND data NOT LIKE 'firebase %'",
                new String[]{"1"}, null, null, "timestamp asc")).thenReturn(cursor);
        assertNotNull(source.getMediaMessages(context, 1));
    }

    @Test
    public void getAllMediaMessages() {
        when(database.query("message", null, "mime_type!=? AND mime_type!=? AND mime_type!=? AND mime_type!=? AND mime_type!=?", new String[]{"text/plain", "media/web", "media/youtube-v2", "media/twitter", "media/map"}, null, null,
                "timestamp desc LIMIT 20")).thenReturn(cursor);
        assertEquals(cursor, source.getAllMediaMessages(context, 20));
    }

    @Test
    public void getFirebaseMediaMessages() {
        when(database.query("message", null, "mime_type!='text/plain' AND data LIKE 'firebase %'", null,
                null, null, "timestamp desc")).thenReturn(cursor);
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
                new String[]{"m._id as _id", "c._id as conversation_id", "m.type as type", "m.data as data", "m.timestamp as timestamp", "m.mime_type as mime_type", "m.read as read", "m.message_from as message_from", "m.color as color", "c.title as convo_title", "c.private_notifications as private_notifications"},
                "data LIKE '%test%' AND mime_type='text/plain' AND private_notifications=0",
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
        source.updateMessageType(context, 1, Message.TYPE_SENT, false);
        verify(database).update(eq("message"), any(ContentValues.class), eq("_id=? AND type<>? AND type<>?"),
                eq(new String[]{"1", "0", "4"}));
    }

    @Test
    public void updateMessageTimestamp() {
        source.updateMessageTimestamp(context, 1, 202L, false);
        verify(database).update(eq("message"), any(ContentValues.class), eq("_id=?"),
                eq(new String[] { "1" }));
    }

    @Test
    public void insertMessage() {
        Message message = new Message();
        message.setData("test");

        source.insertMessage(context, message, 1, false, false);

        verify(database).insert(eq("message"), eq((String) null), any(ContentValues.class));
        verify(database).update(eq("conversation"), any(ContentValues.class), eq("_id=?"),
                any(String[].class));
    }

    @Test
    public void deleteMessage() {
        source.deleteMessage(context, 1, false);

        verify(database).delete("message", "_id=?", new String[]{"1"});
    }

    @Test
    public void cleanupMessages() {
        source.cleanupOldMessages(context, 1, false);

        verify(database).delete("message", "timestamp<?", new String[]{"1"});
        verify(database).delete("conversation", "timestamp<?", new String[]{"1"});
    }

    @Test
    public void cleanupConversationMessages() {
        source.cleanupOldMessagesInConversation(context, 1, 1, false);

        verify(database).delete("message", "timestamp<? AND conversation_id=?", new String[]{"1", "1"});
    }

    @Test
    public void readConversation() {
        source.readConversation(context, 3, false);

        verify(database).update(eq("message"), any(ContentValues.class),
                eq("conversation_id=?"), eq(new String[]{"3"}));
        verify(database).update(eq("conversation"), any(ContentValues.class), eq("_id=?"),
                eq(new String[]{"3"}));
    }

    @Test
    public void unreadConversation() {
        source.markConversationAsUnread(context, 3, false);
        verify(database).update(eq("conversation"), any(ContentValues.class), eq("_id=?"),
                eq(new String[]{"3"}));
    }

    @Test
    public void seenConversation() {
        source.seenConversation(context, 1, false);

        verify(database).update(eq("message"), any(ContentValues.class),
                eq("conversation_id=? AND seen=0"), eq(new String[]{"1"}));
    }

    @Test
    public void seenConversations() {
        source.seenConversations(context, false);

        verify(database).update(eq("message"), any(ContentValues.class),
                eq("seen=0"), eq((String[]) null));
    }

    @Test
    public void seenAllMessages() {
        source.seenAllMessages(context, false);

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
        source.insertDraft(context, 1, "test", "text/plain", false);
        verify(database).insert(eq("draft"), eq((String) null), any(ContentValues.class));
    }

    @Test
    public void insertDraftObject() {
        Draft draft = new Draft();
        draft.setId(1);
        draft.setConversationId(1);
        draft.setData("test");
        draft.setMimeType("text/plain");

        source.insertDraft(context, draft, false);

        ContentValues values = new ContentValues(4);
        values.put("_id", 1L);
        values.put("conversation_id", 1L);
        values.put("data", "test");
        values.put("mime_type", "text/plain");

        verify(database).insert("draft", null, values);
    }

    @Test
    public void deleteDrafts() {
        source.deleteDrafts(context, 1, false);
        verify(database).delete("draft", "conversation_id=?", new String[]{"1"});
    }

    @Test
    public void getBlacklists() {
        when(database.query("blacklist", null, null, null, null, null, "phrase asc, phone_number asc")).thenReturn(cursor);
        assertEquals(cursor, source.getBlacklists(context));
    }

    @Test
    public void insertBlacklist() {
        source.insertBlacklist(context, new Blacklist(), false);
        verify(database).insert(eq("blacklist"), eq((String) null), any(ContentValues.class));
    }

    @Test
    public void deleteBlacklist() {
        source.deleteBlacklist(context, 1, false);
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
        source.insertScheduledMessage(context, new ScheduledMessage(), false);
        verify(database).insert(eq("scheduled_message"), eq((String) null),
                any(ContentValues.class));
    }

    @Test
    public void deleteScheduledMessage() {
        source.deleteScheduledMessage(context, 1, false);
        verify(database).delete("scheduled_message", "_id=?", new String[]{"1"});
    }

    @Test
    public void getTemplates() {
        when(database.query("template", null, null, null, null, null, "text asc"))
                .thenReturn(cursor);
        assertEquals(cursor, source.getTemplates(context));
    }

    @Test
    public void insertTemplate() {
        source.insertTemplate(context, new Template(), false);
        verify(database).insert(eq("template"), eq((String) null),
                any(ContentValues.class));
    }

    @Test
    public void deleteTemplate() {
        source.deleteTemplate(context, 1, false);
        verify(database).delete("template", "_id=?", new String[]{"1"});
    }

    @Test
    public void updateTemplate() {
        Template template = new Template();
        template.setId(1);
        template.setText("test text");

        source.updateTemplate(context, template, false);
        verify(database).update(eq("template"), any(ContentValues.class), eq("_id=?"),
                eq(new String[]{"1"}));
    }

    @Test
    public void getAutoReplies() {
        when(database.query("auto_reply", null, null, null, null, null, "type asc"))
                .thenReturn(cursor);
        assertEquals(cursor, source.getAutoReplies(context));
    }

    @Test
    public void insertAutoReply() {
        source.insertAutoReply(context, new AutoReply(), false);
        verify(database).insert(eq("auto_reply"), eq((String) null),
                any(ContentValues.class));
    }

    @Test
    public void deleteAutoReply() {
        source.deleteAutoReply(context, 1, false);
        verify(database).delete("auto_reply", "_id=?", new String[]{"1"});
    }

    @Test
    public void updateAutoReply() {
        AutoReply reply = new AutoReply();
        reply.setId(1);
        reply.setResponse("test text");

        source.updateAutoReply(context, reply, false);
        verify(database).update(eq("auto_reply"), any(ContentValues.class), eq("_id=?"),
                eq(new String[]{"1"}));
    }

    @Test
    public void getFolders() {
        when(database.query("folder", null, null, null, null, null, "name asc"))
                .thenReturn(cursor);
        assertEquals(cursor, source.getFolders(context));
    }

    @Test
    public void insertFolder() {
        source.insertFolder(context, new Folder(), false);
        verify(database).insert(eq("folder"), eq((String) null),
                any(ContentValues.class));
    }

    @Test
    public void deleteFolder() {
        source.deleteFolder(context, 1, false);
        verify(database).delete("folder", "_id=?", new String[]{"1"});
    }

    @Test
    public void updateFolder() {
        Folder folder = new Folder();
        folder.setId(1);
        folder.setName("edit folder");

        source.updateFolder(context, folder, false);
        verify(database).update(eq("folder"), any(ContentValues.class), eq("_id=?"),
                eq(new String[]{"1"}));
    }

    @Test
    public void getRetryableRequests() {
        when(database.query("retryable_request", null, null, null, null, null, "error_timestamp asc"))
                .thenReturn(cursor);
        assertEquals(cursor, source.getRetryableRequests(context));
    }

    @Test
    public void insertRetryableRequest() {
        source.insertRetryableRequest(context, new RetryableRequest());
        verify(database).insert(eq("retryable_request"), eq((String) null),
                any(ContentValues.class));
    }

    @Test
    public void deleteRetryableRequest() {
        source.deleteRetryableRequest(context, 1);
        verify(database).delete("retryable_request", "_id=?", new String[]{"1"});
    }

    public static List<Conversation> getFakeConversations(Resources resources) {
        List<Conversation> conversations = new ArrayList<>();

        Conversation conversation = new Conversation();
        conversation.setTitle("Luke Klinker");
        conversation.setPhoneNumbers("(515) 991-1493");
//        conversation.colors.color = resources.getColor(R.color.materialIndigo);
//        conversation.colors.colorDark = resources.getColor(R.color.materialIndigoDark);
//        conversation.colors.colorAccent = resources.getColor(R.color.materialGreenAccent);
        conversation.setPinned(true);
        conversation.setRead(true);
        conversation.setTimestamp(TimeUtils.INSTANCE.getNow() - (1000 * 60 * 60));
        conversation.setSnippet("So maybe not going to be able to get platinum huh?");
        conversation.setIdMatcher("11493");
        conversation.setMute(false);
        conversation.setPrivate(false);
        conversation.setLedColor(Color.WHITE);
        conversations.add(conversation);

        conversation = new Conversation();
        conversation.setTitle("Matt Swiontek");
        conversation.setPhoneNumbers("(708) 928-0846");
//        conversation.colors.color = resources.getColor(R.color.materialRed);
//        conversation.colors.colorDark = resources.getColor(R.color.materialRedDark);
//        conversation.colors.colorAccent = resources.getColor(R.color.materialBlueAccent);
        conversation.setPinned(true);
        conversation.setRead(true);
        conversation.setTimestamp(TimeUtils.INSTANCE.getNow() - (1000 * 60 * 60 * 12));
        conversation.setSnippet("Whoops ya idk what happened but anysho drive safe");
        conversation.setIdMatcher("80846");
        conversation.setMute(false);
        conversation.setPrivate(false);
        conversation.setLedColor(Color.WHITE);
        conversations.add(conversation);

        conversation = new Conversation();
        conversation.setTitle("Kris Klinker");
        conversation.setPhoneNumbers("(515) 419-6726");
//        conversation.colors.color = resources.getColor(R.color.materialPink);
//        conversation.colors.colorDark = resources.getColor(R.color.materialPinkDark);
//        conversation.colors.colorAccent = resources.getColor(R.color.materialOrangeAccent);
        conversation.setPinned(false);
        conversation.setRead(false);
        conversation.setTimestamp(TimeUtils.INSTANCE.getNow() - (1000 * 60 * 20));
        conversation.setSnippet("Will probably be there from 6:30-9, just stop by when you can!");
        conversation.setIdMatcher("96726");
        conversation.setMute(false);
        conversation.setPrivate(false);
        conversation.setLedColor(Color.WHITE);
        conversations.add(conversation);

        conversation = new Conversation();
        conversation.setTitle("Andrew Klinker");
        conversation.setPhoneNumbers("(515) 991-8235");
//        conversation.colors.color = resources.getColor(R.color.materialBlue);
//        conversation.colors.colorDark = resources.getColor(R.color.materialBlueDark);
//        conversation.colors.colorAccent = resources.getColor(R.color.materialRedAccent);
        conversation.setPinned(false);
        conversation.setRead(true);
        conversation.setTimestamp(TimeUtils.INSTANCE.getNow() - (1000 * 60 * 60 * 26));
        conversation.setSnippet("Just finished, it was a lot of fun");
        conversation.setIdMatcher("18235");
        conversation.setMute(false);
        conversation.setPrivate(false);
        conversation.setLedColor(Color.WHITE);
        conversations.add(conversation);

        conversation = new Conversation();
        conversation.setTitle("Aaron Klinker");
        conversation.setPhoneNumbers("(515) 556-7749");
//        conversation.colors.color = resources.getColor(R.color.materialGreen);
//        conversation.colors.colorDark = resources.getColor(R.color.materialGreenDark);
//        conversation.colors.colorAccent = resources.getColor(R.color.materialIndigoAccent);
        conversation.setPinned(false);
        conversation.setRead(true);
        conversation.setTimestamp(TimeUtils.INSTANCE.getNow() - (1000 * 60 * 60 * 32));
        conversation.setSnippet("Yeah I'll do it when I get home");
        conversation.setIdMatcher("67749");
        conversation.setMute(false);
        conversation.setPrivate(false);
        conversation.setLedColor(Color.WHITE);
        conversations.add(conversation);

        conversation = new Conversation();
        conversation.setTitle("Mike Klinker");
        conversation.setPhoneNumbers("(515) 480-8532");
//        conversation.colors.color = resources.getColor(R.color.materialBrown);
//        conversation.colors.colorDark = resources.getColor(R.color.materialBrownDark);
//        conversation.colors.colorAccent = resources.getColor(R.color.materialDeepOrangeAccent);
        conversation.setPinned(false);
        conversation.setRead(true);
        conversation.setTimestamp(TimeUtils.INSTANCE.getNow() - (1000 * 60 * 60 * 55));
        conversation.setSnippet("Yeah so hiking around in some place called beaver meadows now.");
        conversation.setIdMatcher("08532");
        conversation.setMute(false);
        conversation.setPrivate(false);
        conversation.setLedColor(Color.WHITE);
        conversations.add(conversation);

        conversation = new Conversation();
        conversation.setTitle("Ben Madden");
        conversation.setPhoneNumbers("(847) 609-0939");
//        conversation.colors.color = resources.getColor(R.color.materialPurple);
//        conversation.colors.colorDark = resources.getColor(R.color.materialPurpleDark);
//        conversation.colors.colorAccent = resources.getColor(R.color.materialTealAccent);
        conversation.setPinned(false);
        conversation.setRead(true);
        conversation.setTimestamp(TimeUtils.INSTANCE.getNow() - (1000 * 60 * 60 * 78));
        conversation.setSnippet("Maybe they'll run into each other on the way back... idk");
        conversation.setIdMatcher("90939");
        conversation.setMute(false);
        conversation.setPrivate(false);
        conversation.setLedColor(Color.WHITE);
        conversations.add(conversation);

        return conversations;
    }

    public static List<Contact> getFakeContacts(Resources resources) {
        List<Contact> contacts = new ArrayList<>();

        Contact contact = new Contact();
        contact.setId(11);
        contact.setName("Luke Klinker");
        contact.setPhoneNumber("(515) 991-1493");
//        contact.colors.color = resources.getColor(R.color.materialIndigo);
//        contact.colors.colorDark = resources.getColor(R.color.materialIndigoDark);
//        contact.colors.colorLight = resources.getColor(R.color.materialIndigoLight);
//        contact.colors.colorAccent = resources.getColor(R.color.materialGreenAccent);
        contacts.add(contact);

        contact = new Contact();
        contact.setId(12);
        contact.setName("Matt Swiontek");
        contact.setPhoneNumber("(708) 928-0846");
//        contact.colors.color = resources.getColor(R.color.materialRed);
//        contact.colors.colorDark = resources.getColor(R.color.materialRedDark);
//        contact.colors.colorLight = resources.getColor(R.color.materialRedLight);
//        contact.colors.colorAccent = resources.getColor(R.color.materialBlueAccent);
        contacts.add(contact);

        contact = new Contact();
        contact.setId(13);
        contact.setName("Kris Klinker");
        contact.setPhoneNumber("(515) 419-6726");
//        contact.colors.color = resources.getColor(R.color.materialPink);
//        contact.colors.colorDark = resources.getColor(R.color.materialPinkDark);
//        contact.colors.colorLight = resources.getColor(R.color.materialPinkLight);
//        contact.colors.colorAccent = resources.getColor(R.color.materialOrangeAccent);
        contacts.add(contact);

        contact = new Contact();
        contact.setId(14);
        contact.setName("Andrew Klinker");
        contact.setPhoneNumber("(515) 991-8235");
//        contact.colors.color = resources.getColor(R.color.materialBlue);
//        contact.colors.colorDark = resources.getColor(R.color.materialBlueDark);
//        contact.colors.colorLight = resources.getColor(R.color.materialBlueLight);
//        contact.colors.colorAccent = resources.getColor(R.color.materialRedAccent);
        contacts.add(contact);

        contact = new Contact();
        contact.setId(15);
        contact.setName("Aaron Klinker");
        contact.setPhoneNumber("(515) 556-7749");
//        contact.colors.color = resources.getColor(R.color.materialGreen);
//        contact.colors.colorDark = resources.getColor(R.color.materialGreenDark);
//        contact.colors.colorLight = resources.getColor(R.color.materialGreenLight);
//        contact.colors.colorAccent = resources.getColor(R.color.materialIndigoAccent);
        contacts.add(contact);

        contact = new Contact();
        contact.setId(16);
        contact.setName("Mike Klinker");
        contact.setPhoneNumber("(515) 480-8532");
//        contact.colors.color = resources.getColor(R.color.materialBrown);
//        contact.colors.colorDark = resources.getColor(R.color.materialBrownDark);
//        contact.colors.colorLight = resources.getColor(R.color.materialBrownLight);
//        contact.colors.colorAccent = resources.getColor(R.color.materialDeepOrangeAccent);
        contacts.add(contact);

        contact = new Contact();
        contact.setId(17);
        contact.setName("Ben Madden");
        contact.setPhoneNumber("(847) 609-0939");
//        contact.colors.color = resources.getColor(R.color.materialPurple);
//        contact.colors.colorDark = resources.getColor(R.color.materialPurpleDark);
//        contact.colors.colorLight = resources.getColor(R.color.materialPurpleLight);
//        contact.colors.colorAccent = resources.getColor(R.color.materialTealAccent);
        contacts.add(contact);

        return contacts;
    }

}