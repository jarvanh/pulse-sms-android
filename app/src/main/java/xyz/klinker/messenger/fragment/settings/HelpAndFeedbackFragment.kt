/*
 * Copyright (C) 2020 Luke Klinker
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

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS
import android.view.View
import androidx.preference.PreferenceCategory

import xyz.klinker.messenger.R
import xyz.klinker.messenger.activity.MessengerActivity

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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val messengerActivity = activity
        if (messengerActivity is MessengerActivity) {
            messengerActivity.insetController.modifyPreferenceFragmentElements(this)
        }
    }

    /**
     * Sends an email to pulsesmsapp@gmail.com
     */
    private fun displayEmail() {
        val email = arrayOf("pulsesmsapp@gmail.com")
        val subject = getString(R.string.app_name) + " " + getString(R.string.support)

        val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:")
            putExtra(Intent.EXTRA_EMAIL, email)
            putExtra(Intent.EXTRA_SUBJECT, subject)
        }

        startActivity(Intent.createChooser(emailIntent, subject))
    }

    private fun openWeb(url: String) {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.data = Uri.parse(url)

        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
        }
    }

}
