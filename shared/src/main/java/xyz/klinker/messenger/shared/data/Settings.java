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

package xyz.klinker.messenger.shared.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.VisibleForTesting;

import java.util.HashSet;
import java.util.Set;

import xyz.klinker.messenger.shared.R;
import xyz.klinker.messenger.shared.data.pojo.BaseTheme;
import xyz.klinker.messenger.shared.data.pojo.KeyboardLayout;
import xyz.klinker.messenger.shared.data.pojo.NotificationAction;
import xyz.klinker.messenger.shared.data.pojo.VibratePattern;
import xyz.klinker.messenger.shared.util.TimeUtils;

/**
 * Holds all settings_global for the application and allows for easily changing the values stored for each
 * setting.
 */
public class Settings {

    private static volatile Settings settings;

    private Context context;

    // initializers
    public boolean firstStart;
    public boolean seenConvoNavToolTip;

    // settings_global
    public VibratePattern vibrate;
    public Set<NotificationAction> notificationActions;
    public boolean useGlobalThemeColor;
    public boolean deliveryReports;
    public boolean giffgaffDeliveryReports;
    public boolean mobileOnly;
    public boolean soundEffects;
    public boolean securePrivateConversations;
    public boolean quickCompose;
    public boolean wakeScreen;
    public boolean headsUp;
    public boolean rounderBubbles;
    public boolean swipeDelete;
    public boolean stripUnicode;
    public boolean historyInNotifications;
    public long snooze;
    public long repeatNotifications;
    public long delayedSendingTimeout;
    public long cleanupMessagesTimeout;
    public String ringtone;
    public String fontSize;
    public String themeColorString;
    public String baseThemeString;
    public String signature;

    // configuration
    public int smallFont;
    public int mediumFont;
    public int largeFont;
    public ColorSet globalColorSet;
    public BaseTheme baseTheme;
    public KeyboardLayout keyboardLayout;

    /**
     * Gets a new instance (singleton) of Settings.
     *
     * @param context the current application context.
     * @return the settings_global instance.
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

        // settings_global
        this.deliveryReports = sharedPrefs.getBoolean(context.getString(R.string.pref_delivery_reports), false);
        this.giffgaffDeliveryReports = sharedPrefs.getBoolean(context.getString(R.string.pref_giffgaff), false);
        this.mobileOnly = sharedPrefs.getBoolean(context.getString(R.string.pref_mobile_only), false);
        this.soundEffects = sharedPrefs.getBoolean(context.getString(R.string.pref_sound_effects), true);
        this.securePrivateConversations = sharedPrefs.getBoolean(context.getString(R.string.pref_secure_private_conversations), false);
        this.quickCompose = sharedPrefs.getBoolean(context.getString(R.string.pref_quick_compose), false);
        this.snooze = sharedPrefs.getLong(context.getString(R.string.pref_snooze), 0);
        this.ringtone = sharedPrefs.getString(context.getString(R.string.pref_ringtone), null);
        this.fontSize = sharedPrefs.getString(context.getString(R.string.pref_font_size), "normal");
        this.themeColorString = sharedPrefs.getString(context.getString(R.string.pref_global_color_theme), "default");
        this.useGlobalThemeColor = !themeColorString.equals("default");
        this.signature = sharedPrefs.getString(context.getString(R.string.pref_signature), "");
        this.wakeScreen = sharedPrefs.getString(context.getString(R.string.pref_wake_screen), "off").equals("on");
        this.headsUp = sharedPrefs.getString(context.getString(R.string.pref_heads_up), "on").equals("on");
        this.rounderBubbles = sharedPrefs.getBoolean(context.getString(R.string.pref_rounder_bubbles), false);
        this.swipeDelete = sharedPrefs.getBoolean(context.getString(R.string.pref_swipe_delete), false);
        this.stripUnicode = sharedPrefs.getBoolean(context.getString(R.string.pref_strip_unicode), false);
        this.historyInNotifications = sharedPrefs.getBoolean(context.getString(R.string.pref_history_in_notifications), true);

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
            case "vibrate_one_long":
                this.vibrate = VibratePattern.ONE_LONG;
                break;
            case "vibrate_one_short":
                this.vibrate = VibratePattern.ONE_SHORT;
                break;
            case "vibrate_one_extra_long":
                this.vibrate = VibratePattern.ONE_EXTRA_LONG;
                break;
            default:
                this.vibrate = VibratePattern.DEFAULT;
                break;
        }

        String repeatNotifications = sharedPrefs.getString(context.getString(R.string.pref_repeat_notifications), "never");
        switch (repeatNotifications) {
            case "never":
                this.repeatNotifications = -1;
                break;
            case "one_min":
                this.repeatNotifications = TimeUtils.MINUTE;
                break;
            case "five_min":
                this.repeatNotifications = TimeUtils.MINUTE * 5;
                break;
            case "ten_min":
                this.repeatNotifications = TimeUtils.MINUTE * 10;
                break;
            case "half_hour":
                this.repeatNotifications = TimeUtils.MINUTE * 30;
                break;
            case "hour":
                this.repeatNotifications = TimeUtils.HOUR;
                break;
            default:
                this.repeatNotifications = -1;
                break;
        }

        String delayedSending = sharedPrefs.getString(context.getString(R.string.pref_delayed_sending), "off");
        switch (delayedSending) {
            case "off":
                this.delayedSendingTimeout = 0;
                break;
            case "one_second":
                this.delayedSendingTimeout = TimeUtils.SECOND * 1;
                break;
            case "three_seconds":
                this.delayedSendingTimeout = TimeUtils.SECOND * 3;
                break;
            case "five_seconds":
                this.delayedSendingTimeout = TimeUtils.SECOND * 5;
                break;
            case "ten_seconds":
                this.delayedSendingTimeout = TimeUtils.SECOND * 10;
                break;
            case "fifteen_seconds":
                this.delayedSendingTimeout = TimeUtils.SECOND * 15;
                break;
            case "thirty_seconds":
                this.delayedSendingTimeout = TimeUtils.SECOND * 30;
                break;
            case "one_minute":
                this.delayedSendingTimeout = TimeUtils.MINUTE;
                break;
            default:
                this.delayedSendingTimeout = 0;
                break;
        }

        String cleanupOldMessages = sharedPrefs.getString(context.getString(R.string.pref_cleanup_messages), "never");
        switch (cleanupOldMessages) {
            case "never":
                this.cleanupMessagesTimeout = -1;
                break;
            case "one_week":
                this.cleanupMessagesTimeout = TimeUtils.DAY * 7;
                break;
            case "two_weeks":
                this.cleanupMessagesTimeout = TimeUtils.DAY * 17;
                break;
            case "one_month":
                this.cleanupMessagesTimeout = TimeUtils.DAY * 30;
                break;
            case "three_months":
                this.cleanupMessagesTimeout = TimeUtils.DAY * 90;
                break;
            case "six_months":
                this.cleanupMessagesTimeout = TimeUtils.YEAR / 2;
                break;
            case "one_year":
                this.cleanupMessagesTimeout = TimeUtils.YEAR;
                break;
            default:
                this.cleanupMessagesTimeout = -1;
                break;
        }

        String keyboardLayoutString = sharedPrefs.getString(context.getString(R.string.pref_keyboard_layout), "default");
        switch (keyboardLayoutString) {
            case "default":
                this.keyboardLayout = KeyboardLayout.DEFAULT;
                break;
            case "send":
                this.keyboardLayout = KeyboardLayout.SEND;
                break;
            case "enter":
                this.keyboardLayout = KeyboardLayout.ENTER;
                break;
        }

        baseThemeString = sharedPrefs.getString(context.getString(R.string.pref_base_theme), "day_night");
        switch (baseThemeString) {
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

        notificationActions = new HashSet<>();
        Set<String> actions = sharedPrefs.getStringSet(context.getString(R.string.pref_notification_actions),
                getDefaultNotificationActions());

        if (actions.contains("reply")) {
            notificationActions.add(NotificationAction.REPLY);
        }

        if (actions.contains("call")) {
            notificationActions.add(NotificationAction.CALL);
        }

        if (actions.contains("read")) {
            notificationActions.add(NotificationAction.READ);
        }

        if (actions.contains("delete")) {
            notificationActions.add(NotificationAction.DELETE);
        }

        this.globalColorSet = ColorSet.getFromString(context, themeColorString);
    }

    private Set<String> getDefaultNotificationActions() {
        Set<String> set = new HashSet<>();

        for (String s : context.getResources().getStringArray(R.array.notification_actions_default)) {
            set.add(s);
        }

        return set;
    }

    public SharedPreferences getSharedPrefs() {
        return PreferenceManager.getDefaultSharedPreferences(context);
    }

    /**
     * Gets the current settings_global context.
     *
     * @return the context.
     */
    public Context getContext() {
        return context;
    }

    /**
     * Forces a reload of all settings_global data.
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

    @VisibleForTesting
    protected void setValue(String key, Set<String> value, boolean forceUpdate) {
        getSharedPrefs().edit()
                .putStringSet(key, value)
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
     * Stores a new long value.
     *
     * @param key   the shared preferences key.
     * @param value the new value.
     */
    public void setValue(String key, Set<String> value) {
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
