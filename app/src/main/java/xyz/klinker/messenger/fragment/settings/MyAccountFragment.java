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
import android.support.v4.app.Fragment;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;

import xyz.klinker.messenger.R;
import xyz.klinker.messenger.data.DataSource;

/**
 * Fragment for displaying information about the user's account. We can display different stats
 * for the user here, along with subscription status.
 */
public class MyAccountFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(Bundle bundle, String s) {
        addPreferencesFromResource(R.xml.my_account);

        initMessageCountPreference();
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

}