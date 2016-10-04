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
import xyz.klinker.messenger.api.implementation.Account;
import xyz.klinker.messenger.api.implementation.ApiUtils;
import xyz.klinker.messenger.data.ColorSet;
import xyz.klinker.messenger.data.Settings;
import xyz.klinker.messenger.util.ColorUtils;

/**
 * Fragment for modifying app settings.
 */
public class SettingsFragment extends PreferenceFragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.settings);
        initBaseTheme();
        initGlobalTheme();
        initFontSize();
        initVibrate();
        initDeliveryReports();
        initSoundEffects();
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
                        ColorSet initialColors = ColorSet.getFromString(getActivity(),
                                Settings.get(getActivity()).themeColorString);
                        String colorString = (String) o;
                        new ApiUtils().updateGlobalThemeColor(Account.get(getActivity()).accountId,
                                colorString);

                        ColorSet colors = ColorSet.getFromString(getActivity(), colorString);
                        ColorUtils.animateToolbarColor(getActivity(),
                                initialColors.color, colors.color);
                        ColorUtils.animateStatusBarColor(getActivity(),
                                initialColors.colorDark, colors.colorDark);

                        return true;
                    }
                });
    }

    private void initBaseTheme() {
        findPreference(getString(R.string.pref_base_theme))
                .setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference preference, Object o) {
                        String newValue = (String) o;
                        if (!newValue.equals("day_night") && !newValue.equals("light")) {
                            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                        } else {
                            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                        }

                        new ApiUtils().updateBaseTheme(Account.get(getActivity()).accountId,
                                newValue);

                        getActivity().recreate();

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
                        new ApiUtils().updateFontSize(Account.get(getActivity()).accountId,
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
                        String pattern = (String) o;
                        new ApiUtils().updateVibrate(Account.get(getActivity()).accountId,
                                pattern);
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
                        new ApiUtils().updateDeliveryReports(Account.get(getActivity()).accountId,
                                delivery);
                        return true;
                    }
                });
    }

    private void initSoundEffects() {
        findPreference(getString(R.string.pref_sound_effects))
                .setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference preference, Object o) {
                        boolean effects = (boolean) o;
                        new ApiUtils().updateDeliveryReports(Account.get(getActivity()).accountId,
                                effects);
                        return true;
                    }
                });
    }

}
