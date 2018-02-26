package xyz.klinker.messenger.shared.data.model

import android.database.Cursor
import xyz.klinker.messenger.api.entity.AutoReplyBody
import xyz.klinker.messenger.encryption.EncryptionUtils

/**
 * Table for holding auto responses.
 */
class AutoReply : DatabaseTable {

    var id: Long = 0
    var type: String? = null
    var pattern: String? = null
    var response: String? = null

    constructor()
    constructor(body: AutoReplyBody) {
        this.id = body.deviceId
        this.type = body.replyType
        this.pattern = body.pattern
        this.response = body.response
    }

    override fun getCreateStatement() = DATABASE_CREATE
    override fun getTableName() = TABLE
    override fun getIndexStatements() = emptyArray<String>()

    override fun fillFromCursor(cursor: Cursor) {
        for (i in 0 until cursor.columnCount) {
            when (cursor.getColumnName(i)) {
                COLUMN_ID -> this.id = cursor.getLong(i)
                COLUMN_TYPE -> this.type = cursor.getString(i)
                COLUMN_PATTERN -> this.pattern = cursor.getString(i)
                COLUMN_RESPONSE -> this.response = cursor.getString(i)
            }
        }
    }

    override fun encrypt(utils: EncryptionUtils) {
        this.pattern = utils.encrypt(this.pattern)
        this.response = utils.encrypt(this.response)
    }

    override fun decrypt(utils: EncryptionUtils) {
        try {
            this.pattern = utils.decrypt(this.pattern)
            this.response = utils.decrypt(this.response)
        } catch (e: Exception) {
        }
    }

    companion object {

        const val TABLE = "auto_reply"
        const val COLUMN_ID = "_id"
        const val COLUMN_TYPE = "type"
        const val COLUMN_PATTERN = "pattern"
        const val COLUMN_RESPONSE = "response"

        private const val DATABASE_CREATE = "create table if not exists " +
                TABLE + " (" +
                COLUMN_ID + " integer primary key, " +
                COLUMN_TYPE + " text not null, " +
                COLUMN_PATTERN + " text not null, " +
                COLUMN_RESPONSE + " text not null" +
                ");"

        const val TYPE_VACATION = "vacation"
        const val TYPE_DRIVING = "driving"
        const val TYPE_CONTACT = "contact"
        const val TYPE_KEYWORD = "keyword"
    }

}
