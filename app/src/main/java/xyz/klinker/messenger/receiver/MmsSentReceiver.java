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

import java.util.List;

import xyz.klinker.messenger.data.DataSource;
import xyz.klinker.messenger.data.MimeType;
import xyz.klinker.messenger.data.model.Conversation;
import xyz.klinker.messenger.data.model.Message;
import xyz.klinker.messenger.util.SmsMmsUtils;

/**
 * Receiver which gets a notification when an MMS message has finished sending. It will mark the
 * message as sent in the database by default. We also need to add functionality for marking it
 * as sent in our own database.
 */
public class MmsSentReceiver extends com.klinker.android.send_message.MmsSentReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);

        Uri uri = Uri.parse(intent.getStringExtra(EXTRA_CONTENT_URI).replace("/outbox", ""));
        Cursor message = SmsMmsUtils.getMmsMessage(context, uri, null);

        if (message != null && message.moveToFirst()) {
            List<ContentValues> mmsParts = SmsMmsUtils.processMessage(message, -1, context);
            message.close();

            DataSource source = DataSource.getInstance(context);
            source.open();

            for (ContentValues values : mmsParts) {
                Cursor messages = source.searchMessages(values.getAsLong(Message.COLUMN_TIMESTAMP));

                if (messages != null && messages.moveToFirst()) {
                    do {
                        Message m = new Message();
                        m.fillFromCursor(messages);

                        if (m.type == Message.TYPE_SENDING) {
                            source.updateMessageType(m.id, Message.TYPE_SENT);

                            MessageListUpdatedReceiver.sendBroadcast(context, m.conversationId);
                        }
                    } while (messages.moveToNext());
                }

                try {
                    messages.close();
                } catch (Exception e) { }
            }

            source.close();
        }

        try {
            message.close();
        } catch (Exception e) { }
    }

}
