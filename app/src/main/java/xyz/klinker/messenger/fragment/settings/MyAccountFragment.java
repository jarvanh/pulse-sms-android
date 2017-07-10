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
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v7.app.AlertDialog;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceCategory;
import android.widget.Toast;

import java.util.Date;

import xyz.klinker.messenger.R;
import xyz.klinker.messenger.activity.AccountPurchaseActivity;
import xyz.klinker.messenger.activity.InitialLoadActivity;
import xyz.klinker.messenger.activity.MessengerActivity;
import xyz.klinker.messenger.activity.OnBoardingPayActivity;
import xyz.klinker.messenger.api.implementation.Account;
import xyz.klinker.messenger.api.implementation.ApiUtils;
import xyz.klinker.messenger.api.implementation.LoginActivity;
import xyz.klinker.messenger.api.implementation.RecreateAccountActivity;
import xyz.klinker.messenger.shared.data.DataSource;
import xyz.klinker.messenger.shared.data.Settings;
import xyz.klinker.messenger.shared.service.ApiUploadService;
import xyz.klinker.messenger.shared.service.SimpleLifetimeSubscriptionCheckService;
import xyz.klinker.messenger.shared.service.SimpleSubscriptionCheckService;
import xyz.klinker.messenger.shared.service.jobs.SignoutJob;
import xyz.klinker.messenger.shared.service.jobs.SubscriptionExpirationCheckJob;
import xyz.klinker.messenger.api.implementation.firebase.AnalyticsHelper;
import xyz.klinker.messenger.shared.util.StringUtils;
import xyz.klinker.messenger.shared.util.billing.BillingHelper;
import xyz.klinker.messenger.shared.util.billing.ProductAvailable;
import xyz.klinker.messenger.shared.util.billing.ProductPurchased;
import xyz.klinker.messenger.shared.util.billing.PurchasedItemCallback;
import xyz.klinker.messenger.view.SelectPurchaseDialog;

/**
 * Fragment for displaying information about the user's account. We can display different stats
 * for the user here, along with subscription status.
 */
public class MyAccountFragment extends MaterialPreferenceFragmentCompat {

    public static final int ONBOARDING_REQUEST = 54320;
    public static final int SETUP_REQUEST = 54321;
    public static final int PURCHASE_REQUEST = 54322;

    public static final int RESPONSE_START_TRIAL = 101;
    public static final int RESPONSE_SKIP_TRIAL_FOR_NOW = 102;


    private BillingHelper billing;

    @Override
    public void onCreatePreferences(Bundle bundle, String s) {
        addPreferencesFromResource(R.xml.my_account);

        billing = new BillingHelper(getActivity());

        if (initSetupPreference()) {
            initLifetimeSubscriberPreference();
            findPreference(getString(R.string.pref_about_device_id)).setSummary(getDeviceId());
            initMessageCountPreference();
            initRemoveAccountPreference();
            initResyncAccountPreference();
            initFirebaseRefreshPreference();
        }

        initWebsitePreference();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        billing.destroy();
    }

    private boolean initSetupPreference() {
        Preference preference = findPreference(getString(R.string.pref_my_account_setup));
        Account account = Account.get(getActivity());

        if ((!account.exists()) && preference != null) {
            preference.setOnPreferenceClickListener(preference1 -> {
                checkSubscriptions();
                return true;
            });

            checkSubscriptions();
            removeAccountOptions();
            return false;
        } else if (preference != null) {
            ((PreferenceCategory) findPreference(getString(R.string.pref_category_account_information)))
                    .removePreference(preference);
            return true;
        } else {
            return true;
        }
    }

    private void initWebsitePreference() {
        Preference preference = findPreference(getString(R.string.pref_go_to_web));
        preference.setOnPreferenceClickListener(preference1 -> {
            Intent web = new Intent(Intent.ACTION_VIEW, Uri.parse("https://messenger.klinkerapps.com"));
            web.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(web);

            return false;
        });
    }

    private void checkSubscriptions() {
        final ProgressDialog dialog = new ProgressDialog(getActivity());
        dialog.setMessage(getString(R.string.checking_for_active_subscriptions));
        dialog.setCancelable(false);
        dialog.setIndeterminate(true);
        dialog.show();

        new Thread(() -> {
            final boolean hasSubs = billing.hasPurchasedProduct();
            dialog.dismiss();

            if (getActivity() == null) {
                return;
            }

            getActivity().runOnUiThread(() -> {
                if (!getResources().getBoolean(R.bool.check_subscription) || hasSubs) {
                    Toast.makeText(getActivity(), R.string.subscription_found, Toast.LENGTH_LONG).show();
                    startLoginActivity();
                } else {
                    pickSubscription();
                }
            });
        }).start();
    }

    private void removeAccountOptions() {
        try {
            ((PreferenceCategory) findPreference(getString(R.string.pref_category_account_information)))
                    .removePreference(findPreference(getString(R.string.pref_subscriber_status)));
            ((PreferenceCategory) findPreference(getString(R.string.pref_category_account_information)))
                    .removePreference(findPreference(getString(R.string.pref_message_count)));
            ((PreferenceCategory) findPreference(getString(R.string.pref_category_account_information)))
                    .removePreference(findPreference(getString(R.string.pref_about_device_id)));
            getPreferenceScreen().removePreference(findPreference(getString(R.string.pref_category_account_actions)));
        } catch (Exception e) {

        }
    }

    private void initLifetimeSubscriberPreference() {
        Preference preference = findPreference(getString(R.string.pref_subscriber_status));

        Drawable icon = getResources().getDrawable(R.drawable.ic_reward);
        icon.setTint(getResources().getColor(R.color.primaryText));
        preference.setIcon(icon);

        if (!Account.get(getActivity()).primary) {
            ((PreferenceCategory) findPreference(getString(R.string.pref_category_account_information)))
                    .removePreference(preference);
            return;
        }

        if (Account.get(getActivity()).subscriptionType == Account.SubscriptionType.SUBSCRIBER ||
                Account.get(getActivity()).subscriptionType == Account.SubscriptionType.TRIAL) {

            long signoutTime = SignoutJob.isScheduled(getActivity());
            if (signoutTime != 0L) {
                preference.setTitle(getString(R.string.account_expiring));
                preference.setSummary(getString(R.string.signout_time, new Date(signoutTime).toString()));
            } else {
                preference.setTitle(R.string.change_subscription);
                preference.setSummary(R.string.cancel_on_the_play_store);
            }

            preference.setOnPreferenceClickListener(preference1 -> {
                new AlertDialog.Builder(getActivity())
                        .setMessage(R.string.change_subscription_message)
                        .setPositiveButton(R.string.ok, (dialogInterface, i) -> pickSubscription()).show();
                return false;
            });
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

        preference.setOnPreferenceClickListener(preference1 -> {
            new AlertDialog.Builder(getActivity())
                    .setMessage(R.string.delete_account_confirmation)
                    .setPositiveButton(android.R.string.yes, (dialogInterface, i) -> {
                        final Account account = Account.get(getActivity());
                        final String accountId = account.accountId;
                        account.clearAccount();

                        new Thread(() -> {
                            new ApiUtils().deleteAccount(accountId);
                        }).start();

                        returnToConversationsAfterLogin();

                        NavigationView nav = (NavigationView) getActivity().findViewById(R.id.navigation_view);
                        if (nav != null) {
                            nav.getMenu().findItem(R.id.drawer_account).setTitle(R.string.menu_device_texting);
                        }
                    })
                    .setNegativeButton(android.R.string.no, (dialogInterface, i) -> {

                    })
                    .show();

            return true;
        });
    }

    private void initResyncAccountPreference() {
        Preference preference = findPreference(getString(R.string.pref_resync_account));

        if (Account.get(getActivity()).primary) {
            preference.setSummary(R.string.resync_account_summary_phone);
            preference.setOnPreferenceClickListener(preference1 -> {
                new AlertDialog.Builder(getActivity())
                        .setMessage(R.string.resync_account_confirmation)
                        .setPositiveButton(android.R.string.yes, (dialogInterface, i) -> cleanAccount())
                        .setNegativeButton(android.R.string.no, null)
                        .show();

                return true;
            });
        } else {
            preference.setOnPreferenceClickListener(preference1 -> {
                new AlertDialog.Builder(getActivity())
                        .setMessage(R.string.resync_account_confirmation)
                        .setPositiveButton(android.R.string.yes, (dialogInterface, i) -> restoreAccount())
                        .setNegativeButton(android.R.string.no, null)
                        .show();

                return true;
            });
        }
    }

    private void initFirebaseRefreshPreference() {
        Preference preference = findPreference(getString(R.string.pref_refresh_firebase));
        if (!Account.get(getActivity()).primary) {
            ((PreferenceCategory) getPreferenceScreen()
                    .findPreference(getString(R.string.pref_category_account_actions)))
                    .removePreference(preference);
            return;
        }

        preference.setOnPreferenceClickListener(preference1 -> {
            new AlertDialog.Builder(getActivity())
                    .setMessage(R.string.refresh_firebase_warning)
                    .setPositiveButton(R.string.ok, (dialog, which) -> {
                        final Account account = Account.get(getActivity());
                        final String accountId = account.accountId;

                        new Thread(() -> {
                            new ApiUtils().deleteAccount(accountId);
                        }).start();

                        startActivity(new Intent(getActivity(), RecreateAccountActivity.class));
                    }).show();
            return true;
        });
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
        if (requestCode == PURCHASE_REQUEST && responseCode == Activity.RESULT_OK) {
            String productId = data.getStringExtra(AccountPurchaseActivity.PRODUCT_ID_EXTRA);
            if (productId.equals(ProductAvailable.createLifetime().getProductId())) {
                purchaseProduct(ProductAvailable.createLifetime());
            } else if (productId.equals(ProductAvailable.createYearly().getProductId())) {
                purchaseProduct(ProductAvailable.createYearly());
            } else if (productId.equals(ProductAvailable.createThreeMonth().getProductId())) {
                purchaseProduct(ProductAvailable.createThreeMonth());
            } else if (productId.equals(ProductAvailable.createMonthly().getProductId())) {
                purchaseProduct(ProductAvailable.createMonthly());
            }
        } else if (!billing.handleOnActivityResult(requestCode, responseCode, data)) {
            if (requestCode == SETUP_REQUEST && responseCode != Activity.RESULT_CANCELED) {
                if (responseCode == LoginActivity.RESULT_START_DEVICE_SYNC) {
                    ApiUploadService.start(getActivity());
                    returnToConversationsAfterLogin();

                    NavigationView nav = (NavigationView) getActivity().findViewById(R.id.navigation_view);
                    if (nav != null) {
                        nav.getMenu().findItem(R.id.drawer_account).setTitle(R.string.menu_account);
                    }

                    getActivity().startService(new Intent(getActivity(), SimpleLifetimeSubscriptionCheckService.class));
                } else if (responseCode == LoginActivity.RESULT_START_NETWORK_SYNC) {
                    restoreAccount();
                }
            } else if (requestCode == ONBOARDING_REQUEST) {
                if (responseCode == RESPONSE_SKIP_TRIAL_FOR_NOW) {
                    returnToConversationsAfterLogin();
                } else if (responseCode == RESPONSE_START_TRIAL) {
                    Preference preference = getPreferenceScreen()
                            .findPreference(getString(R.string.pref_my_account_setup));

                    if (preference.getOnPreferenceClickListener() != null) {
                        preference.getOnPreferenceClickListener().onPreferenceClick(preference);
                    }
                }
            }
        }
    }

    private void restoreAccount() {
        final ProgressDialog dialog = new ProgressDialog(getActivity());
        dialog.setCancelable(false);
        dialog.setIndeterminate(true);
        dialog.setMessage(getString(R.string.preparing_new_account));
        dialog.show();

        new Thread(() -> {
            DataSource source = DataSource.getInstance(getActivity());
            source.open();
            source.clearTables();
            source.close();

            getActivity().runOnUiThread(() -> {
                dialog.dismiss();
                returnToConversationsAfterLogin();

                ((MessengerActivity) getActivity()).startDataDownload();

                NavigationView nav = (NavigationView) getActivity().findViewById(R.id.navigation_view);
                if (nav != null) {
                    nav.getMenu().findItem(R.id.drawer_account).setTitle(R.string.menu_account);
                }
            });
        }).start();

        // after a login, lets query the subscription status and write it to their account for them
        getActivity().startService(new Intent(getActivity(), SimpleSubscriptionCheckService.class));
    }

    private void cleanAccount() {
        final Account account = Account.get(getActivity());
        final ProgressDialog dialog = new ProgressDialog(getActivity());
        dialog.setCancelable(false);
        dialog.setIndeterminate(true);
        dialog.setMessage(getString(R.string.preparing_new_account));
        dialog.show();

        new Thread(() -> {
            DataSource source = DataSource.getInstance(getActivity());
            source.open();
            source.clearTables();
            source.close();

            new ApiUtils().cleanAccount(account.accountId);

            getActivity().runOnUiThread(() -> {
                dialog.dismiss();

                Intent login = new Intent(getActivity(), InitialLoadActivity.class);
                login.putExtra(InitialLoadActivity.UPLOAD_AFTER_SYNC, true);
                login.putExtra(LoginActivity.ARG_SKIP_LOGIN, true);
                startActivity(login);
                getActivity().finish();
            });
        }).start();
    }

    private void returnToConversationsAfterLogin() {
        NavigationView nav = (NavigationView) getActivity().findViewById(R.id.navigation_view);
        if (nav != null) {
            nav.setCheckedItem(R.id.drawer_conversation);
        }

        Account account = Account.get(getActivity());
        if (account.exists() && account.primary) {
            new ApiUtils().updateSubscription(account.accountId,
                    account.subscriptionType.typeCode, account.subscriptionExpiration);
        }

        SubscriptionExpirationCheckJob.scheduleNextRun(getActivity());

        if (getActivity() instanceof MessengerActivity) {
            ((MessengerActivity) getActivity()).displayConversations();
            getActivity().setTitle(StringUtils.titleize(getString(R.string.app_name)));
        } else {
            getActivity().recreate();
        }
    }

    private void startLoginActivity() {
        Intent intent = new Intent(getContext(), LoginActivity.class);
        startActivityForResult(intent, SETUP_REQUEST);
    }

    private void pickSubscription() {
        startActivityForResult(new Intent(getActivity(), AccountPurchaseActivity.class), PURCHASE_REQUEST);
    }

    private void purchaseProduct(final ProductAvailable product) {
        billing.purchaseItem(getActivity(), product, new PurchasedItemCallback() {
            @Override
            public void onItemPurchased(String productId) {
                if (getActivity() != null) {
                    AnalyticsHelper.accountCompetedPurchase(getActivity());
                    AnalyticsHelper.userSubscribed(getActivity(), productId);
                }

                if (Account.get(getActivity()).accountId == null) {
                    // write lifetime here, just so they don't think it is a trial..
                    if (product.getProductId().contains("lifetime")) {
                        Account.get(getActivity()).updateSubscription(Account.SubscriptionType.LIFETIME, new Date(1));
                    }

                    startLoginActivity();
                } else {
                    // they switched their subscription, lets write the new timeout to their account.
                    long newExperation = ProductPurchased.getExperation(product.getProductId());

                    if (product.getProductId().contains("lifetime")) {
                        Account.get(getActivity()).updateSubscription(Account.SubscriptionType.LIFETIME, new Date(newExperation));
                    } else {
                        Account.get(getActivity()).updateSubscription(Account.SubscriptionType.SUBSCRIBER, new Date(newExperation));
                    }

                    returnToConversationsAfterLogin();
                }
            }

            @Override
            public void onPurchaseError(final String message) {
                AnalyticsHelper.purchaseError(getActivity());
                getActivity().runOnUiThread(() -> Toast.makeText(getActivity(), message, Toast.LENGTH_SHORT).show());
            }
        });
    }
}
