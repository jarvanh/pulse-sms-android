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

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.Telephony;
import android.telephony.SmsManager;

import com.klinker.android.send_message.SentReceiver;

import xyz.klinker.messenger.data.DataSource;
import xyz.klinker.messenger.data.model.Message;
import xyz.klinker.messenger.util.SmsMmsUtils;

/**
 * Receiver for getting notifications of when an SMS has finished sending. By default it's super
 * class will mark the internal message as sent, we need to also mark our database as sent.
 */
public class SmsSentReceiver extends SentReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);

        Uri uri = Uri.parse(intent.getStringExtra("message_uri"));

        switch (getResultCode()) {
            case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
            case SmsManager.RESULT_ERROR_NO_SERVICE:
            case SmsManager.RESULT_ERROR_NULL_PDU:
            case SmsManager.RESULT_ERROR_RADIO_OFF:
                markMessageError(context, uri);
                break;
            default:
                markMessageSent(context, uri);
                break;
        }
    }

    private void markMessageSent(Context context, Uri uri) {
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

            DataSource source = DataSource.getInstance(context);
            source.open();
            Cursor messages = source.searchMessages(body);

            if (messages != null && messages.moveToFirst()) {
                long id = messages.getLong(0);
                source.updateMessageType(id, error ? Message.TYPE_ERROR : Message.TYPE_SENT);

                long conversationId = messages
                        .getLong(messages.getColumnIndex(Message.COLUMN_CONVERSATION_ID));
                MessageListUpdatedReceiver.sendBroadcast(context, conversationId);

                messages.close();
            }

            source.close();
        }
    }

}
