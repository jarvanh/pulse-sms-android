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
import android.net.Uri;
import android.os.Build;
import android.support.annotation.VisibleForTesting;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.app.RemoteInput;
import android.text.Html;
import android.util.LongSparseArray;

import java.util.ArrayList;
import java.util.List;

import xyz.klinker.messenger.R;
import xyz.klinker.messenger.activity.MessengerActivity;
import xyz.klinker.messenger.activity.NotificationReplyActivity;
import xyz.klinker.messenger.data.DataSource;
import xyz.klinker.messenger.data.MimeType;
import xyz.klinker.messenger.data.Settings;
import xyz.klinker.messenger.data.model.Conversation;
import xyz.klinker.messenger.data.model.Message;
import xyz.klinker.messenger.receiver.NotificationDismissedReceiver;
import xyz.klinker.messenger.util.ImageUtils;
import xyz.klinker.messenger.widget.MessengerAppWidgetProvider;

/**
 * Service for displaying notifications to the user based on which conversations have not been
 * seen yet.
 */
public class NotificationService extends IntentService {

    private static final boolean DEBUG_QUICK_REPLY = true;

    private static final String GROUP_KEY_MESSAGES = "messenger_notification_group";
    public static final int SUMMARY_ID = 0;

    public NotificationService() {
        super("NotificationService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        long snoozeTil = Settings.get(this).snooze;
        if (snoozeTil > System.currentTimeMillis()) {
            return;
        }

        LongSparseArray<NotificationConversation> conversations = getUnseenConversations();
        List<String> rows = new ArrayList<>();

        if (conversations.size() > 0) {
            for (int i = 0; i < conversations.size(); i++) {
                NotificationConversation conversation = conversations.get(conversations.keyAt(i));
                rows.add(giveConversationNotification(conversation));
            }

            if (conversations.size() > 1) {
                giveSummaryNotification(conversations, rows);
            }
        }

        MessengerAppWidgetProvider.refreshWidget(this);
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
                String from = unseenMessages
                        .getString(unseenMessages.getColumnIndex(Message.COLUMN_FROM));

                NotificationConversation conversation = conversations.get(conversationId);

                if (conversation == null) {
                    Conversation c = source.getConversation(conversationId);
                    if (c != null) {
                        conversation = new NotificationConversation();
                        conversation.id = c.id;
                        conversation.title = c.title;
                        conversation.imageUri = c.imageUri;
                        conversation.color = c.colors.color;
                        conversation.ringtoneUri = c.ringtoneUri;
                        conversation.timestamp = c.timestamp;
                        conversation.mute = c.mute;
                        conversations.put(conversationId, conversation);
                    }
                }

                if (conversation != null) {
                    conversation.messages.add(new NotificationMessage(data, mimeType, timestamp, from));
                }
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
        Bitmap contactImage = ImageUtils.clipToCircle(
                ImageUtils.getBitmap(this, conversation.imageUri));

        int defaults = Notification.DEFAULT_LIGHTS;
        if (Settings.get(this).vibrate) {
            defaults = defaults | Notification.DEFAULT_VIBRATE;
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_stat_notify)
                .setContentTitle(conversation.title)
                .setAutoCancel(true)
                .setCategory(Notification.CATEGORY_MESSAGE)
                .setColor(conversation.color)
                .setDefaults(defaults)
                .setGroup(GROUP_KEY_MESSAGES)
                .setLargeIcon(contactImage)
                .setOnlyAlertOnce(true)
                .setPriority(Notification.PRIORITY_HIGH)
                .setShowWhen(true)
                .setSound(Uri.parse(conversation.ringtoneUri == null ?
                        Settings.get(this).ringtone : conversation.ringtoneUri))
                .setTicker(getString(R.string.notification_ticker, conversation.title))
                .setVisibility(Notification.VISIBILITY_PRIVATE)
                .setWhen(conversation.timestamp);

        NotificationCompat.BigPictureStyle pictureStyle = null;
        NotificationCompat.InboxStyle inboxStyle = null;

        StringBuilder text = new StringBuilder();
        if (conversation.messages.size() > 1 && conversation.messages.get(0).from != null) {
            inboxStyle = new NotificationCompat.InboxStyle();

            for (NotificationMessage message : conversation.messages) {
                if (message.mimeType.equals(MimeType.TEXT_PLAIN)) {
                    String line = "<b>" + message.from + ":</b>  " + message.data;
                    text.append(line);
                    text.append("\n");
                    inboxStyle.addLine(Html.fromHtml(line));
                } else {
                    pictureStyle = new NotificationCompat.BigPictureStyle()
                            .bigPicture(ImageUtils.getBitmap(this, message.data));
                }
            }
        } else {
            for (int i = 0; i < conversation.messages.size(); i++) {
                NotificationMessage message = conversation.messages.get(i);

                if (message.mimeType.equals(MimeType.TEXT_PLAIN)) {
                    if (message.from != null) {
                        text.append("<b>");
                        text.append(message.from);
                        text.append(":</b>  ");
                        text.append(conversation.messages.get(i).data);
                        text.append("\n");
                    } else {
                        text.append(conversation.messages.get(i).data);
                        text.append(" | ");
                    }
                } else if (MimeType.isStaticImage(message.mimeType)) {
                    pictureStyle = new NotificationCompat.BigPictureStyle()
                            .bigPicture(ImageUtils.getBitmap(this, message.data));
                }
            }
        }

        String content = text.toString().trim();
        if (content.endsWith(" |")) {
            content = content.substring(0, content.length() - 2);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            builder.setContentText(Html.fromHtml(content, 0));
        } else {
            builder.setContentText(Html.fromHtml(content));
        }

        if (pictureStyle != null) {
            builder.setStyle(pictureStyle);
        } else if (inboxStyle != null) {
            builder.setStyle(inboxStyle);
        } else {
            builder.setStyle(new NotificationCompat.BigTextStyle()
                    .bigText(Html.fromHtml(content)));
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
                .setDefaults(Notification.DEFAULT_SOUND | Notification.DEFAULT_LIGHTS)
                .setGroup(GROUP_KEY_MESSAGES)
                .build());


        // one thing to keep in mind here... my adding only a wearable extender to the notification,
        // will the action be shown on phones or only on wear devices? If it is shown on phones, is
        // it only shown on Nougat+ where these actions can be accepted?
        RemoteInput remoteInput = new RemoteInput.Builder(ReplyService.EXTRA_REPLY)
                .setLabel(getString(R.string.reply_to, conversation.title))
                .setChoices(getResources().getStringArray(R.array.reply_choices))
                .setAllowFreeFormInput(true)
                .build();

        PendingIntent pendingReply;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && !DEBUG_QUICK_REPLY) {
            // with Android N, we only need to show the the reply service intent through the wearable extender
            Intent reply = new Intent(this, ReplyService.class);
            reply.putExtra(ReplyService.EXTRA_CONVERSATION_ID, conversation.id);
            pendingReply = PendingIntent.getService(this,
                    (int) conversation.id, reply, PendingIntent.FLAG_ONE_SHOT);

            NotificationCompat.Action action = new NotificationCompat.Action.Builder(R.drawable.ic_reply,
                    getString(R.string.reply), pendingReply)
                    .addRemoteInput(remoteInput)
                    .build();

            builder.extend(new NotificationCompat.WearableExtender().addAction(action));
        } else {
            // on older versions, we have to show the reply activity button as an action and add the remote input to it
            // this will allow it to be used on android wear (we will have to handle this from the activity)
            // as well as have a reply quick action button.
            Intent reply = new Intent(this, NotificationReplyActivity.class);
            reply.putExtra(ReplyService.EXTRA_CONVERSATION_ID, conversation.id);
            pendingReply = PendingIntent.getActivity(this,
                    (int) conversation.id, reply, PendingIntent.FLAG_ONE_SHOT);

            if (DEBUG_QUICK_REPLY) {
                // if we are debugging, the assumption is that we are on android N, we have to be stop showing
                // the remote input or else it will keep using the direct reply
                NotificationCompat.Action action = new NotificationCompat.Action.Builder(R.drawable.ic_reply,
                        getString(R.string.reply), pendingReply)
                        .build();

                builder.addAction(action);
            } else {
                NotificationCompat.Action action = new NotificationCompat.Action.Builder(R.drawable.ic_reply,
                        getString(R.string.reply), pendingReply)
                        .addRemoteInput(remoteInput)
                        .build();

                builder.addAction(action);
            }
        }

        Intent delete = new Intent(this, NotificationDismissedReceiver.class);
        delete.putExtra(NotificationDismissedService.EXTRA_CONVERSATION_ID, conversation.id);
        PendingIntent pendingDelete = PendingIntent.getBroadcast(this, (int) conversation.id,
                delete, PendingIntent.FLAG_ONE_SHOT);

        Intent open = new Intent(this, MessengerActivity.class);
        open.putExtra(MessengerActivity.EXTRA_CONVERSATION_ID, conversation.id);
        open.putExtra(MessengerActivity.EXTRA_FROM_NOTIFICATION, true);
        open.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingOpen = PendingIntent.getActivity(this, (int) conversation.id,
                open, PendingIntent.FLAG_ONE_SHOT);

        builder.setDeleteIntent(pendingDelete);
        builder.setContentIntent(pendingOpen);

        NotificationCompat.CarExtender.UnreadConversation.Builder car = new
                NotificationCompat.CarExtender.UnreadConversation.Builder(conversation.title)
                .setReadPendingIntent(pendingDelete)
                .setReplyAction(pendingReply, remoteInput)
                .setLatestTimestamp(conversation.timestamp);

        for (NotificationMessage message : conversation.messages) {
            if (message.mimeType.equals(MimeType.TEXT_PLAIN)) {
                car.addMessage(message.data);
            } else {
                car.addMessage(getString(R.string.new_mms_message));
            }
        }

        builder.extend(new NotificationCompat.CarExtender().setUnreadConversation(car.build()));

        if (!conversation.mute) {
            NotificationManagerCompat.from(this).notify((int) conversation.id, builder.build());
        }

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
        public boolean mute;
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
        public String from;

        private NotificationMessage(String data, String mimeType, long timestamp, String from) {
            this.data = data;
            this.mimeType = mimeType;
            this.timestamp = timestamp;
            this.from = from;
        }
    }

}
