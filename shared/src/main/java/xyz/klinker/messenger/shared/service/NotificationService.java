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

package xyz.klinker.messenger.shared.service;

import android.annotation.TargetApi;
import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.support.annotation.VisibleForTesting;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.app.RemoteInput;
import android.text.Html;
import android.text.Spanned;
import android.view.WindowManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import xyz.klinker.messenger.shared.MessengerActivityExtras;
import xyz.klinker.messenger.shared.R;
import xyz.klinker.messenger.api.implementation.Account;
import xyz.klinker.messenger.shared.data.ColorSet;
import xyz.klinker.messenger.shared.data.DataSource;
import xyz.klinker.messenger.shared.data.FeatureFlags;
import xyz.klinker.messenger.shared.data.MimeType;
import xyz.klinker.messenger.shared.data.Settings;
import xyz.klinker.messenger.shared.data.model.Conversation;
import xyz.klinker.messenger.shared.data.model.Message;
import xyz.klinker.messenger.shared.data.pojo.NotificationAction;
import xyz.klinker.messenger.shared.data.pojo.NotificationConversation;
import xyz.klinker.messenger.shared.data.pojo.NotificationMessage;
import xyz.klinker.messenger.shared.data.pojo.VibratePattern;
import xyz.klinker.messenger.shared.receiver.CarReplyReceiver;
import xyz.klinker.messenger.shared.receiver.NotificationDismissedReceiver;
import xyz.klinker.messenger.shared.service.jobs.RepeatNotificationJob;
import xyz.klinker.messenger.shared.util.ActivityUtils;
import xyz.klinker.messenger.shared.util.AndroidVersionUtil;
import xyz.klinker.messenger.shared.util.CursorUtil;
import xyz.klinker.messenger.shared.util.ImageUtils;
import xyz.klinker.messenger.shared.util.MockableDataSourceWrapper;
import xyz.klinker.messenger.shared.util.NotificationServiceHelper;
import xyz.klinker.messenger.shared.util.NotificationUtils;
import xyz.klinker.messenger.shared.util.NotificationWindowManager;
import xyz.klinker.messenger.shared.util.TimeUtils;
import xyz.klinker.messenger.shared.util.TvUtils;
import xyz.klinker.messenger.shared.view.NotificationView;
import xyz.klinker.messenger.shared.widget.MessengerAppWidgetProvider;

/**
 * Service for displaying notifications to the user based on which conversations have not been
 * seen yet.
 * <p/>
 * I used pseudocode here: http://blog.danlew.net/2017/02/07/correctly-handling-bundled-android-notifications/
 */
public class NotificationService extends IntentService {

    public static final String EXTRA_FOREGROUND = "extra_foreground";
    private static final int FOREGROUND_NOTIFICATION_ID = 9934;

    protected static final boolean DEBUG_QUICK_REPLY = false;
    protected static final boolean AUTO_CANCEL = true;
    protected static final boolean ALWAYS_SET_GROUP_KEY = false;
    
    public static Long CONVERSATION_ID_OPEN = 0L;

    public static final String GROUP_KEY_MESSAGES = "messenger_notification_group";
    public static final int SUMMARY_ID = 0;

    private NotificationWindowManager notificationWindowManager;

    private boolean skipSummaryNotification = false;

    public NotificationService() {
        super("NotificationService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        boolean foreground = false;
        if (intent != null && intent.getBooleanExtra(EXTRA_FOREGROUND, false) && AndroidVersionUtil.INSTANCE.isAndroidO()) {
            foreground = true;
            Notification notification = new NotificationCompat.Builder(this,
                    NotificationUtils.INSTANCE.getSTATUS_NOTIFICATIONS_CHANNEL_ID())
                    .setContentTitle(getString(R.string.repeat_interval))
                    .setSmallIcon(R.drawable.ic_stat_notify_group)
                    .setLocalOnly(true)
                    .setColor(ColorSet.Companion.DEFAULT(this).getColor())
                    .setOngoing(false)
                    .build();
            startForeground(FOREGROUND_NOTIFICATION_ID, notification);
        }

        try {
            long snoozeTil = Settings.INSTANCE.getSnooze();
            if (snoozeTil > System.currentTimeMillis()) {
                return;
            }

            skipSummaryNotification = false;
            List<NotificationConversation> conversations = getUnseenConversations(this, getDataSource(this));

            if (conversations != null && conversations.size() > 0) {
                if (conversations.size() > 1) {
                    List<String> rows = new ArrayList<>();
                    for (NotificationConversation conversation : conversations) {
                        rows.add("<b>" + conversation.getTitle() + "</b>  " + conversation.getSnippet());
                    }

                    giveSummaryNotification(conversations, rows);
                }

                NotificationServiceHelper helper = NotificationServiceHelper.INSTANCE;
                int numberToNotify = helper.calculateNumberOfNotificationsToProvide(this, conversations);
                for (int i = 0; i < numberToNotify; i++) {
                    NotificationConversation conversation = conversations.get(i);
                    giveConversationNotification(conversation, i, conversations.size());
                }

                if (conversations.size() == 1) {
                    NotificationManagerCompat.from(this).cancel(SUMMARY_ID);
                }

                if (Settings.INSTANCE.getRepeatNotifications() != -1) {
                    RepeatNotificationJob.scheduleNextRun(this, System.currentTimeMillis() + Settings.INSTANCE.getRepeatNotifications());
                }

                if (Settings.INSTANCE.getWakeScreen()) {
                    try {
                        Thread.sleep(600);
                    } catch (Exception e) { }

                    PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
                    PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, "NEW_NOTIFICATION");
                    wl.acquire(5000);
                }
            }

            MessengerAppWidgetProvider.Companion.refreshWidget(this);

            if (foreground) {
                stopForeground(true);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @VisibleForTesting
    public static List<NotificationConversation> getUnseenConversations(Context context, MockableDataSourceWrapper source) {
        // timestamps are ASC, so it will start with the oldest message, and move to the newest.
        Cursor unseenMessages = source.getUnseenMessages(context);
        List<NotificationConversation> conversations = new ArrayList<>();
        List<Long> keys = new ArrayList<>();

        if (unseenMessages.moveToFirst()) {
            do {
                long conversationId = unseenMessages
                        .getLong(unseenMessages.getColumnIndex(Message.Companion.getCOLUMN_CONVERSATION_ID()));
                long id = unseenMessages
                        .getLong(unseenMessages.getColumnIndex(Message.Companion.getCOLUMN_ID()));
                String data = unseenMessages
                        .getString(unseenMessages.getColumnIndex(Message.Companion.getCOLUMN_DATA()));
                String mimeType = unseenMessages
                        .getString(unseenMessages.getColumnIndex(Message.Companion.getCOLUMN_MIME_TYPE()));
                long timestamp = unseenMessages
                        .getLong(unseenMessages.getColumnIndex(Message.Companion.getCOLUMN_TIMESTAMP()));
                String from = unseenMessages
                        .getString(unseenMessages.getColumnIndex(Message.Companion.getCOLUMN_FROM()));

                if (!MimeType.INSTANCE.isExpandedMedia(mimeType)) {
                    int conversationIndex = keys.indexOf(conversationId);
                    NotificationConversation conversation = null;

                    if (conversationIndex == -1) {
                        Conversation c = source.getConversation(context, conversationId);
                        if (c != null) {
                            conversation = new NotificationConversation();
                            conversation.setId(c.getId());
                            conversation.setUnseenMessageId(id);
                            conversation.setTitle(c.getTitle());
                            conversation.setSnippet(c.getSnippet());
                            conversation.setImageUri(c.getImageUri());
                            conversation.setColor(c.getColors().getColor());
                            conversation.setRingtoneUri(c.getRingtoneUri());
                            conversation.setLedColor(c.getLedColor());
                            conversation.setTimestamp(c.getTimestamp());
                            conversation.setMute(c.getMute());
                            conversation.setPhoneNumbers(c.getPhoneNumbers());
                            conversation.setGroupConversation(c.getPhoneNumbers().contains(","));

                            if (c.getPrivateNotifications()) {
                                conversation.setTitle(context.getString(R.string.new_message));
                                conversation.setImageUri(null);
                                conversation.setRingtoneUri(null);
                                conversation.setColor(Settings.INSTANCE.getMainColorSet().getColor());
                                conversation.setPrivateNotification(true);
                                conversation.setLedColor(Color.WHITE);
                            } else {
                                conversation.setPrivateNotification(false);
                            }

                            conversations.add(conversation);
                            keys.add(conversationId);
                        }
                    } else {
                        conversation = conversations.get(conversationIndex);
                    }

                    if (conversation != null) {
                        conversation.getMessages().add(new NotificationMessage(id, data, mimeType, timestamp, from));
                    }
                }
            } while (unseenMessages.moveToNext());
        }

        CursorUtil.closeSilent(unseenMessages);

        Collections.sort(conversations, (result1, result2) ->
                new Date(result2.getTimestamp()).compareTo(new Date(result1.getTimestamp())));

        return conversations;
    }

    /**
     * Displays a notification for a single conversation.
     */
    private void giveConversationNotification(NotificationConversation conversation, int conversationIndex, int numConversations) {
        Bitmap contactImage = ImageUtils.INSTANCE.clipToCircle(
                ImageUtils.INSTANCE.getBitmap(this, conversation.getImageUri()));

        try {
            float height = getResources().getDimension(android.R.dimen.notification_large_icon_height);
            float width = getResources().getDimension(android.R.dimen.notification_large_icon_width);
            contactImage = Bitmap.createScaledBitmap(contactImage, (int) width, (int) height, true);
        } catch (Exception e) { }

        VibratePattern vibratePattern = Settings.INSTANCE.getVibrate();
        boolean shouldVibrate = !shouldAlertOnce(conversation.getMessages()) && conversationIndex == 0;
        int defaults = 0;
        if (shouldVibrate && vibratePattern == VibratePattern.DEFAULT) {
            defaults = Notification.DEFAULT_VIBRATE;
        }

        if (conversation.getLedColor() == Color.WHITE) {
            defaults = defaults | Notification.DEFAULT_LIGHTS;
        }

        Settings settings = Settings.INSTANCE;
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this,
                    getNotificationChannel(this, conversation.getId()))
                .setSmallIcon(!conversation.getGroupConversation() ? R.drawable.ic_stat_notify : R.drawable.ic_stat_notify_group)
                .setContentTitle(conversation.getTitle())
                .setAutoCancel(AUTO_CANCEL)
                .setCategory(Notification.CATEGORY_MESSAGE)
                .setColor(settings.getUseGlobalThemeColor() ? settings.getMainColorSet().getColor() : conversation.getColor())
                .setDefaults(defaults)
                .setGroup(numConversations > 1 || ALWAYS_SET_GROUP_KEY ? GROUP_KEY_MESSAGES : null)
                .setLargeIcon(contactImage)
                .setPriority(settings.getHeadsUp() ? Notification.PRIORITY_MAX : Notification.PRIORITY_DEFAULT)
                .setShowWhen(true)
                .setTicker(getString(R.string.notification_ticker, conversation.getTitle()))
                .setVisibility(Notification.VISIBILITY_PRIVATE)
                .setWhen(conversation.getTimestamp());

        if (numConversations == 1 && Build.MANUFACTURER.toLowerCase().contains("moto")) {
            // this is necessary for moto's active display, for some reason
            builder.setGroupSummary(true);
        } else {
            builder.setGroupSummary(false);
        }

        if (conversation.getLedColor() != Color.WHITE) {
            builder.setLights(conversation.getLedColor(), 1000, 500);
        }

        Uri sound = getRingtone(this, conversation.getRingtoneUri());
        if (conversationIndex == 0) {
            if (sound != null) {
                builder.setSound(sound);
            }

            if (vibratePattern.getPattern() != null) {
                builder.setVibrate(vibratePattern.getPattern());
            } else if (vibratePattern == VibratePattern.OFF) {
                builder.setVibrate(new long[0]);
            }
        }

        try {
            if (!conversation.getGroupConversation()) {
                builder.addPerson("tel:" + conversation.getPhoneNumbers());
            }
        } catch (Exception e) { }

        NotificationCompat.BigPictureStyle pictureStyle = null;
        NotificationCompat.InboxStyle inboxStyle = null;
        NotificationCompat.Style messagingStyle = null;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && settings.getHistoryInNotifications()) {
            // build a messaging style notification for Android Nougat
            messagingStyle = new NotificationCompat.MessagingStyle(getString(R.string.you));

            if (conversation.getGroupConversation()) {
                ((NotificationCompat.MessagingStyle) messagingStyle)
                        .setConversationTitle(conversation.getTitle());
            }

            MockableDataSourceWrapper source = getDataSource(this);
            List<Message> messages = source.getMessages(this, conversation.getId(), 10);

            for (int i = messages.size() - 1; i >= 0; i--) {
                Message message = messages.get(i);

                String from = null;
                if (message.getType() == Message.Companion.getTYPE_RECEIVED()) {
                    // we split it so that we only get the first name,
                    // if there is more than one

                    if (message.getFrom() != null) {
                        // it is most likely a group message.
                        from = message.getFrom();
                    } else {
                        from = conversation.getTitle();
                    }
                }

                String messageText = "";
                if (MimeType.INSTANCE.isAudio(message.getMimeType())) {
                    messageText += "<i>" + getString(R.string.audio_message) + "</i>";
                } else if (MimeType.INSTANCE.isVideo(message.getMimeType())) {
                    messageText += "<i>" + getString(R.string.video_message) + "</i>";
                } else if (MimeType.INSTANCE.isVcard(message.getMimeType())) {
                    messageText += "<i>" + getString(R.string.contact_card) + "</i>";
                } else if (MimeType.INSTANCE.isStaticImage(message.getMimeType())) {
                    messageText += "<i>" + getString(R.string.picture_message) + "</i>";
                } else if (message.getMimeType().equals(MimeType.INSTANCE.getIMAGE_GIF())) {
                    messageText += "<i>" + getString(R.string.gif_message) + "</i>";
                } else if (MimeType.INSTANCE.isExpandedMedia(message.getMimeType())) {
                    messageText += "<i>" + getString(R.string.media) + "</i>";
                } else {
                    messageText += message.getData();
                }

                ((NotificationCompat.MessagingStyle) messagingStyle)
                        .addMessage(Html.fromHtml(messageText), message.getTimestamp(), from);
            }
        }

        StringBuilder text = new StringBuilder();
        if (conversation.getMessages().size() > 1 && conversation.getMessages().get(0).getFrom() != null) {
            inboxStyle = new NotificationCompat.InboxStyle();

            for (NotificationMessage message : conversation.getMessages()) {
                if (message.getMimeType().equals(MimeType.INSTANCE.getTEXT_PLAIN())) {
                    String line = "<b>" + message.getFrom() + ":</b>  " + message.getData();
                    text.append(line);
                    text.append("\n");
                    inboxStyle.addLine(Html.fromHtml(line));
                } else {
                    pictureStyle = new NotificationCompat.BigPictureStyle()
                            .bigPicture(ImageUtils.INSTANCE.getBitmap(this, message.getData()));
                }
            }
        } else {
            for (int i = 0; i < conversation.getMessages().size(); i++) {
                NotificationMessage message = conversation.getMessages().get(i);

                if (message.getMimeType().equals(MimeType.INSTANCE.getTEXT_PLAIN())) {
                    if (message.getFrom() != null) {
                        text.append("<b>");
                        text.append(message.getFrom());
                        text.append(":</b>  ");
                        text.append(conversation.getMessages().get(i).getData());
                        text.append("\n");
                    } else {
                        text.append(conversation.getMessages().get(i).getData());
                        text.append("<br/>");
                    }
                } else if (MimeType.INSTANCE.isStaticImage(message.getMimeType())) {
                    pictureStyle = new NotificationCompat.BigPictureStyle()
                            .bigPicture(ImageUtils.INSTANCE.getBitmap(this, message.getData()));
                }
            }
        }

        String content = text.toString().trim();
        if (content.endsWith("<br/>")) {
            content = content.substring(0, content.length() - 5);
        }

        if (!conversation.getPrivateNotification()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                builder.setContentText(Html.fromHtml(content, 0));
            } else {
                builder.setContentText(Html.fromHtml(content));
            }

            if (pictureStyle != null) {
                builder.setStyle(pictureStyle);
                builder.setContentText(getString(R.string.picture_message));
            } else if (messagingStyle != null) {
                builder.setStyle(messagingStyle);
            } else if (inboxStyle != null) {
                builder.setStyle(inboxStyle);
            } else {
                builder.setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(Html.fromHtml(content)));
            }
        }

        NotificationCompat.Builder publicVersion = new NotificationCompat.Builder(this,
                    getNotificationChannel(this, conversation.getId()))
                .setSmallIcon(!conversation.getGroupConversation() ? R.drawable.ic_stat_notify : R.drawable.ic_stat_notify_group)
                .setContentTitle(getResources().getQuantityString(R.plurals.new_conversations, 1, 1))
                .setContentText(getResources().getQuantityString(R.plurals.new_messages,
                        conversation.getMessages().size(), conversation.getMessages().size()))
                .setLargeIcon(null)
                .setColor(settings.getUseGlobalThemeColor() ? settings.getMainColorSet().getColor() : conversation.getColor())
                .setAutoCancel(AUTO_CANCEL)
                .setCategory(Notification.CATEGORY_MESSAGE)
                .setDefaults(defaults)
                .setPriority(settings.getHeadsUp() ? Notification.PRIORITY_MAX : Notification.PRIORITY_DEFAULT)
                .setGroup(numConversations > 1 || ALWAYS_SET_GROUP_KEY ? GROUP_KEY_MESSAGES : null)
                .setVisibility(Notification.VISIBILITY_PUBLIC);

        if (conversation.getLedColor() != Color.WHITE) {
            builder.setLights(conversation.getLedColor(), 1000, 500);
        }

        if (conversationIndex == 0) {
            if (sound != null) {
                builder.setSound(sound);
            }

            if (vibratePattern.getPattern() != null) {
                builder.setVibrate(vibratePattern.getPattern());
            } else if (vibratePattern == VibratePattern.OFF) {
                builder.setVibrate(new long[0]);
            }
        }

        if (numConversations == 1 && Build.MANUFACTURER.toLowerCase().contains("moto")) {
            // this is necessary for moto's active display, for some reason
            publicVersion.setGroupSummary(true);
        } else {
            publicVersion.setGroupSummary(false);
        }

        try {
            if (!conversation.getGroupConversation()) {
                publicVersion.addPerson("tel:" + conversation.getPhoneNumbers());
            } else {
                for (String number : conversation.getPhoneNumbers().split(", ")) {
                    publicVersion.addPerson("tel:" + number);
                }
            }
        } catch (Exception e) { }

        builder.setPublicVersion(publicVersion.build());


        // one thing to keep in mind here... my adding only a wearable extender to the notification,
        // will the action be shown on phones or only on wear devices? If it is shown on phones, is
        // it only shown on Nougat+ where these actions can be accepted?
        RemoteInput remoteInput = new RemoteInput.Builder(ReplyService.EXTRA_REPLY)
                .setLabel(getString(R.string.reply_to, conversation.getTitle()))
                .setChoices(getResources().getStringArray(R.array.reply_choices))
                .setAllowFreeFormInput(true)
                .build();


        // Android wear extender (add a second page with message history
        NotificationCompat.BigTextStyle secondPageStyle = new NotificationCompat.BigTextStyle();
        secondPageStyle.setBigContentTitle(conversation.getTitle())
                .bigText(getWearableSecondPageConversation(conversation));
        NotificationCompat.Builder wear =
                new NotificationCompat.Builder(this, getNotificationChannel(this, conversation.getId()))
                        .setStyle(secondPageStyle);

        NotificationCompat.WearableExtender wearableExtender =
                new NotificationCompat.WearableExtender().addPage(wear.build());

        NotificationCompat.Action.WearableExtender actionExtender =
                new NotificationCompat.Action.WearableExtender()
                        .setHintLaunchesActivity(true)
                        .setHintDisplayActionInline(true);

        PendingIntent pendingReply;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && !DEBUG_QUICK_REPLY) {
            // with Android N, we only need to show the the reply service intent through the wearable extender
            Intent reply = new Intent(this, ReplyService.class);
            reply.putExtra(ReplyService.EXTRA_CONVERSATION_ID, conversation.getId());
            pendingReply = PendingIntent.getService(this,
                    (int) conversation.getId(), reply, PendingIntent.FLAG_UPDATE_CURRENT);

            NotificationCompat.Action action = new NotificationCompat.Action.Builder(R.drawable.ic_reply_white,
                    getString(R.string.reply), pendingReply)
                    .addRemoteInput(remoteInput)
                    .setAllowGeneratedReplies(true)
                    .extend(actionExtender)
                    .build();

            if (!conversation.getPrivateNotification() && settings.getNotificationActions().contains(NotificationAction.REPLY)) {
                builder.addAction(action);
            }

            wearableExtender.addAction(action);
        } else {
            // on older versions, we have to show the reply activity button as an action and add the remote input to it
            // this will allow it to be used on android wear (we will have to handle this from the activity)
            // as well as have a reply quick action button.
            Intent reply = ActivityUtils.INSTANCE.buildForComponent(ActivityUtils.INSTANCE.getNOTIFICATION_REPLY());
            reply.putExtra(ReplyService.EXTRA_CONVERSATION_ID, conversation.getId());
            reply.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            pendingReply = PendingIntent.getActivity(this,
                    (int) conversation.getId(), reply, PendingIntent.FLAG_UPDATE_CURRENT);

            if (DEBUG_QUICK_REPLY) {
                // if we are debugging, the assumption is that we are on android N, we have to be stop showing
                // the remote input or else it will keep using the direct reply
                NotificationCompat.Action action = new NotificationCompat.Action.Builder(R.drawable.ic_reply_dark,
                        getString(R.string.reply), pendingReply)
                        .extend(actionExtender)
                        .setAllowGeneratedReplies(true)
                        .build();

                if (!conversation.getPrivateNotification() && settings.getNotificationActions().contains(NotificationAction.REPLY)) {
                    builder.addAction(action);
                }

                action.icon = R.drawable.ic_reply_white;
                wearableExtender.addAction(action);
            } else {
                NotificationCompat.Action action = new NotificationCompat.Action.Builder(R.drawable.ic_reply_dark,
                        getString(R.string.reply), pendingReply)
                        .build();

                if (!conversation.getPrivateNotification() && settings.getNotificationActions().contains(NotificationAction.REPLY)) {
                    builder.addAction(action);
                }

                Intent wearReply = new Intent(this, ReplyService.class);
                Bundle extras = new Bundle();
                extras.putLong(ReplyService.EXTRA_CONVERSATION_ID, conversation.getId());
                wearReply.putExtras(extras);
                PendingIntent wearPendingReply = PendingIntent.getService(this,
                        (int) conversation.getId() + 1, wearReply, PendingIntent.FLAG_UPDATE_CURRENT);

                NotificationCompat.Action wearAction = new NotificationCompat.Action.Builder(R.drawable.ic_reply_white,
                        getString(R.string.reply), wearPendingReply)
                        .addRemoteInput(remoteInput)
                        .extend(actionExtender)
                        .build();

                wearableExtender.addAction(wearAction);
            }
        }

        if (!conversation.getGroupConversation() && settings.getNotificationActions().contains(NotificationAction.CALL)
                && (!Account.INSTANCE.exists() || Account.INSTANCE.getPrimary())) {
            Intent call = new Intent(this, NotificationCallService.class);
            call.putExtra(NotificationMarkReadService.EXTRA_CONVERSATION_ID, conversation.getId());
            call.putExtra(NotificationCallService.EXTRA_PHONE_NUMBER, conversation.getPhoneNumbers());
            PendingIntent callPending = PendingIntent.getService(this, (int) conversation.getId(),
                    call, PendingIntent.FLAG_UPDATE_CURRENT);

            builder.addAction(new NotificationCompat.Action(R.drawable.ic_call_dark, getString(R.string.call), callPending));
        }

        Intent deleteMessage = new Intent(this, NotificationDeleteService.class);
        deleteMessage.putExtra(NotificationDeleteService.EXTRA_CONVERSATION_ID, conversation.getId());
        deleteMessage.putExtra(NotificationDeleteService.EXTRA_MESSAGE_ID, conversation.getUnseenMessageId());
        PendingIntent pendingDeleteMessage = PendingIntent.getService(this, (int) conversation.getId(),
                deleteMessage, PendingIntent.FLAG_UPDATE_CURRENT);

        if (settings.getNotificationActions().contains(NotificationAction.DELETE)) {
            builder.addAction(new NotificationCompat.Action(R.drawable.ic_delete_dark, getString(R.string.delete), pendingDeleteMessage));
        }

        Intent read = new Intent(this, NotificationMarkReadService.class);
        read.putExtra(NotificationMarkReadService.EXTRA_CONVERSATION_ID, conversation.getId());
        PendingIntent pendingRead = PendingIntent.getService(this, (int) conversation.getId(),
                read, PendingIntent.FLAG_UPDATE_CURRENT);

        if (settings.getNotificationActions().contains(NotificationAction.READ)) {
            builder.addAction(new NotificationCompat.Action(R.drawable.ic_done_dark, getString(R.string.read), pendingRead));
        }

        wearableExtender.addAction(new NotificationCompat.Action(R.drawable.ic_done_white, getString(R.string.read), pendingRead));
        wearableExtender.addAction(new NotificationCompat.Action(R.drawable.ic_delete_white, getString(R.string.delete), pendingDeleteMessage));

        Intent delete = new Intent(this, NotificationDismissedReceiver.class);
        delete.putExtra(NotificationDismissedService.EXTRA_CONVERSATION_ID, conversation.getId());
        PendingIntent pendingDelete = PendingIntent.getBroadcast(this, (int) conversation.getId(),
                delete, PendingIntent.FLAG_UPDATE_CURRENT);

        Intent open = ActivityUtils.INSTANCE.buildForComponent(ActivityUtils.INSTANCE.getMESSENGER_ACTIVITY());
        open.putExtra(MessengerActivityExtras.INSTANCE.getEXTRA_CONVERSATION_ID(), conversation.getId());
        open.putExtra(MessengerActivityExtras.INSTANCE.getEXTRA_FROM_NOTIFICATION(), true);
        open.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingOpen = PendingIntent.getActivity(this,
                (int) conversation.getId(), open, PendingIntent.FLAG_UPDATE_CURRENT);

        builder.setDeleteIntent(pendingDelete);
        builder.setContentIntent(pendingOpen);

        Intent carReply = new Intent().addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
                .setAction("xyz.klinker.messenger.CAR_REPLY")
                .putExtra(ReplyService.EXTRA_CONVERSATION_ID, conversation.getId())
                .setPackage("xyz.klinker.messenger");
        PendingIntent pendingCarReply = PendingIntent.getBroadcast(this, (int) conversation.getId(),
                carReply, PendingIntent.FLAG_UPDATE_CURRENT);

        Intent carRead = new Intent().addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
                .setAction("xyz.klinker.messenger.CAR_READ")
                .putExtra(NotificationMarkReadService.EXTRA_CONVERSATION_ID, conversation.getId())
                .setPackage("xyz.klinker.messenger");
        PendingIntent pendingCarRead = PendingIntent.getBroadcast(this, (int) conversation.getId(),
                carRead, PendingIntent.FLAG_UPDATE_CURRENT);

        // Android Auto extender
        NotificationCompat.CarExtender.UnreadConversation.Builder car = new
                NotificationCompat.CarExtender.UnreadConversation.Builder(conversation.getTitle())
                .setReadPendingIntent(pendingCarRead)
                .setReplyAction(pendingCarReply, remoteInput)
                .setLatestTimestamp(conversation.getTimestamp());

        for (NotificationMessage message : conversation.getMessages()) {
            if (message.getMimeType().equals(MimeType.INSTANCE.getTEXT_PLAIN())) {
                car.addMessage(message.getData());
            } else {
                car.addMessage(getString(R.string.new_mms_message));
            }
        }

        // apply the extenders to the notification
        builder.extend(new NotificationCompat.CarExtender().setUnreadConversation(car.build()));
        builder.extend(wearableExtender);

        if (CONVERSATION_ID_OPEN == conversation.getId()) {
            // skip this notification since we are already on the conversation.
            skipSummaryNotification = true;
        } else {
            NotificationManagerCompat.from(this).notify((int) conversation.getId(), builder.build());
        }

        if (!TvUtils.INSTANCE.hasTouchscreen(this)) {
//            if (notificationWindowManager == null) {
//                notificationWindowManager = new NotificationWindowManager(this);
//            }
//
//            NotificationView.newInstance(notificationWindowManager)
//                    .setImage(null)
//                    .setTitle(conversation.title)
//                    .setDescription(content)
//                    .show();
        }
    }

    /**
     * If the user is getting spammed by the same person over and over again, we don't want to immediately
     * give a vibrate or ringtone again.
     *
     * The 'onlyAlertOnce" flag on the notification builder means that it will not give a vibrate or sound
     * if the notification is already active.
     *
     * @param messages the messages in this conversation
     * @return true if the latest two messages are less than 1 min apart, or there is only one message. False
     *          if the latest messages are more than 1 min apart, so that it will ring again.
     */
    @VisibleForTesting
    protected boolean shouldAlertOnce(List<NotificationMessage> messages) {
        if (messages.size() > 1) {
            NotificationMessage one = messages.get(messages.size() - 2);
            NotificationMessage two = messages.get(messages.size() - 1);

            if (Math.abs(one.getTimestamp() - two.getTimestamp()) > TimeUtils.INSTANCE.getSECOND() * 30) {
                return false;
            }
        }

        // default to true
        return true;
    }

    /**
     * Displays a summary notification for all conversations using the rows returned by each
     * individual notification.
     */
    private void giveSummaryNotification(List<NotificationConversation> conversations,
                                         List<String> rows) {
        StringBuilder summaryBuilder = new StringBuilder();
        for (int i = 0; i < conversations.size(); i++) {
            if (conversations.get(i).getPrivateNotification()) {
                summaryBuilder.append(getString(R.string.new_message));
            } else {
                summaryBuilder.append(conversations.get(i).getTitle());
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

        Notification publicVersion = new NotificationCompat.Builder(this,
                NotificationUtils.INSTANCE.getMESSAGE_GROUP_SUMMARY_CHANNEL_ID())
                .setSmallIcon(R.drawable.ic_stat_notify_group)
                .setContentTitle(title)
                .setGroup(GROUP_KEY_MESSAGES)
                .setGroupSummary(true)
                .setAutoCancel(AUTO_CANCEL)
                .setCategory(Notification.CATEGORY_MESSAGE)
                .setColor(Settings.INSTANCE.getMainColorSet().getColor())
                .setPriority(Settings.INSTANCE.getHeadsUp() ? Notification.PRIORITY_MAX : Notification.PRIORITY_DEFAULT)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .build();

        Intent delete = new Intent(this, NotificationDismissedService.class);
        PendingIntent pendingDelete = PendingIntent.getService(this, 0,
                delete, PendingIntent.FLAG_UPDATE_CURRENT);

        Intent open = ActivityUtils.INSTANCE.buildForComponent(ActivityUtils.INSTANCE.getMESSENGER_ACTIVITY());
        open.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingOpen = PendingIntent.getActivity(this, 0,
                open, PendingIntent.FLAG_UPDATE_CURRENT);

        Notification notification = new NotificationCompat.Builder(this,
                NotificationUtils.INSTANCE.getMESSAGE_GROUP_SUMMARY_CHANNEL_ID())
                .setSmallIcon(R.drawable.ic_stat_notify_group)
                .setContentTitle(title)
                .setContentText(summary)
                .setGroup(GROUP_KEY_MESSAGES)
                .setGroupSummary(true)
                .setAutoCancel(AUTO_CANCEL)
                .setCategory(Notification.CATEGORY_MESSAGE)
                .setColor(Settings.INSTANCE.getMainColorSet().getColor())
                .setPriority(Settings.INSTANCE.getHeadsUp() ? Notification.PRIORITY_MAX : Notification.PRIORITY_DEFAULT)
                .setShowWhen(true)
                .setTicker(title)
                .setVisibility(Notification.VISIBILITY_PRIVATE)
                .setWhen(conversations.get(conversations.size() - 1).getTimestamp())
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
        MockableDataSourceWrapper source = getDataSource(this);
        List<Message> messages = source.getMessages(this, conversation.getId(), 10);

        String you = getString(R.string.you);
        StringBuilder builder = new StringBuilder();

        for (Message message : messages) {
            String messageText = "";
            if (MimeType.INSTANCE.isAudio(message.getMimeType())) {
                messageText += "<i>" + getString(R.string.audio_message) + "</i>";
            } else if (MimeType.INSTANCE.isVideo(message.getMimeType())) {
                messageText += "<i>" + getString(R.string.video_message) + "</i>";
            } else if (MimeType.INSTANCE.isVcard(message.getMimeType())) {
                messageText += "<i>" + getString(R.string.contact_card) + "</i>";
            } else if (MimeType.INSTANCE.isStaticImage(message.getMimeType())) {
                messageText += "<i>" + getString(R.string.picture_message) + "</i>";
            } else if (message.getMimeType().equals(MimeType.INSTANCE.getIMAGE_GIF())) {
                messageText += "<i>" + getString(R.string.gif_message) + "</i>";
            } else if (MimeType.INSTANCE.isExpandedMedia(message.getMimeType())) {
                messageText += "<i>" + getString(R.string.media) + "</i>";
            } else {
                messageText += message.getData();
            }

            if (message.getType() == Message.Companion.getTYPE_RECEIVED()) {
                if (message.getFrom() != null) {
                    builder.append("<b>" + message.getFrom() + "</b>  " + messageText + "<br>");
                } else {
                    builder.append("<b>" + conversation.getTitle() + "</b>  " + messageText + "<br>");
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

    public static Uri getRingtone(Context context, String conversationRingtone) {
        try {
            String globalUri = Settings.INSTANCE.getRingtone();

            if (conversationRingtone == null || conversationRingtone.contains("default") ||
                    conversationRingtone.equals("content://settings/system/notification_sound")) {
                // there is no conversation specific ringtone defined

                if (globalUri == null || globalUri.isEmpty()) {
                    return null;
                } if (ringtoneExists(context, globalUri)) {
                    // the global ringtone is available to use
                    return Uri.parse(globalUri);
                } else {
                    // there is no global ringtone defined, or it doesn't exist on the system
                    return RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                }
            } else {
                if (conversationRingtone.isEmpty()) {
                    return null;
                } else if (ringtoneExists(context, conversationRingtone)) {
                    // conversation ringtone exists and can be played
                    return Uri.parse(conversationRingtone);
                } else {
                    // the global ringtone is available to use
                    return RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                }
            }
        } catch (Exception e) {
            return RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        }
    }

    private static boolean ringtoneExists(Context context, String uri) {
        if (uri.contains("file://")) {
            return false;
        }
        
        return RingtoneManager.getRingtone(context, Uri.parse(uri)) != null;
    }

    @VisibleForTesting
    MockableDataSourceWrapper getDataSource(Context context) {
        return new MockableDataSourceWrapper(DataSource.INSTANCE);
    }

    public static void cancelRepeats(Context context) {
        RepeatNotificationJob.scheduleNextRun(context, 0);
    }

    @TargetApi(Build.VERSION_CODES.O)
    private static String getNotificationChannel(Context context, long conversationId) {
        if (!AndroidVersionUtil.INSTANCE.isAndroidO()) {
            return NotificationUtils.INSTANCE.getDEFAULT_CONVERSATION_CHANNEL_ID();
        }

        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager.getNotificationChannel(conversationId + "") != null) {
            return conversationId + "";
        } else {
            return NotificationUtils.INSTANCE.getDEFAULT_CONVERSATION_CHANNEL_ID();
        }
    }
}
