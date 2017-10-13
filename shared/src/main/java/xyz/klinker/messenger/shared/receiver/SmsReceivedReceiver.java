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

import xyz.klinker.messenger.api.implementation.Account;
import xyz.klinker.messenger.api.implementation.firebase.AnalyticsHelper;
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
        if (Account.INSTANCE.exists() && !Account.INSTANCE.getPrimary()) {
            return;
        }

        final Handler handler = new Handler();
        new Thread(() -> {
            try {
                handleReceiver(context, intent, handler);
            } catch (Exception e) {
                AnalyticsHelper.failedToSaveSms(context, e.getMessage());
                e.printStackTrace();
            }
        }).start();
    }

    private void handleReceiver(Context context, Intent intent, Handler handler) {
        Bundle extras = intent.getExtras();

        final int simSlot = extras.getInt("slot", -1);
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
        final ContentValues values = new ContentValues(5);
        values.put(Telephony.Sms.ADDRESS, address);
        values.put(Telephony.Sms.BODY, body);
        values.put(Telephony.Sms.DATE, System.currentTimeMillis());
        values.put(Telephony.Sms.READ, "1");
        values.put(Telephony.Sms.DATE_SENT, dateSent);

        new Thread(() -> {
            try {
                context.getContentResolver().insert(Telephony.Sms.Inbox.CONTENT_URI, values);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private long insertSms(Context context, Handler handler, String address, String body, int simSlot) {
        Message message = new Message();
        message.setType(Message.Companion.getTYPE_RECEIVED());
        message.setData(body.trim());
        message.setTimestamp(System.currentTimeMillis());
        message.setMimeType(MimeType.INSTANCE.getTEXT_PLAIN());
        message.setRead(false);
        message.setSeen(false);
        message.setSimPhoneNumber(DualSimUtils.get(context).getNumberFromSimSlot(simSlot));
        message.setSentDeviceId(-1L);

        final DataSource source = DataSource.INSTANCE;

        if (shouldSaveMessages(context, source, message)) {
            long conversationId;

            try {
                conversationId = source.insertMessage(message, PhoneNumberUtils.clearFormatting(address), context);
            } catch (Exception e) {
                source.ensureActionable(context);
                conversationId = source.insertMessage(message, PhoneNumberUtils.clearFormatting(address), context);
            }

            final long fConvoId = conversationId;
            Conversation conversation = source.getConversation(context, conversationId);
            handler.post(() -> {
                ConversationListUpdatedReceiver.sendBroadcast(context, fConvoId, body, NotificationService.CONVERSATION_ID_OPEN == fConvoId);
                MessageListUpdatedReceiver.sendBroadcast(context, fConvoId, message.getData(), message.getType());
            });

            if (conversation != null && conversation.getMute()) {
                source.seenConversation(context, conversationId);
                // don't run the notification service
                return -1;
            }

            return conversationId;
        } else {
            return -1;
        }
    }

    public static boolean shouldSaveMessages(Context context, DataSource source, Message message) {
        try {
            List<Message> search = source.searchMessagesAsList(context, message.getData(), 1);
            if (!search.isEmpty()) {
                Message inDatabase = search.get(0);
                if (inDatabase.getData().equals(message.getData()) && inDatabase.getType() == Message.Companion.getTYPE_RECEIVED() &&
                        (message.getTimestamp() - inDatabase.getTimestamp()) < (TimeUtils.MINUTE * 10)) {
                    return false;
                }
            }
        } catch (Exception e) {
        }

        return true;
    }

}
