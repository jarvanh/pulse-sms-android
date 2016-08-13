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

public class Blacklist implements DatabaseSQLiteHelper.DatabaseTable {

    public static final String TABLE = "blacklist";
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_PHONE_NUMBER = "phone_number";

    private static final String DATABASE_CREATE = "create table if not exists " +
            TABLE + " (" +
            COLUMN_ID + " integer primary key, " +
            COLUMN_PHONE_NUMBER + " text not null" +
            ");";

    private static final String[] INDEXES = {

    };

    public long id;
    public String phoneNumber;

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
            } else if (column.equals(COLUMN_PHONE_NUMBER)) {
                this.phoneNumber = cursor.getString(i);
            }
        }
    }

    @Override
    public void encrypt(EncryptionUtils utils) {
        this.phoneNumber = utils.encrypt(this.phoneNumber);
    }

    @Override
    public void decrypt(EncryptionUtils utils) {
        this.phoneNumber = utils.decrypt(this.phoneNumber);
    }

}
