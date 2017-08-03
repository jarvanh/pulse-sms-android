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
import xyz.klinker.messenger.shared.util.AndroidVersionUtil;
import xyz.klinker.messenger.shared.util.BlacklistUtils;
import xyz.klinker.messenger.shared.util.DualSimUtils;
import xyz.klinker.messenger.shared.util.PhoneNumberUtils;
import xyz.klinker.messenger.shared.util.TimeUtils;
import xyz.klinker.messenger.shared.util.media.MediaParser;

public class SmsReceivedReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, final Intent intent) {
        final Handler handler = new Handler();
        //new Thread(() -> {
            try {
                handleReceiver(context, intent, handler);
            } catch (Exception e) {
                e.printStackTrace();
            }
        //}).start();
    }

    private void handleReceiver(Context context, Intent intent, Handler handler) throws Exception {
        Bundle extras = intent.getExtras();

        int simSlot = extras.getInt("slot", -1);
        String body = "";
        String address = "";
        long date = System.currentTimeMillis();
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
            date = sms.getTimestampMillis();
        }

        if (BlacklistUtils.isBlacklisted(context, address)) {
            return;
        }

        insertInternalSms(context, address, body, date);
        long conversationId = insertSms(context, handler, address, body, simSlot);

        if (conversationId != -1L) {
            context.startService(new Intent(context, NotificationService.class));

            if (MediaParserService.createParser(context, body.trim()) != null) {
                MediaParserService.start(context, conversationId, body);
            }
        }
    }

    private void insertInternalSms(Context context, String address, String body, long dateSent) {
        ContentValues values = new ContentValues(5);
        values.put(Telephony.Sms.ADDRESS, address);
        values.put(Telephony.Sms.BODY, body);
        values.put(Telephony.Sms.DATE, System.currentTimeMillis());
        values.put(Telephony.Sms.READ, "1");
        values.put(Telephony.Sms.DATE_SENT, dateSent);

        try {
            context.getContentResolver().insert(Telephony.Sms.Inbox.CONTENT_URI, values);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private long insertSms(Context context, Handler handler, String address, String body, int simSlot) {
        Message message = new Message();
        message.type = Message.TYPE_RECEIVED;
        message.data = body.trim();
        message.timestamp = System.currentTimeMillis();
        message.mimeType = MimeType.TEXT_PLAIN;
        message.read = false;
        message.seen = false;
        message.simPhoneNumber = DualSimUtils.get(context).getNumberFromSimSlot(simSlot);

        DataSource source = DataSource.getInstance(context);
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
            if (!search.isEmpty()) {
                Message inDatabase = search.get(0);
                if (inDatabase.data.equals(message.data) && inDatabase.type == Message.TYPE_RECEIVED &&
                        (message.timestamp - inDatabase.timestamp) < (TimeUtils.MINUTE * 3)) {
                    return false;
                }
            }
        } catch (Exception e) { }

        return true;
    }

}
