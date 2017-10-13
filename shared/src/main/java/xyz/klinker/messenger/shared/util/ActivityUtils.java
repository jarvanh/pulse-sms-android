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

package xyz.klinker.messenger.shared.util;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.view.View;

import xyz.klinker.messenger.shared.R;
import xyz.klinker.messenger.shared.data.ColorSet;
import xyz.klinker.messenger.shared.data.Settings;

/**
 * Utils for helping with different activity tasks such as setting the task description.
 */
public class ActivityUtils {

    public static final ComponentName MESSENGER_ACTIVITY = new ComponentName("xyz.klinker.messenger",
            "xyz.klinker.messenger" + ".activity.MessengerActivity");
    public static final ComponentName COMPOSE_ACTIVITY = new ComponentName("xyz.klinker.messenger",
            "xyz.klinker.messenger" + ".activity.ComposeActivity");
    public static final ComponentName NOTIFICATION_REPLY = new ComponentName("xyz.klinker.messenger",
            "xyz.klinker.messenger" + ".activity.NotificationReplyActivity");

    public static Intent buildForComponent(ComponentName component) {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.setComponent(component);
        return intent;
    }

    public static void setTaskDescription(Activity activity) {
        Bitmap bm = BitmapFactory.decodeResource(activity.getResources(), R.mipmap.ic_launcher);
        ActivityManager.TaskDescription td = new ActivityManager.TaskDescription(
                activity.getString(R.string.app_name), bm, Settings.get(activity).mainColorSet.getColor());

        activity.setTaskDescription(td);
    }

    public static void setTaskDescription(Activity activity, String title, int color) {
        ActivityManager.TaskDescription td = new ActivityManager.TaskDescription(
                title, null, color);

        activity.setTaskDescription(td);
    }

    public static void setStatusBarColor(Activity activity, int color) {
        activity.getWindow().setStatusBarColor(color);
        setUpLightStatusBar(activity, color);
    }

    public static void setUpLightStatusBar(Activity activity, int color) {
        if (!ColorUtils.isColorDark(color)) {
            activateLightStatusBar(activity, true);
        } else {
            activateLightStatusBar(activity, false);
        }
    }

    private static void activateLightStatusBar(Activity activity, boolean activate) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return;
        }

        int oldSystemUiFlags = activity.getWindow().getDecorView().getSystemUiVisibility();
        int newSystemUiFlags = oldSystemUiFlags;
        if (activate) {
            newSystemUiFlags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
        } else {
            newSystemUiFlags &= ~(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        }

        if (newSystemUiFlags != oldSystemUiFlags) {
            activity.getWindow().getDecorView().setSystemUiVisibility(newSystemUiFlags);
        }
    }
}
