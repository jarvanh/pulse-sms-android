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

import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;

import java.util.ArrayList;
import java.util.List;

import xyz.klinker.messenger.data.ColorSet;
import xyz.klinker.messenger.data.DatabaseSQLiteHelper;

/**
 * Data object for holding information about a conversation.
 */
public class Conversation implements DatabaseSQLiteHelper.DatabaseTable {

    public static final String TABLE = "conversation";
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_COLOR = "color";
    public static final String COLUMN_COLOR_DARK = "color_dark";
    public static final String COLUMN_COLOR_LIGHT = "color_light";
    public static final String COLUMN_COLOR_ACCENT = "color_accent";
    public static final String COLUMN_PINNED = "pinned";
    public static final String COLUMN_READ = "read";
    public static final String COLUMN_TIMESTAMP = "timestamp";
    public static final String COLUMN_TITLE = "title";
    public static final String COLUMN_PHONE_NUMBERS = "phone_numbers";
    public static final String COLUMN_SNIPPET = "snippet";
    public static final String COLUMN_RINGTONE = "ringtone";
    public static final String COLUMN_IMAGE_URI = "image_uri";

    private static final String DATABASE_CREATE = "create table if not exists " +
            TABLE + " (" +
            COLUMN_ID + " integer primary key autoincrement, " +
            COLUMN_COLOR + " integer not null, " +
            COLUMN_COLOR_DARK + " integer not null, " +
            COLUMN_COLOR_LIGHT + " integer not null, " +
            COLUMN_COLOR_ACCENT + " integer not null, " +
            COLUMN_PINNED + " integer not null, " +
            COLUMN_READ + " integer not null, " +
            COLUMN_TIMESTAMP + " integer not null, " +
            COLUMN_TITLE + " text not null, " +
            COLUMN_PHONE_NUMBERS + " text not null, " +
            COLUMN_SNIPPET + " text, " +
            COLUMN_RINGTONE + " text, " +
            COLUMN_IMAGE_URI + " text" +
            ");";

    public long id;
    public ColorSet colors = new ColorSet();
    public boolean pinned;
    public boolean read;
    public long timestamp;
    public String title;
    public String phoneNumbers;
    public String snippet;
    public String ringtoneUri;
    public String imageUri;

    public static Cursor getFakeConversations(Context context) {
        MatrixCursor cursor = new MatrixCursor(new String[] {
                COLUMN_ID,
                COLUMN_COLOR,
                COLUMN_COLOR_DARK,
                COLUMN_COLOR_LIGHT,
                COLUMN_COLOR_ACCENT,
                COLUMN_PINNED,
                COLUMN_READ,
                COLUMN_TIMESTAMP,
                COLUMN_TITLE,
                COLUMN_PHONE_NUMBERS,
                COLUMN_SNIPPET,
                COLUMN_RINGTONE,
                COLUMN_IMAGE_URI
        });

        cursor.addRow(new Object[] {
                1,
                ColorSet.INDIGO(context).color,
                ColorSet.INDIGO(context).colorDark,
                ColorSet.INDIGO(context).colorLight,
                ColorSet.INDIGO(context).colorAccent,
                1,
                1,
                System.currentTimeMillis() - (1000 * 60 * 60),
                "Luke Klinker",
                "(515) 991-1493",
                "So maybe not going to be able to get platinum huh?",
                null,
                null
        });

        cursor.addRow(new Object[] {
                2,
                ColorSet.RED(context).color,
                ColorSet.RED(context).colorDark,
                ColorSet.RED(context).colorLight,
                ColorSet.RED(context).colorAccent,
                1,
                1,
                System.currentTimeMillis() - (1000 * 60 * 60 * 12),
                "Matt Swiontek",
                "(708) 928-0846",
                "Whoops ya idk what happened but anysho drive safe",
                null,
                null
        });

        cursor.addRow(new Object[] {
                3,
                ColorSet.PINK(context).color,
                ColorSet.PINK(context).colorDark,
                ColorSet.PINK(context).colorLight,
                ColorSet.PINK(context).colorAccent,
                0,
                0,
                System.currentTimeMillis() - (1000 * 60 * 20),
                "Kris Klinker",
                "(515) 419-6726",
                "Will probably be there from 6:30-9, just stop by when you can!",
                null,
                null
        });

        cursor.addRow(new Object[] {
                4,
                ColorSet.BLUE(context).color,
                ColorSet.BLUE(context).colorDark,
                ColorSet.BLUE(context).colorLight,
                ColorSet.BLUE(context).colorAccent,
                0,
                1,
                System.currentTimeMillis() - (1000 * 60 * 60 * 26),
                "Andrew Klinker",
                "(515) 991-8235",
                "Just finished, it was a lot of fun",
                null,
                null
        });

        cursor.addRow(new Object[] {
                5,
                ColorSet.GREEN(context).color,
                ColorSet.GREEN(context).colorDark,
                ColorSet.GREEN(context).colorLight,
                ColorSet.GREEN(context).colorAccent,
                0,
                1,
                System.currentTimeMillis() - (1000 * 60 * 60 * 32),
                "Aaron Klinker",
                "(515) 556-7749",
                "Yeah I'll do it when I get home",
                null,
                null
        });

        cursor.addRow(new Object[] {
                6,
                ColorSet.BROWN(context).color,
                ColorSet.BROWN(context).colorDark,
                ColorSet.BROWN(context).colorLight,
                ColorSet.BROWN(context).colorAccent,
                0,
                1,
                System.currentTimeMillis() - (1000 * 60 * 60 * 55),
                "Mike Klinker",
                "(515) 480-8532",
                "Yeah so hiking around in some place called beaver meadows now.",
                null,
                null
        });

        cursor.addRow(new Object[] {
                7,
                ColorSet.PURPLE(context).color,
                ColorSet.PURPLE(context).colorDark,
                ColorSet.PURPLE(context).colorLight,
                ColorSet.PURPLE(context).colorAccent,
                0,
                1,
                System.currentTimeMillis() - (1000 * 60 * 60 * 78),
                "Ben Madden",
                "(847) 609-0939",
                "Maybe they'll run into each other on the way back... idk",
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
        return new String[0];
    }

    @Override
    public void fillFromCursor(Cursor cursor) {
        for (int i = 0; i < cursor.getColumnCount(); i++) {
            String column = cursor.getColumnName(i);

             if (column.equals(COLUMN_ID)) {
                 this.id = cursor.getLong(i);
             } else if (column.equals(COLUMN_COLOR)) {
                 this.colors.color = cursor.getInt(i);
             } else if (column.equals(COLUMN_COLOR_DARK)) {
                 this.colors.colorDark = cursor.getInt(i);
             } else if (column.equals(COLUMN_COLOR_LIGHT)) {
                 this.colors.colorLight = cursor.getInt(i);
             } else if (column.equals(COLUMN_COLOR_ACCENT)) {
                 this.colors.colorAccent = cursor.getInt(i);
             } else if (column.equals(COLUMN_PINNED)) {
                 this.pinned = cursor.getInt(i) == 1;
             } else if (column.equals(COLUMN_READ)) {
                 this.read = cursor.getInt(i) == 1;
             } else if (column.equals(COLUMN_TIMESTAMP)) {
                 this.timestamp = cursor.getLong(i);
             } else if (column.equals(COLUMN_TITLE)) {
                 this.title = cursor.getString(i);
             } else if (column.equals(COLUMN_PHONE_NUMBERS)) {
                 this.phoneNumbers = cursor.getString(i);
             } else if (column.equals(COLUMN_SNIPPET)) {
                 this.snippet = cursor.getString(i);
             } else if (column.equals(COLUMN_RINGTONE)) {
                 this.ringtoneUri = cursor.getString(i);
             } else if (column.equals(COLUMN_IMAGE_URI)) {
                 this.imageUri = cursor.getString(i);
             }
        }
    }

}