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
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v7.app.AlertDialog;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;

import xyz.klinker.messenger.R;
import xyz.klinker.messenger.activity.MessengerActivity;
import xyz.klinker.messenger.activity.OnboardingPayWallActivity;
import xyz.klinker.messenger.api.implementation.ApiUtils;
import xyz.klinker.messenger.api.implementation.LoginActivity;
import xyz.klinker.messenger.api.implementation.Account;
import xyz.klinker.messenger.data.DataSource;
import xyz.klinker.messenger.data.Settings;
import xyz.klinker.messenger.service.ApiDownloadService;
import xyz.klinker.messenger.service.ApiUploadService;
import xyz.klinker.messenger.util.StringUtils;

/**
 * Fragment for displaying information about the user's account. We can display different stats
 * for the user here, along with subscription status.
 */
public class MyAccountFragment extends PreferenceFragmentCompat {

    public static final int ONBOARDING_REQUEST = 54320;
    public static final int RESPONSE_START_TRIAL = 101;
    public static final int RESPONSE_SKIP_TRIAL_FOR_NOW = 102;

    private static final int SETUP_REQUEST = 54321;

    @Override
    public void onCreatePreferences(Bundle bundle, String s) {
        addPreferencesFromResource(R.xml.my_account);

        if (initSetupPreference()) {
            findPreference(getString(R.string.pref_about_device_id)).setSummary(getDeviceId());
            initMessageCountPreference();
            initRemoveAccountPreference();
            initResyncAccountPreference();
        } else {
            startActivityForResult(
                    new Intent(getActivity(), OnboardingPayWallActivity.class),
                    ONBOARDING_REQUEST);
        }
    }

    private boolean initSetupPreference() {
        Preference preference = findPreference(getString(R.string.pref_my_account_setup));
        Account account = Account.get(getActivity());

        if ((account.accountId == null || account.deviceId == null) && preference != null) {
            preference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    Intent intent = new Intent(getContext(), LoginActivity.class);
                    startActivityForResult(intent, SETUP_REQUEST);
                    return true;
                }
            });

            removeAccountOptions();
            return false;
        } else if (preference != null) {
            getPreferenceScreen().removePreference(preference);
            return true;
        } else {
            return true;
        }
    }

    private void removeAccountOptions() {
        try {
            getPreferenceScreen()
                    .removePreference(findPreference(getString(R.string.pref_message_count)));
            getPreferenceScreen()
                    .removePreference(findPreference(getString(R.string.pref_about_device_id)));
            getPreferenceScreen()
                    .removePreference(findPreference(getString(R.string.pref_delete_account)));
            getPreferenceScreen()
                    .removePreference(findPreference(getString(R.string.pref_resync_account)));
        } catch (Exception e) {

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

    private void initRemoveAccountPreference() {
        Preference preference = findPreference(getString(R.string.pref_delete_account));

        preference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                new AlertDialog.Builder(getActivity())
                        .setMessage(R.string.delete_account_confirmation)
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                final Account account = Account.get(getActivity());
                                final String accountId = account.accountId;
                                account.clearAccount();

                                new Thread(new Runnable() {
                                    @Override
                                    public void run() {
                                        new ApiUtils().deleteAccount(accountId);
                                    }
                                }).start();

                                returnToConversations();

                                NavigationView nav = (NavigationView) getActivity().findViewById(R.id.navigation_view);
                                if (nav != null) {
                                    nav.getMenu().findItem(R.id.drawer_account).setTitle(R.string.menu_device_texting);
                                }
                            }
                        })
                        .setNegativeButton(android.R.string.no, null)
                        .show();

                return true;
            }
        });
    }

    private void initResyncAccountPreference() {
        Preference preference = findPreference(getString(R.string.pref_resync_account));

        if (Account.get(getActivity()).primary) {
            getPreferenceScreen().removePreference(preference);
        } else {
            preference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    new AlertDialog.Builder(getActivity())
                            .setMessage(R.string.resync_account_confirmation)
                            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    getActivity()
                                            .startService(new Intent(getActivity(), ApiDownloadService.class));
                                }
                            })
                            .setNegativeButton(android.R.string.no, null)
                            .show();

                    return true;
                }
            });
        }
    }

    /**
     * Gets a device id for this device. This will be a 32-bit random hex value.
     *
     * @return the device id.
     */
    private String getDeviceId() {
        return Account.get(getContext()).deviceId;
    }

    @Override
    public void onActivityResult(int requestCode, int responseCode, Intent data) {
        Settings.get(getActivity()).forceUpdate();
        if (requestCode == SETUP_REQUEST && responseCode != Activity.RESULT_CANCELED) {
            if (responseCode == LoginActivity.RESULT_START_DEVICE_SYNC) {
                getActivity().startService(new Intent(getActivity(), ApiUploadService.class));
                returnToConversations();

                NavigationView nav = (NavigationView) getActivity().findViewById(R.id.navigation_view);
                if (nav != null) {
                    nav.getMenu().findItem(R.id.drawer_account).setTitle(R.string.menu_account);
                }
            } else if (responseCode == LoginActivity.RESULT_START_NETWORK_SYNC) {
                restoreAccount();
            }
        } else if (requestCode == ONBOARDING_REQUEST) {
            if (responseCode == RESPONSE_SKIP_TRIAL_FOR_NOW) {
                returnToConversations();
            } else if (responseCode == RESPONSE_START_TRIAL) {
                getPreferenceScreen()
                        .findPreference(getString(R.string.pref_my_account_setup))
                        .performClick();
            }
        }
    }

    private void restoreAccount() {
        final ProgressDialog dialog = new ProgressDialog(getActivity());
        dialog.setCancelable(false);
        dialog.setIndeterminate(true);
        dialog.setMessage(getString(R.string.preparing_new_account));
        dialog.show();

        new Thread(new Runnable() {
            @Override
            public void run() {
                DataSource source = DataSource.getInstance(getActivity());
                source.open();
                source.clearTables();
                source.close();

                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        dialog.dismiss();
                        returnToConversations();

                        ((MessengerActivity) getActivity()).startDataDownload();

                        NavigationView nav = (NavigationView) getActivity().findViewById(R.id.navigation_view);
                        if (nav != null) {
                            nav.getMenu().findItem(R.id.drawer_account).setTitle(R.string.menu_account);
                        }
                    }
                });
            }
        }).start();
    }

    private void returnToConversations() {
        NavigationView nav = (NavigationView) getActivity().findViewById(R.id.navigation_view);
        if (nav != null) {
            nav.setCheckedItem(R.id.drawer_conversation);
        }

        if (getActivity() instanceof MessengerActivity) {
            ((MessengerActivity) getActivity()).displayConversations();
            getActivity().setTitle(StringUtils.titleize(getString(R.string.app_name)));
        } else {
            getActivity().recreate();
        }
    }
}