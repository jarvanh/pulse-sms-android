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
import android.database.Cursor;
import android.database.MatrixCursor;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.TextView;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.robolectric.RuntimeEnvironment;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;
import xyz.klinker.messenger.MessengerRobolectricSuite;
import xyz.klinker.messenger.R;
import xyz.klinker.messenger.adapter.view_holder.ConversationViewHolder;
import xyz.klinker.messenger.data.ColorSet;
import xyz.klinker.messenger.data.SectionType;
import xyz.klinker.messenger.data.model.Conversation;
import xyz.klinker.messenger.util.ConversationExpandedListener;
import xyz.klinker.messenger.util.swipe_to_dismiss.SwipeToDeleteListener;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class ConversationListAdapterTest extends MessengerRobolectricSuite {

    private ConversationListAdapter adapter;

    @Mock
    private SwipeToDeleteListener swipeToDeleteListener;
    @Mock
    private ConversationExpandedListener conversationExpandedListener;
    @Mock
    private TextView header;
    @Mock
    private CircleImageView image;
    @Mock
    private TextView name;
    @Mock
    private TextView summary;

    @Before
    public void setUp() {
        adapter = new ConversationListAdapter(getFakeConversations(RuntimeEnvironment.application),
                swipeToDeleteListener, conversationExpandedListener);
    }

    @Test
    public void sectionCounts() {
        List<SectionType> sectionCounts = adapter.getSections();

        assertEquals(SectionType.PINNED, sectionCounts.get(0).type);
        assertEquals(2, sectionCounts.get(0).count);
        assertEquals(SectionType.TODAY, sectionCounts.get(1).type);
        assertEquals(1, sectionCounts.get(1).count);
        assertEquals(SectionType.YESTERDAY, sectionCounts.get(2).type);
        assertEquals(2, sectionCounts.get(2).count);
        assertEquals(SectionType.OLDER, sectionCounts.get(3).type);
        assertEquals(2, sectionCounts.get(3).count);
    }

    @Test
    public void getSectionCount() {
        assertEquals(4, adapter.getSectionCount());
    }

    @Test
    public void getItemCountPinned() {
        assertEquals(2, adapter.getItemCount(0));
    }

    @Test
    public void getItemCountToday() {
        assertEquals(1, adapter.getItemCount(1));
    }

    @Test
    public void getItemCountYesterday() {
        assertEquals(2, adapter.getItemCount(2));
    }

    @Test
    public void getItemCountOlder() {
        assertEquals(2, adapter.getItemCount(3));
    }

    @Test
    public void bindHeaderPinned() {
        adapter.onBindHeaderViewHolder(getMockedViewHolder(), 0);
        verify(header).setText(R.string.pinned);
    }

    @Test
    public void bindHeaderToday() {
        adapter.onBindHeaderViewHolder(getMockedViewHolder(), 1);
        verify(header).setText(R.string.today);
    }

    @Test
    public void bindHeaderYesterday() {
        adapter.onBindHeaderViewHolder(getMockedViewHolder(), 2);
        verify(header).setText(R.string.yesterday);
    }

    @Test
    public void bindHeaderOlder() {
        adapter.onBindHeaderViewHolder(getMockedViewHolder(), 3);
        verify(header).setText(R.string.older);
    }

    @Test
    public void bindConversationRead() {
        ConversationViewHolder holder = getMockedViewHolder();
        adapter.onBindViewHolder(holder, 0, 0, 0);

        assertNotNull(holder.conversation);
        verify(image).setImageDrawable(any(Drawable.class));
        verify(name).setText("Luke Klinker");
        verify(summary).setText("So maybe not going to be able to get platinum huh?");
        verify(name, times(0)).setTypeface(Typeface.DEFAULT);
        verify(summary, times(0)).setTypeface(Typeface.DEFAULT);
    }

    @Test
    public void bindConversationUnread() {
        ConversationViewHolder holder = getMockedViewHolder();
        adapter.onBindViewHolder(holder, 1, 0, 2);

        assertNotNull(holder.conversation);
        verify(image).setImageDrawable(any(Drawable.class));
        verify(name).setText("Kris Klinker");
        verify(summary).setText("Will probably be there from 6:30-9, just stop by when you can!");
        verify(name).setTypeface(Typeface.DEFAULT_BOLD);
        verify(summary).setTypeface(Typeface.DEFAULT_BOLD);
    }

    @Test
    public void removeItems() {
        adapter.removeItem(1);

        assertEquals(4, adapter.getSectionCount());
        assertEquals(1, adapter.getItemCount(0));
        assertEquals(1, adapter.getItemCount(1));
        assertEquals(2, adapter.getItemCount(2));
        assertEquals(2, adapter.getItemCount(3));

        adapter.removeItem(3);

        assertEquals(3, adapter.getSectionCount());
        assertEquals(1, adapter.getItemCount(0));
        assertEquals(2, adapter.getItemCount(1));
        assertEquals(2, adapter.getItemCount(2));

        adapter.removeItem(7);
        adapter.removeItem(6);

        assertEquals(2, adapter.getSectionCount());
        assertEquals(1, adapter.getItemCount(0));
        assertEquals(2, adapter.getItemCount(1));

        adapter.removeItem(1);

        assertEquals(1, adapter.getSectionCount());
        assertEquals(2, adapter.getItemCount(0));

        adapter.removeItem(2);

        assertEquals(1, adapter.getSectionCount());
        assertEquals(1, adapter.getItemCount(0));

        adapter.removeItem(1);

        assertEquals(0, adapter.getSectionCount());
    }

    private ConversationViewHolder getMockedViewHolder() {
        ConversationViewHolder holder = new ConversationViewHolder(
                new View(RuntimeEnvironment.application), conversationExpandedListener);
        holder.header = header;
        holder.image = image;
        holder.name = name;
        holder.summary = summary;

        return holder;
    }

    private Cursor getFakeConversations(Context context) {
        MatrixCursor cursor = new MatrixCursor(new String[] {
                Conversation.COLUMN_ID,
                Conversation.COLUMN_COLOR,
                Conversation.COLUMN_COLOR_DARK,
                Conversation.COLUMN_COLOR_LIGHT,
                Conversation.COLUMN_COLOR_ACCENT,
                Conversation.COLUMN_PINNED,
                Conversation.COLUMN_READ,
                Conversation.COLUMN_TIMESTAMP,
                Conversation.COLUMN_TITLE,
                Conversation.COLUMN_PHONE_NUMBERS,
                Conversation.COLUMN_SNIPPET,
                Conversation.COLUMN_RINGTONE
        });

        cursor.addRow(new Object[] {
                1,
                ColorSet.INDIGO(context).color,
                ColorSet.INDIGO(context).colorDark,
                ColorSet.INDIGO(context).colorLight,
                ColorSet.INDIGO(context).colorAccent,
                1,
                1,
                System.currentTimeMillis() - (1000 * 60 * 60),
                "Luke Klinker",
                "(515) 991-1493",
                "So maybe not going to be able to get platinum huh?",
                null
        });

        cursor.addRow(new Object[] {
                2,
                ColorSet.RED(context).color,
                ColorSet.RED(context).colorDark,
                ColorSet.RED(context).colorLight,
                ColorSet.RED(context).colorAccent,
                1,
                1,
                System.currentTimeMillis() - (1000 * 60 * 60 * 12),
                "Matt Swiontek",
                "(708) 928-0846",
                "Whoops ya idk what happened but anysho drive safe",
                null
        });

        cursor.addRow(new Object[] {
                3,
                ColorSet.PINK(context).color,
                ColorSet.PINK(context).colorDark,
                ColorSet.PINK(context).colorLight,
                ColorSet.PINK(context).colorAccent,
                0,
                0,
                System.currentTimeMillis() - (1000 * 60 * 20),
                "Kris Klinker",
                "(515) 419-6726",
                "Will probably be there from 6:30-9, just stop by when you can!",
                null
        });

        cursor.addRow(new Object[] {
                4,
                ColorSet.BLUE(context).color,
                ColorSet.BLUE(context).colorDark,
                ColorSet.BLUE(context).colorLight,
                ColorSet.BLUE(context).colorAccent,
                0,
                1,
                System.currentTimeMillis() - (1000 * 60 * 60 * 26),
                "Andrew Klinker",
                "(515) 991-8235",
                "Just finished, it was a lot of fun",
                null
        });

        cursor.addRow(new Object[] {
                5,
                ColorSet.GREEN(context).color,
                ColorSet.GREEN(context).colorDark,
                ColorSet.GREEN(context).colorLight,
                ColorSet.GREEN(context).colorAccent,
                0,
                1,
                System.currentTimeMillis() - (1000 * 60 * 60 * 32),
                "Aaron Klinker",
                "(515) 556-7749",
                "Yeah I'll do it when I get home",
                null
        });

        cursor.addRow(new Object[] {
                6,
                ColorSet.BROWN(context).color,
                ColorSet.BROWN(context).colorDark,
                ColorSet.BROWN(context).colorLight,
                ColorSet.BROWN(context).colorAccent,
                0,
                1,
                System.currentTimeMillis() - (1000 * 60 * 60 * 55),
                "Mike Klinker",
                "(515) 480-8532",
                "Yeah so hiking around in some place called beaver meadows now.",
                null
        });

        cursor.addRow(new Object[] {
                7,
                ColorSet.PURPLE(context).color,
                ColorSet.PURPLE(context).colorDark,
                ColorSet.PURPLE(context).colorLight,
                ColorSet.PURPLE(context).colorAccent,
                0,
                1,
                System.currentTimeMillis() - (1000 * 60 * 60 * 78),
                "Ben Madden",
                "(847) 609-0939",
                "Maybe they'll run into each other on the way back... idk",
                null
        });

        return cursor;
    }

}