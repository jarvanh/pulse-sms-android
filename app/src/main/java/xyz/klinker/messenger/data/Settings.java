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
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.support.annotation.VisibleForTesting;
import android.util.Log;

import xyz.klinker.messenger.R;
import xyz.klinker.messenger.util.TimeUtils;

/**
 * Holds all settings for the application and allows for easily changing the values stored for each
 * setting.
 */
public class Settings {

    public enum BaseTheme {
        DAY_NIGHT(false), ALWAYS_LIGHT(false),
        ALWAYS_DARK(true), BLACK(true);

        public boolean isDark;
        BaseTheme(boolean isDark) {
            this.isDark = isDark;
        }
    }

    public enum VibratePattern {
        OFF(null), DEFAULT(null),
        TWO_LONG(new long[] {0, 1000, 500, 1000}),
        TWO_SHORT(new long[] {0, 300, 200, 300}),
        THREE_SHORT(new long[] {0, 300, 200, 300, 200, 300}),
        ONE_SHORT_ONE_LONG(new long[] {0, 300, 300, 1000}),
        ONE_LONG_ONE_SHORT(new long[] {0, 1000, 300, 300});

        public long[] pattern;
        VibratePattern(long[] pattern) {
            this.pattern = pattern;
        }
    }

    private static volatile Settings settings;

    private Context context;

    // initializers
    public boolean firstStart;
    public boolean seenConvoNavToolTip;

    // settings
    public VibratePattern vibrate;
    public boolean useGlobalThemeColor;
    public boolean deliveryReports;
    public boolean convertLongMessagesToMMS;
    public boolean mobileOnly;
    public boolean soundEffects;
    public long snooze;
    public String ringtone;
    public String fontSize;
    public String themeColorString;
    public String signature;

    // configuration
    public int smallFont;
    public int mediumFont;
    public int largeFont;
    public ColorSet globalColorSet;
    public BaseTheme baseTheme;

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

        // initializers
        this.firstStart = sharedPrefs.getBoolean(context.getString(R.string.pref_first_start), true);
        this.seenConvoNavToolTip = sharedPrefs.getBoolean(context.getString(R.string.pref_seen_convo_nav_tooltip), false);

        // settings
        this.deliveryReports = sharedPrefs.getBoolean(context.getString(R.string.pref_delivery_reports), false);
        this.convertLongMessagesToMMS = sharedPrefs.getBoolean(context.getString(R.string.pref_convert_to_mms), true);
        this.mobileOnly = sharedPrefs.getBoolean(context.getString(R.string.pref_mobile_only), false);
        this.soundEffects = sharedPrefs.getBoolean(context.getString(R.string.pref_sound_effects), true);
        this.snooze = sharedPrefs.getLong(context.getString(R.string.pref_snooze), 0);
        this.ringtone = sharedPrefs.getString(context.getString(R.string.pref_ringtone), null);
        this.fontSize = sharedPrefs.getString(context.getString(R.string.pref_font_size), "normal");
        this.themeColorString = sharedPrefs.getString(context.getString(R.string.pref_global_color_theme), "default");
        this.useGlobalThemeColor = !themeColorString.equals("default");
        this.signature = sharedPrefs.getString(context.getString(R.string.pref_signature), "");

        // configuration
        if (this.ringtone == null) {
            String uri = android.provider.Settings.System.DEFAULT_NOTIFICATION_URI.toString();
            setValue(context.getString(R.string.pref_ringtone), uri, false);
            this.ringtone = uri;
        }

        if (fontSize.equals("small")) {
            this.smallFont = 10;
            this.mediumFont = 12;
            this.largeFont = 14;
        } else if (fontSize.equals("normal")) {
            this.smallFont = 12;
            this.mediumFont = 14;
            this.largeFont = 16;
        } else if (fontSize.equals("large")) {
            this.smallFont = 14;
            this.mediumFont = 16;
            this.largeFont = 18;
        } else if (fontSize.equals("extra_large")) {
            this.smallFont = 16;
            this.mediumFont = 18;
            this.largeFont = 20;
        }

        String vibrateString = sharedPrefs.getString(context.getString(R.string.pref_vibrate), "vibrate_default");
        switch (vibrateString) {
            case "vibrate_off":
                this.vibrate = VibratePattern.OFF;
                break;
            case "vibrate_default":
                this.vibrate = VibratePattern.DEFAULT;
                break;
            case "vibrate_two_short":
                this.vibrate = VibratePattern.TWO_SHORT;
                break;
            case "vibrate_two_long":
                this.vibrate = VibratePattern.TWO_LONG;
                break;
            case "vibrate_three_short":
                this.vibrate = VibratePattern.THREE_SHORT;
                break;
            case "vibrate_one_short_one_long":
                this.vibrate = VibratePattern.ONE_SHORT_ONE_LONG;
                break;
            case "vibrate_one_long_one_short":
                this.vibrate = VibratePattern.ONE_LONG_ONE_SHORT;
                break;
            default:
                this.vibrate = VibratePattern.DEFAULT;
                break;
        }

        String baseTheme = sharedPrefs.getString(context.getString(R.string.pref_base_theme), "day_night");
        switch (baseTheme) {
            case "day_night":
                this.baseTheme = BaseTheme.DAY_NIGHT;
                break;
            case "light":
                this.baseTheme = BaseTheme.ALWAYS_LIGHT;
                break;
            case "dark":
                this.baseTheme = BaseTheme.ALWAYS_DARK;
                break;
            case "black":
                this.baseTheme = BaseTheme.BLACK;
                break;
            default:
                this.baseTheme = BaseTheme.DAY_NIGHT;
                break;
        }

        this.globalColorSet = ColorSet.getFromString(context, themeColorString);
    }

    @VisibleForTesting
    public SharedPreferences getSharedPrefs() {
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

    public boolean isCurrentlyDarkTheme() {
        if (baseTheme == BaseTheme.ALWAYS_LIGHT) {
            return false;
        } else if (baseTheme == BaseTheme.DAY_NIGHT) {
            return TimeUtils.isNight();
        } else {
            return true;
        }
    }

}
