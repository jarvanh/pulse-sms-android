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

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;

import xyz.klinker.messenger.R;

/**
 * Fragment for allowing the user to get some help from the devs or submit feedback. This will
 * contain links where the user can find help, either through a FAQs, Google+, Email, or Twitter.
 */
public class HelpAndFeedbackFragment extends MaterialPreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(Bundle bundle, String s) {
        addPreferencesFromResource(R.xml.help_and_feedback);

        findPreference(getString(R.string.pref_help_faqs))
                .setOnPreferenceClickListener(preference -> {
                    openWeb("https://messenger.klinkerapps.com/faq.html");
                    return true;
                });

        findPreference(getString(R.string.pref_help_features))
                .setOnPreferenceClickListener(preference -> {
                    openWeb("https://messenger.klinkerapps.com/features.html");
                    return true;
                });

        findPreference(getString(R.string.pref_help_google_plus))
                .setOnPreferenceClickListener(preference -> {
                    openWeb("https://plus.google.com/u/0/communities/110320018522684513593");
                    return true;
                });

        findPreference(getString(R.string.pref_help_email))
                .setOnPreferenceClickListener(preference -> {
                    displayEmail();
                    return true;
                });

        findPreference(getString(R.string.pref_help_twitter))
                .setOnPreferenceClickListener(preference -> {
                    openWeb("https://twitter.com/KlinkerApps");
                    return true;
                });

        findPreference(getString(R.string.pref_help_issues))
                .setOnPreferenceClickListener(preference -> {
                    openWeb("https://github.com/klinker-apps/messenger-issues");
                    return true;
                });
    }

    /**
     * Sends an email to support@klinkerapps.com
     */
    public void displayEmail() {
        String[] email = new String[]{"luke@klinkerapps.com"};
        String subject = getString(R.string.app_name) + " " + getString(R.string.support);

        Uri uri = Uri.parse("mailto:luke@klinkerapps.com")
                .buildUpon()
                .appendQueryParameter("subject", subject)
                .build();

        Intent emailIntent = new Intent(Intent.ACTION_SENDTO, uri);
        emailIntent.putExtra(Intent.EXTRA_EMAIL, email);
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, subject);

        startActivity(Intent.createChooser(emailIntent, subject));
    }

    private void openWeb(String url) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(url));
        startActivity(intent);
    }

}
