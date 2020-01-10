/*
 * Copyright (C) 2020 Luke Klinker
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

public class DraftFillTest extends MessengerRobolectricSuite {

    @Test
    public void fillFromCursor() {
        Draft draft = new Draft();
        Cursor cursor = createCursor();
        cursor.moveToFirst();
        draft.fillFromCursor(cursor);

        assertEquals(1, draft.getId());
        assertEquals(1, draft.getConversationId());
        assertEquals("Do you want to go to summerfest this weekend?", draft.getData());
        assertEquals("text/plain", draft.getMimeType());
    }

    private Cursor createCursor() {
        MatrixCursor cursor = new MatrixCursor(new String[]{
                Draft.COLUMN_ID,
                Draft.COLUMN_CONVERSATION_ID,
                Draft.COLUMN_DATA,
                Draft.COLUMN_MIME_TYPE
        });

        cursor.addRow(new Object[]{
                1,
                1,
                "Do you want to go to summerfest this weekend?",
                "text/plain"
        });

        return cursor;
    }

}