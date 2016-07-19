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

import com.klinker.android.send_message.Message;
import com.klinker.android.send_message.Settings;
import com.klinker.android.send_message.Transaction;

/**
 * Utility for helping to send messages.
 */
public class SendUtil {

    /**
     * Sends a new sms message.
     *
     * @param context the application context.
     * @param text the message to send.
     * @param address the address to send to. The comma will be removed from any comma, space
     *                separated addresses.
     */
    public static void send(Context context, String text, String address) {
        Transaction transaction = new Transaction(context, new Settings());
        Message message = new Message(text, address.replace(",", ""));
        transaction.sendNewMessage(message, Transaction.NO_THREAD_ID);
    }

}
