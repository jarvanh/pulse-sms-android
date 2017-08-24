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
import android.preference.PreferenceManager;
import android.preference.RingtonePreference;
import android.preference.SwitchPreference;
import android.support.annotation.VisibleForTesting;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.util.Log;

import java.util.List;

import xyz.klinker.messenger.R;
import xyz.klinker.messenger.activity.ComposeActivity;
import xyz.klinker.messenger.activity.MessengerActivity;
import xyz.klinker.messenger.shared.activity.AbstractSettingsActivity;
import xyz.klinker.messenger.shared.data.DataSource;
import xyz.klinker.messenger.shared.data.Settings;
import xyz.klinker.messenger.shared.data.model.Contact;
import xyz.klinker.messenger.shared.data.model.Conversation;
import xyz.klinker.messenger.shared.util.ActivityUtils;
import xyz.klinker.messenger.shared.util.AndroidVersionUtil;
import xyz.klinker.messenger.shared.util.ColorUtils;
import xyz.klinker.messenger.shared.util.NotificationUtils;
import xyz.klinker.messenger.view.ColorPreference;

/**
 * Fragment for modifying contact preferences. This includes pinning, changing colors, changing
 * ringtone, and changing the group name.
 */
public class ContactSettingsFragment extends MaterialPreferenceFragment {

    private static final String ARG_CONVERSATION_ID = "conversation_id";

    public Conversation conversation;

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

        addPreferencesFromResource(R.xml.settings_contact);

        setUpToolbar();
        setUpPin();
        setUpMute();
        setUpPrivate();
        setUpGroupName();
        setUpEditRecipients();
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
        conversation = source.getConversation(getActivity(), getArguments().getLong(ARG_CONVERSATION_ID));
    }

    @VisibleForTesting
    DataSource getDataSource() {
        return DataSource.INSTANCE;
    }

    private void setUpDefaults() {
        if (conversation == null) {
            getActivity().finish();
            return;
        }

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
        Toolbar toolbar = ((AbstractSettingsActivity) getActivity()).getToolbar();

        if (toolbar != null) {
            if (settings.useGlobalThemeColor) {
                toolbar.setBackgroundColor(settings.mainColorSet.color);
                ActivityUtils.setStatusBarColor(getActivity(), settings.mainColorSet.colorDark);
            } else {
                toolbar.setBackgroundColor(conversation.colors.color);
                ActivityUtils.setStatusBarColor(getActivity(), conversation.colors.colorDark);
            }
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
        SwitchPreference privateNotifications = (SwitchPreference)
                findPreference(getString(R.string.pref_contact_private_conversation));
        Preference ringtone = findPreference(getString(R.string.pref_contact_ringtone));

        if (mute) {
            privateNotifications.setChecked(false);
            privateNotifications.setEnabled(false);

            if (ringtone != null) {
                ringtone.setEnabled(false);
            }

            conversation.privateNotifications = false;
        } else {
            if (privateNotifications != null) {
                privateNotifications.setEnabled(true);
            }

            if (ringtone != null) {
                ringtone.setEnabled(true);
            }
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

    private void setUpEditRecipients() {
        Preference preference = findPreference(getString(R.string.pref_contact_edit_recipients));

        if (!conversation.isGroup()) {
            getPreferenceScreen().removePreference(preference);
            return;
        }

        preference.setOnPreferenceClickListener(preference1 -> {
            final Intent editRecipients = new Intent(getActivity(), ComposeActivity.class);
            editRecipients.setAction(ComposeActivity.ACTION_EDIT_RECIPIENTS);
            editRecipients.putExtra(ComposeActivity.EXTRA_EDIT_RECIPIENTS_TITLE, conversation.title);
            editRecipients.putExtra(ComposeActivity.EXTRA_EDIT_RECIPIENTS_NUMBERS, conversation.phoneNumbers);

            startActivity(editRecipients);
            return true;
        });
    }

    private void setUpRingtone() {
        RingtonePreference preference = (RingtonePreference)
                findPreference(getString(R.string.pref_contact_ringtone));

        preference.setOnPreferenceChangeListener((preference1, o) -> {
            conversation.ringtoneUri = (String) o;
            Log.v("conversation_ringtone", "new ringtone: " + o);

            return true;
        });
    }

    private void setUpNotificationChannels() {
        Preference channelPref = findPreference(getString(R.string.pref_contact_notification_channel));
        Preference restoreDefaultsPref = findPreference(getString(R.string.pref_contact_notification_channel_restore_default));

        if (AndroidVersionUtil.isAndroidO()) {
            channelPref.setOnPreferenceClickListener(preference12 -> {
                NotificationUtils.createNotificationChannel(getActivity(), conversation);

                Intent intent = new Intent(android.provider.Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS);
                intent.putExtra(android.provider.Settings.EXTRA_CHANNEL_ID, conversation.id + "");
                intent.putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, getActivity().getPackageName());
                startActivity(intent);

                return true;
            });

            restoreDefaultsPref.setOnPreferenceClickListener(preference12 -> {
                NotificationUtils.deleteChannel(getActivity(), conversation.id);
                return true;
            });
        } else {
            PreferenceCategory notificationCategory = (PreferenceCategory)
                    findPreference(getString(R.string.pref_contact_notification_group));
            notificationCategory.removePreference(channelPref);
            notificationCategory.removePreference(restoreDefaultsPref);
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
        DataSource source = DataSource.INSTANCE;
        source.updateConversationSettings(getActivity(), conversation);

        List<Contact> contactList;
        if (conversation.phoneNumbers.contains(", ")) {
            contactList = source.getContactsByNames(getActivity(), conversation.title);
        } else {
            contactList = source.getContacts(getActivity(), conversation.phoneNumbers);
        }

        if (contactList.size() == 1) {
            // it is an individual conversation and we have the contact in our database! Yay.
            contactList.get(0).colors = conversation.colors;
            source.updateContact(getActivity(), contactList.get(0));
        }
    }
}
