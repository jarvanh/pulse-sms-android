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

package xyz.klinker.messenger.receiver;

import android.content.Context;
import android.content.Intent;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.robolectric.RuntimeEnvironment;

import java.util.List;

import xyz.klinker.messenger.MessengerRobolectricSuite;
import xyz.klinker.messenger.adapter.ConversationListAdapter;
import xyz.klinker.messenger.data.SectionType;
import xyz.klinker.messenger.data.model.Conversation;
import xyz.klinker.messenger.fragment.ConversationListFragment;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class ConversationListUpdatedReceiverTest extends MessengerRobolectricSuite {

    private ConversationListUpdatedReceiver receiver;
    private Context context;
    private SectionType today;

    @Mock
    private ConversationListFragment fragment;
    @Mock
    private ConversationListAdapter adapter;
    @Mock
    private Intent intent;
    @Mock
    private List<Conversation> conversations;
    @Mock
    private List<SectionType> sectionTypes;

    @Before
    public void setUp() {
        receiver = new ConversationListUpdatedReceiver(fragment);
        context = RuntimeEnvironment.application;
        today = new SectionType(SectionType.TODAY, 1);

        when(fragment.isAdded()).thenReturn(true);
        when(fragment.getExpandedId()).thenReturn(0L);
        when(fragment.getAdapter()).thenReturn(adapter);
        when(intent.getLongExtra("conversation_id", -1)).thenReturn(1L);
        when(intent.getStringExtra("snippet")).thenReturn("hey");
        when(intent.getBooleanExtra("read", false)).thenReturn(false);
        when(adapter.getConversations()).thenReturn(conversations);
        when(adapter.getSectionCounts()).thenReturn(sectionTypes);

        setFakeConversations();
    }

    @Test
    public void fragmentNotAdded() {
        when(fragment.isAdded()).thenReturn(false);
        receiver.onReceive(context, intent);
        verifyNoMoreInteractions(adapter);
    }

    @Test
    public void noConversationId() {
        when(intent.getLongExtra("conversation_id", -1)).thenReturn(-1L);
        receiver.onReceive(context, intent);
        verifyNoMoreInteractions(adapter);
    }

    @Test
    public void conversationAlreadyExpanded() {
        when(fragment.getExpandedId()).thenReturn(1L);
        receiver.onReceive(context, intent);
        verifyNoMoreInteractions(adapter);
    }

    /*@Test
    public void noConversationAndNoTodayAndNoPinned() {
        when(adapter.findPositionForConversationId(1)).thenReturn(-1);
        when(adapter.getCountForSection(SectionType.TODAY)).thenReturn(0);
        when(adapter.getCountForSection(SectionType.PINNED)).thenReturn(0);

        receiver.onReceive(context, intent);

        verify(conversations).add(0, null);
        verify(sectionTypes).add(0, new SectionType(SectionType.TODAY, 1));
        verify(adapter).notifyItemRangeInserted(0, 2);
    }

    @Test
    public void noConversationAndNoTodayAndPinned() {
        when(adapter.findPositionForConversationId(1)).thenReturn(-1);
        when(adapter.getCountForSection(SectionType.TODAY)).thenReturn(0);
        when(adapter.getCountForSection(SectionType.PINNED)).thenReturn(2);

        receiver.onReceive(context, intent);

        verify(conversations).add(2, null);
        verify(sectionTypes).add(1, new SectionType(SectionType.TODAY, 1));
        verify(adapter).notifyItemRangeInserted(3, 2);
    }*/

    @Test
    public void noConversationAndTodayAndNoPinned() {
        when(sectionTypes.get(0)).thenReturn(today);
        when(adapter.findPositionForConversationId(1)).thenReturn(-1);
        when(adapter.getCountForSection(SectionType.TODAY)).thenReturn(1);
        when(adapter.getCountForSection(SectionType.PINNED)).thenReturn(0);

        receiver.onReceive(context, intent);

        verify(conversations).add(0, null);
        assertEquals(2, today.count);
        verify(adapter).notifyItemInserted(1);
    }

    @Test
    public void noConversationAndTodayAndPinned() {
        when(sectionTypes.get(1)).thenReturn(today);
        when(adapter.findPositionForConversationId(1)).thenReturn(-1);
        when(adapter.getCountForSection(SectionType.TODAY)).thenReturn(1);
        when(adapter.getCountForSection(SectionType.PINNED)).thenReturn(2);

        receiver.onReceive(context, intent);

        verify(conversations).add(2, null);
        assertEquals(2, today.count);
        verify(adapter).notifyItemInserted(4);
    }

    @Test
    public void conversationAndNoTodayAndNoPinned() {
        conversations.get(1).id = 1;
        when(adapter.findPositionForConversationId(1)).thenReturn(3);
        when(adapter.getCountForSection(SectionType.TODAY)).thenReturn(0);
        when(adapter.getCountForSection(SectionType.PINNED)).thenReturn(0);

        receiver.onReceive(context, intent);

        verify(adapter).removeItem(3, ConversationListAdapter.ReorderType.NEITHER);
        verify(conversations).add(eq(0), any(Conversation.class));
        verify(sectionTypes).add(0, new SectionType(SectionType.TODAY, 1));
        verify(adapter).notifyItemRangeInserted(0, 2);
    }

    @Test
    public void conversationAndNoTodayAndPinned() {
        conversations.get(2).id = 1;
        when(adapter.findPositionForConversationId(1)).thenReturn(5);
        when(adapter.getCountForSection(SectionType.TODAY)).thenReturn(0);
        when(adapter.getCountForSection(SectionType.PINNED)).thenReturn(1);

        receiver.onReceive(context, intent);

        verify(adapter).removeItem(5, ConversationListAdapter.ReorderType.NEITHER);
        verify(conversations).add(eq(1), any(Conversation.class));
        verify(sectionTypes).add(1, new SectionType(SectionType.TODAY, 1));
        verify(adapter).notifyItemRangeInserted(2, 2);
    }

    @Test
    public void conversationAndTodayAndNoPinned() {
        conversations.get(1).id = 1;
        when(sectionTypes.get(0)).thenReturn(today);
        when(adapter.findPositionForConversationId(1)).thenReturn(4);
        when(adapter.getCountForSection(SectionType.TODAY)).thenReturn(1);
        when(adapter.getCountForSection(SectionType.PINNED)).thenReturn(0);

        receiver.onReceive(context, intent);

        verify(adapter).removeItem(4, ConversationListAdapter.ReorderType.NEITHER);
        verify(conversations).add(eq(0), any(Conversation.class));
        assertEquals(2, today.count);
        verify(adapter).notifyItemInserted(1);
    }

    @Test
    public void conversationAndTodayAndPinned() {
        conversations.get(2).id = 1;
        when(sectionTypes.get(1)).thenReturn(today);
        when(adapter.findPositionForConversationId(1)).thenReturn(6);
        when(adapter.getCountForSection(SectionType.TODAY)).thenReturn(1);
        when(adapter.getCountForSection(SectionType.PINNED)).thenReturn(1);

        receiver.onReceive(context, intent);

        verify(adapter).removeItem(6, ConversationListAdapter.ReorderType.NEITHER);
        verify(conversations).add(eq(1), any(Conversation.class));
        assertEquals(2, today.count);
        verify(adapter).notifyItemInserted(3);
    }

    @Test
    public void conversationAlreadyAtTopWithPinned() {
        conversations.get(2).id = 1;
        when(sectionTypes.get(1)).thenReturn(today);
        when(adapter.findPositionForConversationId(1)).thenReturn(4);
        when(adapter.getCountForSection(SectionType.TODAY)).thenReturn(1);
        when(adapter.getCountForSection(SectionType.PINNED)).thenReturn(2);

        receiver.onReceive(context, intent);

        verify(adapter).notifyItemChanged(4);
    }

    @Test
    public void conversationAlreadyAtTopAndIsPinned() {
        conversations.get(1).id = 1;
        when(sectionTypes.get(1)).thenReturn(today);
        when(adapter.findPositionForConversationId(1)).thenReturn(2);
        when(adapter.getCountForSection(SectionType.TODAY)).thenReturn(1);
        when(adapter.getCountForSection(SectionType.PINNED)).thenReturn(2);

        receiver.onReceive(context, intent);

        verify(adapter).notifyItemChanged(2);
    }

    @Test
    public void conversationAlreadyAtTopWithNoPinned() {
        conversations.get(0).id = 1;
        when(sectionTypes.get(0)).thenReturn(today);
        when(adapter.findPositionForConversationId(1)).thenReturn(1);
        when(adapter.getCountForSection(SectionType.TODAY)).thenReturn(1);
        when(adapter.getCountForSection(SectionType.PINNED)).thenReturn(0);

        receiver.onReceive(context, intent);

        verify(adapter).notifyItemChanged(1);
    }

    @Test
    public void ignoresSnippets() {
        assertThat(receiver.shouldIgnoreSnippet("img.youtube.com"), Matchers.is(true));
        assertThat(receiver.shouldIgnoreSnippet("{ json }"), Matchers.is(true));
    }

    private void setFakeConversations() {
        when(conversations.size()).thenReturn(3);

        when(conversations.get(0)).thenReturn(new Conversation());
        when(conversations.get(1)).thenReturn(new Conversation());
        when(conversations.get(2)).thenReturn(new Conversation());
    }

}