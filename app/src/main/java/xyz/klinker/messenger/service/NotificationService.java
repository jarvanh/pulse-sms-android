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

package xyz.klinker.messenger.service;

import android.app.IntentService;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.VisibleForTesting;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.text.Html;
import android.util.LongSparseArray;

import java.util.ArrayList;
import java.util.List;

import xyz.klinker.messenger.R;
import xyz.klinker.messenger.activity.MessengerActivity;
import xyz.klinker.messenger.data.DataSource;
import xyz.klinker.messenger.data.MimeType;
import xyz.klinker.messenger.data.model.Conversation;
import xyz.klinker.messenger.data.model.Message;
import xyz.klinker.messenger.util.ImageUtil;

/**
 * Service for displaying notifications to the user based on which conversations have not been
 * seen yet.
 */
public class NotificationService extends IntentService {

    private static final String GROUP_KEY_MESSAGES = "messenger_notification_group";
    private static final int SUMMARY_ID = 0;

    public NotificationService() {
        super("NotificationService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        LongSparseArray<NotificationConversation> conversations = getUnseenConversations();
        List<String> rows = new ArrayList<>();

        for (int i = 0; i < conversations.size(); i++) {
            NotificationConversation conversation = conversations.get(conversations.keyAt(i));
            rows.add(giveConversationNotification(conversation));
        }

        giveSummaryNotification(conversations, rows);
    }

    @VisibleForTesting
    LongSparseArray<NotificationConversation> getUnseenConversations() {
        DataSource source = getDataSource();
        source.open();

        Cursor unseenMessages = source.getUnseenMessages();
        LongSparseArray<NotificationConversation> conversations = new LongSparseArray<>();

        if (unseenMessages != null && unseenMessages.moveToFirst()) {
            do {
                long conversationId = unseenMessages
                        .getLong(unseenMessages.getColumnIndex(Message.COLUMN_CONVERSATION_ID));
                String data = unseenMessages
                        .getString(unseenMessages.getColumnIndex(Message.COLUMN_DATA));
                String mimeType = unseenMessages
                        .getString(unseenMessages.getColumnIndex(Message.COLUMN_MIME_TYPE));
                long timestamp = unseenMessages
                        .getLong(unseenMessages.getColumnIndex(Message.COLUMN_TIMESTAMP));

                NotificationConversation conversation = conversations.get(conversationId);

                if (conversation == null) {
                    Conversation c = source.getConversation(conversationId);
                    conversation = new NotificationConversation();
                    conversation.id = c.id;
                    conversation.title = c.title;
                    conversation.imageUri = c.imageUri;
                    conversation.color = c.colors.color;
                    conversation.ringtoneUri = c.ringtoneUri;
                    conversation.timestamp = c.timestamp;
                    conversations.put(conversationId, conversation);
                }

                conversation.messages.add(new NotificationMessage(data, mimeType, timestamp));
            } while (unseenMessages.moveToNext());

            unseenMessages.close();
        }

        source.close();
        return conversations;
    }

    /**
     * Displays a notification for a single conversation.
     */
    private String giveConversationNotification(NotificationConversation conversation) {
        Bitmap contactImage = ImageUtil.clipToCircle(
                ImageUtil.getBitmap(this, conversation.imageUri));

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_stat_notify)
                .setContentTitle(conversation.title)
                .setAutoCancel(true)
                .setCategory(Notification.CATEGORY_MESSAGE)
                .setColor(conversation.color)
                .setDefaults(Notification.DEFAULT_ALL)
                .setGroup(GROUP_KEY_MESSAGES)
                .setLargeIcon(contactImage)
                .setOnlyAlertOnce(true)
                .setPriority(Notification.PRIORITY_HIGH)
                .setShowWhen(true)
                .setSound(conversation.ringtoneUri == null ?
                        null : Uri.parse(conversation.ringtoneUri))
                .setTicker(getString(R.string.notification_ticker, conversation.title))
                .setVisibility(Notification.VISIBILITY_PRIVATE)
                .setWhen(conversation.timestamp);

        NotificationCompat.BigPictureStyle pictureStyle = null;

        StringBuilder text = new StringBuilder();
        for (int i = 0; i < conversation.messages.size(); i++) {
            NotificationMessage message = conversation.messages.get(i);

            if (message.mimeType.equals(MimeType.TEXT_PLAIN)) {
                text.append(conversation.messages.get(i).data);
                text.append(" | ");
            } else if (message.mimeType.startsWith("image/")) {
                pictureStyle = new NotificationCompat.BigPictureStyle()
                        .bigPicture(ImageUtil.getBitmap(this, message.data));
            }
        }

        String content = text.toString();
        if (content.endsWith(" | ")) {
            content = content.substring(0, content.length() - 3);
        }

        builder.setContentText(content);

        if (pictureStyle != null) {
            pictureStyle.setSummaryText(content);
            builder.setStyle(pictureStyle);
        } else {
            builder.setStyle(new NotificationCompat.BigTextStyle()
                    .bigText(content));
        }

        builder.setPublicVersion(new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_stat_notify)
                .setContentTitle(conversation.title)
                .setContentText(getResources().getQuantityString(R.plurals.new_messages,
                        conversation.messages.size(), conversation.messages.size()))
                .setLargeIcon(contactImage)
                .setColor(conversation.color)
                .setAutoCancel(true)
                .setCategory(Notification.CATEGORY_MESSAGE)
                .setColor(conversation.color)
                .setDefaults(Notification.DEFAULT_ALL)
                .setGroup(GROUP_KEY_MESSAGES)
                .build());

        // TODO set reply action and wearable extender

        Intent delete = new Intent(this, NotificationDismissedService.class);
        delete.putExtra(NotificationDismissedService.EXTRA_CONVERSATION_ID, conversation.id);
        PendingIntent pendingDelete = PendingIntent.getService(this, (int) conversation.id,
                delete, PendingIntent.FLAG_ONE_SHOT);

        Intent open = new Intent(this, MessengerActivity.class);
        open.putExtra(MessengerActivity.EXTRA_CONVERSATION_ID, conversation.id);
        open.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingOpen = PendingIntent.getActivity(this, (int) conversation.id,
                open, PendingIntent.FLAG_ONE_SHOT);

        builder.setDeleteIntent(pendingDelete);
        builder.setContentIntent(pendingOpen);

        NotificationManagerCompat.from(this).notify((int) conversation.id, builder.build());

        return "<b>" + conversation.title + "</b>  " + content;
    }

    /**
     * Displays a summary notification for all conversations using the rows returned by each
     * individual notification.
     */
    private void giveSummaryNotification(LongSparseArray<NotificationConversation> conversations,
                                         List<String> rows) {
        StringBuilder summaryBuilder = new StringBuilder();
        for (int i = 0; i < conversations.size(); i++) {
            summaryBuilder.append(conversations.get(conversations.keyAt(i)).title);
            summaryBuilder.append(", ");
        }

        String summary = summaryBuilder.toString();
        if (summary.endsWith(", ")) {
            summary = summary.substring(0, summary.length() - 2);
        }

        String title = getResources().getQuantityString(R.plurals.new_conversations,
                conversations.size(), conversations.size());

        NotificationCompat.InboxStyle style = new NotificationCompat.InboxStyle()
                .setSummaryText(summary)
                .setBigContentTitle(title);

        for (String row : rows) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                style.addLine(Html.fromHtml(row, 0));
            } else {
                style.addLine(Html.fromHtml(row));
            }
        }

        Notification publicVersion = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_stat_notify)
                .setContentTitle(title)
                .setContentText(summary)
                .setGroup(GROUP_KEY_MESSAGES)
                .setGroupSummary(true)
                .setAutoCancel(true)
                .setCategory(Notification.CATEGORY_MESSAGE)
                .setColor(getResources().getColor(R.color.colorPrimary))
                .setPriority(Notification.PRIORITY_HIGH)
                .build();

        Intent delete = new Intent(this, NotificationDismissedService.class);
        PendingIntent pendingDelete = PendingIntent.getService(this, 0,
                delete, PendingIntent.FLAG_ONE_SHOT);

        Intent open = new Intent(this, MessengerActivity.class);
        open.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingOpen = PendingIntent.getActivity(this, 0,
                open, PendingIntent.FLAG_ONE_SHOT);

        Notification notification = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_stat_notify)
                .setContentTitle(title)
                .setContentText(summary)
                .setGroup(GROUP_KEY_MESSAGES)
                .setGroupSummary(true)
                .setAutoCancel(true)
                .setCategory(Notification.CATEGORY_MESSAGE)
                .setColor(getResources().getColor(R.color.colorPrimary))
                .setPriority(Notification.PRIORITY_HIGH)
                .setShowWhen(true)
                .setTicker(title)
                .setVisibility(Notification.VISIBILITY_PRIVATE)
                .setWhen(conversations.get(conversations.keyAt(conversations.size() - 1)).timestamp)
                .setStyle(style)
                .setPublicVersion(publicVersion)
                .setDeleteIntent(pendingDelete)
                .setContentIntent(pendingOpen)
                .build();

        NotificationManagerCompat.from(this).notify(SUMMARY_ID, notification);
    }

    @VisibleForTesting
    DataSource getDataSource() {
        return DataSource.getInstance(this);
    }

    @VisibleForTesting
    class NotificationConversation {
        public long id;
        public String title;
        public String imageUri;
        public int color;
        public String ringtoneUri;
        public long timestamp;
        public List<NotificationMessage> messages;

        private NotificationConversation() {
            messages = new ArrayList<>();
        }
    }

    @VisibleForTesting
    class NotificationMessage {
        public String data;
        public String mimeType;
        public long timestamp;

        private NotificationMessage(String data, String mimeType, long timestamp) {
            this.data = data;
            this.mimeType = mimeType;
            this.timestamp = timestamp;
        }
    }

}
