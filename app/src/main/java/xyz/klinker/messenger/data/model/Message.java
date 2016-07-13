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
import android.database.MatrixCursor;

import java.util.ArrayList;
import java.util.List;

import xyz.klinker.messenger.data.DatabaseSQLiteHelper;

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
    public static final String COLUMN_FROM = "from";
    public static final String COLUMN_COLOR = "color";

    private static final String DATABASE_CREATE = "create table if not exists " +
            TABLE + " (" +
            COLUMN_ID + " integer primary key autoincrement, " +
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

    public static Cursor getFakeMessages() {
        MatrixCursor cursor = new MatrixCursor(new String[] {
                COLUMN_ID,
                COLUMN_CONVERSATION_ID,
                COLUMN_TYPE,
                COLUMN_DATA,
                COLUMN_TIMESTAMP,
                COLUMN_MIME_TYPE,
                COLUMN_READ,
                COLUMN_SEEN,
                COLUMN_FROM,
                COLUMN_COLOR
        });

        cursor.addRow(new Object[] {
                1,
                1,
                TYPE_RECEIVED,
                "Do you want to go to summerfest this weekend?",
                System.currentTimeMillis() - (1000 * 60 * 60 * 12) - (1000 * 60 * 30),
                "text/plain",
                1,
                1,
                "Luke Klinker",
                null
        });

        cursor.addRow(new Object[] {
                2,
                1,
                TYPE_SENT,
                "Yeah, I'll probably go on Friday.",
                System.currentTimeMillis() - (1000 * 60 * 60 * 12),
                "text/plain",
                1,
                1,
                null,
                null
        });

        cursor.addRow(new Object[] {
                3,
                1,
                TYPE_SENT,
                "I started working on the designs for a new messaging app today... I'm thinking " +
                        "that it could be somewhere along the lines of a compliment to Evolve. " +
                        "The main app will be focused on tablet design and so Evolve could " +
                        "support hooking up to the same backend and the two could be used " +
                        "together. Or, users could just use this app on their phone as well... " +
                        "up to them which they prefer.",
                System.currentTimeMillis() - (1000 * 60 * 60 * 8) - (1000 * 60 * 6),
                "text/plain",
                1,
                1,
                null,
                null
        });

        cursor.addRow(new Object[] {
                4,
                1,
                TYPE_RECEIVED,
                "Are you going to make this into an actual app?",
                System.currentTimeMillis() - (1000 * 60 * 60 * 8),
                "text/plain",
                1,
                1,
                "Luke Klinker",
                null
        });

        cursor.addRow(new Object[] {
                5,
                1,
                TYPE_SENT,
                "dunno",
                System.currentTimeMillis() - (1000 * 60 * 60 * 7) - (1000 * 60 * 55),
                "text/plain",
                1,
                1,
                null,
                null
        });

        cursor.addRow(new Object[] {
                6,
                1,
                TYPE_SENT,
                "I got to build some Legos, plus get 5 extra character packs and 3 level packs " +
                        "with the deluxe edition lol",
                System.currentTimeMillis() - (1000 * 60 * 38),
                "text/plain",
                1,
                1,
                null,
                null
        });

        cursor.addRow(new Object[] {
                7,
                1,
                TYPE_RECEIVED,
                "woah nice one haha",
                System.currentTimeMillis() - (1000 * 60 * 37),
                "text/plain",
                1,
                1,
                "Luke Klinker",
                null
        });

        cursor.addRow(new Object[] {
                8,
                1,
                TYPE_SENT,
                "Already shaping up to be a better deal than battlefront!",
                System.currentTimeMillis() - (1000 * 60 * 23),
                "text/plain",
                1,
                1,
                null,
                null
        });

        cursor.addRow(new Object[] {
                9,
                1,
                TYPE_RECEIVED,
                "is it fun?",
                System.currentTimeMillis() - (1000 * 60 * 22),
                "text/plain",
                1,
                1,
                "Luke Klinker",
                null
        });

        cursor.addRow(new Object[] {
                10,
                1,
                TYPE_SENT,
                "So far! Looks like a lot of content in the game too. Based on the trophies " +
                        "required at least",
                System.currentTimeMillis() - (1000 * 60 * 20),
                "text/plain",
                1,
                1,
                null,
                null
        });

        cursor.addRow(new Object[] {
                11,
                1,
                TYPE_RECEIVED,
                "so maybe not going to be able to get platinum huh? haha",
                System.currentTimeMillis() - (1000 * 60 * 16),
                "text/plain",
                1,
                1,
                "Luke Klinker",
                null
        });

        cursor.addRow(new Object[] {
                12,
                1,
                TYPE_SENT,
                "Oh, I will definitely get it! Just might take 24+ hours to do it... and when " +
                        "those 24 hours are in a single week, things get to be a little tedious. " +
                        "Hopefully I don't absolutely hate the game once I finish!",
                System.currentTimeMillis() - (1000 * 60),
                "text/plain",
                1,
                1,
                null,
                null
        });

        return cursor;
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
                try { this.color = cursor.getInt(i); } catch (NullPointerException e) { }
            }
        }
    }

}
