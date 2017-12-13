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

import xyz.klinker.messenger.api.entity.FolderBody
import xyz.klinker.messenger.encryption.EncryptionUtils
import xyz.klinker.messenger.shared.data.ColorSet

/**
 * Table for holding drafts for a conversation.
 */
class Folder : DatabaseTable {

    var id: Long = 0
    var name: String? = null
    var colors = ColorSet()

    constructor()
    constructor(body: FolderBody) {
        this.id = body.deviceId
        this.name = body.name
        this.colors.color = body.color
        this.colors.colorLight = body.colorLight
        this.colors.colorDark = body.colorDark
        this.colors.colorAccent = body.colorAccent
    }

    override fun getCreateStatement() = DATABASE_CREATE
    override fun getTableName() = TABLE
    override fun getIndexStatements() = emptyArray<String>()

    override fun fillFromCursor(cursor: Cursor) {
        for (i in 0 until cursor.columnCount) {
            when (cursor.getColumnName(i)) {
                COLUMN_ID -> this.id = cursor.getLong(i)
                COLUMN_NAME -> this.name = cursor.getString(i)
                COLUMN_COLOR -> this.colors.color = cursor.getInt(i)
                COLUMN_COLOR_DARK -> this.colors.colorDark = cursor.getInt(i)
                COLUMN_COLOR_LIGHT -> this.colors.colorLight = cursor.getInt(i)
                COLUMN_COLOR_ACCENT -> this.colors.colorAccent = cursor.getInt(i)
            }
        }
    }

    override fun encrypt(utils: EncryptionUtils) {
        this.name = utils.encrypt(this.name)
    }

    override fun decrypt(utils: EncryptionUtils) {
        try {
            this.name = utils.decrypt(this.name)
        } catch (e: Exception) {
        }
    }

    companion object {

        val TABLE = "folder"
        val COLUMN_ID = "_id"
        val COLUMN_NAME = "name"
        val COLUMN_COLOR = "color"
        val COLUMN_COLOR_DARK = "color_dark"
        val COLUMN_COLOR_LIGHT = "color_light"
        val COLUMN_COLOR_ACCENT = "color_accent"

        private val DATABASE_CREATE = "create table if not exists " +
                TABLE + " (" +
                COLUMN_ID + " integer primary key, " +
                COLUMN_NAME + " text not null, " +
                COLUMN_COLOR + " integer not null, " +
                COLUMN_COLOR_DARK + " integer not null, " +
                COLUMN_COLOR_LIGHT + " integer not null, " +
                COLUMN_COLOR_ACCENT + " integer not null" +
                ");"
    }

}
