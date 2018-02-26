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

import xyz.klinker.messenger.api.entity.MessageBody
import xyz.klinker.messenger.shared.data.DatabaseSQLiteHelper
import xyz.klinker.messenger.encryption.EncryptionUtils

/**
 * Holds information regarding messages (eg what type they are, what they contain and a timestamp).
 */
class RetryableRequest : DatabaseTable {

    var id: Long = 0
    var type: Int = 0
    var dataId: Long = 0
    var errorTimestamp: Long = 0

    constructor()
    constructor(type: Int, dataId: Long, errorTimestamp: Long) {
        this.type = type
        this.dataId = dataId
        this.errorTimestamp = errorTimestamp
    }

    override fun getCreateStatement() = DATABASE_CREATE
    override fun getTableName() = TABLE
    override fun getIndexStatements() = emptyArray<String>()

    override fun fillFromCursor(cursor: Cursor) {
        for (i in 0 until cursor.columnCount) {
            when (cursor.getColumnName(i)) {
                COLUMN_ID -> this.id = cursor.getLong(i)
                COLUMN_TYPE -> this.type = cursor.getInt(i)
                COLUMN_DATA_ID -> this.dataId = cursor.getLong(i)
                COLUMN_ERROR_TIMESTAMP -> this.errorTimestamp = cursor.getLong(i)
            }
        }
    }

    override fun encrypt(utils: EncryptionUtils) {
        // we aren't uploading this table at all.
    }

    override fun decrypt(utils: EncryptionUtils) {
        // we aren't uploading this table at all.
    }

    companion object {

        const val TABLE = "retryable_request"
        const val COLUMN_ID = "_id"
        const val COLUMN_TYPE = "type"
        const val COLUMN_DATA_ID = "data_id"
        const val COLUMN_ERROR_TIMESTAMP = "error_timestamp"

        private const val DATABASE_CREATE = "create table if not exists " +
                "$TABLE ($COLUMN_ID integer primary key, " +
                "$COLUMN_TYPE integer not null, " +
                "$COLUMN_DATA_ID integer not null, " +
                "$COLUMN_ERROR_TIMESTAMP integer not null);"

        const val TYPE_ADD_MESSAGE = 0
        const val TYPE_ADD_CONVERSATION = 1
    }

}
