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

package xyz.klinker.messenger;

import android.app.Application;
import android.content.Intent;
import android.os.Build;
import android.support.v4.os.BuildCompat;
import android.support.v7.app.AppCompatDelegate;

import java.lang.reflect.Field;
import java.util.List;

import xyz.klinker.messenger.api.implementation.ApiUtils;
import xyz.klinker.messenger.data.DataSource;
import xyz.klinker.messenger.data.Settings;
import xyz.klinker.messenger.data.model.Conversation;
import xyz.klinker.messenger.util.DynamicShortcutUtils;
import xyz.klinker.messenger.util.TimeUtils;

/**
 * Base application that will serve as any intro for any context in the rest of the app. Main
 * function is to enable night mode so that colors change depending on time of day.
 */
public class MessengerApplication extends Application {

    /**
     * Enable night mode and set it to auto. It will switch depending on time of day.
     */
    static {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_AUTO);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        ApiUtils.environment = getString(R.string.environment);
        enableSecurity();

        Settings.BaseTheme theme = Settings.get(this).baseTheme;
        if (theme == Settings.BaseTheme.ALWAYS_LIGHT) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        } else if (theme.isDark || TimeUtils.isNight()) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        }
    }

    public void refreshDynamicShortcuts() {
        if (!"robolectric".equals(Build.FINGERPRINT) && BuildCompat.isAtLeastNMR1()) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(10 * 1000);
                        DataSource source = DataSource.getInstance(MessengerApplication.this);
                        source.open();
                        List<Conversation> conversations = source.getPinnedConversationsAsList();
                        if (conversations.size() == 0) {
                            conversations = source.getUnarchivedConversationsAsList();
                        }
                        source.close();

                        new DynamicShortcutUtils(MessengerApplication.this).buildDynamicShortcuts(conversations);
                    } catch (Exception e) { }
                }
            }).start();
        }
    }

    /**
     * By default, java does not allow for strong security schemes due to export laws in other
     * countries. This gets around that. Might not be necessary on Android, but we'll put it here
     * anyways just in case.
     */
    private static void enableSecurity() {
        try {
            Field field = Class.forName("javax.crypto.JceSecurity").
                    getDeclaredField("isRestricted");
            field.setAccessible(true);
            field.set(null, Boolean.FALSE);
        } catch (Exception e) {

        }
    }
}
