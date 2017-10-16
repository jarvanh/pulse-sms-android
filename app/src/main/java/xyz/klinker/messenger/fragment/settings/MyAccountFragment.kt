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

@file:Suppress("DEPRECATION")

package xyz.klinker.messenger.fragment.settings

import android.app.Activity
import android.app.ProgressDialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.design.widget.NavigationView
import android.support.v7.app.AlertDialog
import android.support.v7.preference.PreferenceCategory
import android.util.Log
import android.view.View
import android.widget.Toast

import java.util.Date

import xyz.klinker.messenger.R
import xyz.klinker.messenger.activity.AccountPurchaseActivity
import xyz.klinker.messenger.activity.InitialLoadActivity
import xyz.klinker.messenger.activity.MessengerActivity
import xyz.klinker.messenger.api.implementation.Account
import xyz.klinker.messenger.api.implementation.ApiUtils
import xyz.klinker.messenger.api.implementation.LoginActivity
import xyz.klinker.messenger.api.implementation.RecreateAccountActivity
import xyz.klinker.messenger.shared.data.DataSource
import xyz.klinker.messenger.shared.data.Settings
import xyz.klinker.messenger.shared.service.ApiUploadService
import xyz.klinker.messenger.shared.service.SimpleLifetimeSubscriptionCheckService
import xyz.klinker.messenger.shared.service.SimpleSubscriptionCheckService
import xyz.klinker.messenger.shared.service.jobs.SignoutJob
import xyz.klinker.messenger.shared.service.jobs.SubscriptionExpirationCheckJob
import xyz.klinker.messenger.api.implementation.firebase.AnalyticsHelper
import xyz.klinker.messenger.shared.util.StringUtils
import xyz.klinker.messenger.shared.util.billing.BillingHelper
import xyz.klinker.messenger.shared.util.billing.ProductAvailable
import xyz.klinker.messenger.shared.util.billing.ProductPurchased
import xyz.klinker.messenger.shared.util.billing.PurchasedItemCallback

/**
 * Fragment for displaying information about the user's account. We can display different stats
 * for the user here, along with subscription status.
 */
class MyAccountFragment : MaterialPreferenceFragmentCompat() {

    private var billing: BillingHelper? = null

    /**
     * Gets a device id for this device. This will be a 32-bit random hex value.
     *
     * @return the device id.
     */
    private val deviceId: String?
        get() = Account.deviceId

    override fun onCreatePreferences(bundle: Bundle?, s: String?) {
        addPreferencesFromResource(R.xml.my_account)

        billing = BillingHelper(activity)

        if (initSetupPreference()) {
            initLifetimeSubscriberPreference()
            findPreference(getString(R.string.pref_about_device_id)).summary = deviceId
            initMessageCountPreference()
            initRemoveAccountPreference()
            initResyncAccountPreference()
            initFirebaseRefreshPreference()
        }

        initWebsitePreference()
    }

    override fun onDestroy() {
        super.onDestroy()
        billing?.destroy()
    }

    private fun initSetupPreference(): Boolean {
        val preference = findPreference(getString(R.string.pref_my_account_setup))
        val account = Account

        if (!account.exists() && preference != null) {
            preference.setOnPreferenceClickListener {
                checkSubscriptions()
                true
            }

            checkSubscriptions()
            removeAccountOptions()
            return false
        } else if (preference != null) {
            (findPreference(getString(R.string.pref_category_account_information)) as PreferenceCategory)
                    .removePreference(preference)
            return true
        } else {
            return true
        }
    }

    private fun initWebsitePreference() {
        val preference = findPreference(getString(R.string.pref_go_to_web))
        preference.setOnPreferenceClickListener {
            val web = Intent(Intent.ACTION_VIEW, Uri.parse("https://messenger.klinkerapps.com"))
            web.flags = Intent.FLAG_ACTIVITY_NEW_TASK

            try {
                startActivity(web)
            } catch (e: ActivityNotFoundException) {
                e.printStackTrace()
            }

            false
        }
    }

    private fun checkSubscriptions() {
        val dialog = ProgressDialog(activity)
        dialog.setMessage(getString(R.string.checking_for_active_subscriptions))
        dialog.setCancelable(false)
        dialog.isIndeterminate = true
        dialog.show()

        Thread {
            val hasSubs = billing?.hasPurchasedProduct() ?: false
            dialog.dismiss()

            if (activity == null) {
                return@Thread
            }

            activity.runOnUiThread {
                if (!resources.getBoolean(R.bool.check_subscription) || hasSubs) {
                    Toast.makeText(activity, R.string.subscription_found, Toast.LENGTH_LONG).show()
                    startLoginActivity()
                } else {
                    pickSubscription()
                }
            }
        }.start()
    }

    private fun removeAccountOptions() {
        try {
            (findPreference(getString(R.string.pref_category_account_information)) as PreferenceCategory)
                    .removePreference(findPreference(getString(R.string.pref_subscriber_status)))
            (findPreference(getString(R.string.pref_category_account_information)) as PreferenceCategory)
                    .removePreference(findPreference(getString(R.string.pref_message_count)))
            (findPreference(getString(R.string.pref_category_account_information)) as PreferenceCategory)
                    .removePreference(findPreference(getString(R.string.pref_about_device_id)))
            preferenceScreen.removePreference(findPreference(getString(R.string.pref_category_account_actions)))
        } catch (e: Exception) {
        }
    }

    private fun initLifetimeSubscriberPreference() {
        val preference = findPreference(getString(R.string.pref_subscriber_status))

        val icon = resources.getDrawable(R.drawable.ic_reward)
        icon.setTint(resources.getColor(R.color.primaryText))
        preference.icon = icon

        if (!Account.primary) {
            (findPreference(getString(R.string.pref_category_account_information)) as PreferenceCategory)
                    .removePreference(preference)
            return
        }

        if (Account.subscriptionType === Account.SubscriptionType.SUBSCRIBER || Account.subscriptionType === Account.SubscriptionType.TRIAL) {
            val signoutTime = SignoutJob.isScheduled(activity)
            if (signoutTime != 0L) {
                preference.title = getString(R.string.account_expiring)
                preference.summary = getString(R.string.signout_time, Date(signoutTime).toString())
            } else {
                preference.setTitle(R.string.change_subscription)
                preference.setSummary(R.string.cancel_on_the_play_store)
            }

            preference.setOnPreferenceClickListener {
                AlertDialog.Builder(activity)
                        .setMessage(R.string.change_subscription_message)
                        .setPositiveButton(R.string.ok) { _, _ -> pickSubscription() }.show()
                false
            }
       }
    }

    private fun initMessageCountPreference() {
        val preference = findPreference(getString(R.string.pref_message_count))

        val source = DataSource
        val conversationCount = source.getConversationCount(activity)
        val messageCount = source.getMessageCount(activity)

        val title = resources.getQuantityString(R.plurals.message_count, messageCount,
                messageCount)
        val summary = resources.getQuantityString(R.plurals.conversation_count,
                conversationCount, conversationCount)

        preference.title = title
        preference.summary = summary
    }

    private fun initRemoveAccountPreference() {
        val preference = findPreference(getString(R.string.pref_delete_account))

        preference.setOnPreferenceClickListener {
            AlertDialog.Builder(activity)
                    .setMessage(R.string.delete_account_confirmation)
                    .setPositiveButton(android.R.string.yes) { _, _ ->
                        val account = Account
                        val accountId = account.accountId

                        Thread { ApiUtils.deleteAccount(accountId) }.start()
                        account.clearAccount(activity)

                        returnToConversationsAfterLogin()

                        val nav = activity.findViewById<View>(R.id.navigation_view) as NavigationView
                        nav.menu.findItem(R.id.drawer_account).setTitle(R.string.menu_device_texting)
                    }
                    .setNegativeButton(android.R.string.no, null)
                    .show()

            true
        }
    }

    private fun initResyncAccountPreference() {
        val preference = findPreference(getString(R.string.pref_resync_account))

        if (Account.primary) {
            preference.setSummary(R.string.resync_account_summary_phone)
            preference.setOnPreferenceClickListener {
                AlertDialog.Builder(activity)
                        .setMessage(R.string.resync_account_confirmation)
                        .setPositiveButton(android.R.string.yes) { _, _ -> cleanAccount() }
                        .setNegativeButton(android.R.string.no, null)
                        .show()

                true
            }
        } else {
            preference.setOnPreferenceClickListener {
                AlertDialog.Builder(activity)
                        .setMessage(R.string.resync_account_confirmation)
                        .setPositiveButton(android.R.string.yes) { _, _ -> restoreAccount() }
                        .setNegativeButton(android.R.string.no, null)
                        .show()

                true
            }
        }
    }

    private fun initFirebaseRefreshPreference() {
        val preference = findPreference(getString(R.string.pref_refresh_firebase))
        if (!Account.primary) {
            (preferenceScreen
                    .findPreference(getString(R.string.pref_category_account_actions)) as PreferenceCategory)
                    .removePreference(preference)
            return
        }

        preference.setOnPreferenceClickListener {
            AlertDialog.Builder(activity)
                    .setMessage(R.string.refresh_firebase_warning)
                    .setPositiveButton(R.string.ok) { _, _ ->
                        val account = Account
                        val accountId = account.accountId

                        Thread { ApiUtils.deleteAccount(accountId) }.start()

                        startActivity(Intent(activity, RecreateAccountActivity::class.java))
                    }.show()
            true
        }
    }

    override fun onActivityResult(requestCode: Int, responseCode: Int, data: Intent?) {
        Settings.forceUpdate(activity)
        if (requestCode == PURCHASE_REQUEST && responseCode == Activity.RESULT_OK) {
            val productId = data?.getStringExtra(AccountPurchaseActivity.PRODUCT_ID_EXTRA)
            Log.v("pulse_purchase", "on activity result. Purchasing product: " + productId)

            when (productId) {
                ProductAvailable.createLifetime().productId -> purchaseProduct(ProductAvailable.createLifetime())
                ProductAvailable.createYearly().productId -> purchaseProduct(ProductAvailable.createYearly())
                ProductAvailable.createThreeMonth().productId -> purchaseProduct(ProductAvailable.createThreeMonth())
                ProductAvailable.createMonthly().productId -> purchaseProduct(ProductAvailable.createMonthly())
            }
        } else if (!billing!!.handleOnActivityResult(requestCode, responseCode, data)) {
            if (requestCode == SETUP_REQUEST && responseCode != Activity.RESULT_CANCELED) {
                if (responseCode == LoginActivity.RESULT_START_DEVICE_SYNC) {
                    ApiUploadService.start(activity)
                    returnToConversationsAfterLogin()

                    val nav = activity.findViewById<View>(R.id.navigation_view) as NavigationView
                    nav.menu.findItem(R.id.drawer_account).setTitle(R.string.menu_account)

                    activity.startService(Intent(activity, SimpleLifetimeSubscriptionCheckService::class.java))
                } else if (responseCode == LoginActivity.RESULT_START_NETWORK_SYNC) {
                    restoreAccount()
                }
            } else if (requestCode == ONBOARDING_REQUEST) {
                if (responseCode == RESPONSE_SKIP_TRIAL_FOR_NOW) {
                    returnToConversationsAfterLogin()
                } else if (responseCode == RESPONSE_START_TRIAL) {
                    val preference = preferenceScreen
                            .findPreference(getString(R.string.pref_my_account_setup))

                    if (preference.onPreferenceClickListener != null) {
                        preference.onPreferenceClickListener.onPreferenceClick(preference)
                    }
                }
            }
        }
    }

    private fun restoreAccount() {
        val dialog = ProgressDialog(activity)
        dialog.setCancelable(false)
        dialog.isIndeterminate = true
        dialog.setMessage(getString(R.string.preparing_new_account))
        dialog.show()

        Thread {
            DataSource.clearTables(activity)

            activity.runOnUiThread {
                dialog.dismiss()
                returnToConversationsAfterLogin()

                (activity as MessengerActivity).startDataDownload()

                val nav = activity.findViewById<View>(R.id.navigation_view) as NavigationView
                nav.menu.findItem(R.id.drawer_account).setTitle(R.string.menu_account)
            }
        }.start()

        // after a login, lets query the subscription status and write it to their account for them
        activity.startService(Intent(activity, SimpleSubscriptionCheckService::class.java))
    }

    private fun cleanAccount() {
        val account = Account
        val dialog = ProgressDialog(activity)
        dialog.setCancelable(false)
        dialog.isIndeterminate = true
        dialog.setMessage(getString(R.string.preparing_new_account))
        dialog.show()

        Thread {
            DataSource.clearTables(activity)
            ApiUtils.cleanAccount(account.accountId)

            activity.runOnUiThread {
                dialog.dismiss()

                val login = Intent(activity, InitialLoadActivity::class.java)
                login.putExtra(InitialLoadActivity.UPLOAD_AFTER_SYNC, true)
                login.putExtra(LoginActivity.ARG_SKIP_LOGIN, true)
                startActivity(login)
                activity.finish()
            }
        }.start()
    }

    private fun returnToConversationsAfterLogin() {
        if (activity == null) {
            return
        }

        val nav = activity.findViewById<View>(R.id.navigation_view) as NavigationView
        nav.setCheckedItem(R.id.drawer_conversation)

        val account = Account
        if (account.exists() && account.primary) {
            ApiUtils.updateSubscription(account.accountId,
                    account.subscriptionType!!.typeCode, account.subscriptionExpiration)
        }

        SubscriptionExpirationCheckJob.scheduleNextRun(activity)

        if (activity is MessengerActivity) {
            (activity as MessengerActivity).displayConversations()
            activity.title = StringUtils.titleize(getString(R.string.app_name))
        } else {
            activity.recreate()
        }
    }

    private fun startLoginActivity() {
        val intent = Intent(context, LoginActivity::class.java)
        startActivityForResult(intent, SETUP_REQUEST)
    }

    private fun pickSubscription() {
        startActivityForResult(Intent(activity, AccountPurchaseActivity::class.java), PURCHASE_REQUEST)
    }

    private fun purchaseProduct(product: ProductAvailable) {
        billing!!.purchaseItem(activity, product, object : PurchasedItemCallback {
            override fun onItemPurchased(productId: String) {
                if (activity != null) {
                    AnalyticsHelper.accountCompetedPurchase(activity)
                    AnalyticsHelper.userSubscribed(activity, productId)
                }

                if (Account.accountId == null) {
                    // write lifetime here, just so they don't think it is a trial..
                    if (product.productId.contains("lifetime")) {
                        Account.updateSubscription(activity, Account.SubscriptionType.LIFETIME, Date(1))
                    }

                    startLoginActivity()
                } else {
                    // they switched their subscription, lets write the new timeout to their account.
                    val newExperation = ProductPurchased.getExpiration(product.productId)

                    if (product.productId.contains("lifetime")) {
                        Account.updateSubscription(activity, Account.SubscriptionType.LIFETIME, Date(newExperation))
                    } else {
                        Account.updateSubscription(activity, Account.SubscriptionType.SUBSCRIBER, Date(newExperation))
                    }

                    returnToConversationsAfterLogin()
                }
            }

            override fun onPurchaseError(message: String) {
                AnalyticsHelper.purchaseError(activity)
                activity.runOnUiThread { Toast.makeText(activity, message, Toast.LENGTH_SHORT).show() }
            }
        })
    }

    companion object {
        val ONBOARDING_REQUEST = 54320
        val SETUP_REQUEST = 54321
        val PURCHASE_REQUEST = 54322

        val RESPONSE_START_TRIAL = 101
        val RESPONSE_SKIP_TRIAL_FOR_NOW = 102
    }
}
