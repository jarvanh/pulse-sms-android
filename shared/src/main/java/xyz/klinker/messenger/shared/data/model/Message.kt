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
class Message : DatabaseTable {

    var id: Long = 0
    var conversationId: Long = 0
    var type: Int = 0
    var data: String? = null
    var timestamp: Long = 0
    var mimeType: String? = null
    var read: Boolean = false
    var seen: Boolean = false
    var from: String? = null
    var color: Int? = null
    var simPhoneNumber: String? = null
    var sentDeviceId = -1L
    var nullableConvoTitle: String? = null

    constructor()
    constructor(body: MessageBody) {
        this.id = body.deviceId
        this.conversationId = body.deviceConversationId
        this.type = body.messageType
        this.data = body.data
        this.timestamp = body.timestamp
        this.mimeType = body.mimeType
        this.read = body.read
        this.seen = body.seen
        this.from = body.messageFrom
        this.color = body.color
        this.simPhoneNumber = body.simStamp

        if (body.sentDevice != null) {
            this.sentDeviceId = body.sentDevice
        }
    }

    override fun getCreateStatement() = DATABASE_CREATE
    override fun getTableName() = TABLE
    override fun getIndexStatements() = INDEXES

    override fun fillFromCursor(cursor: Cursor) {
        for (i in 0 until cursor.columnCount) {
            when (cursor.getColumnName(i)) {
                COLUMN_ID -> this.id = cursor.getLong(i)
                COLUMN_CONVERSATION_ID -> this.conversationId = cursor.getLong(i)
                COLUMN_TYPE -> this.type = cursor.getInt(i)
                COLUMN_DATA -> this.data = cursor.getString(i)
                COLUMN_TIMESTAMP -> this.timestamp = cursor.getLong(i)
                COLUMN_MIME_TYPE -> this.mimeType = cursor.getString(i)
                COLUMN_READ -> this.read = cursor.getInt(i) == 1
                COLUMN_SEEN -> this.seen = cursor.getInt(i) == 1
                COLUMN_FROM -> this.from = cursor.getString(i)
                COLUMN_SIM_NUMBER -> this.simPhoneNumber = cursor.getString(i)
                COLUMN_SENT_DEVICE -> this.sentDeviceId = cursor.getLong(i)
                JOIN_COLUMN_CONVO_TITLE -> this.nullableConvoTitle = cursor.getString(i)
                COLUMN_COLOR -> try {
                    this.color = Integer.parseInt(cursor.getString(i))
                } catch (e: NumberFormatException) {
                    this.color = null
                }
            }
        }
    }

    override fun encrypt(utils: EncryptionUtils) {
        this.data = utils.encrypt(this.data)
        this.mimeType = utils.encrypt(this.mimeType)
        this.from = utils.encrypt(this.from)
        this.simPhoneNumber = utils.encrypt(this.simPhoneNumber)
    }

    override fun decrypt(utils: EncryptionUtils) {
        this.mimeType = utils.decrypt(this.mimeType)
        this.from = utils.decrypt(this.from)
        this.data = utils.decrypt(this.data)

        try {
            this.simPhoneNumber = utils.decrypt(this.simPhoneNumber)
        } catch (e: Exception) {
        }
    }

    companion object {

        const val TABLE = "message"

        const val COLUMN_ID = "_id"
        const val COLUMN_CONVERSATION_ID = "conversation_id"
        const val COLUMN_TYPE = "type"
        const val COLUMN_DATA = "data"
        const val COLUMN_TIMESTAMP = "timestamp"
        const val COLUMN_MIME_TYPE = "mime_type"
        const val COLUMN_READ = "read"
        const val COLUMN_SEEN = "seen"
        const val COLUMN_FROM = "message_from"
        const val COLUMN_COLOR = "color"
        const val COLUMN_SIM_NUMBER = "sim_phone_number" // added with v7 of database
        const val COLUMN_SENT_DEVICE = "sent_device" // added with v10 of database

        // not in this table, but used in a join statement for searches
        private const val JOIN_COLUMN_CONVO_TITLE = "convo_title"

        private const val DATABASE_CREATE = "create table if not exists " +
                TABLE + " (" +
                COLUMN_ID + " integer primary key, " +
                COLUMN_CONVERSATION_ID + " integer not null, " +
                COLUMN_TYPE + " integer not null, " +
                COLUMN_DATA + " text not null, " +
                COLUMN_TIMESTAMP + " integer not null, " +
                COLUMN_MIME_TYPE + " text not null, " +
                COLUMN_READ + " integer not null, " +
                COLUMN_SEEN + " integer not null, " +
                COLUMN_FROM + " text, " +
                COLUMN_COLOR + " integer, " +
                COLUMN_SIM_NUMBER + " text, " +
                COLUMN_SENT_DEVICE + " integer" +
                ");"

        private val INDEXES = arrayOf("create index if not exists conversation_id_message_index on $TABLE ($COLUMN_CONVERSATION_ID);")

        const val TYPE_RECEIVED = 0
        const val TYPE_SENT = 1
        const val TYPE_SENDING = 2
        const val TYPE_ERROR = 3
        const val TYPE_DELIVERED = 4
        const val TYPE_INFO = 5
        const val TYPE_MEDIA = 6

        // only used in the adapter, this TYPE_IMAGE should NEVER be written to the database.
        const val TYPE_IMAGE_SENDING = 7
        const val TYPE_IMAGE_SENT = 8
        const val TYPE_IMAGE_RECEIVED = 9
    }

}
