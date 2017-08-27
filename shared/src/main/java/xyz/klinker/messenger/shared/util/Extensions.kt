package xyz.klinker.messenger.shared.util

import android.database.Cursor

fun Cursor.closeSilent() {
    try {
        this.close()
    } catch (e: Exception) { }
}