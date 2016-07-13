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

import static org.junit.Assert.*;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.robolectric.RuntimeEnvironment;

import xyz.klinker.messenger.MessengerRobolectricSuite;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DataSourceTest extends MessengerRobolectricSuite {

    private DataSource source;

    @Mock
    private SQLiteDatabase database;
    @Mock
    private DatabaseSQLiteHelper helper;
    @Mock
    private Cursor cursor;

    @Before
    public void setUp() {
        source = new DataSource(helper);
        when(helper.getWritableDatabase()).thenReturn(database);
        source.open();
    }

    @After
    public void tearDown() {
        source.close();
        verify(helper).close();
    }

    @Test
    public void test_realConstructor() {
        DataSource dataSource = DataSource.getInstance(RuntimeEnvironment.application);
        dataSource.open();
        dataSource.close();
    }

    @Test
    public void test_getDatabase() {
        assertEquals(database, source.getDatabase());
    }

    @Test
    public void test_beginTransaction() {
        source.beginTransaction();
        verify(database).beginTransaction();
    }

    @Test
    public void test_setTransactionSuccessful() {
        source.setTransactionSuccessful();
        verify(database).setTransactionSuccessful();
    }

    @Test
    public void test_endTransaction() {
        source.endTransaction();
        verify(database).endTransaction();
    }

    @Test
    public void test_execSql() {
        source.execSql("test sql");
        verify(database).execSQL("test sql");
    }

    @Test
    public void test_rawQuery() {
        source.rawQuery("test sql");
        verify(database).rawQuery("test sql", null);
    }

}