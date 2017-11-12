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

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS
import android.support.v7.preference.Preference
import android.support.v7.preference.PreferenceCategory
import android.support.v7.preference.PreferenceFragmentCompat

import xyz.klinker.messenger.R

/**
 * Fragment for allowing the user to get some help from the devs or submit feedback. This will
 * contain links where the user can find help, either through a FAQs, Google+, Email, or Twitter.
 */
class HelpAndFeedbackFragment : MaterialPreferenceFragmentCompat() {

    override fun onCreatePreferences(bundle: Bundle?, s: String?) {
        addPreferencesFromResource(R.xml.help_and_feedback)

        findPreference(getString(R.string.pref_help_faqs))
                .setOnPreferenceClickListener {
                    openWeb("https://messenger.klinkerapps.com/help/")
                    true
                }

        findPreference(getString(R.string.pref_help_features))
                .setOnPreferenceClickListener {
                    openWeb("https://messenger.klinkerapps.com/overview/")
                    true
                }

        findPreference(getString(R.string.pref_help_google_plus))
                .setOnPreferenceClickListener {
                    openWeb("https://plus.google.com/u/0/communities/110320018522684513593")
                    true
                }

        findPreference(getString(R.string.pref_help_email))
                .setOnPreferenceClickListener {
                    displayEmail()
                    true
                }

        findPreference(getString(R.string.pref_help_twitter))
                .setOnPreferenceClickListener {
                    openWeb("https://twitter.com/KlinkerApps")
                    true
                }

        findPreference(getString(R.string.pref_help_issues))
                .setOnPreferenceClickListener {
                    openWeb("https://github.com/klinker-apps/messenger-issues")
                    true
                }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            findPreference(getString(R.string.pref_help_battery_optimization))
                    .setOnPreferenceClickListener {
                        val batteryOptimization = Intent(ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                        startActivity(batteryOptimization)

                        true
                    }
        } else {
            (findPreference(getString(R.string.pref_help_troubleshooting_category)) as PreferenceCategory)
                    .removePreference(findPreference(getString(R.string.pref_help_battery_optimization)))
        }
    }

    /**
     * Sends an email to support@klinkerapps.com
     */
    private fun displayEmail() {
        val email = arrayOf("luke@klinkerapps.com")
        val subject = getString(R.string.app_name) + " " + getString(R.string.support)

        val uri = Uri.parse("mailto:luke@klinkerapps.com")
                .buildUpon()
                .appendQueryParameter("subject", subject)
                .build()

        val emailIntent = Intent(Intent.ACTION_SENDTO, uri)
        emailIntent.putExtra(Intent.EXTRA_EMAIL, email)
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, subject)

        startActivity(Intent.createChooser(emailIntent, subject))
    }

    private fun openWeb(url: String) {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.data = Uri.parse(url)
        startActivity(intent)
    }

}
