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

package xyz.klinker.messenger.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.VisibleForTesting;

/**
 * Holds all settings for the application and allows for easily changing the values stored for each
 * setting.
 */
public class Settings {

    private static volatile Settings settings;

    public static final String FIRST_START = "first_start";
    public static final String MY_NAME = "my_name";
    public static final String MY_PHONE_NUMBER = "my_phone_number";

    private Context context;

    public boolean firstStart;
    public String myName;
    public String myPhoneNumber;

    /**
     * Gets a new instance (singleton) of Settings.
     *
     * @param context the current application context.
     * @return the settings instance.
     */
    public static synchronized Settings get(Context context) {
        if (settings == null) {
            settings = new Settings(context);
        }

        return settings;
    }

    protected Settings() {
        throw new RuntimeException("Don't initialize this!");
    }

    private Settings(final Context context) {
        init(context);
    }

    @VisibleForTesting
    protected void init(Context context) {
        this.context = context;
        SharedPreferences sharedPrefs = getSharedPrefs();

        this.firstStart = sharedPrefs.getBoolean(FIRST_START, true);
        this.myName = sharedPrefs.getString(MY_NAME, null);
        this.myPhoneNumber = sharedPrefs.getString(MY_PHONE_NUMBER, null);
    }

    @VisibleForTesting
    protected SharedPreferences getSharedPrefs() {
        return PreferenceManager.getDefaultSharedPreferences(context);
    }

    /**
     * Gets the current settings context.
     *
     * @return the context.
     */
    public Context getContext() {
        return context;
    }

    /**
     * Forces a reload of all settings data.
     */
    public void forceUpdate() {
        init(context);
    }

    @VisibleForTesting
    protected void setValue(String key, boolean value, boolean forceUpdate) {
        getSharedPrefs().edit()
                .putBoolean(key, value)
                .apply();

        if (forceUpdate) {
            forceUpdate();
        }
    }

    @VisibleForTesting
    protected void setValue(String key, int value, boolean forceUpdate) {
        getSharedPrefs().edit()
                .putInt(key, value)
                .apply();

        if (forceUpdate) {
            forceUpdate();
        }
    }

    @VisibleForTesting
    protected void setValue(String key, String value, boolean forceUpdate) {
        getSharedPrefs().edit()
                .putString(key, value)
                .apply();

        if (forceUpdate) {
            forceUpdate();
        }
    }

    @VisibleForTesting
    protected void setValue(String key, long value, boolean forceUpdate) {
        getSharedPrefs().edit()
                .putLong(key, value)
                .apply();

        if (forceUpdate) {
            forceUpdate();
        }
    }

    /**
     * Stores a new boolean value.
     *
     * @param key   the shared preferences key.
     * @param value the new value.
     */
    public void setValue(String key, boolean value) {
        setValue(key, value, true);
    }

    /**
     * Stores a new integer value.
     *
     * @param key   the shared preferences key.
     * @param value the new value.
     */
    public void setValue(String key, int value) {
        setValue(key, value, true);
    }

    /**
     * Stores a new String value.
     *
     * @param key   the shared preferences key.
     * @param value the new value.
     */
    public void setValue(String key, String value) {
        setValue(key, value, true);
    }

    /**
     * Stores a new long value.
     *
     * @param key   the shared preferences key.
     * @param value the new value.
     */
    public void setValue(String key, long value) {
        setValue(key, value, true);
    }

    /**
     * Removes a value from the shared preferences and refreshes the data.
     *
     * @param key the shared preferences key to remove.
     */
    public void removeValue(String key) {
        getSharedPrefs().edit().remove(key).apply();
        forceUpdate();
    }

}
