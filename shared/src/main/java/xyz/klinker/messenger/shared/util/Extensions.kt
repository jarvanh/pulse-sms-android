package xyz.klinker.messenger.shared.util

import android.database.Cursor
import android.graphics.Color
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream

fun Cursor.closeSilent() {
    try {
        this.close()
    } catch (e: Exception) { }
}

fun Int.isDarkColor(): Boolean {
    val darkness = 1 - (0.299 * Color.red(this) + 0.587 * Color.green(this) + 0.114 * Color.blue(this)) / 255
    return darkness >= 0.30
}
fun InputStream.writeToOutputAndCleanup(out: FileOutputStream) {
    // Transfer bytes from in to out
    val buf = ByteArray(1024)
    var len = this.read(buf)
    while (len > 0) {
        out.write(buf, 0, len)
        len = this.read(buf)
    }

    this.closeSilent()
    out.closeSilent()
}

fun InputStream.closeSilent() {
    try {
        this.close()
    } catch (e: Exception) {
    }
}

fun OutputStream.closeSilent() {
    try {
        this.close()
    } catch (e: Exception) {
    }
}