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

package xyz.klinker.messenger.shared.service;

import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.util.LongSparseArray;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;

import java.util.ArrayList;
import java.util.List;

import xyz.klinker.messenger.MessengerRobolectricSuite;
import xyz.klinker.messenger.shared.data.DataSource;
import xyz.klinker.messenger.shared.data.model.Conversation;
import xyz.klinker.messenger.shared.data.model.Message;
import xyz.klinker.messenger.shared.data.pojo.NotificationConversation;
import xyz.klinker.messenger.shared.data.pojo.NotificationMessage;
import xyz.klinker.messenger.shared.util.MockableDataSourceWrapper;
import xyz.klinker.messenger.shared.util.TimeUtils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

public class NotificationServiceTest extends MessengerRobolectricSuite {

    private NotificationService service;
    private Context context = spy(RuntimeEnvironment.application);

    @Mock
    private MockableDataSourceWrapper source;

    @Before
    public void setUp() {
        service = Robolectric.setupService(NotificationService.class);
    }

    @Test
    public void notNull() {
        assertNotNull(service);
    }

    @Test
    public void shouldNotDebugNotifications() {
        assertFalse(NotificationService.DEBUG_QUICK_REPLY);
    }

    @Test
    public void getUnseenConversations() {
        service = spy(service);
        doReturn(getUnseenCursor()).when(source).getUnseenMessages(any(Context.class));
        doReturn(getConversation1()).when(source).getConversation(any(Context.class), eq(1L));
        doReturn(getConversation2()).when(source).getConversation(any(Context.class), eq(2L));
        doReturn(getConversation3()).when(source).getConversation(any(Context.class), eq(3L));

        List<NotificationConversation> conversations = NotificationService.getUnseenConversations(service, source);

        assertEquals(3, conversations.size());
        assertEquals("Luke Klinker", conversations.get(2).title);
        assertEquals(3, conversations.get(2).messages.size());
        assertEquals("Hey what's up?", conversations.get(2).messages.get(0).data);
        assertEquals("Yo, you around?", conversations.get(2).messages.get(1).data);
        assertEquals("Hello?", conversations.get(2).messages.get(2).data);
        assertEquals("Aaron Klinker", conversations.get(1).title);
        assertEquals(1, conversations.get(1).messages.size());
        assertEquals("Can we hang out tonight?", conversations.get(1).messages.get(0).data);
        assertEquals(1, conversations.get(0).messages.size());
        assertEquals("image/jpg", conversations.get(0).messages.get(0).mimeType);
    }

    @Test
    public void shouldAlertAgainForConversationWhereLatestTimestampsAreMoreThanThirtySeconds() {
        List<NotificationMessage> messages = new ArrayList<>();
        messages.add(new NotificationMessage(1, "", "", 1000, ""));
        messages.add(new NotificationMessage(1, "", "", TimeUtils.DAY, ""));
        messages.add(new NotificationMessage(1, "", "", 0, ""));
        messages.add(new NotificationMessage(1, "", "", (TimeUtils.SECOND * 30) + 1, ""));

        assertThat(service.shouldAlertOnce(messages), Matchers.is(false));
    }

    @Test
    public void shouldOnlyAlertOnceForConversationWhereLatestTimestampsAreLessThanThirtySeconds() {
        List<NotificationMessage> messages = new ArrayList<>();
        messages.add(new NotificationMessage(1, "", "", 1000, ""));
        messages.add(new NotificationMessage(1, "", "", TimeUtils.DAY, ""));
        messages.add(new NotificationMessage(1, "", "", 0, ""));
        messages.add(new NotificationMessage(1, "", "", (TimeUtils.SECOND * 30) - 100, ""));

        assertThat(service.shouldAlertOnce(messages), Matchers.is(true));
    }

    @Test
    public void smallMessageListsShouldOnlyAlertOnce() {
        List<NotificationMessage> messages = new ArrayList<>();
        messages.add(new NotificationMessage(1, "", "", TimeUtils.DAY - 100, ""));

        assertThat(service.shouldAlertOnce(messages), Matchers.is(true));
    }

    private Cursor getUnseenCursor() {
        MatrixCursor cursor = new MatrixCursor(new String[]{
                Message.COLUMN_ID,
                Message.COLUMN_CONVERSATION_ID,
                Message.COLUMN_DATA,
                Message.COLUMN_MIME_TYPE,
                Message.COLUMN_TIMESTAMP,
                Message.COLUMN_FROM
        });

        cursor.addRow(new Object[]{
                1,
                1,
                "Hey what's up?",
                "text/plain",
                1000L,
                null
        });

        cursor.addRow(new Object[]{
                1,
                1,
                "Yo, you around?",
                "text/plain",
                2000L,
                null
        });

        cursor.addRow(new Object[]{
                1,
                2,
                "Can we hang out tonight?",
                "text/plain",
                3000L,
                null
        });

        cursor.addRow(new Object[]{
                1,
                1,
                "Hello?",
                "text/plain",
                4000L,
                null
        });

        cursor.addRow(new Object[]{
                1,
                3,
                "content://mms/part/1",
                "image/jpg",
                5000L,
                null
        });

        return cursor;
    }

    private Conversation getConversation1() {
        Conversation conversation = new Conversation();
        conversation.title = "Luke Klinker";
        conversation.phoneNumbers = "test";
        conversation.timestamp = 1;
        return conversation;
    }

    private Conversation getConversation2() {
        Conversation conversation = new Conversation();
        conversation.title = "Aaron Klinker";
        conversation.phoneNumbers = "test";
        conversation.timestamp = 2;
        return conversation;
    }

    private Conversation getConversation3() {
        Conversation conversation = new Conversation();
        conversation.title = "Andrew Klinker";
        conversation.phoneNumbers = "test";
        conversation.timestamp = 3;
        return conversation;
    }

}