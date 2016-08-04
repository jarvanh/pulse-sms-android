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

import org.junit.Test;

import xyz.klinker.messenger.MessengerRobolectricSuite;

import static org.junit.Assert.*;

public class ScheduledMessageFillTest extends MessengerRobolectricSuite {

    @Test
    public void fillFromCursor() {
        ScheduledMessage message = new ScheduledMessage();
        Cursor cursor = createCursor();
        cursor.moveToFirst();
        message.fillFromCursor(cursor);

        assertEquals(1, message.id);
        assertEquals("luke", message.title);
        assertEquals("5159911493", message.to);
        assertEquals("Do you want to go to summerfest this weekend?", message.data);
        assertEquals("text/plain", message.mimeType);
        assertEquals(1001L, message.timestamp);
    }

    private Cursor createCursor() {
        MatrixCursor cursor = new MatrixCursor(new String[] {
                ScheduledMessage.COLUMN_ID,
                ScheduledMessage.COLUMN_TITLE,
                ScheduledMessage.COLUMN_TO,
                ScheduledMessage.COLUMN_DATA,
                ScheduledMessage.COLUMN_MIME_TYPE,
                ScheduledMessage.COLUMN_TIMESTAMP
        });

        cursor.addRow(new Object[] {
                1,
                "luke",
                "5159911493",
                "Do you want to go to summerfest this weekend?",
                "text/plain",
                1001L
        });

        return cursor;
    }

}