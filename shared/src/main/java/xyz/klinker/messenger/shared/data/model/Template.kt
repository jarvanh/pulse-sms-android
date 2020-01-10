/*
 * Copyright (C) 2020 Luke Klinker
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

import xyz.klinker.messenger.api.entity.DraftBody
import xyz.klinker.messenger.api.entity.TemplateBody
import xyz.klinker.messenger.shared.data.DatabaseSQLiteHelper
import xyz.klinker.messenger.encryption.EncryptionUtils

/**
 * Table for holding drafts for a conversation.
 */
class Template : DatabaseTable {

    var id: Long = 0
    var text: String? = null

    constructor()
    constructor(body: TemplateBody) {
        this.id = body.deviceId
        this.text = body.text
    }

    override fun getCreateStatement() = DATABASE_CREATE
    override fun getTableName() = TABLE
    override fun getIndexStatements() = emptyArray<String>()

    override fun fillFromCursor(cursor: Cursor) {
        for (i in 0 until cursor.columnCount) {
            when (cursor.getColumnName(i)) {
                COLUMN_ID -> this.id = cursor.getLong(i)
                COLUMN_TEXT -> this.text = cursor.getString(i)
            }
        }
    }

    override fun encrypt(utils: EncryptionUtils) {
        this.text = utils.encrypt(this.text)
    }

    override fun decrypt(utils: EncryptionUtils) {
        try {
            this.text = utils.decrypt(this.text)
        } catch (e: Exception) {
        }
    }

    companion object {

        const val TABLE = "template"

        const val COLUMN_ID = "_id"
        const val COLUMN_TEXT = "text"

        private const val DATABASE_CREATE = "create table if not exists " +
                TABLE + " (" +
                COLUMN_ID + " integer primary key, " +
                COLUMN_TEXT + " text not null" +
                ");"
    }

}
