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
import xyz.klinker.messenger.encryption.EncryptionUtils;

/**
 * Holds information regarding messages (eg what type they are, what they contain and a timestamp).
 */
public class Message implements DatabaseSQLiteHelper.DatabaseTable {

    public static final String TABLE = "message";
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_CONVERSATION_ID = "conversation_id";
    public static final String COLUMN_TYPE = "type";
    public static final String COLUMN_DATA = "data";
    public static final String COLUMN_TIMESTAMP = "timestamp";
    public static final String COLUMN_MIME_TYPE = "mime_type";
    public static final String COLUMN_READ = "read";
    public static final String COLUMN_SEEN = "seen";
    public static final String COLUMN_FROM = "message_from";
    public static final String COLUMN_COLOR = "color";

    private static final String DATABASE_CREATE = "create table if not exists " +
            TABLE + " (" +
            COLUMN_ID + " integer primary key, " +
            COLUMN_CONVERSATION_ID + " integer not null, " +
            COLUMN_TYPE + " integer not null, " +
            COLUMN_DATA + " text not null, " +
            COLUMN_TIMESTAMP + " integer not null, " +
            COLUMN_MIME_TYPE + " text not null, " +
            COLUMN_READ + " integer not null, " +
            COLUMN_SEEN + " integer not null, " +
            COLUMN_FROM + " text, " +
            COLUMN_COLOR + " integer" +
            ");";

    private static final String[] INDEXES = {
            "create index if not exists conversation_id_message_index on " + TABLE +
                    " (" + COLUMN_CONVERSATION_ID + ");"
    };

    public static final int TYPE_RECEIVED = 0;
    public static final int TYPE_SENT = 1;
    public static final int TYPE_SENDING = 2;
    public static final int TYPE_ERROR = 3;
    public static final int TYPE_DELIVERED = 4;
    public static final int TYPE_INFO = 5;

    public long id;
    public long conversationId;
    public int type;
    public String data;
    public long timestamp;
    public String mimeType;
    public boolean read;
    public boolean seen;
    public String from;
    public Integer color;

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
            } else if (column.equals(COLUMN_TYPE)) {
                this.type = cursor.getInt(i);
            } else if (column.equals(COLUMN_DATA)) {
                this.data = cursor.getString(i);
            } else if (column.equals(COLUMN_TIMESTAMP)) {
                this.timestamp = cursor.getLong(i);
            } else if (column.equals(COLUMN_MIME_TYPE)) {
                this.mimeType = cursor.getString(i);
            } else if (column.equals(COLUMN_READ)) {
                this.read = cursor.getInt(i) == 1;
            } else if (column.equals(COLUMN_SEEN)) {
                this.seen = cursor.getInt(i) == 1;
            } else if (column.equals(COLUMN_FROM)) {
                this.from = cursor.getString(i);
            } else if (column.equals(COLUMN_COLOR)) {
                try {
                    this.color = Integer.parseInt(cursor.getString(i));
                } catch (NumberFormatException e) {
                    this.color = null;
                }
            }
        }
    }

    @Override
    public void encrypt(EncryptionUtils utils) {
        this.data = utils.encrypt(this.data);
        this.mimeType = utils.encrypt(this.mimeType);
        this.from = utils.encrypt(this.from);
    }

    @Override
    public void decrypt(EncryptionUtils utils) {
        this.data = utils.decrypt(this.data);
        this.mimeType = utils.decrypt(this.mimeType);
        this.from = utils.decrypt(this.mimeType);
    }

}
