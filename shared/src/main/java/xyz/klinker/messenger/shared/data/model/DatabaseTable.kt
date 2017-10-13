package xyz.klinker.messenger.shared.data.model

import android.database.Cursor
import xyz.klinker.messenger.encryption.EncryptionUtils


interface DatabaseTable {

    fun getCreateStatement(): String

    fun getTableName(): String

    fun getIndexStatements(): Array<String>

    fun fillFromCursor(cursor: Cursor)

    fun encrypt(utils: EncryptionUtils)

    fun decrypt(utils: EncryptionUtils)

}