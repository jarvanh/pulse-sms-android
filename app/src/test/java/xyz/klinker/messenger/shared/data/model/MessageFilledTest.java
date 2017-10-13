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

package xyz.klinker.messenger.shared.data.model;

import android.database.Cursor;
import android.database.MatrixCursor;

import org.junit.Test;

import xyz.klinker.messenger.MessengerRobolectricSuite;

import static org.junit.Assert.assertEquals;

public class MessageFilledTest extends MessengerRobolectricSuite {

    @Test
    public void fillFromCursor() {
        Message message = new Message();
        Cursor cursor = createCursor();
        cursor.moveToFirst();
        message.fillFromCursor(cursor);

        assertEquals(1, message.getId());
        assertEquals(1, message.getConversationId());
        assertEquals(Message.Companion.getTYPE_RECEIVED(), message.getType());
        assertEquals("Do you want to go to summerfest this weekend?", message.getData());
        assertEquals(1001L, message.getTimestamp());
        assertEquals("text/plain", message.getMimeType());
        assertEquals(true, message.getSeen());
        assertEquals(true, message.getRead());
        assertEquals("Luke Klinker", message.getFrom());
        assertEquals(null, message.getColor());
    }

    private Cursor createCursor() {
        MatrixCursor cursor = new MatrixCursor(new String[]{
                Message.Companion.getCOLUMN_ID(),
                Message.Companion.getCOLUMN_CONVERSATION_ID(),
                Message.Companion.getCOLUMN_TYPE(),
                Message.Companion.getCOLUMN_DATA(),
                Message.Companion.getCOLUMN_TIMESTAMP(),
                Message.Companion.getCOLUMN_MIME_TYPE(),
                Message.Companion.getCOLUMN_READ(),
                Message.Companion.getCOLUMN_SEEN(),
                Message.Companion.getCOLUMN_FROM(),
                Message.Companion.getCOLUMN_COLOR()
        });

        cursor.addRow(new Object[]{
                1,
                1,
                Message.Companion.getTYPE_RECEIVED(),
                "Do you want to go to summerfest this weekend?",
                1001L,
                "text/plain",
                1,
                1,
                "Luke Klinker",
                null
        });

        return cursor;
    }

}