package xyz.klinker.messenger.shared.util;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationChannelGroup;
import android.app.NotificationManager;
import android.content.Context;
import android.media.AudioAttributes;
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

    public static void createNotificationChannelIfNonExistant(Context context, Conversation conversation) {
        if (!AndroidVersionUtil.isAndroidO()) {
            return;
        }

        final String notificationChannelId = conversation.id + "";
        final NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        NotificationChannel existingChannel = manager.getNotificationChannel(notificationChannelId);
        if (existingChannel == null) {
            NotificationChannel channel = createChannel(context, conversation);
            manager.createNotificationChannel(channel);
        }
    }

    public static void createNotificationChannels(Context context, DataSource source) {
        if (!AndroidVersionUtil.isAndroidO()) {
            return;
        }

        final NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationChannelGroup conversationsGroup = new NotificationChannelGroup("conversations",
                context.getString(R.string.conversations));
        manager.createNotificationChannelGroup(conversationsGroup);

        List<Conversation> conversations = source.getAllConversationsAsList();
        List<NotificationChannel> channels = new ArrayList<>();

        for (int i = 0; i < conversations.size(); i++) {
            final Conversation conversation = conversations.get(i);
            final String notificationChannelId = conversation.id + "";

            NotificationChannel existingChannel = manager.getNotificationChannel(notificationChannelId);
            if (existingChannel == null) {
                NotificationChannel channel = createChannel(context, conversation);
                channels.add(channel);
            }
        }

        manager.createNotificationChannels(channels);
    }

    private static NotificationChannel createChannel(Context context, Conversation conversation) {
        NotificationChannel channel = new NotificationChannel(conversation.id + "", conversation.title, NotificationManager.IMPORTANCE_MAX);
        channel.setGroup("conversations");
        channel.enableLights(true);
        channel.setLightColor(conversation.ledColor);
        channel.setShowBadge(true);
        channel.setVibrationPattern(Settings.get(context).vibrate.pattern);
        channel.setSound(NotificationService.getRingtone(context, conversation.ringtoneUri),
                new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION).build());


        return channel;
    }

}
