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

public class BlacklistFillTest extends MessengerRobolectricSuite {

    @Test
    public void fillFromCursor() {
        Blacklist blacklist = new Blacklist();
        Cursor cursor = createCursor();
        cursor.moveToFirst();
        blacklist.fillFromCursor(cursor);

        assertEquals(1, blacklist.id);
        assertEquals("5154224558", blacklist.phoneNumber);
    }

    private Cursor createCursor() {
        MatrixCursor cursor = new MatrixCursor(new String[]{
                Blacklist.COLUMN_ID,
                Blacklist.COLUMN_PHONE_NUMBER,
        });

        cursor.addRow(new Object[]{
                1,
                "5154224558"
        });

        return cursor;
    }

}