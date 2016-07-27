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

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;

import xyz.klinker.messenger.R;
import xyz.klinker.messenger.adapter.FaqsAdapter;
import xyz.klinker.messenger.util.xml.FaqsParser;

/**
 * Fragment for allowing the user to get some help from the devs or submit feedback. This will
 * contain links where the user can find help, either through a FAQs, Google+, Email, or Twitter.
 */
public class HelpAndFeedbackFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(Bundle bundle, String s) {
        addPreferencesFromResource(R.xml.help_and_feedback);

        findPreference(getString(R.string.pref_help_faqs))
                .setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        displayFaqs();
                        return true;
                    }
                });

        findPreference(getString(R.string.pref_help_google_plus))
                .setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        displayGooglePlus();
                        return true;
                    }
                });

        findPreference(getString(R.string.pref_help_email))
                .setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        displayEmail();
                        return true;
                    }
                });

        findPreference(getString(R.string.pref_help_twitter))
                .setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        displayTwitter();
                        return true;
                    }
                });
    }

    /**
     * Displays the FAQs.
     */
    public void displayFaqs() {
        Activity activity = getActivity();

        new AlertDialog.Builder(activity)
                .setTitle(R.string.faqs)
                .setAdapter(new FaqsAdapter(activity, FaqsParser.parse(activity)), null)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    /**
     * Displays Google+ community.
     */
    public void displayGooglePlus() {
        String url = "https://plus.google.com/+KlinkerAppsIowaCity";
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(url));
        startActivity(intent);
    }

    /**
     * Sends an email to support@klinkerapps.com
     */
    public void displayEmail() {
        String[] email = new String[]{"support@klinkerapps.com"};
        String subject = getString(R.string.app_name) + " " + getString(R.string.support);

        Uri uri = Uri.parse("mailto:support@klinkerapps.com")
                .buildUpon()
                .appendQueryParameter("subject", subject)
                .build();

        Intent emailIntent = new Intent(Intent.ACTION_SENDTO, uri);
        emailIntent.putExtra(Intent.EXTRA_EMAIL, email);
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, subject);

        startActivity(Intent.createChooser(emailIntent, subject));
    }

    /**
     * Display's @KlinkerApps Twitter page.
     */
    public void displayTwitter() {
        String url = "https://twitter.com/KlinkerApps";
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(url));
        startActivity(intent);
    }

}
