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

import android.database.sqlite.SQLiteDatabase;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.robolectric.RuntimeEnvironment;

import xyz.klinker.messenger.MessengerRobolectricSuite;
import xyz.klinker.messenger.data.model.Blacklist;
import xyz.klinker.messenger.data.model.Contact;
import xyz.klinker.messenger.data.model.Conversation;
import xyz.klinker.messenger.data.model.Draft;
import xyz.klinker.messenger.data.model.Message;
import xyz.klinker.messenger.data.model.ScheduledMessage;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

public class DatabaseSQLiteHelperTest extends MessengerRobolectricSuite {

    private DatabaseSQLiteHelper helper;

    @Mock
    private SQLiteDatabase database;

    @Before
    public void setUp() {
        helper = new DatabaseSQLiteHelper(RuntimeEnvironment.application);
    }

    @Test
    public void onCreate() {
        helper.onCreate(database);
        verifyCreateStatement();
    }

    @Test
    public void onUpgrade_1to2() {
        helper.onUpgrade(database, 1, 2);
        verify2Upgrade();
    }

    @Test
    public void onDrop() {
        helper.onDrop(database);
        verifyDropStatement();
    }

    private void verifyCreateStatement() {
        verify(database).execSQL(new Contact().getCreateStatement());
        verify(database).execSQL(new Conversation().getCreateStatement());
        verify(database).execSQL(new Draft().getCreateStatement());
        verify(database).execSQL(new Message().getCreateStatement());
        verify(database).execSQL(new ScheduledMessage().getCreateStatement());
        verify(database).execSQL(new Blacklist().getCreateStatement());
        verify(database).execSQL(new Message().getIndexStatements()[0]);
        verify(database).execSQL(new Draft().getIndexStatements()[0]);
        verifyNoMoreInteractions(database);
    }

    private void verify2Upgrade() {
        // do nothing for now, should be filled when database needs updated.
    }

    private void verifyDropStatement() {
        verify(database).execSQL("drop table if exists " + Contact.TABLE);
        verify(database).execSQL("drop table if exists " + Conversation.TABLE);
        verify(database).execSQL("drop table if exists " + Draft.TABLE);
        verify(database).execSQL("drop table if exists " + Message.TABLE);
        verify(database).execSQL("drop table if exists " + ScheduledMessage.TABLE);
        verify(database).execSQL("drop table if exists " + Blacklist.TABLE);
        verifyNoMoreInteractions(database);
    }

}