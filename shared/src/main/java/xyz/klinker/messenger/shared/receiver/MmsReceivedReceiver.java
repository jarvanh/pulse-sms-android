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
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.support.annotation.VisibleForTesting;

import java.util.List;

import xyz.klinker.messenger.api.implementation.Account;
import xyz.klinker.messenger.shared.data.DataSource;
import xyz.klinker.messenger.shared.data.MimeType;
import xyz.klinker.messenger.shared.data.MmsSettings;
import xyz.klinker.messenger.shared.data.model.Message;
import xyz.klinker.messenger.shared.service.MediaParserService;
import xyz.klinker.messenger.shared.service.NotificationService;
import xyz.klinker.messenger.shared.util.BlacklistUtils;
import xyz.klinker.messenger.shared.util.ContactUtils;
import xyz.klinker.messenger.shared.util.DualSimUtils;
import xyz.klinker.messenger.shared.util.MediaSaver;
import xyz.klinker.messenger.shared.util.PhoneNumberUtils;
import xyz.klinker.messenger.shared.util.SmsMmsUtils;

/**
 * Receiver for notifying us when a new MMS has been received by the device. By default it will
 * persist the message to the internal database. We also need to add functionality for
 * persisting it to our own database and giving a notification that it has been received.
 */
public class MmsReceivedReceiver extends com.klinker.android.send_message.MmsReceivedReceiver {

    private Long conversationId = null;

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);

        String nullableOrBlankBodyText = insertMms(context);

        if (nullableOrBlankBodyText != null && !nullableOrBlankBodyText.isEmpty() && conversationId != null) {
            Intent mediaParser = new Intent(context, MediaParserService.class);
            mediaParser.putExtra(MediaParserService.EXTRA_CONVERSATION_ID, conversationId);
            mediaParser.putExtra(MediaParserService.EXTRA_BODY_TEXT, nullableOrBlankBodyText.trim());
            new Handler().postDelayed(() -> context.startService(mediaParser), 2000);
        }

        context.startService(new Intent(context, NotificationService.class));
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
            String cleanNumber = PhoneNumberUtils.clearFormatting(number);
            String myCleanNumber = PhoneNumberUtils.clearFormatting(myNumber);
            String contactName = ContactUtils.findContactNames(number, context);
            
            if (!cleanNumber.contains(myCleanNumber) && !myCleanNumber.contains(cleanNumber) && !contactName.equals(myName) &&
                    !builder.toString().contains(cleanNumber)) {
                builder.append(number);
                builder.append(", ");
            }
        }

        builder.append(from);
        return SmsMmsUtils.stripDuplicatePhoneNumbers(builder.toString());
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
