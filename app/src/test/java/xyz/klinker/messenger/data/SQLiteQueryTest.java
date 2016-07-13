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

package xyz.klinker.messenger.data;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import org.junit.Before;
import org.junit.Test;
import org.robolectric.RuntimeEnvironment;

import xyz.klinker.messenger.MessengerRobolectricSuite;
import xyz.klinker.messenger.util.FixtureLoader;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;

public class SQLiteQueryTest extends MessengerRobolectricSuite {

    private DataSource source;

    @Before
    public void setUp() throws Exception {
        SQLiteDatabase database = SQLiteDatabase.create(null);
        DatabaseSQLiteHelper helper = new DatabaseSQLiteHelper(RuntimeEnvironment.application);
        helper.onCreate(database);

        source = new DataSource(database);
        insertData();
    }

    @Test
    public void test_databaseCreated() {
        assertNotNull(source.getDatabase());

        int numTables = 0;
        Cursor cursor = source.getDatabase().rawQuery("SELECT count(*) FROM sqlite_master " +
                "WHERE type = 'table' AND name != 'android_metadata' AND name != " +
                "'sqlite_sequence';", null);
        if (cursor != null && cursor.moveToFirst()) {
            numTables = cursor.getInt(0);
            cursor.close();
        }

        assertTrue(numTables > 0);
    }

    private void insertData() throws Exception {
        SQLiteDatabase database = source.getDatabase();
        FixtureLoader loader = new FixtureLoader();
        loader.loadFixturesToDatabase(database);
    }

}