package xyz.klinker.messenger.shared.data.model

import android.database.Cursor

import xyz.klinker.messenger.api.entity.ContactBody
import xyz.klinker.messenger.shared.data.ColorSet
import xyz.klinker.messenger.shared.data.DatabaseSQLiteHelper
import xyz.klinker.messenger.encryption.EncryptionUtils

/**
 * Data object for holding information about a contact
 */
open class Contact : DatabaseTable {

    var id: Long = 0
    var phoneNumber: String? = null
    var idMatcher: String? = null
    var name: String? = null
    var colors = ColorSet()

    constructor()
    constructor(body: ContactBody) {
        this.id = body.deviceId
        this.phoneNumber = body.phoneNumber
        this.idMatcher = body.idMatcher
        this.name = body.name
        this.colors.color = body.color
        this.colors.colorDark = body.colorDark
        this.colors.colorLight = body.colorLight
        this.colors.colorAccent = body.colorAccent
    }

    override fun getCreateStatement() = DATABASE_CREATE
    override fun getTableName() = TABLE
    override fun getIndexStatements() = emptyArray<String>()

    override fun fillFromCursor(cursor: Cursor) {
        for (i in 0 until cursor.columnCount) {
            when (cursor.getColumnName(i)) {
                COLUMN_ID -> this.id = cursor.getLong(i)
                COLUMN_PHONE_NUMBER -> this.phoneNumber = cursor.getString(i)
                COLUMN_ID_MATCHER -> this.idMatcher = cursor.getString(i)
                COLUMN_NAME -> this.name = cursor.getString(i)
                COLUMN_COLOR -> this.colors.color = cursor.getInt(i)
                COLUMN_COLOR_DARK -> this.colors.colorDark = cursor.getInt(i)
                COLUMN_COLOR_LIGHT -> this.colors.colorLight = cursor.getInt(i)
                COLUMN_COLOR_ACCENT -> this.colors.colorAccent = cursor.getInt(i)
            }
        }
    }

    override fun encrypt(utils: EncryptionUtils) {
        this.phoneNumber = utils.encrypt(this.phoneNumber)
        this.name = utils.encrypt(this.name)
        this.idMatcher = utils.encrypt(this.idMatcher)
    }

    override fun decrypt(utils: EncryptionUtils) {
        try {
            this.phoneNumber = utils.decrypt(this.phoneNumber)
            this.name = utils.decrypt(this.name)
            this.idMatcher = utils.decrypt(this.idMatcher)
        } catch (e: Exception) {
        }
    }

    companion object {

        val TABLE = "contact"
        val COLUMN_ID = "_id"
        val COLUMN_PHONE_NUMBER = "phone_number"
        val COLUMN_NAME = "name"
        val COLUMN_COLOR = "color"
        val COLUMN_COLOR_DARK = "color_dark"
        val COLUMN_COLOR_LIGHT = "color_light"
        val COLUMN_COLOR_ACCENT = "color_accent"
        val COLUMN_ID_MATCHER = "id_matcher" // created in database v9

        private val DATABASE_CREATE = "create table if not exists " +
                TABLE + " (" +
                COLUMN_ID + " integer primary key, " +
                COLUMN_PHONE_NUMBER + " varchar(255) not null, " +
                COLUMN_ID_MATCHER + " text not null, " +
                COLUMN_NAME + " varchar(255) not null, " +
                COLUMN_COLOR + " integer not null, " +
                COLUMN_COLOR_DARK + " integer not null, " +
                COLUMN_COLOR_LIGHT + " integer not null, " +
                COLUMN_COLOR_ACCENT + " integer not null" +
                ");"
    }

}

class ImageContact : Contact() {
    var image: String? = null
}