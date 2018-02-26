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

import android.content.Context
import android.database.Cursor
import android.graphics.Color
import android.provider.ContactsContract

import xyz.klinker.messenger.api.entity.ConversationBody
import xyz.klinker.messenger.shared.data.ColorSet
import xyz.klinker.messenger.shared.data.DatabaseSQLiteHelper
import xyz.klinker.messenger.encryption.EncryptionUtils
import xyz.klinker.messenger.shared.util.ColorUtils

/**
 * Data object for holding information about a conversation.
 */
class Conversation : DatabaseTable {

    var id: Long = 0
    var colors = ColorSet()
    var ledColor: Int = 0
    var pinned: Boolean = false
    var read: Boolean = false
    var timestamp: Long = 0
    var title: String? = null
    var phoneNumbers: String? = null
    var snippet: String? = null
    var ringtoneUri: String? = null
    var imageUri: String? = null
    var idMatcher: String? = null
    var mute: Boolean = false
    var archive: Boolean = false
    var privateNotifications: Boolean = false
    var simSubscriptionId: Int? = null
    var folderId: Long? = null

    val isGroup: Boolean
        get() = phoneNumbers?.contains(", ") == true

    constructor()
    constructor(body: ConversationBody) {
        this.id = body.deviceId
        this.colors.color = body.color
        this.colors.colorDark = body.colorDark
        this.colors.colorLight = body.colorLight
        this.colors.colorAccent = body.colorAccent
        this.ledColor = body.ledColor
        this.pinned = body.pinned
        this.read = body.read
        this.timestamp = body.timestamp
        this.title = body.title
        this.phoneNumbers = body.phoneNumbers
        this.snippet = body.snippet
        this.ringtoneUri = body.ringtone
        this.imageUri = body.imageUri
        this.idMatcher = body.idMatcher
        this.mute = body.mute
        this.archive = body.archive
        this.privateNotifications = body.privateNotifications
        this.folderId = body.folderId
    }

    override fun getCreateStatement() =  DATABASE_CREATE
    override fun getTableName() = TABLE
    override fun getIndexStatements() = INDEXES

    override fun fillFromCursor(cursor: Cursor) {
        for (i in 0 until cursor.columnCount) {
            when (cursor.getColumnName(i)) {
                COLUMN_ID -> this.id = cursor.getLong(i)
                COLUMN_COLOR -> this.colors.color = cursor.getInt(i)
                COLUMN_COLOR_DARK -> this.colors.colorDark = cursor.getInt(i)
                COLUMN_COLOR_LIGHT -> this.colors.colorLight = cursor.getInt(i)
                COLUMN_COLOR_ACCENT -> this.colors.colorAccent = cursor.getInt(i)
                COLUMN_LED_COLOR -> this.ledColor = cursor.getInt(i)
                COLUMN_PINNED -> this.pinned = cursor.getInt(i) == 1
                COLUMN_READ -> this.read = cursor.getInt(i) == 1
                COLUMN_TIMESTAMP -> this.timestamp = cursor.getLong(i)
                COLUMN_TITLE -> this.title = cursor.getString(i)
                COLUMN_PHONE_NUMBERS -> this.phoneNumbers = cursor.getString(i)
                COLUMN_SNIPPET -> this.snippet = cursor.getString(i)
                COLUMN_RINGTONE -> this.ringtoneUri = cursor.getString(i)
                COLUMN_IMAGE_URI -> this.imageUri = cursor.getString(i)
                COLUMN_ID_MATCHER -> this.idMatcher = cursor.getString(i)
                COLUMN_MUTE -> this.mute = cursor.getInt(i) == 1
                COLUMN_ARCHIVED -> this.archive = cursor.getInt(i) == 1
                COLUMN_PRIVATE_NOTIFICATIONS -> this.privateNotifications = cursor.getInt(i) == 1
                COLUMN_SIM_SUBSCRIPTION_ID -> this.simSubscriptionId = if (cursor.getInt(i) == -1) null else cursor.getInt(i)
                COLUMN_FOLDER_ID -> this.folderId = cursor.getLong(i)
            }
        }
    }

    fun fillFromContactGroupCursor(context: Context, cursor: Cursor) {
        colors = ColorUtils.getRandomMaterialColor(context)

        for (i in 0 until cursor.columnCount) {
            val column = cursor.getColumnName(i)

            if (column == ContactsContract.Groups._ID) {
                this.id = cursor.getLong(i)
            } else if (column == ContactsContract.Groups.TITLE) {
                this.title = cursor.getString(i)
                if (title != null && title!!.contains("Group:")) {
                    title = title!!.substring(title!!.indexOf("Group:") + "Group:".length).trim { it <= ' ' }
                }
            }
        }
    }

    override fun encrypt(utils: EncryptionUtils) {
        this.title = utils.encrypt(this.title)
        this.phoneNumbers = utils.encrypt(this.phoneNumbers)
        this.snippet = utils.encrypt(this.snippet)
        this.ringtoneUri = utils.encrypt(this.ringtoneUri)
        this.imageUri = utils.encrypt(this.imageUri)
        this.idMatcher = utils.encrypt(this.idMatcher)
    }

    override fun decrypt(utils: EncryptionUtils) {
        this.title = utils.decrypt(this.title)
        this.phoneNumbers = utils.decrypt(this.phoneNumbers)
        this.snippet = utils.decrypt(this.snippet)
        this.ringtoneUri = utils.decrypt(this.ringtoneUri)
        this.imageUri = utils.decrypt(this.imageUri)
        this.idMatcher = utils.decrypt(this.idMatcher)
    }

    companion object {

        const val TABLE = "conversation"

        const val COLUMN_ID = "_id"
        const val COLUMN_COLOR = "color"
        const val COLUMN_COLOR_DARK = "color_dark"
        const val COLUMN_COLOR_LIGHT = "color_light"
        const val COLUMN_COLOR_ACCENT = "color_accent"
        const val COLUMN_PINNED = "pinned"
        const val COLUMN_READ = "read"
        const val COLUMN_TIMESTAMP = "timestamp"
        const val COLUMN_TITLE = "title"
        const val COLUMN_PHONE_NUMBERS = "phone_numbers"
        const val COLUMN_SNIPPET = "snippet"
        const val COLUMN_RINGTONE = "ringtone"
        const val COLUMN_IMAGE_URI = "image_uri"
        const val COLUMN_ID_MATCHER = "id_matcher"
        const val COLUMN_MUTE = "mute"
        const val COLUMN_ARCHIVED = "archive" // created in database v2
        const val COLUMN_PRIVATE_NOTIFICATIONS = "private_notifications" // created in database v4
        const val COLUMN_LED_COLOR = "led_color" // created in database v5
        const val COLUMN_SIM_SUBSCRIPTION_ID = "sim_subscription_id" // created in database v6
        const val COLUMN_FOLDER_ID = "folder_id" // created in database v12

        val INDEXES = arrayOf("create index if not exists folder_id_conversation_index on $TABLE ($COLUMN_FOLDER_ID);")
        private const val DATABASE_CREATE = "create table if not exists " +
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
                COLUMN_SIM_SUBSCRIPTION_ID + " integer default -1, " +
                COLUMN_FOLDER_ID + " integer default -1" +
                ");"
    }

}