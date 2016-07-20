/*
 * Copyright (C) 2016 Jacob Klinker
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package xyz.klinker.messenger.util;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.klinker.android.send_message.Message;
import com.klinker.android.send_message.Settings;
import com.klinker.android.send_message.Transaction;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Utility for helping to send messages.
 */
public class SendUtil {

    public static void send(Context context, String text, String address) {
        send(context, text, address.split(", "));
    }

    public static void send(Context context, String text, String[] addresses) {
        send(context, text, addresses, null, null);
    }

    public static void send(Context context, String text, String[] addresses, Uri data,
                            String mimeType) {
        Transaction transaction = new Transaction(context, new Settings());
        Message message = new Message(text, addresses);

        try {
            message.addMedia(getBytes(context, data), mimeType);
        } catch (IOException e) {
            Log.e("Sending Exception", "Could not attach media", e);
        }

        transaction.sendNewMessage(message, Transaction.NO_THREAD_ID);
    }

    private static byte[] getBytes(Context context, Uri data) throws IOException {
        InputStream stream = context.getContentResolver().openInputStream(data);
        ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
        int bufferSize = 1024;
        byte[] buffer = new byte[bufferSize];

        int len = 0;
        while ((len = stream.read(buffer)) != -1) {
            byteBuffer.write(buffer, 0, len);
        }
        return byteBuffer.toByteArray();
    }

}
