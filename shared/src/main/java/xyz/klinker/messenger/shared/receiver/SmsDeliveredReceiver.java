/*
 * Copyright (C) 2017 Luke Klinker
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

package xyz.klinker.messenger.shared.receiver;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.Telephony;

import com.klinker.android.send_message.DeliveredReceiver;

import xyz.klinker.messenger.shared.data.DataSource;
import xyz.klinker.messenger.shared.data.Settings;
import xyz.klinker.messenger.shared.data.model.Message;
import xyz.klinker.messenger.shared.util.CursorUtil;
import xyz.klinker.messenger.shared.util.SmsMmsUtils;

/**
 * Receiver for getting notifications of when an SMS has been delivered. By default it's super
 * class will mark the internal message as delivered, we need to also mark our database as delivered.
 */
public class SmsDeliveredReceiver extends DeliveredReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        new Thread(() -> {
            try {
                super.onReceive(context, intent);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();

        new Thread(() -> {
            try {
                handleReceiver(context, intent);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void handleReceiver(Context context, Intent intent) throws Exception {
        Uri uri = Uri.parse(intent.getStringExtra("message_uri"));

        switch (getResultCode()) {
            case Activity.RESULT_OK:
                markMessageDelivered(context, uri);
                break;
            case Activity.RESULT_CANCELED:
                markMessageError(context, uri);
                break;
        }
    }

    private void markMessageDelivered(Context context, Uri uri) {
        markMessage(context, uri, false);
    }

    private void markMessageError(Context context, Uri uri) {
        markMessage(context, uri, true);
    }

    private void markMessage(Context context, Uri uri, boolean error) {
        Cursor message = SmsMmsUtils.getSmsMessage(context, uri, null);

        if (message != null && message.moveToFirst()) {
            String body = message.getString(message.getColumnIndex(Telephony.Sms.BODY));
            message.close();

            Settings settings = Settings.get(context);
            if (settings.signature != null && !settings.signature.isEmpty()) {
                body = body.replace("\n" + settings.signature, "");
            }

            DataSource source = DataSource.INSTANCE;
            Cursor messages = source.searchMessages(context, body);

            if (messages != null && messages.moveToFirst()) {
                long id = messages.getLong(0);
                source.updateMessageType(context, id, error ? Message.Companion.getTYPE_ERROR() : Message.Companion.getTYPE_DELIVERED());

                long conversationId = messages
                        .getLong(messages.getColumnIndex(Message.Companion.getCOLUMN_CONVERSATION_ID()));
                MessageListUpdatedReceiver.sendBroadcast(context, conversationId);
            }

            CursorUtil.closeSilent(messages);
        }
    }

}
