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

package xyz.klinker.messenger.data.model;

import android.database.Cursor;
import android.database.MatrixCursor;
import android.graphics.Color;

import org.junit.Test;

import xyz.klinker.messenger.MessengerRobolectricSuite;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ConversationFillTest extends MessengerRobolectricSuite {

    @Test
    public void fillFromCursor() {
        Conversation conversation = new Conversation();
        Cursor cursor = createCursor();
        cursor.moveToFirst();
        conversation.fillFromCursor(cursor);

        assertEquals(1, conversation.id);
        assertEquals(Color.RED, conversation.colors.color);
        assertEquals(Color.BLUE, conversation.colors.colorDark);
        assertEquals(Color.YELLOW, conversation.colors.colorLight);
        assertEquals(Color.GREEN, conversation.colors.colorAccent);
        assertTrue(conversation.pinned);
        assertTrue(conversation.read);
        assertEquals(1000L, conversation.timestamp);
        assertEquals("Luke Klinker", conversation.title);
        assertEquals("So maybe not going to be able to get platinum huh?", conversation.snippet);
        assertEquals("uri", conversation.ringtoneUri);
        assertEquals("image_uri", conversation.imageUri);
        assertEquals("11493", conversation.idMatcher);
        assertFalse(conversation.mute);
    }

    private Cursor createCursor() {
        MatrixCursor cursor = new MatrixCursor(new String[]{
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
                Conversation.COLUMN_RINGTONE,
                Conversation.COLUMN_IMAGE_URI,
                Conversation.COLUMN_ID_MATCHER,
                Conversation.COLUMN_MUTE
        });

        cursor.addRow(new Object[]{
                1,
                Color.RED,
                Color.BLUE,
                Color.YELLOW,
                Color.GREEN,
                1,
                1,
                1000L,
                "Luke Klinker",
                "(515) 991-1493",
                "So maybe not going to be able to get platinum huh?",
                "uri",
                "image_uri",
                "11493",
                0
        });

        return cursor;
    }

}