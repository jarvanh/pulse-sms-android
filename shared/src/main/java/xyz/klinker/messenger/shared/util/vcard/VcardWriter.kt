package xyz.klinker.messenger.shared.util.vcard

import android.content.Context
import android.net.Uri
import android.os.Environment
import xyz.klinker.messenger.shared.util.ImageUtils

import java.io.File
import java.io.FileWriter
import java.io.IOException

object VcardWriter {

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
        fw.write("TEL;TYPE=CELL,VOICE:" + phoneNumber + "\r\n")
        fw.write("END:VCARD\r\n")
        fw.close()

        // example location vcards

//        fw.write("BEGIN:VCARD\r\n")
//        fw.write("VERSION:3.0\r\n")
//        fw.write("PRODID:-//Apple Inc.//iOS 8.1//EN\r\n")
//        fw.write("N:;Current geo;;;\r\n")
//        fw.write("FN:Current geo\r\n")
//        fw.write("item1.URL;type=pref:http://maps.apple.com/?ll=55.369117\\,39.079991\r\n")
//        fw.write("item1.X-ABLabel:map url\r\n")
//        fw.write("END:VCARD\r\n")
//        fw.close()

//        fw.write("BEGIN:VCARD\r\n")
//        fw.write("VERSION:3.0\r\n")
//        fw.write("TITLE:My Geo Point\r\n")
//        fw.write("GEO:37.386013;-122.082932\r\n")
//        fw.write("END:VCARD\r\n")
//        fw.close()

        return ImageUtils.createContentUri(context, contactCard)
    }
}
