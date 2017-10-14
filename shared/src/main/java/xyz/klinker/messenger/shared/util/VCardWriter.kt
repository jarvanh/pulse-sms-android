package xyz.klinker.messenger.shared.util

import android.content.Context
import android.net.Uri
import android.os.Environment

import java.io.File
import java.io.FileWriter
import java.io.IOException

object VCardWriter {

    @Throws(IOException::class)
    fun writeContactCard(context: Context, firstName: String, lastName: String, phoneNumber: String): Uri {
        val pulseDir = File(Environment.getExternalStorageDirectory(), "Pulse")
        val contactCard = File(pulseDir, "contact.vcf")
        if (!pulseDir.exists()) {
            pulseDir.mkdir()
        }

        val fw = FileWriter(contactCard)
        fw.write("BEGIN:VCARD\r\n")
        fw.write("VERSION:3.0\r\n")
        fw.write("N:$lastName;$firstName\r\n")
        fw.write("FN:$firstName $lastName\r\n")
        fw.write("TEL;TYPE=HOME,VOICE:" + phoneNumber + "\r\n")
        fw.write("END:VCARD\r\n")
        fw.close()

        return ImageUtils.createContentUri(context, contactCard)
    }
}
