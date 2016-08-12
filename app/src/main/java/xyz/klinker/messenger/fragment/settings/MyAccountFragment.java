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
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;

import xyz.klinker.messenger.R;
import xyz.klinker.messenger.api.implementation.LoginActivity;
import xyz.klinker.messenger.data.DataSource;
import xyz.klinker.messenger.data.Settings;
import xyz.klinker.messenger.util.StringUtils;

/**
 * Fragment for displaying information about the user's account. We can display different stats
 * for the user here, along with subscription status.
 */
public class MyAccountFragment extends PreferenceFragmentCompat {

    private static final int SETUP_REQUEST = 54321;

    @Override
    public void onCreatePreferences(Bundle bundle, String s) {
        addPreferencesFromResource(R.xml.my_account);

        if (initSetupPreference()) {
            findPreference(getString(R.string.pref_about_device_id)).setSummary(getDeviceId());
            initMessageCountPreference();
        }
    }

    private boolean initSetupPreference() {
        Preference preference = findPreference(getString(R.string.pref_my_account_setup));
        Settings settings = Settings.get(getActivity());

        if (preference != null) {
            preference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    Intent intent = new Intent(getContext(), LoginActivity.class);
                    startActivityForResult(intent, SETUP_REQUEST);
                    return true;
                }
            });

            getPreferenceScreen()
                    .removePreference(findPreference(getString(R.string.pref_message_count)));
            getPreferenceScreen()
                    .removePreference(findPreference(getString(R.string.pref_about_device_id)));

            return false;
        } else if (preference != null) {
            //getPreferenceScreen().removePreference(preference);
            return true;
        } else {
            return true;
        }
    }

    private void initMessageCountPreference() {
        Preference preference = findPreference(getString(R.string.pref_message_count));

        DataSource source = DataSource.getInstance(getContext());
        source.open();
        int conversationCount = source.getConversationCount();
        int messageCount = source.getMessageCount();
        source.close();

        String title = getResources().getQuantityString(R.plurals.message_count, messageCount,
                messageCount);
        String summary = getResources().getQuantityString(R.plurals.conversation_count,
                conversationCount, conversationCount);

        preference.setTitle(title);
        preference.setSummary(summary);
    }

    /**
     * Gets a device id for this device. This will be a 32-bit random hex value.
     *
     * @return the device id.
     */
    private String getDeviceId() {
        return Settings.get(getContext()).deviceId;
    }

    @Override
    public void onActivityResult(int requestCode, int responseCode, Intent data) {
        Settings.get(getActivity()).forceUpdate();
        if (requestCode == SETUP_REQUEST && responseCode == Activity.RESULT_OK) {
            initSetupPreference();
        }
    }

}