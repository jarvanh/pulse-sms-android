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
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.VisibleForTesting;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.app.RemoteInput;
import android.text.Html;
import android.text.Spanned;
import android.util.LongSparseArray;
import android.view.WindowManager;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import xyz.klinker.messenger.R;
import xyz.klinker.messenger.activity.MessengerActivity;
import xyz.klinker.messenger.activity.NotificationReplyActivity;
import xyz.klinker.messenger.data.ColorSet;
import xyz.klinker.messenger.data.DataSource;
import xyz.klinker.messenger.data.FeatureFlags;
import xyz.klinker.messenger.data.MimeType;
import xyz.klinker.messenger.data.Settings;
import xyz.klinker.messenger.data.model.Conversation;
import xyz.klinker.messenger.data.model.Message;
import xyz.klinker.messenger.receiver.CarReplyReceiver;
import xyz.klinker.messenger.receiver.NotificationDismissedReceiver;
import xyz.klinker.messenger.util.ImageUtils;
import xyz.klinker.messenger.util.NotificationWindowManager;
import xyz.klinker.messenger.util.TvUtils;
import xyz.klinker.messenger.view.NotificationView;
import xyz.klinker.messenger.widget.MessengerAppWidgetProvider;

/**
 * Service for displaying notifications to the user based on which conversations have not been
 * seen yet.
 */
public class NotificationService extends IntentService {

    private static final boolean DEBUG_QUICK_REPLY = false;

    public static Long CONVERSATION_ID_OPEN = 0L;

    private static final String GROUP_KEY_MESSAGES = "messenger_notification_group";
    public static final int SUMMARY_ID = 0;

    private NotificationWindowManager notificationWindowManager;

    private boolean skipSummaryNotification = false;

    public NotificationService() {
        super("NotificationService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        long snoozeTil = Settings.get(this).snooze;
        if (snoozeTil > System.currentTimeMillis()) {
            return;
        }

        skipSummaryNotification = false;

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
                        conversation.phoneNumbers = c.phoneNumbers;
                        conversation.groupConversation = c.phoneNumbers.contains(",");

                        if (c.privateNotifications) {
                            conversation.title = getString(R.string.new_message);
                            conversation.imageUri = null;
                            conversation.ringtoneUri = null;
                            conversation.color = Settings.get(this).globalColorSet.color;
                            conversation.privateNotification = true;
                        } else {
                            conversation.privateNotification = false;
                        }

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

        try {
            float height = getResources().getDimension(android.R.dimen.notification_large_icon_height);
            float width = getResources().getDimension(android.R.dimen.notification_large_icon_width);
            contactImage = Bitmap.createScaledBitmap(contactImage, (int) width, (int) height, true);
        } catch (Exception e) { }

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
                .setSound(getRingtone(conversation))
                .setTicker(getString(R.string.notification_ticker, conversation.title))
                .setVisibility(Notification.VISIBILITY_PRIVATE)
                .setWhen(conversation.timestamp);

        try {
            if (!conversation.groupConversation) {
                builder.addPerson("tel:" + conversation.phoneNumbers);
            }
        } catch (Exception e) { }

        NotificationCompat.BigPictureStyle pictureStyle = null;
        NotificationCompat.InboxStyle inboxStyle = null;
        NotificationCompat.Style messagingStyle = null;

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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            // build a messaging style notifation for Android Nougat
            messagingStyle = new NotificationCompat.MessagingStyle(getString(R.string.you));

            if (conversation.groupConversation) {
                ((NotificationCompat.MessagingStyle) messagingStyle)
                        .setConversationTitle(conversation.title);
            }

            DataSource source = getDataSource();
            source.open();
            List<Message> messages = source.getMessages(conversation.id, 10);
            source.close();

            for (int i = messages.size() - 1; i >= 0; i--) {
                Message message = messages.get(i);

                String from = null;
                if (message.type == Message.TYPE_RECEIVED) {
                    // we split it so that we only get the first name,
                    // if there is more than one

                    if (message.from != null) {
                        // it is most likely a group message.
                        from = message.from;
                    } else {
                        from = conversation.title;
                    }
                }

                String messageText = "";
                if (MimeType.isAudio(message.mimeType)) {
                    messageText += "<i>" + getString(R.string.audio_message) + "</i>";
                } else if (MimeType.isVideo(message.mimeType)) {
                    messageText += "<i>" + getString(R.string.video_message) + "</i>";
                } else if (MimeType.isVcard(message.mimeType)) {
                    messageText += "<i>" + getString(R.string.contact_card) + "</i>";
                } else if (MimeType.isStaticImage(message.mimeType)) {
                    messageText += "<i>" + getString(R.string.picture_message) + "</i>";
                } else {
                    messageText += message.data;
                }

                ((NotificationCompat.MessagingStyle) messagingStyle)
                        .addMessage(Html.fromHtml(messageText), message.timestamp, from);
            }
        }

        String content = text.toString().trim();
        if (content.endsWith(" |")) {
            content = content.substring(0, content.length() - 2);
        }

        if (!conversation.privateNotification) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                builder.setContentText(Html.fromHtml(content, 0));
            } else {
                builder.setContentText(Html.fromHtml(content));
            }

            if (pictureStyle != null) {
                builder.setStyle(pictureStyle);
            } else if (messagingStyle != null) {
                builder.setStyle(messagingStyle);
            } else if (inboxStyle != null) {
                builder.setStyle(inboxStyle);
            } else {
                builder.setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(Html.fromHtml(content)));
            }
        }

        NotificationCompat.Builder publicVersion = new NotificationCompat.Builder(this)
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
                .setVisibility(Notification.VISIBILITY_PUBLIC);

        try {
            if (!conversation.groupConversation) {
                publicVersion.addPerson("tel:" + conversation.phoneNumbers);
            }
        } catch (Exception e) { }

        builder.setPublicVersion(publicVersion.build());


        // one thing to keep in mind here... my adding only a wearable extender to the notification,
        // will the action be shown on phones or only on wear devices? If it is shown on phones, is
        // it only shown on Nougat+ where these actions can be accepted?
        RemoteInput remoteInput = new RemoteInput.Builder(ReplyService.EXTRA_REPLY)
                .setLabel(getString(R.string.reply_to, conversation.title))
                .setChoices(getResources().getStringArray(R.array.reply_choices))
                .setAllowFreeFormInput(true)
                .build();


        // Android wear extender (add a second page with message history
        NotificationCompat.BigTextStyle secondPageStyle = new NotificationCompat.BigTextStyle();
        secondPageStyle.setBigContentTitle(conversation.title)
                .bigText(getWearableSecondPageConversation(conversation));
        NotificationCompat.Builder wear =
                new NotificationCompat.Builder(this)
                        .setStyle(secondPageStyle);

        NotificationCompat.WearableExtender wearableExtender =
                new NotificationCompat.WearableExtender().addPage(wear.build());

        PendingIntent pendingReply;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && !DEBUG_QUICK_REPLY) {
            // with Android N, we only need to show the the reply service intent through the wearable extender
            Intent reply = new Intent(this, ReplyService.class);
            reply.putExtra(ReplyService.EXTRA_CONVERSATION_ID, conversation.id);
            pendingReply = PendingIntent.getService(this,
                    (int) conversation.id, reply, PendingIntent.FLAG_ONE_SHOT);

            NotificationCompat.Action action = new NotificationCompat.Action.Builder(R.drawable.ic_reply_white,
                    getString(R.string.reply), pendingReply)
                    .addRemoteInput(remoteInput)
                    .build();

            if (!conversation.privateNotification) builder.addAction(action);

            wearableExtender.addAction(action);
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
                NotificationCompat.Action action = new NotificationCompat.Action.Builder(R.drawable.ic_reply_dark,
                        getString(R.string.reply), pendingReply)
                        .build();

                if (!conversation.privateNotification) builder.addAction(action);

                action.icon = R.drawable.ic_reply_white;
                wearableExtender.addAction(action);
            } else {
                NotificationCompat.Action action = new NotificationCompat.Action.Builder(R.drawable.ic_reply_dark,
                        getString(R.string.reply), pendingReply)
                        .addRemoteInput(remoteInput)
                        .build();

                if (!conversation.privateNotification) builder.addAction(action);

                action.icon = R.drawable.ic_reply_white;
                wearableExtender.addAction(action);
            }
        }

        Intent read = new Intent(this, NotificationMarkReadService.class);
        read.putExtra(NotificationMarkReadService.EXTRA_CONVERSATION_ID, conversation.id);
        PendingIntent pendingRead = PendingIntent.getService(this, (int) conversation.id,
                read, PendingIntent.FLAG_ONE_SHOT);

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

        wearableExtender.addAction(new NotificationCompat.Action(R.drawable.ic_done_white, getString(R.string.read), pendingRead));
        builder.addAction(new NotificationCompat.Action(R.drawable.ic_done_dark, getString(R.string.read), pendingRead));

        builder.setDeleteIntent(pendingDelete);
        builder.setContentIntent(pendingOpen);

        Intent carReply = new Intent(this, CarReplyReceiver.class);
        carReply.putExtra(ReplyService.EXTRA_CONVERSATION_ID, conversation.id);
        PendingIntent pendingCarReply = PendingIntent.getBroadcast(this, (int) conversation.id,
                carReply, PendingIntent.FLAG_ONE_SHOT);

        // Android Auto extender
        NotificationCompat.CarExtender.UnreadConversation.Builder car = new
                NotificationCompat.CarExtender.UnreadConversation.Builder(conversation.title)
                .setReadPendingIntent(pendingDelete)
                .setReplyAction(pendingCarReply, remoteInput)
                .setLatestTimestamp(conversation.timestamp);

        for (NotificationMessage message : conversation.messages) {
            if (message.mimeType.equals(MimeType.TEXT_PLAIN)) {
                car.addMessage(message.data);
            } else {
                car.addMessage(getString(R.string.new_mms_message));
            }
        }

        // apply the extenders to the notification
        builder.extend(new NotificationCompat.CarExtender().setUnreadConversation(car.build()));
        builder.extend(wearableExtender);

        if (!conversation.mute) {
            if (CONVERSATION_ID_OPEN == conversation.id) {
                // skip this notification since we are already on the conversation.
                skipSummaryNotification = true;
            } else {
                NotificationManagerCompat.from(this).notify((int) conversation.id, builder.build());
            }

            try {
                if (!TvUtils.hasTouchscreen(this)) {
                    if (notificationWindowManager == null) {
                        notificationWindowManager = new NotificationWindowManager(this);
                    }

                    NotificationView.newInstance(notificationWindowManager)
                            .setImage(null)
                            .setTitle(conversation.title)
                            .setDescription(content)
                            .show();
                }
            } catch (WindowManager.BadTokenException e) {
                e.printStackTrace();
            }
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
            if (conversations.get(conversations.keyAt(i)).privateNotification) {
                summaryBuilder.append(getString(R.string.new_message));
            } else {
                summaryBuilder.append(conversations.get(conversations.keyAt(i)).title);
            }
            
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
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    style.addLine(Html.fromHtml(row, 0));
                } else {
                    style.addLine(Html.fromHtml(row));
                }
            } catch (Throwable t) {
                // there was a motorola device running api 24, but was on 6.0.1? WTF?
                // so catch the throwable instead of checking the api version
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
                .setVisibility(Notification.VISIBILITY_PUBLIC)
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

        if (!skipSummaryNotification) {
            NotificationManagerCompat.from(this).notify(SUMMARY_ID, notification);
        }
    }

    private Spanned getWearableSecondPageConversation(NotificationConversation conversation) {
        DataSource source = getDataSource();
        source.open();
        List<Message> messages = source.getMessages(conversation.id, 10);
        source.close();

        String you = getString(R.string.you);

        StringBuilder builder = new StringBuilder();

        for (Message message : messages) {
            String messageText = "";
            if (MimeType.isAudio(message.mimeType)) {
                messageText += "<i>" + getString(R.string.audio_message) + "</i>";
            } else if (MimeType.isVideo(message.mimeType)) {
                messageText += "<i>" + getString(R.string.video_message) + "</i>";
            } else if (MimeType.isVcard(message.mimeType)) {
                messageText += "<i>" + getString(R.string.contact_card) + "</i>";
            } else if (MimeType.isStaticImage(message.mimeType)) {
                messageText += "<i>" + getString(R.string.picture_message) + "</i>";
            } else {
                messageText += message.data;
            }

            if (message.type == Message.TYPE_RECEIVED) {
                if (message.from != null) {
                    builder.append("<b>" + message.from + "</b>  " + messageText + "<br>");
                } else {
                    builder.append("<b>" + conversation.title + "</b>  " + messageText + "<br>");
                }
            } else {
                builder.append("<b>" + you + "</b>  " + messageText + "<br>");
            }

        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return Html.fromHtml(builder.toString(), 0);
        } else {
            return Html.fromHtml(builder.toString());
        }
    }

    private Uri getRingtone(NotificationConversation conversation) {
        try {
            String globalUri = Settings.get(this).ringtone;

            if (conversation.ringtoneUri == null || conversation.ringtoneUri.isEmpty()) {
                // there is no conversation specific ringtone defined

                if (globalUri == null || globalUri.isEmpty() || !ringtoneExists(globalUri)) {
                    // there is no global ringtone defined, or it doesn't exist on the system
                    return RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                } else {
                    // the global ringtone is available to use
                    return Uri.parse(globalUri);
                }
            } else {
                if (ringtoneExists(conversation.ringtoneUri)) {
                    // conversation ringtone exists and can be played
                    return Uri.parse(conversation.ringtoneUri);
                } else {
                    // the global ringtone is available to use
                    return RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                }
            }
        } catch (Exception e) {
            return RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        }
    }

    private boolean ringtoneExists(String uri) {
        try {
            InputStream stream = getContentResolver().openInputStream(Uri.parse(uri));

            if (stream != null) {
                stream.close();
                return true;
            }
        } catch (Exception e) { }

        return false;
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
        public boolean privateNotification;
        public boolean groupConversation;
        public String phoneNumbers;
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
