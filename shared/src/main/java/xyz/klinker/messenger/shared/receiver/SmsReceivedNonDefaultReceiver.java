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

import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Telephony;
import android.telephony.SmsMessage;
import android.util.Log;

import java.util.List;

import xyz.klinker.messenger.shared.data.DataSource;
import xyz.klinker.messenger.shared.data.MimeType;
import xyz.klinker.messenger.shared.data.model.Conversation;
import xyz.klinker.messenger.shared.data.model.Message;
import xyz.klinker.messenger.shared.service.MediaParserService;
import xyz.klinker.messenger.shared.service.NotificationService;
import xyz.klinker.messenger.shared.util.BlacklistUtils;
import xyz.klinker.messenger.shared.util.DualSimUtils;
import xyz.klinker.messenger.shared.util.PermissionsUtils;
import xyz.klinker.messenger.shared.util.PhoneNumberUtils;
import xyz.klinker.messenger.shared.util.SmsMmsUtils;
import xyz.klinker.messenger.shared.util.TimeUtils;

public class SmsReceivedNonDefaultReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, final Intent intent) {
        final Handler handler = new Handler();

        new Thread(() -> {
            try {
                Thread.sleep(4000);
                handleReceiver(context, intent, handler);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void handleReceiver(Context context, Intent intent, Handler handler) throws Exception {
        Bundle extras = intent.getExtras();

        String body = "";
        String address = "";
        Object[] smsExtra = (Object[]) extras.get("pdus");

        if (smsExtra == null) {
            return;
        }

        for (Object message : smsExtra) {
            SmsMessage sms;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                String format = extras.getString("format");
                sms = SmsMessage.createFromPdu((byte[]) message, format);
            } else {
                sms = SmsMessage.createFromPdu((byte[]) message);
            }

            body += sms.getMessageBody();
            address = sms.getOriginatingAddress();
        }

        if (BlacklistUtils.isBlacklisted(context, address)) {
            return;
        }

        long conversationId = insertSms(context, handler, address, body);

        if (conversationId != -1L && PermissionsUtils.isDefaultSmsApp(context)) {
            context.startService(new Intent(context, NotificationService.class));
        }
    }

    private long insertSms(Context context, Handler handler, String address, String body) {
        Message message = new Message();
        message.type = Message.TYPE_RECEIVED;
        message.data = body.trim();
        message.timestamp = System.currentTimeMillis();
        message.mimeType = MimeType.TEXT_PLAIN;
        message.read = false;
        message.seen = false;
        message.simPhoneNumber = null;

        DataSource source = DataSource.Companion.getInstance(context);
        source.open();

        if (shouldSaveMessages(source, message)) {
            long conversationId = source
                    .insertMessage(message, PhoneNumberUtils.clearFormatting(address), context);

            Conversation conversation = source.getConversation(conversationId);

            handler.post(() -> {
                ConversationListUpdatedReceiver.sendBroadcast(context, conversationId, body, NotificationService.CONVERSATION_ID_OPEN == conversationId);
                MessageListUpdatedReceiver.sendBroadcast(context, conversationId, message.data, message.type);
            });

            if (conversation.mute) {
                source.seenConversation(conversationId);
                source.close();

                // don't run the notification service
                return -1;
            }

            source.close();
            return conversationId;
        } else {
            source.close();
            return -1;
        }
    }

    public static boolean shouldSaveMessages(DataSource source, Message message) {
        try {
            List<Message> search = source.searchMessagesAsList(message.data, 1);
            if (search.isEmpty()) {
                return true;
            }
        } catch (Exception e) { }

        return false;
    }

}
