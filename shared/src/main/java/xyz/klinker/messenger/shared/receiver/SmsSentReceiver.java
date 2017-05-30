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

package xyz.klinker.messenger.shared.receiver;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.Telephony;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.telephony.SmsManager;

import com.klinker.android.send_message.SentReceiver;
import com.klinker.android.send_message.StripAccents;

import java.util.List;

import xyz.klinker.messenger.shared.MessengerActivityExtras;
import xyz.klinker.messenger.shared.R;
import xyz.klinker.messenger.shared.data.DataSource;
import xyz.klinker.messenger.shared.data.Settings;
import xyz.klinker.messenger.shared.data.model.Message;
import xyz.klinker.messenger.shared.service.ResendFailedMessage;
import xyz.klinker.messenger.shared.util.ActivityUtils;
import xyz.klinker.messenger.shared.util.SmsMmsUtils;

/**
 * Receiver for getting notifications of when an SMS has finished sending. By default it's super
 * class will mark the internal message as sent, we need to also mark our database as sent.
 */
public class SmsSentReceiver extends SentReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);

        new Thread(() -> {
            try {
                handleReceiver(context, intent);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void handleReceiver(Context context, Intent intent) {
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

            Settings settings = Settings.get(context);
            if (settings.signature != null && !settings.signature.isEmpty()) {
                body = body.replace("\n" + settings.signature, "");
            }

            DataSource source = DataSource.getInstance(context);
            source.open();
            Cursor messages = source.searchMessages(body);

            if (messages != null && messages.moveToFirst()) {
                long id = messages.getLong(0);
                long conversationId = messages
                        .getLong(messages.getColumnIndex(Message.COLUMN_CONVERSATION_ID));
                String data = messages.getString(messages.getColumnIndex(Message.COLUMN_DATA));

                markMessage(source, context, error, id, conversationId, data);
            } else if (Settings.get(context).stripUnicode) {
                // if the message was unicode, then it won't match here and would never get marked as sent or error
                List<Message> messageList = source.getMessages(5);
                for (Message m : messageList) {
                    if (StripAccents.stripAccents(m.data).equals(body)) {
                        markMessage(source, context, error, m.id, m.conversationId, m.data);
                        break;
                    }
                }
            }

            try {
                messages.close();
            } catch (Exception e) { }

            source.close();
        }
    }

    private void markMessage(DataSource source, Context context, boolean error, long messageId, long conversationId, String data) {
        source.updateMessageType(messageId, error ? Message.TYPE_ERROR : Message.TYPE_SENT);

        MessageListUpdatedReceiver.sendBroadcast(context, conversationId);

        if (error) {
            Intent open = ActivityUtils.buildForComponent(ActivityUtils.MESSENGER_ACTIVITY);
            open.putExtra(MessengerActivityExtras.EXTRA_CONVERSATION_ID, conversationId);
            open.putExtra(MessengerActivityExtras.EXTRA_FROM_NOTIFICATION, true);
            open.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            PendingIntent pendingOpen = PendingIntent.getActivity(context,
                    (int) conversationId, open, PendingIntent.FLAG_UPDATE_CURRENT);

            NotificationCompat.Builder notification = new NotificationCompat.Builder(context)
                    .setSmallIcon(R.drawable.ic_stat_notify)
                    .setContentTitle(context.getString(R.string.message_sending_failed))
                    .setContentText(data)
                    .setColor(context.getResources().getColor(R.color.colorPrimary))
                    .setAutoCancel(true)
                    .setContentIntent(pendingOpen);

            Intent resend = new Intent(context, ResendFailedMessage.class);
            resend.putExtra(ResendFailedMessage.EXTRA_MESSAGE_ID, messageId);
            resend.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            PendingIntent resendPending = PendingIntent.getService(context,
                    (int) messageId, resend, PendingIntent.FLAG_UPDATE_CURRENT);
            NotificationCompat.Action action = new NotificationCompat.Action(
                    R.drawable.ic_reply_dark, context.getString(R.string.resend), resendPending);

            notification.addAction(action);
            NotificationManagerCompat.from(context)
                    .notify(6666 + (int) messageId, notification.build());
        }
    }

}
