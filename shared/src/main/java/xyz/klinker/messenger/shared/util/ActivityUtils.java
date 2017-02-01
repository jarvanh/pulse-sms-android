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

package xyz.klinker.messenger.shared.util;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import xyz.klinker.messenger.shared.R;

/**
 * Utils for helping with different activity tasks such as setting the task description.
 */
public class ActivityUtils {

    public static final ComponentName MESSENGER_ACTIVITY = new ComponentName("xyz.klinker.messenger",
            "xyz.klinker.messenger" + ".activity.MessengerActivity");
    public static final ComponentName COMPOSE_ACTIVITY = new ComponentName("xyz.klinker.messenger",
            "xyz.klinker.messenger" + ".activity.ComposeActivity");
    public static final ComponentName NOTIFICATION_REPLY = new ComponentName("xyz.klinker.messenger",
            "xyz.klinker.messenger" + ".activity.MessengerActivity");

    public static Intent buildForComponent(ComponentName component) {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.setComponent(component);
        return intent;
    }

    public static void setTaskDescription(Activity activity) {
        Bitmap bm = BitmapFactory.decodeResource(activity.getResources(), R.mipmap.ic_launcher);
        ActivityManager.TaskDescription td = new ActivityManager.TaskDescription(
                activity.getString(R.string.app_name), bm,
                activity.getResources().getColor(R.color.colorPrimary));

        activity.setTaskDescription(td);
    }

    public static void setTaskDescription(Activity activity, String title, int color) {
        ActivityManager.TaskDescription td = new ActivityManager.TaskDescription(
                title, null, color);

        activity.setTaskDescription(td);
    }

}
