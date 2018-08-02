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

package xyz.klinker.messenger.fragment.settings

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.preference.PreferenceCategory
import android.preference.PreferenceGroup
import android.support.v7.app.AlertDialog
import android.telephony.TelephonyManager
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.Spinner
import xyz.klinker.messenger.R
import xyz.klinker.messenger.activity.SettingsActivity
import xyz.klinker.messenger.api.implementation.Account
import xyz.klinker.messenger.api.implementation.ApiUtils
import xyz.klinker.messenger.shared.data.FeatureFlags
import xyz.klinker.messenger.shared.data.Settings
import xyz.klinker.messenger.shared.util.EmojiInitializer
import xyz.klinker.messenger.shared.util.SetUtils
import xyz.klinker.messenger.view.preference.NotificationAlertsPreference
import android.widget.ArrayAdapter
import xyz.klinker.messenger.shared.util.AndroidVersionUtil


/**
 * Fragment for modifying app settings_global.
 */
class GlobalSettingsFragment : MaterialPreferenceFragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        addPreferencesFromResource(R.xml.settings_global)

        initThemeRedirect()
        initMmsConfigurationRedirect()

        initPhoneNumber()
        initKeyboardLayout()
        initSwipeDelete()
        initNotificationActions()
        initDeliveryReports()
        initReadReceipts()
        initSoundEffects()
        initStripUnicode()
        initNotificationHistory()
        initDismissNotificationsOnReply()
        initEmojiStyle()
    }

    override fun onStop() {
        super.onStop()
        Settings.forceUpdate(activity)
    }

    private fun initThemeRedirect() {
        findPreference(getString(R.string.pref_theme_settings))
                .setOnPreferenceClickListener {
                    SettingsActivity.startThemeSettings(activity)
                    false
                }
    }

    private fun initMmsConfigurationRedirect() {
        findPreference(getString(R.string.pref_mms_configuration))
                .setOnPreferenceClickListener {
                    SettingsActivity.startMmsSettings(activity)
                    false
                }
    }

    private fun initPhoneNumber() {
        if (Account.exists() && !Account.primary) {
            val preference = findPreference(getString(R.string.pref_phone_number))
            preferenceScreen.removePreference(preference)
        }
    }

    private fun initKeyboardLayout() {
        findPreference(getString(R.string.pref_keyboard_layout))
                .setOnPreferenceChangeListener { _, o ->
                    val layout = o as String
                    ApiUtils.updateKeyboardLayout(Account.accountId,
                            layout)

                    true
                }
    }

    private fun initNotificationActions() {
        val actions = findPreference(getString(R.string.pref_notification_actions))
        actions.setOnPreferenceChangeListener { _, o ->
            val options = o as Set<String>
            ApiUtils.updateNotificationActions(Account.accountId,
                    SetUtils.stringify(options))
            true
        }
    }

    private fun initSwipeDelete() {
        val swipeActions = findPreference(getString(R.string.pref_swipe_choices))
        swipeActions.setOnPreferenceClickListener {
            val layout = LayoutInflater.from(activity).inflate(R.layout.dialog_swipe_actions, null, false)
            val representations = resources.getStringArray(R.array.swipe_actions_values)

            val leftToRight = layout.findViewById<Spinner>(R.id.left_to_right)
            val leftToRightAdapter = ArrayAdapter<String>(activity, android.R.layout.simple_spinner_item, resources.getStringArray(R.array.swipe_actions))
            leftToRightAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            leftToRight.adapter = leftToRightAdapter
            leftToRight.setSelection(representations.indexOf(Settings.leftToRightSwipe.rep))

            val rightToLeft = layout.findViewById<Spinner>(R.id.right_to_left)
            val rightToLeftAdapter = ArrayAdapter<String>(activity, android.R.layout.simple_spinner_item, resources.getStringArray(R.array.swipe_actions))
            rightToLeftAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            rightToLeft.adapter = rightToLeftAdapter
            rightToLeft.setSelection(representations.indexOf(Settings.rightToLeftSwipe.rep))

            AlertDialog.Builder(activity)
                    .setView(layout)
                    .setPositiveButton(R.string.save) { _, _ ->
                        val leftToRightRepresentation = representations[leftToRight.selectedItemPosition]
                        val rightToLeftRepresentation = representations[rightToLeft.selectedItemPosition]

                        Settings.setValue(activity, getString(R.string.pref_left_to_right_swipe), leftToRightRepresentation)
                        Settings.setValue(activity, getString(R.string.pref_right_to_left_swipe), rightToLeftRepresentation)

                        ApiUtils.updateLeftToRightSwipeAction(Account.accountId, leftToRightRepresentation)
                        ApiUtils.updateRightToLeftSwipeAction(Account.accountId, rightToLeftRepresentation)
                    }
                    .setNegativeButton(android.R.string.cancel, null)
                    .show()
            false
        }
    }

    private fun initDeliveryReports() {
        val normal = findPreference(getString(R.string.pref_delivery_reports))
        val giffgaff = findPreference(getString(R.string.pref_giffgaff))

        normal.setOnPreferenceChangeListener { _, o ->
            val delivery = o as Boolean
            ApiUtils.updateDeliveryReports(Account.accountId,
                    delivery)
            true
        }

        giffgaff.setOnPreferenceChangeListener { _, o ->
            val delivery = o as Boolean
            ApiUtils.updateGiffgaffDeliveryReports(Account.accountId,
                    delivery)
            true
        }

        val manager = activity.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        val carrierName = manager.networkOperatorName

        if (carrierName != null && !(carrierName.equals("giffgaff", ignoreCase = true) || carrierName.replace(" ", "").equals("o2-uk", ignoreCase = true))) {
            (findPreference(getString(R.string.pref_advanced_category)) as PreferenceGroup).removePreference(giffgaff)
        } else {
            (findPreference(getString(R.string.pref_advanced_category)) as PreferenceGroup).removePreference(normal)
        }
    }

    private fun initReadReceipts() {
        val preference = findPreference(getString(R.string.pref_mms_read_receipts))
        preference.setOnPreferenceChangeListener { _, o ->
            val receipts = o as Boolean
            ApiUtils.updateGroupMMS(Account.accountId,
                    receipts)
            true
        }

        (findPreference(getString(R.string.pref_advanced_category)) as PreferenceGroup).removePreference(preference)
    }

    private fun initStripUnicode() {
        findPreference(getString(R.string.pref_strip_unicode))
                .setOnPreferenceChangeListener { _, o ->
                    val strip = o as Boolean
                    ApiUtils.updateStripUnicode(Account.accountId,
                            strip)
                    true
                }
    }

    private fun initNotificationHistory() {
        val pref = findPreference(getString(R.string.pref_history_in_notifications))
        pref.setOnPreferenceChangeListener { _, o ->
            val history = o as Boolean
            ApiUtils.updateShowHistoryInNotification(Account.accountId,
                    history)
            true
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            (findPreference(getString(R.string.pref_notification_category)) as PreferenceCategory)
                    .removePreference(pref)
        }
    }

    private fun initDismissNotificationsOnReply() {
        val pref = findPreference(getString(R.string.pref_dismiss_notifications_on_reply_android_p))
        pref.setOnPreferenceChangeListener { _, o ->
            val dismiss = o as Boolean
            ApiUtils.updateDismissNotificationsAfterReply(Account.accountId,
                    dismiss)
            true
        }

        // this wasn't working.. Remove it for all
//        if (!AndroidVersionUtil.isAndroidP) {
            (findPreference(getString(R.string.pref_notification_category)) as PreferenceCategory)
                    .removePreference(pref)
//        }
    }

    private fun initEmojiStyle() {
        val pref = findPreference(getString(R.string.pref_emoji_style))

        if (EmojiInitializer.isAlreadyUsingGoogleAndroidO()) {
            (findPreference(getString(R.string.pref_customization_category)) as PreferenceCategory)
                    .removePreference(pref)
        } else {
            pref.setOnPreferenceChangeListener { _, o ->
                val value = o as String
                ApiUtils.updateEmojiStyle(Account.accountId, value)

                Handler().postDelayed({
                    Settings.forceUpdate(activity)
                    EmojiInitializer.initializeEmojiCompat(activity)
                }, 500)

                true
            }
        }
    }

    private fun initSoundEffects() {
        findPreference(getString(R.string.pref_sound_effects))
                .setOnPreferenceChangeListener { _, o ->
                    val effects = o as Boolean
                    ApiUtils.updateSoundEffects(Account.accountId,
                            effects)
                    true
                }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        (findPreference(getString(R.string.pref_alert_types)) as NotificationAlertsPreference)
                .handleRingtoneResult(requestCode, resultCode, data)
    }
}
