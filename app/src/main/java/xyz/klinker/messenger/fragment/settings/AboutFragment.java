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

package xyz.klinker.messenger.fragment.settings;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.widget.Toast;

import xyz.klinker.messenger.R;
import xyz.klinker.messenger.adapter.ChangelogAdapter;
import xyz.klinker.messenger.adapter.OpenSourceAdapter;
import xyz.klinker.messenger.shared.util.xml.ChangelogParser;
import xyz.klinker.messenger.shared.util.xml.OpenSourceParser;
import xyz.klinker.messenger.utils.multi_select.MessageMultiSelectDelegate;

import static android.content.Context.CLIPBOARD_SERVICE;

/**
 * Fragment for displaying information about the app.
 */
public class AboutFragment extends MaterialPreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(Bundle bundle, String s) {
        addPreferencesFromResource(R.xml.about);

        findPreference(getString(R.string.pref_about_app_version)).setSummary(getVersionName());
        findPreference(getString(R.string.pref_about_device_info)).setSummary(getDeviceInfo());

        findPreference(getString(R.string.pref_about_app_version))
                .setOnPreferenceClickListener(preference -> {
                    copyToClipboard(getVersionName());
                    return true;
                });

        findPreference(getString(R.string.pref_about_changelog))
                .setOnPreferenceClickListener(preference -> {
                    displayChangelog();
                    return true;
                });

        findPreference(getString(R.string.pref_about_copyright))
                .setOnPreferenceClickListener(preference -> {
                    displayOpenSource();
                    return true;
                });

        findPreference(getString(R.string.pref_about_privacy_policy))
                .setOnPreferenceClickListener(preference -> {
                    openWebsite("https://messenger.klinkerapps.com/privacy.html");
                    return true;
                });

        findPreference(getString(R.string.pref_website))
                .setOnPreferenceClickListener(preference -> {
                    openWebsite("https://messenger.klinkerapps.com");
                    return false;
                });

        findPreference(getString(R.string.pref_supported_platforms))
                .setOnPreferenceClickListener(preference -> {
                    openWebsite("https://messenger.klinkerapps.com/supported_platforms.html");
                    return true;
                });

        /*findPreference(getString(R.string.pref_about_beta))
                .setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        displayIssueTracker();
                        return true;
                    }
                });*/
    }

    /**
     * Gets the version name associated with the current build.
     *
     * @return the version name.
     */
    public String getVersionName() {
        try {
            return getActivity().getPackageManager()
                    .getPackageInfo(getActivity().getPackageName(), 0).versionName;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Gets device info (manufacturer and model).
     *
     * @return the device info.
     */
    public String getDeviceInfo() {
        return Build.MANUFACTURER + ", " + Build.MODEL;
    }

    /**
     * Copy app version to clipboard
     */
    public void copyToClipboard(String text) {
        ClipboardManager clipboard = (ClipboardManager)
                getActivity().getSystemService(CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("app_version", text);
        clipboard.setPrimaryClip(clip);
        Toast.makeText(getActivity(), R.string.message_copied_to_clipboard,
                Toast.LENGTH_SHORT).show();
    }

    /**
     * Shows the apps changelog in a dialog box.
     */
    public void displayChangelog() {
        Activity activity = getActivity();

        new AlertDialog.Builder(activity)
                .setTitle(R.string.changelog)
                .setAdapter(new ChangelogAdapter(activity, ChangelogParser.parse(activity)), null)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    /**
     * Displays information from the open source libraries used in the project.
     */
    public void displayOpenSource() {
        Activity activity = getActivity();

        new AlertDialog.Builder(activity)
                .setAdapter(new OpenSourceAdapter(activity, OpenSourceParser.parse(activity)), null)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    /**
     * Displays a website from a url
     *
     * @param url
     */
    private void openWebsite(String url) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(url));
        startActivity(intent);
    }

}
