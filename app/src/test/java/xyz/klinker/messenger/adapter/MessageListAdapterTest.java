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

import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.graphics.Color;
import android.support.v7.widget.LinearLayoutManager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.robolectric.RuntimeEnvironment;

import xyz.klinker.messenger.MessengerRobolectricSuite;
import xyz.klinker.messenger.R;
import xyz.klinker.messenger.adapter.view_holder.MessageViewHolder;
import xyz.klinker.messenger.shared.data.model.Message;
import xyz.klinker.messenger.fragment.MessageListFragment;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class MessageListAdapterTest extends MessengerRobolectricSuite {

    private MessageListAdapter adapter;
    private Context context;

    @Mock
    private MessageListFragment fragment;
    @Mock
    private LinearLayoutManager manager;
    @Mock
    private TextView message;
    @Mock
    private TextView timestamp;
    @Mock
    private ImageView image;
    @Mock
    private Cursor cursor;
    @Mock
    private ViewGroup.LayoutParams params;

    @Before
    public void setUp() {
        context = spy(RuntimeEnvironment.application);
        when(timestamp.getLayoutParams()).thenReturn(params);
        when(timestamp.getContext()).thenReturn(context);
        adapter = new MessageListAdapter(getFakeMessages(), Color.BLUE, Color.RED, false, manager, fragment);
    }

    @Test
    public void getItemCount() {
        assertEquals(12, adapter.getItemCount());
    }

    @Test
    public void getItemCountNullCursor() {
        adapter.addMessage(null);
        assertEquals(0, adapter.getItemCount());
    }

    @Test
    public void getItemCountZeroCursor() {
        adapter.addMessage(new MatrixCursor(new String[]{}));
        assertEquals(0, adapter.getItemCount());
    }

    @Test
    public void getItemViewTypeReceived() {
        assertEquals(Message.TYPE_RECEIVED, adapter.getItemViewType(0));
    }

    @Test
    public void getItemViewTypeSent() {
        assertEquals(Message.TYPE_SENT, adapter.getItemViewType(1));
    }

    @Test
    public void addMessage() {
        when(cursor.getCount()).thenReturn(20);
        when(cursor.moveToFirst()).thenReturn(true);
        when(manager.findLastVisibleItemPosition()).thenReturn(15);
        adapter = spy(adapter);
        adapter.addMessage(cursor);
        verify(adapter).notifyItemInserted(19);
        verify(manager).scrollToPosition(19);
    }

    @Test
    public void changeMessage() {
        when(cursor.getCount()).thenReturn(12);
        when(cursor.moveToFirst()).thenReturn(true);
        adapter = spy(adapter);
        adapter.addMessage(cursor);
        verify(adapter).notifyItemChanged(11);
    }

    @Test
    public void removeMessage() {
        when(cursor.getCount()).thenReturn(11);
        when(cursor.moveToFirst()).thenReturn(true);
        adapter = spy(adapter);
        adapter.addMessage(cursor);
        verify(adapter).notifyDataSetChanged();
    }

    @Test
    public void addMessageNullCursor() {
        adapter.addMessage(null);
        verify(manager, times(0)).scrollToPosition(anyInt());
    }

    @Test
    public void bindViewHolderMessageNoTimestamp() {
        when(timestamp.getContext()).thenReturn(context);
        adapter.onBindViewHolder(getMockedViewHolder(), adapter.getItemCount() - 1);
        verify(message).setText(anyString());
        assertEquals(0, params.height);
    }

    @Test
    public void bindViewHolderMessageTimestamp() {
        adapter.setTimestampHeight(1);
        adapter.onBindViewHolder(getMockedViewHolder(), adapter.getItemCount() - 2);
        verify(message).setText(anyString());
        verify(timestamp).setText(anyString());
        assertEquals(1, params.height);
    }

    private MessageViewHolder getMockedViewHolder() {
        View itemView = spy(new View(RuntimeEnvironment.application));
        when(itemView.findViewById(R.id.timestamp)).thenReturn(timestamp);
        when(itemView.findViewById(R.id.message)).thenReturn(message);
        MessageViewHolder holder = new MessageViewHolder(fragment, itemView,
                Color.RED, 1, 0, 0, null);
        holder.message = message;
        holder.timestamp = timestamp;
        holder.image = image;

        when(message.getText()).thenReturn("test text");

        return holder;
    }

    private Cursor getFakeMessages() {
        MatrixCursor cursor = new MatrixCursor(new String[]{
                Message.COLUMN_ID,
                Message.COLUMN_CONVERSATION_ID,
                Message.COLUMN_TYPE,
                Message.COLUMN_DATA,
                Message.COLUMN_TIMESTAMP,
                Message.COLUMN_MIME_TYPE,
                Message.COLUMN_READ,
                Message.COLUMN_SEEN,
                Message.COLUMN_FROM,
                Message.COLUMN_COLOR
        });

        cursor.addRow(new Object[]{
                1,
                1,
                Message.TYPE_RECEIVED,
                "Do you want to go to summerfest this weekend?",
                System.currentTimeMillis() - (1000 * 60 * 60 * 12) - (1000 * 60 * 30),
                "text/plain",
                1,
                1,
                "Luke Klinker",
                null
        });

        cursor.addRow(new Object[]{
                2,
                1,
                Message.TYPE_SENT,
                "Yeah, I'll probably go on Friday.",
                System.currentTimeMillis() - (1000 * 60 * 60 * 12),
                "text/plain",
                1,
                1,
                null,
                null
        });

        cursor.addRow(new Object[]{
                3,
                1,
                Message.TYPE_SENT,
                "I started working on the designs for a new messaging app today... I'm thinking " +
                        "that it could be somewhere along the lines of a compliment to Evolve. " +
                        "The main app will be focused on tablet design and so Evolve could " +
                        "support hooking up to the same backend and the two could be used " +
                        "together. Or, users could just use this app on their phone as well... " +
                        "up to them which they prefer.",
                System.currentTimeMillis() - (1000 * 60 * 60 * 8) - (1000 * 60 * 6),
                "text/plain",
                1,
                1,
                null,
                null
        });

        cursor.addRow(new Object[]{
                4,
                1,
                Message.TYPE_RECEIVED,
                "Are you going to make this into an actual app?",
                System.currentTimeMillis() - (1000 * 60 * 60 * 8),
                "text/plain",
                1,
                1,
                "Luke Klinker",
                null
        });

        cursor.addRow(new Object[]{
                5,
                1,
                Message.TYPE_SENT,
                "dunno",
                System.currentTimeMillis() - (1000 * 60 * 60 * 7) - (1000 * 60 * 55),
                "text/plain",
                1,
                1,
                null,
                null
        });

        cursor.addRow(new Object[]{
                6,
                1,
                Message.TYPE_SENT,
                "I got to build some Legos, plus get 5 extra character packs and 3 level packs " +
                        "with the deluxe edition lol",
                System.currentTimeMillis() - (1000 * 60 * 38),
                "text/plain",
                1,
                1,
                null,
                null
        });

        cursor.addRow(new Object[]{
                7,
                1,
                Message.TYPE_RECEIVED,
                "woah nice one haha",
                System.currentTimeMillis() - (1000 * 60 * 37),
                "text/plain",
                1,
                1,
                "Luke Klinker",
                null
        });

        cursor.addRow(new Object[]{
                8,
                1,
                Message.TYPE_SENT,
                "Already shaping up to be a better deal than battlefront!",
                System.currentTimeMillis() - (1000 * 60 * 23),
                "text/plain",
                1,
                1,
                null,
                null
        });

        cursor.addRow(new Object[]{
                9,
                1,
                Message.TYPE_RECEIVED,
                "is it fun?",
                System.currentTimeMillis() - (1000 * 60 * 22),
                "text/plain",
                1,
                1,
                "Luke Klinker",
                null
        });

        cursor.addRow(new Object[]{
                10,
                1,
                Message.TYPE_SENT,
                "So far! Looks like a lot of content in the game too. Based on the trophies " +
                        "required at least",
                System.currentTimeMillis() - (1000 * 60 * 20),
                "text/plain",
                1,
                1,
                null,
                null
        });

        cursor.addRow(new Object[]{
                11,
                1,
                Message.TYPE_RECEIVED,
                "so maybe not going to be able to get platinum huh? haha",
                System.currentTimeMillis() - (1000 * 60 * 18),
                "text/plain",
                1,
                1,
                "Luke Klinker",
                null
        });

        cursor.addRow(new Object[]{
                12,
                1,
                Message.TYPE_SENT,
                "Oh, I will definitely get it! Just might take 24+ hours to do it... and when " +
                        "those 24 hours are in a single week, things get to be a little tedious. " +
                        "Hopefully I don't absolutely hate the game once I finish!",
                System.currentTimeMillis() - (1000 * 60),
                "text/plain",
                1,
                1,
                null,
                null
        });

        return cursor;
    }

}