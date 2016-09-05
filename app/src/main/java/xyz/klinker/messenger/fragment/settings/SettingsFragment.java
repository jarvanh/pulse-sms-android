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

import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.support.v7.app.AppCompatDelegate;

import xyz.klinker.messenger.R;
import xyz.klinker.messenger.api.implementation.ApiUtils;
import xyz.klinker.messenger.data.Settings;

/**
 * Fragment for modifying app settings.
 */
public class SettingsFragment extends PreferenceFragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.settings);
        initGlobalTheme();
        initNightMode();
        initFontSize();
        initVibrate();
        initDeliveryReports();
    }

    @Override
    public void onStop() {
        super.onStop();
        Settings.get(getActivity()).forceUpdate();
    }

    private void initGlobalTheme() {
        findPreference(getString(R.string.pref_global_color_theme))
                .setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference preference, Object o) {
                        String colorString = (String) o;
                        new ApiUtils().updateGlobalThemeColor(Settings.get(getActivity()).accountId,
                                colorString);

                        return true;
                    }
                });
    }

    private void initNightMode() {
        findPreference(getString(R.string.pref_dark_theme))
                .setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference preference, Object o) {
                        boolean night = (boolean) o;
                        if (night) {
                            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                        } else {
                            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                        }

                        new ApiUtils().updateDarkTheme(Settings.get(getActivity()).accountId,
                                night);

                        return true;
                    }
                });
    }

    private void initFontSize() {
        findPreference(getString(R.string.pref_font_size))
                .setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference preference, Object o) {
                        String size = (String) o;
                        new ApiUtils().updateFontSize(Settings.get(getActivity()).accountId,
                                size);

                        return true;
                    }
                });
    }

    private void initVibrate() {
        findPreference(getString(R.string.pref_vibrate))
                .setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference preference, Object o) {
                        boolean vibrate = (boolean) o;
                        new ApiUtils().updateVibrate(Settings.get(getActivity()).accountId,
                                vibrate);
                        return true;
                    }
                });
    }

    private void initDeliveryReports() {
        findPreference(getString(R.string.pref_delivery_reports))
                .setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference preference, Object o) {
                        boolean delivery = (boolean) o;
                        new ApiUtils().updateDeliveryReports(Settings.get(getActivity()).accountId,
                                delivery);
                        return true;
                    }
                });
    }

}
