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

package xyz.klinker.messenger.adapter;

import android.app.Activity;
import android.database.Cursor;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bignerdranch.android.multiselector.MultiSelector;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;

import java.util.List;

import xyz.klinker.messenger.MessengerRobolectricSuite;
import xyz.klinker.messenger.R;
import xyz.klinker.messenger.adapter.view_holder.ConversationViewHolder;
import xyz.klinker.messenger.adapter.view_holder.MessageViewHolder;
import xyz.klinker.messenger.data.model.Conversation;
import xyz.klinker.messenger.data.model.Message;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SearchAdapterTest extends MessengerRobolectricSuite {

    private SearchAdapter adapter;

    @Mock
    private List<Conversation> conversations;
    @Mock
    private List<Message> messages;
    @Mock
    private View view;
    @Mock
    private TextView textView;

    @Before
    public void setUp() {
        adapter = new SearchAdapter("test", conversations, messages, null);
        when(view.getContext()).thenReturn(RuntimeEnvironment.application);
    }

    @Test
    public void getSectionCount() {
        assertEquals(2, adapter.getSectionCount());
    }

    @Test
    public void getItemCountSection0() {
        when(conversations.size()).thenReturn(10);
        assertEquals(10, adapter.getItemCount(0));
    }

    @Test
    public void getItemCountSection0Null() {
        adapter = new SearchAdapter("test", null, messages, null);
        assertEquals(0, adapter.getItemCount(0));
    }

    @Test
    public void getItemCountSection1() {
        when(messages.size()).thenReturn(20);
        assertEquals(20, adapter.getItemCount(1));
    }

    @Test
    public void getItemCountSection1Null() {
        adapter = new SearchAdapter("test", conversations, null, null);
        assertEquals(0, adapter.getItemCount(1));
    }

    @Test
    public void bindHeaderViewHolderConversations() {
        when(view.findViewById(R.id.header)).thenReturn(textView);
        ConversationViewHolder holder = new ConversationViewHolder(view, null, null);
        adapter.onBindHeaderViewHolder(holder, 0);
        verify(textView).setText(R.string.conversations);
    }

    @Test
    public void bindHeaderViewHolderMessages() {
        when(view.findViewById(R.id.header)).thenReturn(textView);
        ConversationViewHolder holder = new ConversationViewHolder(view, null, null);
        adapter.onBindHeaderViewHolder(holder, 1);
        verify(textView).setText(R.string.messages);
    }

    @Test
    public void createViewHolderHeader() {
        ViewGroup group = new LinearLayout(Robolectric.setupActivity(Activity.class));
        RecyclerView.ViewHolder holder = adapter.onCreateViewHolder(group, -2);
        assertTrue(holder instanceof ConversationViewHolder);
    }

    @Test
    public void createViewHolderConversation() {
        ViewGroup group = new LinearLayout(Robolectric.setupActivity(Activity.class));
        RecyclerView.ViewHolder holder = adapter.onCreateViewHolder(group, -3);
        assertTrue(holder instanceof ConversationViewHolder);
    }

    @Test
    public void createViewHolderMessageReceived() {
        ViewGroup group = new LinearLayout(Robolectric.setupActivity(Activity.class));
        RecyclerView.ViewHolder holder = adapter.onCreateViewHolder(group, Message.TYPE_RECEIVED);
        assertTrue(holder instanceof MessageViewHolder);
    }

    @Test
    public void createViewHolderMessageSending() {
        ViewGroup group = new LinearLayout(Robolectric.setupActivity(Activity.class));
        RecyclerView.ViewHolder holder = adapter.onCreateViewHolder(group, Message.TYPE_SENDING);
        assertTrue(holder instanceof MessageViewHolder);
    }

    @Test
    public void createViewHolderMessageError() {
        ViewGroup group = new LinearLayout(Robolectric.setupActivity(Activity.class));
        RecyclerView.ViewHolder holder = adapter.onCreateViewHolder(group, Message.TYPE_ERROR);
        assertTrue(holder instanceof MessageViewHolder);
    }

    @Test
    public void createViewHolderMessageDelivered() {
        ViewGroup group = new LinearLayout(Robolectric.setupActivity(Activity.class));
        RecyclerView.ViewHolder holder = adapter.onCreateViewHolder(group, Message.TYPE_DELIVERED);
        assertTrue(holder instanceof MessageViewHolder);
    }

    @Test
    public void createViewHolderMessageInfo() {
        ViewGroup group = new LinearLayout(Robolectric.setupActivity(Activity.class));
        RecyclerView.ViewHolder holder = adapter.onCreateViewHolder(group, Message.TYPE_INFO);
        assertTrue(holder instanceof MessageViewHolder);
    }

    @Test
    public void createViewHolderMessageSent() {
        ViewGroup group = new LinearLayout(Robolectric.setupActivity(Activity.class));
        RecyclerView.ViewHolder holder = adapter.onCreateViewHolder(group, Message.TYPE_SENT);
        assertTrue(holder instanceof MessageViewHolder);
    }

    @Test
    public void getHeaderViewType() {
        assertEquals(-2, adapter.getHeaderViewType(0));
        assertEquals(-2, adapter.getHeaderViewType(1));
    }

    @Test
    public void getItemViewTypeConversation() {
        assertEquals(-3, adapter.getItemViewType(0, 0, 0));
    }

    @Test
    public void getItemViewTypeMessage() {
        Message message = new Message();
        message.type = Message.TYPE_RECEIVED;

        when(messages.get(anyInt())).thenReturn(message);
        assertEquals(Message.TYPE_RECEIVED, adapter.getItemViewType(1, 0, 0));
    }

}