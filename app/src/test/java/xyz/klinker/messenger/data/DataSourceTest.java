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

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.robolectric.RuntimeEnvironment;

import xyz.klinker.messenger.MessengerRobolectricSuite;
import xyz.klinker.messenger.activity.InitialLoadActivity;
import xyz.klinker.messenger.data.model.Conversation;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
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
    public void realConstructor() {
        DataSource dataSource = DataSource.getInstance(RuntimeEnvironment.application);
        dataSource.open();
        dataSource.close();
    }

    @Test
    public void getDatabase() {
        assertEquals(database, source.getDatabase());
    }

    @Test
    public void beginTransaction() {
        source.beginTransaction();
        verify(database).beginTransaction();
    }

    @Test
    public void setTransactionSuccessful() {
        source.setTransactionSuccessful();
        verify(database).setTransactionSuccessful();
    }

    @Test
    public void endTransaction() {
        source.endTransaction();
        verify(database).endTransaction();
    }

    @Test
    public void execSql() {
        source.execSql("test sql");
        verify(database).execSQL("test sql");
    }

    @Test
    public void rawQuery() {
        source.rawQuery("test sql");
        verify(database).rawQuery("test sql", null);
    }

    @Test
    public void insertConversations() {
        source.insertConversations(InitialLoadActivity
                .getFakeConversations(RuntimeEnvironment.application.getResources()));

        verify(database, times(7)).insert(eq("conversation"), eq((String) null),
                any(ContentValues.class));
    }

    @Test
    public void getConversations() {
        when(database.query("conversation", null, null, null, null, null,
                "pinned desc, timestamp desc")).thenReturn(cursor);

        assertEquals(cursor, source.getConversations());
    }

    @Test
    public void deleteConversation() {
        Conversation conversation = new Conversation();
        conversation.id = 1;
        source.deleteConversation(conversation);

        verify(database).delete("conversation", "_id=?", new String[] {"1"});
    }

}