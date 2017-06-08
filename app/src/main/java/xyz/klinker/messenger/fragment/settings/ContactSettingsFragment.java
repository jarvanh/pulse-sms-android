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

import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.RingtonePreference;
import android.preference.SwitchPreference;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.support.v7.app.AppCompatActivity;
import android.text.InputType;
import android.view.View;
import android.widget.ListView;

import java.util.List;

import xyz.klinker.messenger.R;
import xyz.klinker.messenger.shared.data.ColorSet;
import xyz.klinker.messenger.shared.data.DataSource;
import xyz.klinker.messenger.shared.data.Settings;
import xyz.klinker.messenger.shared.data.model.Contact;
import xyz.klinker.messenger.shared.data.model.Conversation;
import xyz.klinker.messenger.shared.util.AndroidVersionUtil;
import xyz.klinker.messenger.shared.util.ColorUtils;
import xyz.klinker.messenger.shared.util.DensityUtil;
import xyz.klinker.messenger.shared.util.listener.ColorSelectedListener;
import xyz.klinker.messenger.view.ColorPreference;

/**
 * Fragment for modifying contact preferences. This includes pinning, changing colors, changing
 * ringtone, and changing the group name.
 */
public class ContactSettingsFragment extends MaterialPreferenceFragment {

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
        setUpMute();
        setUpPrivate();
        setUpGroupName();
        setUpRingtone();
        setUpNotificationChannels();
        setUpColors();

        if (Settings.get(getActivity()).useGlobalThemeColor) {
            // remove the color customizations since they don't apply to anything except group messages
            PreferenceCategory customizationCategory = (PreferenceCategory)
                    findPreference(getString(R.string.pref_contact_customization_group));
            getPreferenceScreen().removePreference(customizationCategory);
        }

        if (AndroidVersionUtil.isAndroidO()) {
            // remove the LED customizations since the user won't be able to configure this here, they will
            // have to go through channels instead.
            // The ringtone pref has been converted to push them to notification channel settings
            PreferenceCategory notificationCategory = (PreferenceCategory)
                    findPreference(getString(R.string.pref_contact_notification_group));
            notificationCategory.removePreference(findPreference(getString(R.string.pref_contact_led_color)));
            notificationCategory.removePreference(findPreference(getString(R.string.pref_contact_ringtone)));
        }
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
                .putBoolean(getString(R.string.pref_contact_pin_conversation), conversation.pinned)
                .putBoolean(getString(R.string.pref_contact_private_conversation), conversation.privateNotifications)
                .putString(getString(R.string.pref_contact_group_name), conversation.title)
                .putString(getString(R.string.pref_contact_ringtone),
                        conversation.ringtoneUri == null ?
                                Settings.get(getActivity()).ringtone :
                                conversation.ringtoneUri)
                .putInt(getString(R.string.pref_contact_primary_color), conversation.colors.color)
                .putInt(getString(R.string.pref_contact_primary_dark_color), conversation.colors.colorDark)
                .putInt(getString(R.string.pref_contact_primary_light_color), conversation.colors.colorLight)
                .putInt(getString(R.string.pref_contact_accent_color), conversation.colors.colorAccent)
                .putInt(getString(R.string.pref_contact_led_color), conversation.ledColor)
                .apply();
    }

    private void setUpToolbar() {
        getActivity().setTitle(conversation.title);

        Settings settings = Settings.get(getActivity());
        if (settings.useGlobalThemeColor) {
            ((AppCompatActivity) getActivity()).getSupportActionBar()
                    .setBackgroundDrawable(new ColorDrawable(settings.globalColorSet.color));
            getActivity().getWindow().setStatusBarColor(settings.globalColorSet.colorDark);
        } else {
            ((AppCompatActivity) getActivity()).getSupportActionBar()
                    .setBackgroundDrawable(new ColorDrawable(conversation.colors.color));
            getActivity().getWindow().setStatusBarColor(conversation.colors.colorDark);
        }
    }

    private void setUpPin() {
        SwitchPreference preference = (SwitchPreference)
                findPreference(getString(R.string.pref_contact_pin_conversation));
        preference.setChecked(conversation.pinned);

        preference.setOnPreferenceChangeListener((preference1, o) -> {
            conversation.pinned = (boolean) o;
            return true;
        });
    }

    private void setUpMute() {
        SwitchPreference preference = (SwitchPreference)
                findPreference(getString(R.string.pref_contact_mute_conversation));
        preference.setChecked(conversation.mute);
        enableNotificationBasedOnMute(conversation.mute);

        preference.setOnPreferenceChangeListener((preference1, o) -> {
            conversation.mute = (boolean) o;
            enableNotificationBasedOnMute(conversation.mute);

            return true;
        });
    }

    private void enableNotificationBasedOnMute(boolean mute) {
        if (mute) {
            SwitchPreference privateNotifications = (SwitchPreference)
                    findPreference(getString(R.string.pref_contact_private_conversation));

            privateNotifications.setChecked(false);
            privateNotifications.setEnabled(false);
            findPreference(getString(R.string.pref_contact_ringtone)).setEnabled(false);

            conversation.privateNotifications = false;
        } else {
            findPreference(getString(R.string.pref_contact_private_conversation)).setEnabled(true);
            findPreference(getString(R.string.pref_contact_ringtone)).setEnabled(true);
        }
    }

    private void setUpPrivate() {
        SwitchPreference preference = (SwitchPreference)
                findPreference(getString(R.string.pref_contact_private_conversation));
        preference.setChecked(conversation.privateNotifications);

        preference.setOnPreferenceChangeListener((preference1, o) -> {
            conversation.privateNotifications = (boolean) o;
            return true;
        });
    }

    private void setUpGroupName() {
        EditTextPreference preference = (EditTextPreference)
                findPreference(getString(R.string.pref_contact_group_name));

        if (!conversation.isGroup()) {
            getPreferenceScreen().removePreference(preference);
            return;
        }

        preference.getEditText().setInputType(InputType.TYPE_TEXT_FLAG_CAP_WORDS|InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        preference.setSummary(conversation.title);
        preference.setOnPreferenceChangeListener((preference1, o) -> {
            conversation.title = (String) o;
            preference1.setSummary(conversation.title);
            return true;
        });
    }

    private void setUpRingtone() {
        RingtonePreference preference = (RingtonePreference)
                findPreference(getString(R.string.pref_contact_ringtone));

        preference.setOnPreferenceChangeListener((preference1, o) -> {
            conversation.ringtoneUri = (String) o;
            return true;
        });
    }

    private void setUpNotificationChannels() {
        Preference preference = findPreference(getString(R.string.pref_contact_notification_channel));

        if (AndroidVersionUtil.isAndroidO()) {
            preference.setOnPreferenceClickListener(preference12 -> {
                Intent intent = new Intent(android.provider.Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS);
                intent.putExtra(android.provider.Settings.EXTRA_CHANNEL_ID, conversation.id + "");
                intent.putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, getActivity().getPackageName());
                startActivity(intent);

                return true;
            });
        } else {
            PreferenceCategory notificationCategory = (PreferenceCategory)
                    findPreference(getString(R.string.pref_contact_notification_group));
            notificationCategory.removePreference(preference);
        }
    }

    private void setUpColors() {
        final ColorPreference preference = (ColorPreference)
                findPreference(getString(R.string.pref_contact_primary_color));
        final ColorPreference darkColorPreference = (ColorPreference)
                findPreference(getString(R.string.pref_contact_primary_dark_color));
        final ColorPreference accentColorPreference = (ColorPreference)
                findPreference(getString(R.string.pref_contact_accent_color));
        final ColorPreference ledColorPreference = (ColorPreference)
                findPreference(getString(R.string.pref_contact_led_color));

        preference.setOnPreferenceChangeListener((preference1, o) -> {
            ColorUtils.animateToolbarColor(getActivity(), conversation.colors.color, (int) o);
            conversation.colors.color = (int) o;
            return true;
        });

        preference.setColorSelectedListener(colors -> {
            darkColorPreference.setColor(colors.colorDark);
            accentColorPreference.setColor(colors.colorAccent);
        });

        darkColorPreference.setOnPreferenceChangeListener((preference12, o) -> {
            ColorUtils.animateStatusBarColor(getActivity(), conversation.colors.colorDark, (int) o);
            conversation.colors.colorDark = (int) o;
            return true;
        });

        accentColorPreference.setOnPreferenceChangeListener((preference13, o) -> {
            conversation.colors.colorAccent = (int) o;
            return true;
        });

        ledColorPreference.setOnPreferenceChangeListener((preference14, newValue) -> {
            conversation.ledColor = (int) newValue;
            return true;
        });
    }

    public void saveSettings() {
        DataSource source = DataSource.getInstance(getActivity());
        source.open();
        source.updateConversationSettings(conversation);

        List<Contact> contactList;
        if (conversation.phoneNumbers.contains(", ")) {
            contactList = source.getContactsByNames(conversation.title);
        } else {
            contactList = source.getContacts(conversation.phoneNumbers);
        }

        if (contactList.size() == 1) {
            // it is an individual conversation and we have the contact in our database! Yay.
            contactList.get(0).colors = conversation.colors;
            source.updateContact(contactList.get(0));
        }

        source.close();
    }

}
