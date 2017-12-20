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

package xyz.klinker.messenger.shared.data

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import android.support.annotation.VisibleForTesting

import java.util.Date
import java.util.HashSet

import xyz.klinker.messenger.shared.R
import xyz.klinker.messenger.shared.data.pojo.BaseTheme
import xyz.klinker.messenger.shared.data.pojo.EmojiStyle
import xyz.klinker.messenger.shared.data.pojo.KeyboardLayout
import xyz.klinker.messenger.shared.data.pojo.NotificationAction
import xyz.klinker.messenger.shared.data.pojo.VibratePattern
import xyz.klinker.messenger.shared.util.EmojiInitializer
import xyz.klinker.messenger.shared.util.PhoneNumberUtils
import xyz.klinker.messenger.shared.util.TimeUtils

/**
 * Holds all settings_global for the application and allows for easily changing the values stored for each
 * setting.
 */
object Settings {

    // initializers
    var firstStart: Boolean = false
    var installTime: Long = 0
    var seenConvoNavToolTip: Boolean = false
    var showTextOnlineOnConversationList: Boolean = false
    var phoneNumber: String? = null

    // settings_global
    var vibrate: VibratePattern = VibratePattern.DEFAULT
    var notificationActions: MutableSet<NotificationAction> = mutableSetOf()
    var useGlobalThemeColor: Boolean = false
    var deliveryReports: Boolean = false
    var giffgaffDeliveryReports: Boolean = false
    var mobileOnly: Boolean = false
    var soundEffects: Boolean = false
    var securePrivateConversations: Boolean = false
    var quickCompose: Boolean = false
    var wakeScreen: Boolean = false
    var headsUp: Boolean = false
    var rounderBubbles: Boolean = false
    var swipeDelete: Boolean = false
    var stripUnicode: Boolean = false
    var historyInNotifications: Boolean = false
    var internalBrowser: Boolean = false
    var snooze: Long = 0
    var repeatNotifications: Long = 0
    var delayedSendingTimeout: Long = 0
    var cleanupMessagesTimeout: Long = 0
    var ringtone: String? = null
    var fontSize: String? = null
    var themeColorString: String? = null
    var baseThemeString: String? = null
    var signature: String? = null
    var adjustableNavBar: Boolean = false

    // configuration
    var smallFont: Int = 0
    var mediumFont: Int = 0
    var largeFont: Int = 0
    var mainColorSet: ColorSet = ColorSet()
    var baseTheme: BaseTheme = BaseTheme.DAY_NIGHT
    var keyboardLayout: KeyboardLayout = KeyboardLayout.DEFAULT
    var emojiStyle: EmojiStyle = EmojiStyle.DEFAULT

    val isCurrentlyDarkTheme: Boolean
        get() = when {
            baseTheme === BaseTheme.ALWAYS_LIGHT -> false
            baseTheme === BaseTheme.DAY_NIGHT -> TimeUtils.isNight
            else -> true
        }

    fun init(context: Context) {
        val sharedPrefs = getSharedPrefs(context)

        // initializers
        this.firstStart = sharedPrefs.getBoolean(context.getString(R.string.pref_first_start), true)
        this.seenConvoNavToolTip = sharedPrefs.getBoolean(context.getString(R.string.pref_seen_convo_nav_tooltip), false)
        this.showTextOnlineOnConversationList = sharedPrefs.getBoolean(
                context.getString(R.string.pref_show_text_online_on_conversation_list), true)

        this.phoneNumber = sharedPrefs.getString(context.getString(R.string.pref_phone_number), null)
        if (phoneNumber == null) {
            phoneNumber = PhoneNumberUtils.getMyPhoneNumber(context, false)

            if (phoneNumber != null && !phoneNumber!!.isEmpty()) {
                phoneNumber = PhoneNumberUtils.format(phoneNumber)
                sharedPrefs.edit().putString(context.getString(R.string.pref_phone_number), phoneNumber).apply()
            } else {
                phoneNumber = null
            }
        }

        this.installTime = sharedPrefs.getLong(context.getString(R.string.pref_install_time), 0)
        if (installTime == 0L) {
            installTime = Date().time
            sharedPrefs.edit().putLong(context.getString(R.string.pref_install_time), installTime).apply()
        }

        // settings_global
        this.deliveryReports = sharedPrefs.getBoolean(context.getString(R.string.pref_delivery_reports), false)
        this.giffgaffDeliveryReports = sharedPrefs.getBoolean(context.getString(R.string.pref_giffgaff), false)
        this.mobileOnly = sharedPrefs.getBoolean(context.getString(R.string.pref_mobile_only), false)
        this.soundEffects = sharedPrefs.getBoolean(context.getString(R.string.pref_sound_effects), true)
        this.securePrivateConversations = sharedPrefs.getBoolean(context.getString(R.string.pref_secure_private_conversations), false)
        this.quickCompose = sharedPrefs.getBoolean(context.getString(R.string.pref_quick_compose), false)
        this.snooze = sharedPrefs.getLong(context.getString(R.string.pref_snooze), 0)
        this.ringtone = sharedPrefs.getString(context.getString(R.string.pref_ringtone), null)
        this.fontSize = sharedPrefs.getString(context.getString(R.string.pref_font_size), "normal")
        this.themeColorString = sharedPrefs.getString(context.getString(R.string.pref_global_color_theme), "default")
        this.useGlobalThemeColor = sharedPrefs.getBoolean(context.getString(R.string.pref_apply_theme_globally), false)
        this.signature = sharedPrefs.getString(context.getString(R.string.pref_signature), "")
        this.wakeScreen = sharedPrefs.getString(context.getString(R.string.pref_wake_screen), "off") == "on"
        this.headsUp = sharedPrefs.getString(context.getString(R.string.pref_heads_up), "on") == "on"
        this.rounderBubbles = sharedPrefs.getBoolean(context.getString(R.string.pref_rounder_bubbles), false)
        this.swipeDelete = sharedPrefs.getBoolean(context.getString(R.string.pref_swipe_delete), false)
        this.stripUnicode = sharedPrefs.getBoolean(context.getString(R.string.pref_strip_unicode), false)
        this.historyInNotifications = sharedPrefs.getBoolean(context.getString(R.string.pref_history_in_notifications), true)
        this.internalBrowser = sharedPrefs.getBoolean(context.getString(R.string.pref_internal_browser), true)
        this.adjustableNavBar = sharedPrefs.getBoolean(context.getString(R.string.pref_adjustable_nav_bar), false)

        // configuration
        if (this.ringtone == null) {
            val uri = android.provider.Settings.System.DEFAULT_NOTIFICATION_URI.toString()
            setValue(context, context.getString(R.string.pref_ringtone), uri, false)
            this.ringtone = uri
        }

        if (fontSize == "small") {
            this.smallFont = 10
            this.mediumFont = 12
            this.largeFont = 14
        } else if (fontSize == "normal") {
            this.smallFont = 12
            this.mediumFont = 14
            this.largeFont = 16
        } else if (fontSize == "large") {
            this.smallFont = 14
            this.mediumFont = 16
            this.largeFont = 18
        } else if (fontSize == "extra_large") {
            this.smallFont = 16
            this.mediumFont = 18
            this.largeFont = 20
        }

        if (EmojiInitializer.isAlreadyUsingGoogleAndroidO()) {
            this.emojiStyle = EmojiStyle.ANDROID_O
        } else {
            val emojiStyle = sharedPrefs.getString(context.getString(R.string.pref_emoji_style), "default")
            when (emojiStyle) {
                "android_o" -> this.emojiStyle = EmojiStyle.ANDROID_O
                else -> this.emojiStyle = EmojiStyle.DEFAULT
            }
        }

        val vibrateString = sharedPrefs.getString(context.getString(R.string.pref_vibrate), "vibrate_default")
        when (vibrateString) {
            "vibrate_off" -> this.vibrate = VibratePattern.OFF
            "vibrate_default" -> this.vibrate = VibratePattern.DEFAULT
            "vibrate_two_short" -> this.vibrate = VibratePattern.TWO_SHORT
            "vibrate_two_long" -> this.vibrate = VibratePattern.TWO_LONG
            "vibrate_three_short" -> this.vibrate = VibratePattern.THREE_SHORT
            "vibrate_one_short_one_long" -> this.vibrate = VibratePattern.ONE_SHORT_ONE_LONG
            "vibrate_one_long_one_short" -> this.vibrate = VibratePattern.ONE_LONG_ONE_SHORT
            "vibrate_one_long" -> this.vibrate = VibratePattern.ONE_LONG
            "vibrate_one_short" -> this.vibrate = VibratePattern.ONE_SHORT
            "vibrate_one_extra_long" -> this.vibrate = VibratePattern.ONE_EXTRA_LONG
            "vibrate_two_short_one_long" -> this.vibrate = VibratePattern.TWO_SHORT_ONE_LONG
            "vibrate_one_long_one_short_one_long" -> this.vibrate = VibratePattern.ONE_LONG_ONE_SHORT_ONE_LONG
            else -> this.vibrate = VibratePattern.DEFAULT
        }

        val repeatNotifications = sharedPrefs.getString(context.getString(R.string.pref_repeat_notifications), "never")
        when (repeatNotifications) {
            "never" -> this.repeatNotifications = -1
            "one_min" -> this.repeatNotifications = TimeUtils.MINUTE
            "two_mins" -> this.repeatNotifications = TimeUtils.MINUTE * 2
            "five_min" -> this.repeatNotifications = TimeUtils.MINUTE * 5
            "ten_min" -> this.repeatNotifications = TimeUtils.MINUTE * 10
            "half_hour" -> this.repeatNotifications = TimeUtils.MINUTE * 30
            "hour" -> this.repeatNotifications = TimeUtils.HOUR
            else -> this.repeatNotifications = -1
        }

        val delayedSending = sharedPrefs.getString(context.getString(R.string.pref_delayed_sending), "off")
        when (delayedSending) {
            "off" -> this.delayedSendingTimeout = 0
            "one_second" -> this.delayedSendingTimeout = TimeUtils.SECOND * 1
            "three_seconds" -> this.delayedSendingTimeout = TimeUtils.SECOND * 3
            "five_seconds" -> this.delayedSendingTimeout = TimeUtils.SECOND * 5
            "ten_seconds" -> this.delayedSendingTimeout = TimeUtils.SECOND * 10
            "fifteen_seconds" -> this.delayedSendingTimeout = TimeUtils.SECOND * 15
            "thirty_seconds" -> this.delayedSendingTimeout = TimeUtils.SECOND * 30
            "one_minute" -> this.delayedSendingTimeout = TimeUtils.MINUTE
            else -> this.delayedSendingTimeout = 0
        }

        val cleanupOldMessages = sharedPrefs.getString(context.getString(R.string.pref_cleanup_messages), "never")
        when (cleanupOldMessages) {
            "never" -> this.cleanupMessagesTimeout = -1
            "one_week" -> this.cleanupMessagesTimeout = TimeUtils.DAY * 7
            "two_weeks" -> this.cleanupMessagesTimeout = TimeUtils.DAY * 17
            "one_month" -> this.cleanupMessagesTimeout = TimeUtils.DAY * 30
            "three_months" -> this.cleanupMessagesTimeout = TimeUtils.DAY * 90
            "six_months" -> this.cleanupMessagesTimeout = TimeUtils.YEAR / 2
            "one_year" -> this.cleanupMessagesTimeout = TimeUtils.YEAR
            else -> this.cleanupMessagesTimeout = -1
        }

        val keyboardLayoutString = sharedPrefs.getString(context.getString(R.string.pref_keyboard_layout), "default")
        when (keyboardLayoutString) {
            "default" -> this.keyboardLayout = KeyboardLayout.DEFAULT
            "send" -> this.keyboardLayout = KeyboardLayout.SEND
            "enter" -> this.keyboardLayout = KeyboardLayout.ENTER
        }

        baseThemeString = sharedPrefs.getString(context.getString(R.string.pref_base_theme), "day_night")
        when (baseThemeString) {
            "day_night" -> this.baseTheme = BaseTheme.DAY_NIGHT
            "light" -> this.baseTheme = BaseTheme.ALWAYS_LIGHT
            "dark" -> this.baseTheme = BaseTheme.ALWAYS_DARK
            "black" -> this.baseTheme = BaseTheme.BLACK
            else -> this.baseTheme = BaseTheme.DAY_NIGHT
        }

        notificationActions = HashSet()
        val actions = sharedPrefs.getStringSet(context.getString(R.string.pref_notification_actions),
                getDefaultNotificationActions(context))

        if (actions!!.contains("reply")) {
            notificationActions.add(NotificationAction.REPLY)
        }

        if (actions.contains("call")) {
            notificationActions.add(NotificationAction.CALL)
        }

        if (actions.contains("read")) {
            notificationActions.add(NotificationAction.READ)
        }

        if (actions.contains("delete")) {
            notificationActions.add(NotificationAction.DELETE)
        }

        if (actions.contains("mute")) {
            notificationActions.add(NotificationAction.MUTE)
        }

        this.mainColorSet = ColorSet.create(
                sharedPrefs.getInt(context.getString(R.string.pref_global_primary_color), ColorSet.DEFAULT(context).color),
                sharedPrefs.getInt(context.getString(R.string.pref_global_primary_dark_color), ColorSet.DEFAULT(context).colorDark),
                sharedPrefs.getInt(context.getString(R.string.pref_global_accent_color), ColorSet.DEFAULT(context).colorAccent)
        )
    }

    private fun getDefaultNotificationActions(context: Context): Set<String> {
        return context.resources.getStringArray(R.array.notification_actions_default).toSet()
    }

    fun getSharedPrefs(context: Context): SharedPreferences {
        return PreferenceManager.getDefaultSharedPreferences(context)
    }

    /**
     * Forces a reload of all settings_global data.
     */
    fun forceUpdate(context: Context) {
        init(context)
    }

    fun setValue(context: Context, key: String, value: Boolean, forceUpdate: Boolean) {
        getSharedPrefs(context).edit()
                .putBoolean(key, value)
                .apply()

        if (forceUpdate) {
            forceUpdate(context)
        }
    }

    fun setValue(context: Context, key: String, value: Int, forceUpdate: Boolean) {
        getSharedPrefs(context).edit()
                .putInt(key, value)
                .apply()

        if (forceUpdate) {
            forceUpdate(context)
        }
    }

    fun setValue(context: Context, key: String, value: String, forceUpdate: Boolean) {
        getSharedPrefs(context).edit()
                .putString(key, value)
                .apply()

        if (forceUpdate) {
            forceUpdate(context)
        }
    }

    fun setValue(context: Context, key: String, value: Long, forceUpdate: Boolean) {
        getSharedPrefs(context).edit()
                .putLong(key, value)
                .apply()

        if (forceUpdate) {
            forceUpdate(context)
        }
    }

    fun setValue(context: Context, key: String, value: Set<String>, forceUpdate: Boolean) {
        getSharedPrefs(context).edit()
                .putStringSet(key, value)
                .apply()

        if (forceUpdate) {
            forceUpdate(context)
        }
    }

    /**
     * Stores a new boolean value.
     *
     * @param key   the shared preferences key.
     * @param value the new value.
     */
    fun setValue(context: Context, key: String, value: Boolean) {
        setValue(context, key, value, true)
    }

    /**
     * Stores a new integer value.
     *
     * @param key   the shared preferences key.
     * @param value the new value.
     */
    fun setValue(context: Context, key: String, value: Int) {
        setValue(context, key, value, true)
    }

    /**
     * Stores a new String value.
     *
     * @param key   the shared preferences key.
     * @param value the new value.
     */
    fun setValue(context: Context, key: String, value: String) {
        setValue(context, key, value, true)
    }

    /**
     * Stores a new long value.
     *
     * @param key   the shared preferences key.
     * @param value the new value.
     */
    fun setValue(context: Context, key: String, value: Long) {
        setValue(context, key, value, true)
    }

    /**
     * Stores a new long value.
     *
     * @param key   the shared preferences key.
     * @param value the new value.
     */
    fun setValue(context: Context, key: String, value: Set<String>) {
        setValue(context, key, value, true)
    }

    /**
     * Removes a value from the shared preferences and refreshes the data.
     *
     * @param key the shared preferences key to remove.
     */
    fun removeValue(context: Context, key: String) {
        getSharedPrefs(context).edit().remove(key).apply()
        forceUpdate(context)
    }
}
