package xyz.klinker.messenger.shared.util

import android.database.Cursor
import android.graphics.Color

fun Cursor.closeSilent() {
    try {
        this.close()
    } catch (e: Exception) { }
}

fun Int.isDarkColor(): Boolean {
    val darkness = 1 - (0.299 * Color.red(this) + 0.587 * Color.green(this) + 0.114 * Color.blue(this)) / 255
    return darkness >= 0.30
}