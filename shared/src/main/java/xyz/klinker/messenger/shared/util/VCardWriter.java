package xyz.klinker.messenger.shared.util;

import android.content.Context;
import android.net.Uri;
import android.os.Environment;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class VCardWriter {

    public static Uri writeContactCard(Context context, String firstName, String lastName, String phoneNumber) throws IOException {
        File pulseDir = new File(Environment.getExternalStorageDirectory(), "Pulse");
        File contactCard = new File(pulseDir, "contact.vcf");
        if (!pulseDir.exists()) {
            pulseDir.mkdir();
        }

        FileWriter fw = new FileWriter(contactCard);
        fw.write("BEGIN:VCARD\r\n");
        fw.write("VERSION:3.0\r\n");
        fw.write("N:" + lastName + ";" + firstName + "\r\n");
        fw.write("FN:" + firstName + " " + lastName + "\r\n");
        fw.write("TEL;TYPE=HOME,VOICE:" + phoneNumber + "\r\n");
        fw.write("END:VCARD\r\n");
        fw.close();

        return ImageUtils.createContentUri(context, contactCard);
    }
}
