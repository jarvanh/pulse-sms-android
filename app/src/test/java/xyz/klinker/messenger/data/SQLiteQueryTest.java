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

import java.util.ArrayList;
import java.util.List;

import xyz.klinker.messenger.MessengerRobolectricSuite;
import xyz.klinker.messenger.activity.InitialLoadActivity;
import xyz.klinker.messenger.data.model.Conversation;
import xyz.klinker.messenger.data.model.Message;
import xyz.klinker.messenger.util.FixtureLoader;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNotSame;
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
    public void databaseCreated() {
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

    @Test
    public void insertConversations() {
        int initialSize = source.getConversations().getCount();
        source.insertConversations(InitialLoadActivity
                .getFakeConversations(RuntimeEnvironment.application.getResources()),
                RuntimeEnvironment.application);
        int newSize = source.getConversations().getCount();

        assertEquals(7, newSize - initialSize);
    }

    @Test
    public void getConversations() {
        List<String> titles = new ArrayList<>();
        Cursor cursor = source.getConversations();

        if (cursor.moveToFirst()) {
            do {
                titles.add(cursor.getString(cursor.getColumnIndex(Conversation.COLUMN_TITLE)));
            } while (cursor.moveToNext());

            cursor.close();
        }

        assertEquals("Andrew Klinker", titles.get(0));
        assertEquals("Luke Klinker", titles.get(1));
        assertEquals("Aaron Klinker", titles.get(2));
    }

    @Test
    public void deleteConversation() {
        assertNotSame(0, source.getMessages(1).getCount());
        int initialConversationSize = source.getConversations().getCount();
        source.deleteConversation(1);
        int newConversationSize = source.getConversations().getCount();
        int newMessageSize = source.getMessages(1).getCount();

        assertEquals(-1, newConversationSize - initialConversationSize);
        assertEquals(0, newMessageSize);
    }

    @Test
    public void getMessages() {
        assertNotSame(0, source.getMessages(1).getCount());
        assertNotSame(0, source.getMessages(2).getCount());
        assertNotSame(0, source.getMessages(3).getCount());
    }

    @Test
    public void insertMessage() {
        int initialSize = source.getMessages(2).getCount();

        Message m = new Message();
        m.conversationId = 2;
        m.type = Message.TYPE_SENT;
        m.data = "test message";
        m.timestamp = System.currentTimeMillis();
        m.mimeType = "text/plain";
        m.read = true;
        m.seen = true;
        m.from = null;
        m.color = null;
        source.insertMessage(m);

        int newSize = source.getMessages(2).getCount();

        assertEquals(1, newSize - initialSize);

        Cursor conversation = source.getConversations();
        conversation.moveToFirst();
        assertEquals("test message", conversation
                .getString(conversation.getColumnIndex(Conversation.COLUMN_SNIPPET)));
    }

    private void insertData() throws Exception {
        SQLiteDatabase database = source.getDatabase();
        FixtureLoader loader = new FixtureLoader();
        loader.loadFixturesToDatabase(database);
    }

}