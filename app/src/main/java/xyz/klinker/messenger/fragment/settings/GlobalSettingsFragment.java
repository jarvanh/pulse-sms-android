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

package xyz.klinker.messenger.fragment.settings;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceGroup;
import android.support.v7.app.AppCompatDelegate;
import android.telephony.TelephonyManager;

import java.util.Set;

import xyz.klinker.messenger.R;
import xyz.klinker.messenger.activity.SettingsActivity;
import xyz.klinker.messenger.api.implementation.Account;
import xyz.klinker.messenger.api.implementation.ApiUtils;
import xyz.klinker.messenger.shared.data.ColorSet;
import xyz.klinker.messenger.shared.data.FeatureFlags;
import xyz.klinker.messenger.shared.data.Settings;
import xyz.klinker.messenger.shared.util.ColorUtils;
import xyz.klinker.messenger.shared.util.EmojiInitializer;
import xyz.klinker.messenger.shared.util.SetUtils;
import xyz.klinker.messenger.view.NotificationAlertsPreference;

/**
 * Fragment for modifying app settings_global.
 */
public class GlobalSettingsFragment extends MaterialPreferenceFragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.settings_global);

        initThemeRedirect();
        initMmsConfigurationRedirect();

        initKeyboardLayout();
        initSwipeDelete();
        initNotificationActions();
        initDeliveryReports();
        initSoundEffects();
        initStripUnicode();
        initNotificationHistory();
        initEmojiStyle();
    }

    @Override
    public void onStop() {
        super.onStop();
        Settings.get(getActivity()).forceUpdate(getActivity());
    }

    private void initThemeRedirect() {
        findPreference(getString(R.string.pref_theme_settings))
                .setOnPreferenceClickListener(preference -> {
                    SettingsActivity.startThemeSettings(getActivity());
                    return false;
                });
    }

    private void initMmsConfigurationRedirect() {
        findPreference(getString(R.string.pref_mms_configuration))
                .setOnPreferenceClickListener(preference -> {
                    SettingsActivity.startMmsSettings(getActivity());
                    return false;
                });
    }

    private void initKeyboardLayout() {
        findPreference(getString(R.string.pref_keyboard_layout))
                .setOnPreferenceChangeListener((preference, o) -> {
                    String layout = (String) o;
                    new ApiUtils().updateKeyboardLayout(Account.get(getActivity()).accountId,
                            layout);

                    return true;
                });
    }

    private void initNotificationActions() {
        Preference actions = findPreference(getString(R.string.pref_notification_actions));
        actions.setOnPreferenceChangeListener((preference, o) -> {
            Set<String> options = (Set<String>) o;
            new ApiUtils().updateNotificationActions(Account.get(getActivity()).accountId,
                    SetUtils.stringify(options));
            return true;
        });
    }

    private void initSwipeDelete() {
        findPreference(getString(R.string.pref_swipe_delete))
                .setOnPreferenceChangeListener((preference, o) -> {
                    boolean delete = (boolean) o;
                    new ApiUtils().updateSwipeToDelete(Account.get(getActivity()).accountId,
                            delete);
                    return true;
                });
    }

    private void initDeliveryReports() {
        Preference normal = findPreference(getString(R.string.pref_delivery_reports));
        Preference giffgaff = findPreference(getString(R.string.pref_giffgaff));

        normal.setOnPreferenceChangeListener((preference, o) -> {
                    boolean delivery = (boolean) o;
                    new ApiUtils().updateDeliveryReports(Account.get(getActivity()).accountId,
                            delivery);
                    return true;
                });

        giffgaff.setOnPreferenceChangeListener((preference, o) -> {
                    boolean delivery = (boolean) o;
                    new ApiUtils().updateGiffgaffDeliveryReports(Account.get(getActivity()).accountId,
                            delivery);
                    return true;
                });

        TelephonyManager manager = (TelephonyManager) getActivity().getSystemService(Context.TELEPHONY_SERVICE);
        String carrierName = manager.getNetworkOperatorName();

        if (carrierName != null && !(carrierName.equalsIgnoreCase("giffgaff") ||
                carrierName.replace(" ", "").equalsIgnoreCase("o2-uk"))) {
            ((PreferenceGroup) findPreference(getString(R.string.pref_advanced_category))).removePreference(giffgaff);
        } else {
            ((PreferenceGroup) findPreference(getString(R.string.pref_advanced_category))).removePreference(normal);
        }
    }

    private void initStripUnicode() {
        findPreference(getString(R.string.pref_strip_unicode))
                .setOnPreferenceChangeListener((preference, o) -> {
                    boolean strip = (boolean) o;
                    new ApiUtils().updateStripUnicode(Account.get(getActivity()).accountId,
                            strip);
                    return true;
                });
    }

    private void initNotificationHistory() {
        Preference pref = findPreference(getString(R.string.pref_history_in_notifications));
        pref.setOnPreferenceChangeListener((preference, o) -> {
                    boolean history = (boolean) o;
                    new ApiUtils().updateShowHistoryInNotification(Account.get(getActivity()).accountId,
                            history);
                    return true;
                });

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            ((PreferenceCategory) findPreference(getString(R.string.pref_notification_category)))
                    .removePreference(pref);
        }
    }

    private void initEmojiStyle() {
        Preference pref = findPreference(getString(R.string.pref_emoji_style));

        if (EmojiInitializer.INSTANCE.isAlreadyUsingGoogleAndroidO()) {
            ((PreferenceCategory) findPreference(getString(R.string.pref_customization_category)))
                    .removePreference(pref);
        } else {
            pref.setOnPreferenceChangeListener((preference, o) -> {
                String value = (String) o;
                new ApiUtils().updateEmojiStyle(Account.get(getActivity()).accountId, value);

                new Handler().postDelayed(() -> {
                    Settings.get(getActivity()).forceUpdate(getActivity());
                    EmojiInitializer.INSTANCE.initializeEmojiCompat(getActivity());
                }, 500);

                return true;
            });
        }
    }

    private void initSoundEffects() {
        findPreference(getString(R.string.pref_sound_effects))
                .setOnPreferenceChangeListener((preference, o) -> {
                    boolean effects = (boolean) o;
                    new ApiUtils().updateSoundEffects(Account.get(getActivity()).accountId,
                            effects);
                    return true;
                });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        ((NotificationAlertsPreference) findPreference(getString(R.string.pref_alert_types)))
                .handleRingtoneResult(requestCode, resultCode, data);
    }

}
