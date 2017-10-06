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

import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.provider.ContactsContract;

import xyz.klinker.messenger.api.entity.ConversationBody;
import xyz.klinker.messenger.shared.data.ColorSet;
import xyz.klinker.messenger.shared.data.DatabaseSQLiteHelper;
import xyz.klinker.messenger.encryption.EncryptionUtils;
import xyz.klinker.messenger.shared.util.ColorUtils;

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
    public static final String COLUMN_ID_MATCHER = "id_matcher";
    public static final String COLUMN_MUTE = "mute";
    public static final String COLUMN_ARCHIVED = "archive"; // created in database v2
    public static final String COLUMN_PRIVATE_NOTIFICATIONS = "private_notifications"; // created in database v4
    public static final String COLUMN_LED_COLOR = "led_color"; // created in database v5
    public static final String COLUMN_SIM_SUBSCRIPTION_ID = "sim_subscription_id"; // created in database v6

    private static final String DATABASE_CREATE = "create table if not exists " +
            TABLE + " (" +
            COLUMN_ID + " integer primary key, " +
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
            COLUMN_IMAGE_URI + " text, " +
            COLUMN_ID_MATCHER + " text not null unique, " +
            COLUMN_MUTE + " integer not null, " +
            COLUMN_ARCHIVED + " integer not null default 0, " +
            COLUMN_PRIVATE_NOTIFICATIONS + " integer not null default 0, " +
            COLUMN_LED_COLOR + " integer not null default " + Color.WHITE + ", " +
            COLUMN_SIM_SUBSCRIPTION_ID + " integer default -1" +
            ");";

    public long id;
    public ColorSet colors = new ColorSet();
    public int ledColor;
    public boolean pinned;
    public boolean read;
    public long timestamp;
    public String title;
    public String phoneNumbers;
    public String snippet;
    public String ringtoneUri;
    public String imageUri;
    public String idMatcher;
    public boolean mute;
    public boolean archive;
    public boolean privateNotifications;
    public Integer simSubscriptionId;

    public Conversation() {

    }

    public Conversation(ConversationBody body) {
        this.id = body.deviceId;
        this.colors.color = body.color;
        this.colors.colorDark = body.colorDark;
        this.colors.colorLight = body.colorLight;
        this.colors.colorAccent = body.colorAccent;
        this.ledColor = body.ledColor;
        this.pinned = body.pinned;
        this.read = body.read;
        this.timestamp = body.timestamp;
        this.title = body.title;
        this.phoneNumbers = body.phoneNumbers;
        this.snippet = body.snippet;
        this.ringtoneUri = body.ringtone;
        this.imageUri = body.imageUri;
        this.idMatcher = body.idMatcher;
        this.mute = body.mute;
        this.archive = body.archive;
        this.privateNotifications = body.privateNotifications;
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
            } else if (column.equals(COLUMN_LED_COLOR)) {
                this.ledColor = cursor.getInt(i);
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
            } else if (column.equals(COLUMN_ID_MATCHER)) {
                this.idMatcher = cursor.getString(i);
            } else if (column.equals(COLUMN_MUTE)) {
                this.mute = cursor.getInt(i) == 1;
            } else if (column.equals(COLUMN_ARCHIVED)) {
                this.archive = cursor.getInt(i) == 1;
            } else if (column.equals(COLUMN_PRIVATE_NOTIFICATIONS)) {
                this.privateNotifications = cursor.getInt(i) == 1;
            } else if (column.equals(COLUMN_SIM_SUBSCRIPTION_ID)) {
                this.simSubscriptionId = cursor.getInt(i) == -1 ? null : cursor.getInt(i);
            }
        }
    }

    public void fillFromContactGroupCursor(Context context, Cursor cursor) {
        colors = ColorUtils.getRandomMaterialColor(context);

        for (int i = 0; i < cursor.getColumnCount(); i++) {
            String column = cursor.getColumnName(i);

            if (column.equals(ContactsContract.Groups._ID)) {
                this.id = cursor.getLong(i);
            } else if (column.equals(ContactsContract.Groups.TITLE)) {
                this.title = cursor.getString(i);
                if (title.contains("Group:")) {
                    title = title.substring(title.indexOf("Group:") + "Group:".length()).trim();
                }
            }
        }
    }

    @Override
    public void encrypt(EncryptionUtils utils) {
        this.title = utils.encrypt(this.title);
        this.phoneNumbers = utils.encrypt(this.phoneNumbers);
        this.snippet = utils.encrypt(this.snippet);
        this.ringtoneUri = utils.encrypt(this.ringtoneUri);
        this.imageUri = utils.encrypt(this.imageUri);
        this.idMatcher = utils.encrypt(this.idMatcher);
    }

    @Override
    public void decrypt(EncryptionUtils utils) {
        this.title = utils.decrypt(this.title);
        this.phoneNumbers = utils.decrypt(this.phoneNumbers);
        this.snippet = utils.decrypt(this.snippet);
        this.ringtoneUri = utils.decrypt(this.ringtoneUri);
        this.imageUri = utils.decrypt(this.imageUri);
        this.idMatcher = utils.decrypt(this.idMatcher);
    }

    public boolean isGroup() {
        return phoneNumbers.split(", ").length > 1;
    }

}