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

package xyz.klinker.messenger.util;

import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import android.service.notification.StatusBarNotification;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Helper for working with notifications.
 */
public class NotificationUtils {

    /**
     * Works around a bug that seems to keep a grouped notification on screen after all of it's
     * children have been cleared away.
     */
    public static void cancelGroupedNotificationWithNoContent(Context context) {
        if (Build.VERSION.SDK_INT >= 23) {
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

                it.remove(); // avoids a ConcurrentModificationException
            }
        }
    }

}
