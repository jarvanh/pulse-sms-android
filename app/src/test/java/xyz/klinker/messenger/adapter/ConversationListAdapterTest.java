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

package xyz.klinker.messenger.adapter;

import android.content.Context;
import android.database.MatrixCursor;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.robolectric.RuntimeEnvironment;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;
import xyz.klinker.messenger.MessengerRobolectricSuite;
import xyz.klinker.messenger.activity.MessengerActivity;
import xyz.klinker.messenger.adapter.view_holder.ConversationViewHolder;
import xyz.klinker.messenger.shared.data.ColorSet;
import xyz.klinker.messenger.shared.data.SectionType;
import xyz.klinker.messenger.shared.data.model.Conversation;
import xyz.klinker.messenger.utils.listener.ConversationExpandedListener;
import xyz.klinker.messenger.utils.swipe_to_dismiss.SwipeToDeleteListener;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ConversationListAdapterTest extends MessengerRobolectricSuite {

    private ConversationListAdapter adapter;

    @Mock
    private MessengerActivity context;
    @Mock
    private SwipeToDeleteListener swipeToDeleteListener;
    @Mock
    private ConversationExpandedListener conversationExpandedListener;
    @Mock
    private TextView header;
    @Mock
    private ImageButton headerDoneButton;
    @Mock
    private View headerDialog;
    @Mock
    private CircleImageView image;
    @Mock
    private TextView name;
    @Mock
    private TextView summary;
    @Mock
    private TextView imageLetter;
    @Mock
    private ImageView groupIcon;
    @Mock
    private CircleImageView unreadIndicator;

    @Before
    public void setUp() {
        adapter = spy(new ConversationListAdapter(context, new ArrayList<>(),
                null, swipeToDeleteListener, conversationExpandedListener));

        when(adapter.showHeaderAboutTextingOnline()).thenReturn(false);
        when(header.getContext()).thenReturn(RuntimeEnvironment.application);

        adapter.setConversations(getFakeConversations(RuntimeEnvironment.application));
    }

    @Test
    public void sectionCounts() {
        List<SectionType> sectionCounts = adapter.getSections();

        assertEquals(SectionType.Companion.getPINNED(), sectionCounts.get(0).getType());
        assertEquals(2, sectionCounts.get(0).getCount());
        assertEquals(SectionType.Companion.getTODAY(), sectionCounts.get(1).getType());
        assertEquals(1, sectionCounts.get(1).getCount());
        assertEquals(SectionType.Companion.getYESTERDAY(), sectionCounts.get(2).getType());
        assertEquals(2, sectionCounts.get(2).getCount());
        assertEquals(SectionType.Companion.getLAST_WEEK(), sectionCounts.get(3).getType());
        assertEquals(2, sectionCounts.get(3).getCount());
        assertEquals(SectionType.Companion.getLAST_MONTH(), sectionCounts.get(4).getType());
        assertEquals(1, sectionCounts.get(4).getCount());
        assertEquals(SectionType.Companion.getOLDER(), sectionCounts.get(5).getType());
        assertEquals(1, sectionCounts.get(5).getCount());
    }

    @Test
    public void getSectionCount() {
        assertEquals(6, adapter.getSectionCount());
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
        verify(header).setText("Pinned");
    }

    @Test
    public void bindHeaderToday() {
        adapter.onBindHeaderViewHolder(getMockedViewHolder(), 1);
        verify(header).setText("Today");
    }

    @Test
    public void bindHeaderYesterday() {
        adapter.onBindHeaderViewHolder(getMockedViewHolder(), 2);
        verify(header).setText("Yesterday");
    }

    @Test
    public void bindHeaderThisWeek() {
        adapter.onBindHeaderViewHolder(getMockedViewHolder(), 3);
        verify(header).setText("This week");
    }

    @Test
    public void bindHeaderThisMonth() {
        adapter.onBindHeaderViewHolder(getMockedViewHolder(), 4);
        verify(header).setText("This month");
    }

    @Test
    public void bindHeaderOlder() {
        adapter.onBindHeaderViewHolder(getMockedViewHolder(), 5);
        verify(header).setText("Older");
    }

    @Test
    public void bindConversationRead_unmuted() {
        ConversationViewHolder holder = getMockedViewHolder();
        adapter.onBindViewHolder(holder, 0, 0, 0);

        assertNotNull(holder.conversation);
        verify(image).setImageDrawable(any(Drawable.class));
        verify(name).setText("Luke Klinker");
        verify(summary).setText("So maybe not going to be able to get platinum huh?");
        verify(name, times(1)).setTypeface(any(Typeface.class), anyInt());
        verify(summary, times(1)).setTypeface(any(Typeface.class), anyInt());
        verify(imageLetter).setText("L");
    }

    @Test
    public void bindConversationRead_muted() {
        ConversationViewHolder holder = getMockedViewHolder();
        adapter.onBindViewHolder(holder, 0, 0, 1);

        assertNotNull(holder.conversation);
        verify(image).setImageDrawable(any(Drawable.class));
        verify(name).setText("Matt Swiontek");
        verify(summary).setText("Whoops ya idk what happened but anysho drive safe");
        verify(name).setTypeface(Typeface.DEFAULT, Typeface.ITALIC);
        verify(summary).setTypeface(Typeface.DEFAULT, Typeface.ITALIC);
        verify(imageLetter).setText("M");
    }

    @Test
    public void bindConversationUnread_unmuted() {
        ConversationViewHolder holder = getMockedViewHolder();
        adapter.onBindViewHolder(holder, 1, 0, 2);

        assertNotNull(holder.conversation);
        verify(image).setImageDrawable(any(Drawable.class));
        verify(name).setText("Kris Klinker");
        verify(summary).setText("Will probably be there from 6:30-9, just stop by when you can!");
        verify(name).setTypeface(Typeface.DEFAULT_BOLD, Typeface.NORMAL);
        verify(summary).setTypeface(Typeface.DEFAULT_BOLD, Typeface.NORMAL);
        verify(imageLetter).setText("K");
    }

    @Test
    public void bindConversationUnread_muted() {
        ConversationViewHolder holder = getMockedViewHolder();
        adapter.onBindViewHolder(holder, 1, 0, 8);

        assertNotNull(holder.conversation);
        verify(image).setImageDrawable(any(Drawable.class));
        verify(name).setText("test 2");
        verify(summary).setText("Maybe they'll run into each other on the way back... idk");
        verify(name).setTypeface(Typeface.DEFAULT_BOLD, Typeface.ITALIC);
        verify(summary).setTypeface(Typeface.DEFAULT_BOLD, Typeface.ITALIC);
        verify(imageLetter).setText("t");
    }

    @Test
    public void removeItems() {
        adapter.deleteItem(1);

        assertEquals(6, adapter.getSectionCount());
        assertEquals(1, adapter.getItemCount(0));
        assertEquals(1, adapter.getItemCount(1));
        assertEquals(2, adapter.getItemCount(2));
        assertEquals(2, adapter.getItemCount(3));
        assertEquals(1, adapter.getItemCount(4));
        assertEquals(1, adapter.getItemCount(5));

        adapter.deleteItem(3);

        assertEquals(5, adapter.getSectionCount());
        assertEquals(1, adapter.getItemCount(0));
        assertEquals(2, adapter.getItemCount(1));
        assertEquals(2, adapter.getItemCount(2));
        assertEquals(1, adapter.getItemCount(3));
        assertEquals(1, adapter.getItemCount(4));

        adapter.deleteItem(7);
        adapter.deleteItem(6);

        assertEquals(4, adapter.getSectionCount());
        assertEquals(1, adapter.getItemCount(0));
        assertEquals(2, adapter.getItemCount(1));
        assertEquals(1, adapter.getItemCount(2));
        assertEquals(1, adapter.getItemCount(3));

        adapter.deleteItem(1);

        assertEquals(3, adapter.getSectionCount());
        assertEquals(2, adapter.getItemCount(0));
        assertEquals(1, adapter.getItemCount(1));
        assertEquals(1, adapter.getItemCount(2));

        adapter.deleteItem(2);

        assertEquals(3, adapter.getSectionCount());
        assertEquals(1, adapter.getItemCount(0));
        assertEquals(1, adapter.getItemCount(1));
        assertEquals(1, adapter.getItemCount(2));

        adapter.deleteItem(1);

        assertEquals(2, adapter.getSectionCount());
        assertEquals(1, adapter.getItemCount(0));
        assertEquals(1, adapter.getItemCount(1));

        adapter.deleteItem(3);

        assertEquals(1, adapter.getSectionCount());
        assertEquals(1, adapter.getItemCount(0));

        adapter.deleteItem(1);

        assertEquals(0, adapter.getSectionCount());
    }

    @Test
    public void findPositionForConversationId() {
        assertEquals(1, adapter.findPositionForConversationId(1));
        assertEquals(2, adapter.findPositionForConversationId(2));
        assertEquals(4, adapter.findPositionForConversationId(3));
        assertEquals(6, adapter.findPositionForConversationId(4));
        assertEquals(7, adapter.findPositionForConversationId(5));
        assertEquals(9, adapter.findPositionForConversationId(6));
        assertEquals(10, adapter.findPositionForConversationId(7));
        assertEquals(12, adapter.findPositionForConversationId(8));
        assertEquals(14, adapter.findPositionForConversationId(9));
    }

    @Test
    public void findPositionForConversationIdNonexistant() {
        assertEquals(-1, adapter.findPositionForConversationId(-1));
        assertEquals(-1, adapter.findPositionForConversationId(0));
        assertEquals(-1, adapter.findPositionForConversationId(10));
    }

    private ConversationViewHolder getMockedViewHolder() {
        ConversationViewHolder holder = new ConversationViewHolder(
                new View(RuntimeEnvironment.application), conversationExpandedListener, adapter);
        holder.header = header;
        holder.headerCardForTextOnline = headerDialog;
        holder.headerDone = headerDoneButton;
        holder.image = image;
        holder.name = name;
        holder.summary = summary;
        holder.imageLetter = imageLetter;
        holder.unreadIndicator = unreadIndicator;
        holder.groupIcon = groupIcon;

        return holder;
    }

    private List<Conversation> getFakeConversations(Context context) {
        MatrixCursor cursor = new MatrixCursor(new String[]{
                Conversation.Companion.getCOLUMN_ID(),
                Conversation.Companion.getCOLUMN_COLOR(),
                Conversation.Companion.getCOLUMN_COLOR_DARK(),
                Conversation.Companion.getCOLUMN_COLOR_LIGHT(),
                Conversation.Companion.getCOLUMN_COLOR_ACCENT(),
                Conversation.Companion.getCOLUMN_PINNED(),
                Conversation.Companion.getCOLUMN_READ(),
                Conversation.Companion.getCOLUMN_TIMESTAMP(),
                Conversation.Companion.getCOLUMN_TITLE(),
                Conversation.Companion.getCOLUMN_PHONE_NUMBERS(),
                Conversation.Companion.getCOLUMN_SNIPPET(),
                Conversation.Companion.getCOLUMN_RINGTONE(),
                Conversation.Companion.getCOLUMN_MUTE()
        });

        cursor.addRow(new Object[]{
                1,
                ColorSet.Companion.INDIGO(context).getColor(),
                ColorSet.Companion.INDIGO(context).getColorDark(),
                ColorSet.Companion.INDIGO(context).getColorLight(),
                ColorSet.Companion.INDIGO(context).getColorAccent(),
                1,
                1,
                System.currentTimeMillis() - (1000 * 60 * 60),
                "Luke Klinker",
                "(515) 991-1493",
                "So maybe not going to be able to get platinum huh?",
                null,
                0
        });

        cursor.addRow(new Object[]{
                2,
                ColorSet.Companion.RED(context).getColor(),
                ColorSet.Companion.RED(context).getColorDark(),
                ColorSet.Companion.RED(context).getColorLight(),
                ColorSet.Companion.RED(context).getColorAccent(),
                1,
                1,
                System.currentTimeMillis() - (1000 * 60 * 60 * 12),
                "Matt Swiontek",
                "(708) 928-0846",
                "Whoops ya idk what happened but anysho drive safe",
                null,
                1
        });

        cursor.addRow(new Object[]{
                3,
                ColorSet.Companion.PINK(context).getColor(),
                ColorSet.Companion.PINK(context).getColorDark(),
                ColorSet.Companion.PINK(context).getColorLight(),
                ColorSet.Companion.PINK(context).getColorAccent(),
                0,
                0,
                System.currentTimeMillis() - (1000 * 60),
                "Kris Klinker",
                "(515) 419-6726",
                "Will probably be there from 6:30-9, just stop by when you can!",
                null,
                0
        });

        cursor.addRow(new Object[]{
                4,
                ColorSet.Companion.BLUE(context).getColor(),
                ColorSet.Companion.BLUE(context).getColorDark(),
                ColorSet.Companion.BLUE(context).getColorLight(),
                ColorSet.Companion.BLUE(context).getColorAccent(),
                0,
                1,
                System.currentTimeMillis() - (1000 * 60 * 60 * 24),
                "Andrew Klinker",
                "(515) 991-8235",
                "Just finished, it was a lot of fun",
                null,
                0
        });

        cursor.addRow(new Object[]{
                5,
                ColorSet.Companion.GREEN(context).getColor(),
                ColorSet.Companion.GREEN(context).getColorDark(),
                ColorSet.Companion.GREEN(context).getColorLight(),
                ColorSet.Companion.GREEN(context).getColorAccent(),
                0,
                1,
                System.currentTimeMillis() - (1000 * 60 * 60 * 24),
                "Aaron Klinker",
                "(515) 556-7749",
                "Yeah I'll do it when I get home",
                null,
                0
        });

        cursor.addRow(new Object[]{
                6,
                ColorSet.Companion.BROWN(context).getColor(),
                ColorSet.Companion.BROWN(context).getColorDark(),
                ColorSet.Companion.BROWN(context).getColorLight(),
                ColorSet.Companion.BROWN(context).getColorAccent(),
                0,
                1,
                System.currentTimeMillis() - (1000 * 60 * 60 * 55),
                "Mike Klinker",
                "(515) 480-8532",
                "Yeah so hiking around in some place called beaver meadows now.",
                null,
                0
        });

        cursor.addRow(new Object[]{
                7,
                ColorSet.Companion.PURPLE(context).getColor(),
                ColorSet.Companion.PURPLE(context).getColorDark(),
                ColorSet.Companion.PURPLE(context).getColorLight(),
                ColorSet.Companion.PURPLE(context).getColorAccent(),
                0,
                1,
                System.currentTimeMillis() - (1000 * 60 * 60 * 78),
                "Ben Madden",
                "(847) 609-0939",
                "Maybe they'll run into each other on the way back... idk",
                null,
                0
        });

        cursor.addRow(new Object[] {
                8,
                ColorSet.Companion.PURPLE(context).getColor(),
                ColorSet.Companion.PURPLE(context).getColorDark(),
                ColorSet.Companion.PURPLE(context).getColorLight(),
                ColorSet.Companion.PURPLE(context).getColorAccent(),
                0,
                1,
                System.currentTimeMillis() - (1000 * 60 * 60 * 24 * 8),
                "test 1",
                "(847) 609-0939",
                "Maybe they'll run into each other on the way back... idk",
                null,
                0
        });

        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.MONTH, cal.get(Calendar.MONTH) - 2);
        cursor.addRow(new Object[] {
                9,
                ColorSet.Companion.PURPLE(context).getColor(),
                ColorSet.Companion.PURPLE(context).getColorDark(),
                ColorSet.Companion.PURPLE(context).getColorLight(),
                ColorSet.Companion.PURPLE(context).getColorAccent(),
                0,
                0,
                cal.getTimeInMillis(),
                "test 2",
                "(847) 609-0939",
                "Maybe they'll run into each other on the way back... idk",
                null,
                1
        });

        List<Conversation> conversations = new ArrayList<>();

        if (cursor.moveToFirst()) {
            do {
                Conversation conversation = new Conversation();
                conversation.fillFromCursor(cursor);

                conversations.add(conversation);
            } while (cursor.moveToNext());
        }

        return conversations;
    }

}