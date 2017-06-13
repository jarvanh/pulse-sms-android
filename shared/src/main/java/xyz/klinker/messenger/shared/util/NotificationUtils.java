package xyz.klinker.messenger.shared.util;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationChannelGroup;
import android.app.NotificationManager;
import android.content.Context;
import android.media.AudioAttributes;
import android.net.Uri;
import android.os.Build;
import android.service.notification.StatusBarNotification;
import android.support.v4.app.NotificationManagerCompat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import xyz.klinker.messenger.shared.R;
import xyz.klinker.messenger.shared.data.DataSource;
import xyz.klinker.messenger.shared.data.Settings;
import xyz.klinker.messenger.shared.data.model.Conversation;
import xyz.klinker.messenger.shared.service.NotificationService;

public class NotificationUtils {

    public static final String MESSAGE_GROUP_SUMMARY_CHANNEL_ID = "message-group-summary";
    public static final String FAILED_MESSAGES_CHANNEL_ID = "failed-messages";
    public static final String TEST_NOTIFICATIONS_CHANNEL_ID = "test-notifications";
    public static final String STATUS_NOTIFICATIONS_CHANNEL_ID = "status-notifications";
    public static final String MEDIA_PARSE_CHANNEL_ID = "media-parsing";
    public static final String GENERAL_CHANNEL_ID = "general";

    public static void cancelGroupedNotificationWithNoContent(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Map<String, Integer> map = new HashMap();

            NotificationManager manager = (NotificationManager) context.getSystemService(
                    Context.NOTIFICATION_SERVICE);

            StatusBarNotification[] notifications = manager.getActiveNotifications();

            for (StatusBarNotification notification : notifications) {
                String keyString = notification.getGroupKey();
                if (keyString.contains("|g:")) { // this is a grouped notification
                    keyString = keyString.substring(keyString.indexOf("|g:") + 3, keyString.length());

                    if (map.containsKey(keyString)) {
                        map.put(keyString, map.get(keyString) + 1);
                    } else {
                        map.put(keyString, 1);
                    }
                }
            }

            Iterator it = map.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry pair = (Map.Entry)it.next();
                String key = (String) pair.getKey();
                int value = (Integer) pair.getValue();

                if (value == 1) {
                    for (StatusBarNotification notification : notifications) {
                        String keyString = notification.getGroupKey();
                        if (keyString.contains("|g:")) { // this is a grouped notification
                            keyString = keyString.substring(keyString.indexOf("|g:") + 3, keyString.length());

                            if (key.equals(keyString)) {
                                manager.cancel(notification.getId());
                                break;
                            }
                        }
                    }
                }

                it.remove();
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.O)
    public static void createNotificationChannelIfNonExistent(Context context, Conversation conversation) {
        if (!AndroidVersionUtil.isAndroidO()) {
            return;
        }

        final NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        final NotificationChannel channel = createChannel(context, conversation);
        manager.createNotificationChannel(channel);
    }

    @TargetApi(Build.VERSION_CODES.O)
    public static void createTestChannel(Context context) {
        if (!AndroidVersionUtil.isAndroidO()) {
            return;
        }

        final NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationChannel testChannel = new NotificationChannel(TEST_NOTIFICATIONS_CHANNEL_ID,
                context.getString(R.string.test_notifications_channel),
                Settings.get(context).headsUp ? NotificationManager.IMPORTANCE_MAX : NotificationManager.IMPORTANCE_DEFAULT);
        manager.createNotificationChannel(testChannel);
    }

    @TargetApi(Build.VERSION_CODES.O)
    public static void createStatusChannel(Context context) {
        if (!AndroidVersionUtil.isAndroidO()) {
            return;
        }

        final NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationChannel statusChannel = new NotificationChannel(STATUS_NOTIFICATIONS_CHANNEL_ID,
                context.getString(R.string.status_notifications_channel), NotificationManager.IMPORTANCE_DEFAULT);
        manager.createNotificationChannel(statusChannel);
    }

    @TargetApi(Build.VERSION_CODES.O)
    public static void createMediaParseChannel(Context context) {
        if (!AndroidVersionUtil.isAndroidO()) {
            return;
        }

        final NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationChannel statusChannel = new NotificationChannel(MEDIA_PARSE_CHANNEL_ID,
                context.getString(R.string.status_notifications_channel), NotificationManager.IMPORTANCE_MIN);
        manager.createNotificationChannel(statusChannel);
    }

    @TargetApi(Build.VERSION_CODES.O)
    public static void createGeneralChannel(Context context) {
        if (!AndroidVersionUtil.isAndroidO()) {
            return;
        }

        final NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationChannel statusChannel = new NotificationChannel(GENERAL_CHANNEL_ID,
                context.getString(R.string.general_notifications_channel), NotificationManager.IMPORTANCE_MIN);
        manager.createNotificationChannel(statusChannel);
    }

    @TargetApi(Build.VERSION_CODES.O)
    public static void createFailedMessageChannel(Context context) {
        if (!AndroidVersionUtil.isAndroidO()) {
            return;
        }

        final NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationChannel testChannel = new NotificationChannel(FAILED_MESSAGES_CHANNEL_ID,
                context.getString(R.string.failed_messages_channel),
                Settings.get(context).headsUp ? NotificationManager.IMPORTANCE_MAX : NotificationManager.IMPORTANCE_DEFAULT);
        manager.createNotificationChannel(testChannel);
    }

    @TargetApi(Build.VERSION_CODES.O)
    public static void createMessageGroupChannel(Context context) {
        if (!AndroidVersionUtil.isAndroidO()) {
            return;
        }

        final NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        final NotificationChannel messageGroupChannel = new NotificationChannel(MESSAGE_GROUP_SUMMARY_CHANNEL_ID,
                context.getString(R.string.group_summary_notifications),
                Settings.get(context).headsUp ? NotificationManager.IMPORTANCE_MAX : NotificationManager.IMPORTANCE_DEFAULT);
        manager.createNotificationChannel(messageGroupChannel);
    }

    @TargetApi(Build.VERSION_CODES.O)
    public static void createNotificationChannels(Context context, DataSource source) {
        if (!AndroidVersionUtil.isAndroidO()) {
            return;
        }

        final NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        // notification channel group for conversations
        NotificationChannelGroup conversationsGroup = new NotificationChannelGroup("conversations",
                context.getString(R.string.conversations));
        manager.createNotificationChannelGroup(conversationsGroup);

        // channels to place the notifications in
        createTestChannel(context);
        createStatusChannel(context);
        createFailedMessageChannel(context);
        createMessageGroupChannel(context);
        createMediaParseChannel(context);
        createGeneralChannel(context);

        List<Conversation> conversations = source.getAllConversationsAsList();
        for (int i = 0; i < conversations.size(); i++) {
            final Conversation conversation = conversations.get(i);
            final NotificationChannel channel = createChannel(context, conversation);

            try {
                manager.createNotificationChannel(channel);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.O)
    private static NotificationChannel createChannel(Context context, Conversation conversation) {
        Settings settings = Settings.get(context);

        NotificationChannel channel = new NotificationChannel(conversation.id + "", conversation.title,
                settings.headsUp ? NotificationManager.IMPORTANCE_MAX : NotificationManager.IMPORTANCE_DEFAULT);
        channel.setGroup("conversations");
        channel.enableLights(true);
        channel.setLightColor(conversation.ledColor);
        channel.setBypassDnd(false);
        channel.setShowBadge(true);
        channel.setVibrationPattern(Settings.get(context).vibrate.pattern);
        channel.setLockscreenVisibility(conversation.privateNotifications ?
                Notification.VISIBILITY_PRIVATE : Notification.VISIBILITY_PUBLIC);

        Uri ringtone = NotificationService.getRingtone(context, conversation.ringtoneUri);
        if (ringtone != null) {
            channel.setSound(ringtone, new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION).build());
        }

        return channel;
    }

    @TargetApi(Build.VERSION_CODES.O)
    public static void deleteChannel(Context context, long conversationId) {
        if (!AndroidVersionUtil.isAndroidO()) {
            return;
        }

        final NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        manager.deleteNotificationChannel(conversationId + "");
    }

}
