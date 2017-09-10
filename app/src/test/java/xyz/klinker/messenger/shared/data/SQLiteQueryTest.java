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

import android.database.Cursor;
import android.graphics.Color;

import org.junit.Assert;
import org.junit.Test;
import org.robolectric.RuntimeEnvironment;

import java.util.ArrayList;
import java.util.List;

import xyz.klinker.messenger.MessengerRealDataSuite;
import xyz.klinker.messenger.shared.data.model.Blacklist;
import xyz.klinker.messenger.shared.data.model.Contact;
import xyz.klinker.messenger.shared.data.model.Conversation;
import xyz.klinker.messenger.shared.data.model.Draft;
import xyz.klinker.messenger.shared.data.model.Message;
import xyz.klinker.messenger.shared.data.model.ScheduledMessage;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNotSame;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;

public class SQLiteQueryTest extends MessengerRealDataSuite {

    @Test
    public void databaseCreated() {
        assertNotNull(source.get_database());

        int numTables = 0;
        Cursor cursor = source.get_database().rawQuery("SELECT count(*) FROM sqlite_master " +
                "WHERE type = 'table' AND name != 'android_metadata' AND name != " +
                "'sqlite_sequence';", null);
        if (cursor != null && cursor.moveToFirst()) {
            numTables = cursor.getInt(0);
            cursor.close();
        }

        assertTrue(numTables > 0);
    }

    @Test
    public void clearTables() {
        source.clearTables(context);
        assertEquals(0, source.getMessageCount(context));
        assertEquals(0, source.getConversationCount(context));
    }

    @Test
    public void insertContacts() {
        int initialSize = source.getContacts(context).getCount();
        source.insertContacts(context, DataSourceTest.getFakeContacts(context.getResources()), null);
        int newSize = source.getContacts(context).getCount();

        assertEquals(7, newSize - initialSize);
    }

    @Test
    public void insertContact() {
        Contact contact = new Contact();
        contact.name = "Aaron K";
        contact.phoneNumber = "5155725868";

        int initialSize = source.getContacts(context).getCount();
        source.insertContact(context, contact);
        int newSize = source.getContacts(context).getCount();

        Assert.assertEquals(1, newSize - initialSize);
    }


    @Test
    public void insertConversations() {
        int initialSize = source.getUnarchivedConversations(context).getCount();
        source.insertConversations(DataSourceTest
                        .getFakeConversations(context.getResources()),
                context, null);
        int newSize = source.getUnarchivedConversations(context).getCount();

        assertEquals(7, newSize - initialSize);
    }

    @Test
    public void getContacts() {
        List<String> names = new ArrayList<>();
        Cursor cursor = source.getContacts(context);

        if (cursor.moveToFirst()) {
            do {
                names.add(cursor.getString(cursor.getColumnIndex(Contact.COLUMN_NAME)));
            } while (cursor.moveToNext());

            cursor.close();
        }

        assertEquals(2, names.size());

        // alpabetical order
        assertEquals("Jake K", names.get(0));
        assertEquals("Luke K", names.get(1));
    }

    @Test
    public void getContact_number_noMatches() {
        List<Contact> contacts = source.getContacts(context, "");
        assertEquals(0, contacts.size());

        contacts = source.getContacts(context, null);
        assertEquals(0, contacts.size());

        contacts = source.getContacts(context, "7729004");
        assertEquals(0, contacts.size());
    }

    @Test
    public void getContact_number_oneNumber() {
        List<Contact> contacts = source.getContacts(context, "5159911493");

        assertEquals(1, contacts.size());
        assertEquals("Luke K", contacts.get(0).name);
    }

    @Test
    public void getContact_number_multipleNumbers() {
        List<Contact> contacts = source.getContacts(context, "5159911493, 5154224558");

        assertEquals(2, contacts.size());
        assertEquals("Jake K", contacts.get(0).name);
        assertEquals("Luke K", contacts.get(1).name);
    }

    @Test
    public void getContact_name_noMatches() {
        List<Contact> contacts = source.getContactsByNames(context, "");
        assertEquals(0, contacts.size());

        contacts = source.getContactsByNames(context, null);
        assertEquals(0, contacts.size());

        contacts = source.getContactsByNames(context, "test");
        assertEquals(0, contacts.size());
    }

    @Test
    public void getContact_name_oneNumber() {
        List<Contact> contacts = source.getContactsByNames(context, "Luke K");

        assertEquals(1, contacts.size());
        assertEquals("5159911493", contacts.get(0).phoneNumber);
    }

    @Test
    public void getContact_name_multipleNumbers() {
        List<Contact> contacts = source.getContactsByNames(context, "Luke K, Jake K");

        assertEquals(2, contacts.size());
        assertEquals("5154224558", contacts.get(0).phoneNumber);
        assertEquals("5159911493", contacts.get(1).phoneNumber);
    }

    @Test
    public void getContact() {
        Contact contact = source.getContact(context, "5159911493");
        assertEquals("Luke K", contact.name);
    }

    @Test
    public void updateContact() {
        source.updateContact(context, "5159911493", "Lucas Klinker", 2, 3, 4, 5);
        Contact contact = source.getContact(context, "5159911493");
        assertEquals("5159911493", contact.phoneNumber);
        assertEquals("Lucas Klinker", contact.name);
        assertEquals(2, contact.colors.color);
        assertEquals(3, contact.colors.colorDark);
        assertEquals(4, contact.colors.colorLight);
        assertEquals(5, contact.colors.colorAccent);
    }

    @Test
    public void getContactNull() {
        Contact contact = source.getContact(context, "1111111111");
        assertNull(contact);
    }

    @Test
    public void deleteContact() {
        int initialContactSize = source.getContacts(context).getCount();
        source.deleteContact(context, "5159911493");
        int newContactSize = source.getContacts(context).getCount();

        assertEquals(-1, newContactSize - initialContactSize);
    }

    @Test
    public void deleteAllContacts() {
        source.deleteAllContacts(context);
        int contactSize = source.getContacts(context).getCount();

        assertEquals(0, contactSize);
    }

    @Test
    public void deleteSingleContactById() {
        int initialContactSize = source.getContacts(context).getCount();
        source.deleteContacts(context, new String[] { "1" });
        int newContactSize = source.getContacts(context).getCount();

        assertEquals(-1, newContactSize - initialContactSize);
    }

    @Test
    public void deleteMultipleContactsById() {
        int initialContactSize = source.getContacts(context).getCount();
        source.deleteContacts(context, new String[] { "1", "2" });
        int newContactSize = source.getContacts(context).getCount();

        assertEquals(-2, newContactSize - initialContactSize);
    }

    @Test
    public void getContactCount() {
        assertEquals(2, source.getContactsCount(context));
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
        conversation.mute = false;
        conversation.privateNotifications = false;
        conversation.ledColor = Color.WHITE;

        int initialSize = source.getUnarchivedConversations(context).getCount();
        source.insertConversation(context, conversation);
        int newSize = source.getUnarchivedConversations(context).getCount();

        Assert.assertEquals(1, newSize - initialSize);
    }

    @Test
    public void getConversations() {
        List<String> titles = new ArrayList<>();
        Cursor cursor = source.getUnarchivedConversations(context);

        if (cursor.moveToFirst()) {
            do {
                titles.add(cursor.getString(cursor.getColumnIndex(Conversation.COLUMN_TITLE)));
            } while (cursor.moveToNext());

            cursor.close();
        }

        assertEquals("Luke Klinker", titles.get(0));
        assertEquals("Aaron Klinker", titles.get(1));
        assertEquals("Aaron, Luke", titles.get(2));
    }

    @Test
    public void getUnreadConversations() {
        List<String> titles = new ArrayList<>();
        Cursor cursor = source.getUnreadConversations(context);

        if (cursor.moveToFirst()) {
            do {
                titles.add(cursor.getString(cursor.getColumnIndex(Conversation.COLUMN_TITLE)));
            } while (cursor.moveToNext());

            cursor.close();
        }

        assertEquals(2, titles.size());
        assertEquals("Luke Klinker", titles.get(0));
        assertEquals("Aaron Klinker", titles.get(1));
    }

    @Test
    public void getUnreadConversationsCount() {
        assertEquals(2, source.getUnreadConversationCount(context));
    }

    @Test
    public void findConversationByNumber() {
        Long id = source.findConversationId(context, "11111");
        assertEquals(id, Long.valueOf(1));
    }

    @Test
    public void findConversationByName() {
        Long id = source.findConversationIdByTitle(context, "Luke Klinker");
        assertEquals(id, Long.valueOf(1));
    }

    @Test
    public void getPinnedConversations() {
        Cursor pinned = source.getPinnedConversations(context);
        assertEquals(1, pinned.getCount());
        pinned.close();
    }

    @Test
    public void getArchivedConversations() {
        Cursor archived = source.getArchivedConversations(context);
        assertEquals(1, archived.getCount());
        archived.close();
    }

    @Test
    public void searchConversations() {
        Cursor search = source.searchConversations(context, "luke");
        assertEquals(2, search.getCount());
        search.close();
    }

    @Test
    public void getConversation() {
        Conversation conversation = source.getConversation(context, 1L);
        assertEquals("Luke Klinker", conversation.title);
    }

    @Test
    public void getConversationNull() {
        Conversation conversation = source.getConversation(context, 10L);
        assertNull(conversation);
    }

    @Test
    public void deleteConversation() {
        assertNotSame(0, source.getMessages(context, 1L).getCount());
        int initialConversationSize = source.getUnarchivedConversations(context).getCount();
        source.deleteConversation(context, 1);
        int newConversationSize = source.getUnarchivedConversations(context).getCount();
        int newMessageSize = source.getMessages(context, 1L).getCount();

        assertEquals(-1, newConversationSize - initialConversationSize);
        assertEquals(0, newMessageSize);
    }

    @Test
    public void getConversationCount() {
        assertEquals(4, source.getConversationCount(context));
    }

    @Test
    public void getMessageCount() {
        assertEquals(7, source.getMessageCount(context));
    }

    @Test
    public void archiveConversation() {
        source.archiveConversation(context, 1, true);
        Conversation conversation = source.getConversation(context, 1);
        assertEquals(true, conversation.archive);

        source.archiveConversation(context, 1, false);
        conversation = source.getConversation(context, 1);
        assertEquals(false, conversation.archive);
    }

    @Test
    public void updateConversation() {
        source.updateConversation(context, 1, false, System.currentTimeMillis(), "test updated message",
                MimeType.TEXT_PLAIN, false);
        Conversation conversation = source.getConversation(context, 1);
        assertEquals("test updated message", conversation.snippet);
    }

    @Test
    public void updateConversationSettings() {
        source.updateConversationTitle(context, 1, "test title");
        Conversation conversation = source.getConversation(context, 1);
        assertEquals("test title", conversation.title);
    }

    @Test
    public void updateConversationImage() {
        source.updateConversation(context, 1, false, System.currentTimeMillis(), "test updated message",
                MimeType.IMAGE_PNG, false);
        Conversation conversation = source.getConversation(context, 1);
        assertEquals("", conversation.snippet);
    }

    @Test
    public void getMessages() {
        assertNotSame(0, source.getMessages(context, 1L).getCount());
        assertNotSame(0, source.getMessages(context, 2L).getCount());
        assertNotSame(0, source.getMessages(context, 3L).getCount());
    }

    @Test
    public void getMessage() {
        assertNotNull(source.getMessage(context, 1L));
    }

    @Test
    public void getLatestMessage() {
        assertNotNull(source.getLatestMessage(context));
    }

    @Test
    public void getMessageNull() {
        assertNull(source.getMessage(context, 100L));
    }

    @Test
    public void getMediaMessages() {
        assertEquals(0, source.getMediaMessages(context, 1).size());
        assertEquals(2, source.getMediaMessages(context, 4).size());
    }

    @Test
    public void getAllMediaMessages() {
        assertEquals(2, source.getAllMediaMessages(context, 20).getCount());
    }

    @Test
    public void getFirebaseMediaMessages() {
        assertEquals(1, source.getFirebaseMediaMessages(context).getCount());
    }

    @Test
    public void searchMessages() {
        Cursor messages = source.searchMessages(context, "How is");
        assertEquals(2, messages.getCount());
    }

    @Test
    public void searchMessagesWithApostrophe() {
        Cursor messages = source.searchMessages(context, "How's");
        assertEquals(0, messages.getCount());
    }

    @Test
    public void searchMessagesTimestamp() {
        Cursor messages = source.searchMessages(context, 1000);
        assertEquals(7, messages.getCount());
    }

    @Test
    public void updateMessageType() {
        source.updateMessageType(context, 1, Message.TYPE_SENT);
        Cursor messages = source.getMessages(context, 1L);
        messages.moveToLast();
        assertEquals(Message.TYPE_SENT, messages.getInt(messages.getColumnIndex(Message.COLUMN_TYPE)));
    }

    @Test
    public void insertSentMessage() {
        int initialMessageSize = source.getMessages(context, 1L).getCount();
        source.insertSentMessage("1111111", "test", MimeType.TEXT_PLAIN, context);
        int newMessageSize = source.getMessages(context, 1L).getCount();

        assertEquals(1, newMessageSize - initialMessageSize);
    }

    @Test
    public void insertMessageExistingConversation() {
        int initialMessageSize = source.getMessages(context, 1L).getCount();
        int initialConversationSize = source.getUnarchivedConversations(context).getCount();
        source.insertMessage(getFakeMessage(), "1111111", context);
        int newMessageSize = source.getMessages(context, 1L).getCount();
        int newConversationSize = source.getUnarchivedConversations(context).getCount();

        assertEquals(initialConversationSize, newConversationSize);
        assertEquals(1, newMessageSize - initialMessageSize);
    }

    @Test
    public void insertMessageExistingGroupConversation() {
        int initialMessageSize = source.getMessages(context, 4L).getCount();
        int initialConversationSize = source.getUnarchivedConversations(context).getCount();
        source.insertMessage(getFakeMessage(), "1111111, 3333333", context);
        int newMessageSize = source.getMessages(context, 4L).getCount();
        int newConversationSize = source.getUnarchivedConversations(context).getCount();

        assertEquals(initialConversationSize, newConversationSize);
        assertEquals(1, newMessageSize - initialMessageSize);
    }

    @Test
    public void insertMessageNewConversation() {
        int initialConversationSize = source.getUnarchivedConversations(context).getCount();
        source.insertMessage(getFakeMessage(), "4444444", context);
        int newConversationSize = source.getUnarchivedConversations(context).getCount();

        assertEquals(1, newConversationSize - initialConversationSize);
    }

    @Test
    public void insertMessageNewGroupConversation() {
        int initialConversationSize = source.getUnarchivedConversations(context).getCount();
        source.insertMessage(getFakeMessage(), "1111111, 2222222", context);
        int newConversationSize = source.getUnarchivedConversations(context).getCount();

        assertEquals(1, newConversationSize - initialConversationSize);
    }

    @Test
    public void insertMessage() {
        int initialSize = source.getMessages(context, 2L).getCount();
        source.insertMessage(context, getFakeMessage(), 2);
        int newSize = source.getMessages(context, 2L).getCount();

        assertEquals(1, newSize - initialSize);

        Cursor conversation = source.getUnarchivedConversations(context);
        conversation.moveToFirst();
        assertEquals("You: test message", conversation
                .getString(conversation.getColumnIndex(Conversation.COLUMN_SNIPPET)));
    }

    @Test
    public void deleteMessage() {
        int initialSize = source.getMessages(context, 2L).getCount();
        source.deleteMessage(context, 3);
        int newSize = source.getMessages(context, 2L).getCount();

        assertEquals(1, initialSize - newSize);
    }

    @Test
    public void cleanupMessages() {
        int initialMessageSize = source.getMessageCount(context);
        int initialConversationSize = source.getConversationCount(context);

        source.cleanupOldMessages(context, 750);

        int newMessageSize = source.getMessageCount(context);
        int newConversationSize = source.getConversationCount(context);

        assertEquals(4, initialMessageSize - newMessageSize);
        assertEquals(2, initialConversationSize - newConversationSize);
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
        assertEquals(1, source.getUnseenMessages(context).getCount());
        assertEquals(2, source.getUnreadMessages(context).getCount());
        source.readConversation(context, 3);
        assertEquals(1, source.getUnseenMessages(context).getCount());
        assertEquals(1, source.getUnreadMessages(context).getCount());
    }

    @Test
    public void seenConversation() {
        assertEquals(1, source.getUnseenMessages(context).getCount());
        assertEquals(2, source.getUnreadMessages(context).getCount());
        source.seenConversation(context, 1);
        assertEquals(0, source.getUnseenMessages(context).getCount());
        assertEquals(2, source.getUnreadMessages(context).getCount());
    }

    @Test
    public void seenAllMessages() {
        assertEquals(1, source.getUnseenMessages(context).getCount());
        assertEquals(2, source.getUnreadMessages(context).getCount());
        source.seenAllMessages(context);
        assertEquals(0, source.getUnseenMessages(context).getCount());
        assertEquals(2, source.getUnreadMessages(context).getCount());
    }

    @Test
    public void insertDraft() {
        int initialSize = source.getDrafts(context, 3).size();
        source.insertDraft(context, 3, "test", "text/plain");
        int finalSize = source.getDrafts(context, 3).size();

        assertEquals(1, finalSize - initialSize);
    }

    @Test
    public void insertDraftObject() {
        Draft draft = new Draft();
        draft.id = 10524;
        draft.conversationId = 1;
        draft.data = "test";
        draft.mimeType = "text/plain";

        int initialSize = source.getDrafts(context, 1).size();
        long id = source.insertDraft(context, draft);
        int finalSize = source.getDrafts(context, 1).size();

        assertEquals(1, finalSize - initialSize);
        assertEquals(10524, id);
    }

    @Test
    public void getAllDrafts() {
        Cursor drafts = source.getDrafts(context);
        assertEquals(3, drafts.getCount());
    }

    @Test
    public void getDrafts() {
        List<Draft> drafts = source.getDrafts(context, 1);
        assertEquals(2, drafts.size());
    }

    @Test
    public void deleteDrafts() {
        assertEquals(1, source.getDrafts(context, 2).size());
        source.deleteDrafts(context, 2);
        assertEquals(0, source.getDrafts(context, 2).size());
    }

    @Test
    public void getBlacklists() {
        assertEquals(2, source.getBlacklists(context).getCount());
    }

    @Test
    public void insertBlacklist() {
        Blacklist blacklist = new Blacklist();
        blacklist.phoneNumber = "5154224558";
        int initialSize = source.getBlacklists(context).getCount();
        source.insertBlacklist(context, blacklist);
        int finalSize = source.getBlacklists(context).getCount();

        assertEquals(1, finalSize - initialSize);
    }

    @Test
    public void deleteBlacklist() {
        int initialSize = source.getBlacklists(context).getCount();
        source.deleteBlacklist(context, 1);
        int finalSize = source.getBlacklists(context).getCount();

        assertEquals(1, initialSize - finalSize);
    }

    @Test
    public void getScheduledMessages() {
        assertEquals(1, source.getScheduledMessages(context).getCount());
    }

    @Test
    public void insertScheduledMessage() {
        ScheduledMessage message = new ScheduledMessage();
        message.title = "Jake Klinker";
        message.to = "515-422-4558";
        message.data = "hey!";
        message.mimeType = "text/plain";
        message.timestamp = 1;

        int initialSize = source.getScheduledMessages(context).getCount();
        source.insertScheduledMessage(context, message);
        int finalSize = source.getScheduledMessages(context).getCount();

        assertEquals(1, finalSize - initialSize);
    }

    @Test
    public void deleteScheduledMessage() {
        int initialSize = source.getScheduledMessages(context).getCount();
        source.deleteScheduledMessage(context, 1);
        int finalSize = source.getScheduledMessages(context).getCount();

        assertEquals(1, initialSize - finalSize);
    }

}