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

import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context.CLIPBOARD_SERVICE
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.support.v4.app.FragmentActivity
import android.widget.Toast
import xyz.klinker.messenger.R
import xyz.klinker.messenger.adapter.ChangelogAdapter
import xyz.klinker.messenger.adapter.OpenSourceAdapter
import xyz.klinker.messenger.shared.util.xml.ChangelogParser
import xyz.klinker.messenger.shared.util.xml.OpenSourceParser

/**
 * Fragment for displaying information about the app.
 */
class AboutFragment : MaterialPreferenceFragmentCompat() {

    private val fragmentActivity: FragmentActivity? by lazy { activity }

    /**
     * Gets the version name associated with the current build.
     *
     * @return the version name.
     */
    val versionName: String?
        get() = try {
            fragmentActivity?.packageManager
                    ?.getPackageInfo(fragmentActivity?.packageName, 0)
                    ?.versionName
        } catch (e: Exception) {
            null
        }

    /**
     * Gets device info (manufacturer and model).
     *
     * @return the device info.
     */
    private val deviceInfo: String
        get() = Build.MANUFACTURER + ", " + Build.MODEL

    override fun onCreatePreferences(bundle: Bundle?, s: String?) {
        addPreferencesFromResource(R.xml.about)

        findPreference(getString(R.string.pref_about_app_version)).summary = versionName
        findPreference(getString(R.string.pref_about_device_info)).summary = deviceInfo

        findPreference(getString(R.string.pref_about_app_version))
                .setOnPreferenceClickListener {
                    copyToClipboard(versionName)
                    true
                }

        findPreference(getString(R.string.pref_about_changelog))
                .setOnPreferenceClickListener {
                    displayChangelog()
                    true
                }

        findPreference(getString(R.string.pref_about_copyright))
                .setOnPreferenceClickListener {
                    displayOpenSource()
                    true
                }

        findPreference(getString(R.string.pref_about_privacy_policy))
                .setOnPreferenceClickListener {
                    openWebsite("https://messenger.klinkerapps.com/privacy.html")
                    true
                }

        findPreference(getString(R.string.pref_website))
                .setOnPreferenceClickListener {
                    openWebsite("https://messenger.klinkerapps.com")
                    false
                }

        findPreference(getString(R.string.pref_supported_platforms))
                .setOnPreferenceClickListener {
                    openWebsite("https://messenger.klinkerapps.com/overview")
                    true
                }

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
     * Copy app version to clipboard
     */
    private fun copyToClipboard(text: String?) {
        val clipboard = fragmentActivity?.getSystemService(CLIPBOARD_SERVICE) as ClipboardManager?
        val clip = ClipData.newPlainText("app_version", text)
        clipboard?.primaryClip = clip
        Toast.makeText(fragmentActivity, R.string.message_copied_to_clipboard,
                Toast.LENGTH_SHORT).show()
    }

    /**
     * Shows the apps changelog in a dialog box.
     */
    private fun displayChangelog() {
        if (fragmentActivity == null) {
            return
        }

        AlertDialog.Builder(fragmentActivity!!)
                .setTitle(R.string.changelog)
                .setAdapter(ChangelogAdapter(fragmentActivity!!, ChangelogParser.parse(fragmentActivity!!)!!), null)
                .setPositiveButton(android.R.string.ok, null)
                .show()
    }

    /**
     * Displays information from the open source libraries used in the project.
     */
    private fun displayOpenSource() {
        if (fragmentActivity == null) {
            return
        }

        AlertDialog.Builder(fragmentActivity!!)
                .setAdapter(OpenSourceAdapter(fragmentActivity!!, OpenSourceParser.parse(fragmentActivity!!)!!), null)
                .setPositiveButton(android.R.string.ok, null)
                .show()
    }

    /**
     * Displays a website from a url
     *
     * @param url
     */
    private fun openWebsite(url: String) {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.data = Uri.parse(url)
        startActivity(intent)
    }

}
