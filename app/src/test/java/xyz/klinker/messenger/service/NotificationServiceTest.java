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

package xyz.klinker.messenger.service;

import android.database.Cursor;
import android.database.MatrixCursor;
import android.util.LongSparseArray;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.robolectric.Robolectric;

import xyz.klinker.messenger.MessengerRobolectricSuite;
import xyz.klinker.messenger.data.DataSource;
import xyz.klinker.messenger.data.model.Conversation;
import xyz.klinker.messenger.data.model.Message;
import xyz.klinker.messenger.service.NotificationService.NotificationConversation;

import static org.junit.Assert.*;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

public class NotificationServiceTest extends MessengerRobolectricSuite {

    private NotificationService service;

    @Mock
    private DataSource source;

    @Before
    public void setUp() {
        service = Robolectric.setupService(NotificationService.class);
    }

    @Test
    public void notNull() {
        assertNotNull(service);
    }

    @Test
    public void getUnseenConversations() {
        service = spy(service);
        when(service.getDataSource()).thenReturn(source);
        when(source.getUnseenMessages()).thenReturn(getUnseenCursor());
        when(source.getConversation(1)).thenReturn(getConversation1());
        when(source.getConversation(2)).thenReturn(getConversation2());

        LongSparseArray<NotificationConversation> conversations = service.getUnseenConversations();

        assertEquals(2, conversations.size());
        assertEquals("Luke Klinker", conversations.get(1).title);
        assertEquals(2, conversations.get(1).messages.size());
        assertEquals("Hey what's up?", conversations.get(1).messages.get(0).data);
        assertEquals("Yo, you around?", conversations.get(1).messages.get(1).data);
        assertEquals("Aaron Klinker", conversations.get(2).title);
        assertEquals(1, conversations.get(2).messages.size());
        assertEquals("Can we hang out tonight?", conversations.get(2).messages.get(0).data);
    }

    private Cursor getUnseenCursor() {
        MatrixCursor cursor = new MatrixCursor(new String[] {
                Message.COLUMN_CONVERSATION_ID,
                Message.COLUMN_DATA,
                Message.COLUMN_MIME_TYPE,
                Message.COLUMN_TIMESTAMP
        });

        cursor.addRow(new Object[] {
                1,
                "Hey what's up?",
                "text/plain",
                1000L
        });

        cursor.addRow(new Object[] {
                1,
                "Yo, you around?",
                "text/plain",
                2000L
        });

        cursor.addRow(new Object[] {
                2,
                "Can we hang out tonight?",
                "text/plain",
                3000L
        });

        return cursor;
    }

    private Conversation getConversation1() {
        Conversation conversation = new Conversation();
        conversation.title = "Luke Klinker";
        return conversation;
    }

    private Conversation getConversation2() {
        Conversation conversation = new Conversation();
        conversation.title = "Aaron Klinker";
        return conversation;
    }

}