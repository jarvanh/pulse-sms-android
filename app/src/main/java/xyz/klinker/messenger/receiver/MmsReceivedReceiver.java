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

package xyz.klinker.messenger.receiver;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.VisibleForTesting;

import java.util.List;

import xyz.klinker.messenger.data.DataSource;
import xyz.klinker.messenger.data.MimeType;
import xyz.klinker.messenger.data.model.Message;
import xyz.klinker.messenger.service.NotificationService;
import xyz.klinker.messenger.util.BlacklistUtils;
import xyz.klinker.messenger.util.ContactUtils;
import xyz.klinker.messenger.util.PhoneNumberUtils;
import xyz.klinker.messenger.util.SmsMmsUtils;

/**
 * Receiver for notifying us when a new MMS has been received by the device. By default it will
 * persist the message to the internal database. We also need to add functionality for
 * persisting it to our own database and giving a notification that it has been received.
 */
public class MmsReceivedReceiver extends com.klinker.android.send_message.MmsReceivedReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);

        insertMms(context);
        context.startService(new Intent(context, NotificationService.class));
    }

    private void insertMms(Context context) {
        Cursor lastMessage = SmsMmsUtils.getLastMmsMessage(context);

        if (lastMessage != null && lastMessage.moveToFirst()) {
            Uri uri = Uri.parse("content://mms/" + lastMessage.getLong(0));
            final String from = SmsMmsUtils.getMmsFrom(uri, context);

            if (BlacklistUtils.isBlacklisted(context, from)) {
                return;
            }

            final String to = SmsMmsUtils.getMmsTo(uri, context);
            final String phoneNumbers = getPhoneNumbers(from, to,
                    PhoneNumberUtils.getMyPhoneNumber(context));
            List<ContentValues> values = SmsMmsUtils.processMessage(lastMessage, -1L, context);

            DataSource source = DataSource.getInstance(context);
            source.open();

            Long conversationId = null;
            String snippet = "";
            for (ContentValues value : values) {
                Message message = new Message();
                message.type = value.getAsInteger(Message.COLUMN_TYPE);
                message.data = value.getAsString(Message.COLUMN_DATA).trim();
                message.timestamp = value.getAsLong(Message.COLUMN_TIMESTAMP);
                message.mimeType = value.getAsString(Message.COLUMN_MIME_TYPE);
                message.read = false;
                message.seen = false;
                message.from = ContactUtils.findContactNames(from, context);

                if (message.mimeType.equals(MimeType.TEXT_PLAIN)) {
                    snippet = message.data;
                }

                conversationId = source.insertMessage(message, phoneNumbers, context);
            }

            source.close();

            if (conversationId != null) {
                ConversationListUpdatedReceiver.sendBroadcast(context, conversationId,
                        snippet, false);
                MessageListUpdatedReceiver.sendBroadcast(context, conversationId);
            }
        }
    }

    @VisibleForTesting
    String getPhoneNumbers(String from, String to, String myNumber) {
        String[] toNumbers = to.split(", ");
        StringBuilder builder = new StringBuilder();

        for (String number : toNumbers) {
            if (!number.contains(myNumber) && !myNumber.contains(number)) {
                builder.append(number);
                builder.append(", ");
            }
        }

        builder.append(from);
        return builder.toString();
    }

}
