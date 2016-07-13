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

import xyz.klinker.messenger.data.DatabaseSQLiteHelper;

/**
 * Table for holding drafts for a conversation.
 */
public class Draft implements DatabaseSQLiteHelper.DatabaseTable {

    public static final String TABLE = "draft";
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_CONVERSATION_ID = "conversation_id";
    public static final String COLUMN_DATA = "data";
    public static final String COLUMN_MIME_TYPE = "mime_type";

    private static final String DATABASE_CREATE = "create table if not exists " +
            TABLE + " (" +
            COLUMN_ID + " integer primary key autoincrement, " +
            COLUMN_CONVERSATION_ID + " integer not null, " +
            COLUMN_DATA + " text not null, " +
            COLUMN_MIME_TYPE + " text not null" +
            ");";

    private static final String[] INDEXES = {
            "create index if not exists conversation_id_draft_index on " + TABLE +
                    " (" + COLUMN_CONVERSATION_ID + ");"
    };

    public long id;
    public long conversationId;
    public String data;
    public String mimeType;

    @Override
    public String getCreateStatement() {
        return DATABASE_CREATE;
    }

    @Override
    public String getTableName() {
        return TABLE;
    }

    @Override
    public String[] getIndexStatements() {
        return INDEXES;
    }

    @Override
    public void fillFromCursor(Cursor cursor) {
        for (int i = 0; i < cursor.getColumnCount(); i++) {
            String column = cursor.getColumnName(i);

            if (column.equals(COLUMN_ID)) {
                this.id = cursor.getLong(i);
            } else if (column.equals(COLUMN_CONVERSATION_ID)) {
                this.conversationId = cursor.getLong(i);
            } else if (column.equals(COLUMN_DATA)) {
                this.data = cursor.getString(i);
            } else if (column.equals(COLUMN_MIME_TYPE)) {
                this.mimeType = cursor.getString(i);
            }
        }
    }

}
