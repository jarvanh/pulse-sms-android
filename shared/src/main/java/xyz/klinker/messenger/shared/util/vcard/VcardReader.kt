package xyz.klinker.messenger.shared.util.vcard

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.IOException
import java.net.URI

object VcardReader {

    @Throws(IOException::class)
    fun readCotactCard(context: Context, uri: String): String {
        return readCotactCard(context, Uri.parse(uri))
    }

    @Throws(IOException::class)
    fun readCotactCard(context: Context, uri: Uri): String {
        val inputStream = context.contentResolver.openInputStream(uri)
        return inputStream.bufferedReader().use { it.readText() }
    }
}