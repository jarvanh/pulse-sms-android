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
import android.support.v7.app.AppCompatDelegate;

import java.lang.reflect.Field;

import xyz.klinker.messenger.api.implementation.ApiUtils;
import xyz.klinker.messenger.data.Settings;
import xyz.klinker.messenger.service.ContentObserverService;

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

        Settings.get(this).setValue("device_id", null);
        Settings.get(this).setValue("account_id", null);

        ApiUtils.environment = getString(R.string.environment);
        enableSecurity();

        if (Settings.get(this).darkTheme) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        }

        startService(new Intent(this, ContentObserverService.class));
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
