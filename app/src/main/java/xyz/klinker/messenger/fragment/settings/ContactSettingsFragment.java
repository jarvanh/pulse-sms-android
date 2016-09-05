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

import android.animation.ValueAnimator;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.RingtonePreference;
import android.preference.SwitchPreference;
import android.support.annotation.VisibleForTesting;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;

import java.util.Set;

import xyz.klinker.messenger.R;
import xyz.klinker.messenger.data.ColorSet;
import xyz.klinker.messenger.data.DataSource;
import xyz.klinker.messenger.data.Settings;
import xyz.klinker.messenger.data.model.Conversation;
import xyz.klinker.messenger.util.listener.ColorSelectedListener;
import xyz.klinker.messenger.view.ColorPreference;

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
        setUpMute();
        setUpGroupName();
        setUpRingtone();
        setUpColors();
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
                .putString(getString(R.string.pref_contact_group_name), conversation.title)
                .putString(getString(R.string.pref_contact_ringtone),
                        conversation.ringtoneUri == null ?
                                Settings.get(getActivity()).ringtone :
                                conversation.ringtoneUri)
                .putInt(getString(R.string.pref_contact_primary_color), conversation.colors.color)
                .putInt(getString(R.string.pref_contact_primary_dark_color), conversation.colors.colorDark)
                .putInt(getString(R.string.pref_contact_primary_light_color), conversation.colors.colorLight)
                .putInt(getString(R.string.pref_contact_accent_color), conversation.colors.colorAccent)
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

        preference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object o) {
                conversation.pinned = (boolean) o;
                return true;
            }
        });
    }

    private void setUpMute() {
        SwitchPreference preference = (SwitchPreference)
                findPreference(getString(R.string.pref_contact_mute_conversation));
        preference.setChecked(conversation.mute);

        preference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object o) {
                conversation.mute = (boolean) o;
                return true;
            }
        });
    }

    private void setUpGroupName() {
        EditTextPreference preference = (EditTextPreference)
                findPreference(getString(R.string.pref_contact_group_name));

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
                findPreference(getString(R.string.pref_contact_ringtone));

        preference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object o) {
                conversation.ringtoneUri = (String) o;
                return true;
            }
        });
    }

    private void setUpColors() {
        ColorPreference preference = (ColorPreference)
                findPreference(getString(R.string.pref_contact_primary_color));
        final ColorPreference darkColorPreference = (ColorPreference)
                findPreference(getString(R.string.pref_contact_primary_dark_color));
        final ColorPreference accentColorPreference = (ColorPreference)
                findPreference(getString(R.string.pref_contact_accent_color));

        preference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object o) {
                animateToolbar(conversation.colors.color, (int) o);
                conversation.colors.color = (int) o;
                return true;
            }
        });
        preference.setColorSelectedListener(new ColorSelectedListener() {
            @Override
            public void onColorSelected(ColorSet colors) {
                darkColorPreference.setColor(colors.colorDark);
                accentColorPreference.setColor(colors.colorAccent);
            }
        });

        darkColorPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object o) {
                animateStatusBar(conversation.colors.colorDark, (int) o);
                conversation.colors.colorDark = (int) o;
                return true;
            }
        });

        accentColorPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object o) {
                conversation.colors.colorAccent = (int) o;
                return true;
            }
        });
    }

    private void animateToolbar(int originalColor, int newColor) {
        final ColorDrawable drawable = new ColorDrawable(originalColor);
        final ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
        actionBar.setBackgroundDrawable(drawable);

        ValueAnimator animator = ValueAnimator.ofArgb(originalColor, newColor);
        animator.setDuration(200);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                int color = (int) valueAnimator.getAnimatedValue();
                drawable.setColor(color);
                actionBar.setBackgroundDrawable(drawable);
            }
        });
        animator.start();
    }

    private void animateStatusBar(int originalColor, int newColor) {
        ValueAnimator animator = ValueAnimator.ofArgb(originalColor, newColor);
        animator.setDuration(200);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                int color = (int) valueAnimator.getAnimatedValue();
                getActivity().getWindow().setStatusBarColor(color);
            }
        });
        animator.start();
    }

    public void saveSettings() {
        DataSource source = DataSource.getInstance(getActivity());
        source.open();
        source.updateConversationSettings(conversation);
        source.close();
    }

}
