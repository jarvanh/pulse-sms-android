package xyz.klinker.messenger.shared.util;

import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import android.service.notification.StatusBarNotification;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

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

}
