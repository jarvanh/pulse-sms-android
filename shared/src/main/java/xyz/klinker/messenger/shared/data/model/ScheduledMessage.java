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

import xyz.klinker.messenger.api.entity.ScheduledMessageBody;
import xyz.klinker.messenger.shared.data.DatabaseSQLiteHelper;
import xyz.klinker.messenger.encryption.EncryptionUtils;

/**
 * Table for holding drafts for a conversation.
 */
public class ScheduledMessage implements DatabaseSQLiteHelper.DatabaseTable {

    public static final String TABLE = "scheduled_message";
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_TITLE = "title";
    public static final String COLUMN_TO = "phone_number";
    public static final String COLUMN_DATA = "data";
    public static final String COLUMN_MIME_TYPE = "mime_type";
    public static final String COLUMN_TIMESTAMP = "timestamp";

    private static final String DATABASE_CREATE = "create table if not exists " +
            TABLE + " (" +
            COLUMN_ID + " integer primary key, " +
            COLUMN_TITLE + " text not null, " +
            COLUMN_TO + " text not null, " +
            COLUMN_DATA + " text not null, " +
            COLUMN_MIME_TYPE + " text not null, " +
            COLUMN_TIMESTAMP + " integer not null" +
            ");";

    private static final String[] INDEXES = {};

    public long id;
    public String title;
    public String to;
    public String data;
    public String mimeType;
    public long timestamp;

    public ScheduledMessage() {

    }

    public ScheduledMessage(ScheduledMessageBody body) {
        this.id = body.deviceId;
        this.title = body.title;
        this.to = body.to;
        this.data = body.data;
        this.mimeType = body.mimeType;
        this.timestamp = body.timestamp;
    }

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
            } else if (column.equals(COLUMN_TITLE)) {
                this.title = cursor.getString(i);
            } else if (column.equals(COLUMN_TO)) {
                this.to = cursor.getString(i);
            } else if (column.equals(COLUMN_DATA)) {
                this.data = cursor.getString(i);
            } else if (column.equals(COLUMN_MIME_TYPE)) {
                this.mimeType = cursor.getString(i);
            } else if (column.equals(COLUMN_TIMESTAMP)) {
                this.timestamp = cursor.getLong(i);
            }
        }
    }

    @Override
    public void encrypt(EncryptionUtils utils) {
        this.title = utils.encrypt(this.title);
        this.to = utils.encrypt(this.to);
        this.data = utils.encrypt(this.data);
        this.mimeType = utils.encrypt(this.mimeType);
    }

    @Override
    public void decrypt(EncryptionUtils utils) {
        try {
            this.title = utils.decrypt(this.title);
            this.to = utils.decrypt(this.to);
            this.data = utils.decrypt(this.data);
            this.mimeType = utils.decrypt(this.mimeType);
        } catch (Exception e) {

        }
    }

}
