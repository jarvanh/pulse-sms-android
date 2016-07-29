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

import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.RingtonePreference;
import android.preference.SwitchPreference;
import android.provider.Settings;
import android.support.annotation.VisibleForTesting;
import android.support.v7.app.AppCompatActivity;

import xyz.klinker.messenger.R;
import xyz.klinker.messenger.data.DataSource;
import xyz.klinker.messenger.data.model.Conversation;

/**
 * Fragment for modifying contact preferences. This includes pinning, changing colors, changing
 * ringtone, and changing the group name.
 */
public class ContactSettingsFragment extends PreferenceFragment {

    private static final String ARG_CONVERSATION_ID = "conversation_id";

    private Conversation conversation;

    public static ContactSettingsFragment newInstance(long conversationId) {
        ContactSettingsFragment fragment = new ContactSettingsFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_CONVERSATION_ID, conversationId);
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        loadConversation();
        setUpDefaults();

        addPreferencesFromResource(R.xml.contact_settings);

        setUpToolbar();
        setUpPin();
        setUpGroupName();
        setUpRingtone();
    }

    private void loadConversation() {
        DataSource source = getDataSource();
        source.open();
        conversation = source.getConversation(getArguments().getLong(ARG_CONVERSATION_ID));
        source.close();
    }

    @VisibleForTesting
    DataSource getDataSource() {
        return DataSource.getInstance(getActivity());
    }

    private void setUpDefaults() {
        PreferenceManager.getDefaultSharedPreferences(getActivity()).edit()
                .putBoolean(getString(R.string.pref_pin_conversation), conversation.pinned)
                .putString(getString(R.string.pref_group_name), conversation.title)
                .putString(getString(R.string.pref_ringtone),
                        conversation.ringtoneUri == null ?
                                Settings.System.DEFAULT_NOTIFICATION_URI.toString() :
                                conversation.ringtoneUri)
                .putInt(getString(R.string.pref_primary_color), conversation.colors.color)
                .putInt(getString(R.string.pref_primary_dark_color), conversation.colors.colorDark)
                .putInt(getString(R.string.pref_primary_light_color), conversation.colors.colorLight)
                .putInt(getString(R.string.pref_accent_color), conversation.colors.colorAccent)
                .apply();
    }

    private void setUpToolbar() {
        getActivity().setTitle(conversation.title);
        ((AppCompatActivity) getActivity()).getSupportActionBar()
                .setBackgroundDrawable(new ColorDrawable(conversation.colors.color));
        getActivity().getWindow().setStatusBarColor(conversation.colors.colorDark);
    }

    private void setUpPin() {
        SwitchPreference preference = (SwitchPreference)
                findPreference(getString(R.string.pref_pin_conversation));
        preference.setChecked(conversation.pinned);

        preference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object o) {
                conversation.pinned = (boolean) o;
                return true;
            }
        });
    }

    private void setUpGroupName() {
        EditTextPreference preference = (EditTextPreference)
                findPreference(getString(R.string.pref_group_name));

        if (!conversation.isGroup()) {
            getPreferenceScreen().removePreference(preference);
            return;
        }

        preference.setSummary(conversation.title);
        preference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object o) {
                conversation.title = (String) o;
                preference.setSummary(conversation.title);
                return true;
            }
        });
    }

    private void setUpRingtone() {
        RingtonePreference preference = (RingtonePreference)
                findPreference(getString(R.string.pref_ringtone));

        preference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object o) {
                conversation.ringtoneUri = (String) o;
                return true;
            }
        });
    }

    public void saveSettings() {
        DataSource source = DataSource.getInstance(getActivity());
        source.open();
        source.updateConversationSettings(conversation);
        source.close();
    }

}
