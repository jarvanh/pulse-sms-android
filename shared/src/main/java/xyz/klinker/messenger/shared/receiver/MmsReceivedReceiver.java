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

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.support.annotation.VisibleForTesting;
import android.util.Log;

import java.util.List;

import xyz.klinker.messenger.api.implementation.Account;
import xyz.klinker.messenger.shared.data.DataSource;
import xyz.klinker.messenger.shared.data.FeatureFlags;
import xyz.klinker.messenger.shared.data.MimeType;
import xyz.klinker.messenger.shared.data.MmsSettings;
import xyz.klinker.messenger.shared.data.Settings;
import xyz.klinker.messenger.shared.data.model.Conversation;
import xyz.klinker.messenger.shared.data.model.Message;
import xyz.klinker.messenger.shared.service.MediaParserService;
import xyz.klinker.messenger.shared.service.NotificationService;
import xyz.klinker.messenger.shared.util.AndroidVersionUtil;
import xyz.klinker.messenger.shared.util.BlacklistUtils;
import xyz.klinker.messenger.shared.util.ContactUtils;
import xyz.klinker.messenger.shared.util.DualSimUtils;
import xyz.klinker.messenger.shared.util.MediaSaver;
import xyz.klinker.messenger.shared.util.PhoneNumberUtils;
import xyz.klinker.messenger.shared.util.SmsMmsUtils;
import xyz.klinker.messenger.shared.util.TimeUtils;

/**
 * Receiver for notifying us when a new MMS has been received by the device. By default it will
 * persist the message to the internal database. We also need to add functionality for
 * persisting it to our own database and giving a notification that it has been received.
 */
public class MmsReceivedReceiver extends com.klinker.android.send_message.MmsReceivedReceiver {

    private Long conversationId = null;
    private boolean ignoreNotification = false;

    @Override
    public void onReceive(Context context, Intent intent) {
        new Thread(() -> {
            try {
                super.onReceive(context, intent);
            } catch (Exception e) {
                e.printStackTrace();
            }

            String nullableOrBlankBodyText = insertMms(context);

            if (!ignoreNotification) {
                try {
                    context.startService(new Intent(context, NotificationService.class));
                } catch (Exception e) {
                    if (AndroidVersionUtil.isAndroidO()) {
                        Intent foregroundNotificationService = new Intent(context, NotificationService.class);
                        foregroundNotificationService.putExtra(NotificationService.EXTRA_FOREGROUND, true);
                        context.startForegroundService(foregroundNotificationService);
                    }
                }
            }

            if (nullableOrBlankBodyText != null && !nullableOrBlankBodyText.isEmpty() && conversationId != null) {
                if (MediaParserService.createParser(context, nullableOrBlankBodyText.trim()) != null) {
                    MediaParserService.start(context, conversationId, nullableOrBlankBodyText);
                }
            }
        }).start();
    }

    private String insertMms(Context context) {
        Cursor lastMessage = SmsMmsUtils.getLastMmsMessage(context);

        String snippet = "";
        if (lastMessage != null && lastMessage.moveToFirst()) {
            Uri uri = Uri.parse("content://mms/" + lastMessage.getLong(0));
            final String from = SmsMmsUtils.getMmsFrom(uri, context);

            if (BlacklistUtils.isBlacklisted(context, from)) {
                return null;
            }

            final String to = SmsMmsUtils.getMmsTo(uri, context);
            final String phoneNumbers = getPhoneNumbers(from, to,
                    PhoneNumberUtils.getMyPhoneNumber(context), context);
            List<ContentValues> values = SmsMmsUtils.processMessage(lastMessage, -1L, context);

            DataSource source = DataSource.getInstance(context);
            source.open();

            for (ContentValues value : values) {
                Message message = new Message();
                message.type = value.getAsInteger(Message.COLUMN_TYPE);
                message.data = value.getAsString(Message.COLUMN_DATA).trim();
                message.timestamp = value.getAsLong(Message.COLUMN_TIMESTAMP);
                message.mimeType = value.getAsString(Message.COLUMN_MIME_TYPE);
                message.read = false;
                message.seen = false;
                message.from = ContactUtils.findContactNames(from, context);
                message.simPhoneNumber = DualSimUtils.get(context).getAvailableSims().isEmpty() ? null : to;

                if (message.mimeType.equals(MimeType.TEXT_PLAIN)) {
                    snippet = message.data;
                }

                if (phoneNumbers.split(", ").length == 1) {
                    message.from = null;
                }

                if (SmsReceivedReceiver.shouldSaveMessages(source, message)) {
                    conversationId = source.insertMessage(message, phoneNumbers, context);

                    Conversation conversation = source.getConversation(conversationId);
                    if (conversation != null && conversation.mute) {
                        source.seenConversation(conversationId);
                        ignoreNotification = true;
                    }

                    if (MmsSettings.get(context).autoSaveMedia &&
                            !MimeType.TEXT_PLAIN.equals(message.mimeType)) {
                        new MediaSaver(context).saveMedia(message);
                    }
                }
            }

            source.close();

            if (conversationId != null) {
                ConversationListUpdatedReceiver.sendBroadcast(context, conversationId,
                        snippet, false);
                MessageListUpdatedReceiver.sendBroadcast(context, conversationId);
            }
        }

        try {
            lastMessage.close();
        } catch (Exception e) { }

        return snippet;
    }

    @VisibleForTesting
    protected String getPhoneNumbers(String from, String to, String myNumber, Context context) {
        String[] toNumbers = to.split(", ");
        StringBuilder builder = new StringBuilder();
        String myName = getMyName(context);
        
        for (String number : toNumbers) {
            String cleanNumber = PhoneNumberUtils.clearFormatting(number).replace("+", "");
            String myCleanNumber = PhoneNumberUtils.clearFormatting(myNumber).replace("+", "");
            String contactName = ContactUtils.findContactNames(number, context);
            String idMatcher = SmsMmsUtils.createIdMatcher(cleanNumber).sevenLetter;
            String myIdMatcher = SmsMmsUtils.createIdMatcher(myCleanNumber).sevenLetter;
            
            if (!cleanNumber.contains(myCleanNumber) && !myCleanNumber.contains(cleanNumber) && !contactName.equals(myName) &&
                    !builder.toString().contains(cleanNumber) && !idMatcher.contains(myIdMatcher) &&
                    !myIdMatcher.equals(idMatcher) && !contactName.toLowerCase().equals("me")) {
                builder.append(number);
                builder.append(", ");
            }
        }

        builder.append(from);
        return builder.toString().replaceAll(",", ", ").replaceAll("  ", " ");
    }

    @VisibleForTesting
    protected String getMyName(Context context) {
        return Account.get(context).myName;
    }

    @VisibleForTesting
    protected String getContactName(Context context, String number) {
        return ContactUtils.findContactNames(number, context);
    }

}
