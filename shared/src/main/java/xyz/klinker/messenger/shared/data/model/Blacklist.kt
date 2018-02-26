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

package xyz.klinker.messenger.shared.data.model

import android.database.Cursor

import xyz.klinker.messenger.api.entity.BlacklistBody
import xyz.klinker.messenger.shared.data.DatabaseSQLiteHelper
import xyz.klinker.messenger.encryption.EncryptionUtils

class Blacklist : DatabaseTable {

    var id: Long = 0
    var phoneNumber: String? = null

    constructor()
    constructor(body: BlacklistBody) {
        this.id = body.deviceId
        this.phoneNumber = body.phoneNumber
    }

    override fun getCreateStatement() = DATABASE_CREATE
    override fun getTableName() = TABLE
    override fun getIndexStatements() = emptyArray<String>()

    override fun fillFromCursor(cursor: Cursor) {
        for (i in 0 until cursor.columnCount) {
            when (cursor.getColumnName(i)) {
                COLUMN_ID -> this.id = cursor.getLong(i)
                COLUMN_PHONE_NUMBER -> this.phoneNumber = cursor.getString(i)
            }
        }
    }

    override fun encrypt(utils: EncryptionUtils) {
        this.phoneNumber = utils.encrypt(this.phoneNumber)
    }

    override fun decrypt(utils: EncryptionUtils) {
        try {
            this.phoneNumber = utils.decrypt(this.phoneNumber)
        } catch (e: Exception) {
        }
    }

    companion object {

        const val TABLE = "blacklist"
        const val COLUMN_ID = "_id"
        const val COLUMN_PHONE_NUMBER = "phone_number"

        private const val DATABASE_CREATE = "create table if not exists " +
                TABLE + " (" +
                COLUMN_ID + " integer primary key, " +
                COLUMN_PHONE_NUMBER + " text not null" +
                ");"
    }

}
