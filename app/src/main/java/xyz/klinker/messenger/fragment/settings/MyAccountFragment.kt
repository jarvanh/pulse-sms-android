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

import android.annotation.SuppressLint
import android.app.Activity
import android.app.ProgressDialog
import android.content.ActivityNotFoundException
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.support.design.widget.NavigationView
import android.support.v4.app.FragmentActivity
import android.support.v7.app.AlertDialog
import android.support.v7.preference.PreferenceCategory
import android.util.Log
import android.view.View
import android.widget.Toast

import java.util.Date

import xyz.klinker.messenger.R
import xyz.klinker.messenger.activity.AccountPurchaseActivity
import xyz.klinker.messenger.activity.AccountTrialActivity
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
import xyz.klinker.messenger.shared.data.FeatureFlags
import xyz.klinker.messenger.shared.service.ContactResyncService
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

    private val fragmentActivity: FragmentActivity? by lazy { activity }
    private var billing: BillingHelper? = null

    /**
     * Gets a device id for this device. This will be a 32-bit random hex value.
     *
     * @return the device id.
     */
    private val deviceId: String?
        get() = Account.deviceId

    @SuppressLint("RestrictedApi")
    override fun onCreatePreferences(bundle: Bundle?, s: String?) {
        addPreferencesFromResource(R.xml.my_account)

        billing = BillingHelper(fragmentActivity)

        if (initSetupPreference()) {
            initLifetimeSubscriberPreference()
            findPreference(getString(R.string.pref_about_device_id)).summary = deviceId
            initMessageCountPreference()
            initRemoveAccountPreference()
            initResyncAccountPreference()
            initResyncContactsPreference()
            initFirebaseRefreshPreference()
        }

        initWebsitePreference()

//        startTrial()
//        upgradeTrial()
//        pickSubscription(true)
//        pickSubscription(false)

        if (openTrialUpgradePreference) {
            upgradeTrial()
            openTrialUpgradePreference = false
        }
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

            checkSubscriptions(false)
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

    private fun checkSubscriptions(runUi: Boolean = true) {
        val dialog = ProgressDialog(fragmentActivity!!)
        dialog.setMessage(getString(R.string.checking_for_active_subscriptions))
        dialog.setCancelable(false)
        dialog.isIndeterminate = true

        if (runUi) {
            dialog.show()
        }

        Thread {
            val hasSubs = billing?.hasPurchasedProduct() ?: false

            if (runUi) {
                try {
                    dialog.dismiss()
                } catch (e: Exception) {
                }
            }

            if (fragmentActivity == null) {
                return@Thread
            }

            if (!runUi && FeatureFlags.CHECK_SUB_STATUS_ON_ACCOUNT_PAGE) {
                if (hasSubs && Account.exists() && Account.subscriptionType == Account.SubscriptionType.FREE_TRIAL) {
                    Account.updateSubscription(fragmentActivity!!, Account.SubscriptionType.SUBSCRIBER, 1L, true)
                }

                return@Thread
            }

            fragmentActivity?.runOnUiThread {
                try {
                    if (!resources.getBoolean(R.bool.check_subscription) || hasSubs || Account.hasPurchased) {
                        Toast.makeText(fragmentActivity, R.string.subscription_found, Toast.LENGTH_LONG).show()
                        startLoginActivity()
                    } else if (Settings.hasUsedFreeTrial) {
                        Toast.makeText(fragmentActivity, R.string.trial_finished, Toast.LENGTH_LONG).show()
                        pickSubscription(false)
                    } else {
                        startTrial()
                    }
                } catch (e: IllegalStateException) {
                    // resources bad, fragment destroyed
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

        if (Account.subscriptionType == Account.SubscriptionType.SUBSCRIBER || Account.subscriptionType == Account.SubscriptionType.TRIAL) {
            preference.setTitle(R.string.change_subscription)
            preference.setSummary(R.string.cancel_on_the_play_store)

            preference.setOnPreferenceClickListener {
                pickSubscription(true)
                false
            }
       } else if (Account.subscriptionType == Account.SubscriptionType.FREE_TRIAL) {
            var daysLeftInTrial = Account.getDaysLeftInTrial()
            if (daysLeftInTrial < 0) {
                daysLeftInTrial = 0
            }

            preference.title = resources.getString(R.string.trial_subscription_title, daysLeftInTrial.toString())
            preference.setSummary(R.string.trial_subscription_summary)

            preference.setOnPreferenceClickListener {
                upgradeTrial()
                AnalyticsHelper.accountFreeTrialUpgradeDialogShown(fragmentActivity!!)
                false
            }
        }
    }

    private fun initMessageCountPreference() {
        val preference = findPreference(getString(R.string.pref_message_count))

        val source = DataSource
        val conversationCount = source.getConversationCount(fragmentActivity!!)
        val messageCount = source.getMessageCount(fragmentActivity!!)

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
            AlertDialog.Builder(fragmentActivity!!)
                    .setMessage(R.string.delete_account_confirmation)
                    .setPositiveButton(android.R.string.yes) { _, _ -> deleteAccount() }
                    .setNegativeButton(android.R.string.no, null)
                    .show()

            true
        }
    }

    private fun deleteAccount() {
        Settings.setValue(fragmentActivity!!, getString(R.string.pref_has_used_free_trial), true)

        val account = Account
        val accountId = account.accountId

        Thread { ApiUtils.deleteAccount(accountId) }.start()
        account.clearAccount(fragmentActivity!!)

        returnToConversationsAfterLogin()

        val nav = fragmentActivity?.findViewById<View>(R.id.navigation_view) as NavigationView?
        nav?.menu?.findItem(R.id.drawer_account)?.setTitle(R.string.menu_device_texting)
    }

    private fun initResyncAccountPreference() {
        val preference = findPreference(getString(R.string.pref_resync_account))

        if (Account.primary) {
            preference.setSummary(R.string.resync_account_summary_phone)
            preference.setOnPreferenceClickListener {
                AlertDialog.Builder(fragmentActivity!!)
                        .setMessage(R.string.resync_account_confirmation)
                        .setPositiveButton(android.R.string.yes) { _, _ -> cleanAccount() }
                        .setNegativeButton(android.R.string.no, null)
                        .show()

                true
            }
        } else {
            preference.setOnPreferenceClickListener {
                AlertDialog.Builder(fragmentActivity!!)
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
            AlertDialog.Builder(fragmentActivity!!)
                    .setMessage(R.string.refresh_firebase_warning)
                    .setPositiveButton(R.string.ok) { _, _ ->
                        val account = Account
                        val accountId = account.accountId

                        Thread { ApiUtils.deleteAccount(accountId) }.start()

                        startActivity(Intent(fragmentActivity!!, RecreateAccountActivity::class.java))
                    }.show()
            true
        }
    }

    private fun initResyncContactsPreference() {
        val preference = findPreference(getString(R.string.pref_resync_contacts))
        if (!Account.primary) {
            (preferenceScreen
                    .findPreference(getString(R.string.pref_category_account_actions)) as PreferenceCategory)
                    .removePreference(preference)
            return
        }

        preference.setOnPreferenceClickListener {
            val intent = Intent(activity, ContactResyncService::class.java)
            intent.putExtra(ContactResyncService.EXTRA_FORCE_SYNC_ALL_CONTACTS, true)
            activity?.startService(intent)
            activity?.onBackPressed()
            true
        }
    }

    override fun onActivityResult(requestCode: Int, responseCode: Int, data: Intent?) {
        Settings.forceUpdate(fragmentActivity!!)
        if (requestCode == TRIAL_REQUEST && responseCode == RESULT_START_TRIAL) {
            startLoginActivity(false)
        } else if (requestCode == PURCHASE_REQUEST && responseCode == RESULT_SIGN_IN) {
            startLoginActivity(true)
        } else if (requestCode == PURCHASE_REQUEST && responseCode == AccountPurchaseActivity.RESULT_CANCEL_TRIAL) {
            promptCancelTrial()
        } else if (requestCode == PURCHASE_REQUEST && responseCode == Activity.RESULT_CANCELED) {
            purchaseCancelled()
        } else if (requestCode == PURCHASE_REQUEST && responseCode == Activity.RESULT_OK) {
            val productId = data?.getStringExtra(AccountPurchaseActivity.PRODUCT_ID_EXTRA)
            Log.v("pulse_purchase", "on activity result. Purchasing product: $productId")

            when (productId) {
                ProductAvailable.createLifetime().productId -> purchaseProduct(ProductAvailable.createLifetime())
                ProductAvailable.createYearly().productId -> purchaseProduct(ProductAvailable.createYearly())
                ProductAvailable.createThreeMonth().productId -> purchaseProduct(ProductAvailable.createThreeMonth())
                ProductAvailable.createMonthly().productId -> purchaseProduct(ProductAvailable.createMonthly())
            }
        } else if (!billing!!.handleOnActivityResult(requestCode, responseCode, data)) {
            if (requestCode == SETUP_REQUEST && responseCode != Activity.RESULT_CANCELED) {
                if (responseCode == LoginActivity.RESULT_START_DEVICE_SYNC) {
                    ApiUploadService.start(fragmentActivity!!)
                    returnToConversationsAfterLogin()

                    val nav = fragmentActivity!!.findViewById<View>(R.id.navigation_view) as NavigationView
                    nav.menu.findItem(R.id.drawer_account).setTitle(R.string.menu_account)

                    fragmentActivity!!.startService(Intent(fragmentActivity!!, SimpleLifetimeSubscriptionCheckService::class.java))
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
        val dialog = ProgressDialog(fragmentActivity!!)
        dialog.setCancelable(false)
        dialog.isIndeterminate = true
        dialog.setMessage(getString(R.string.preparing_new_account))
        dialog.show()

        Thread {
            DataSource.clearTables(fragmentActivity!!)

            fragmentActivity?.runOnUiThread {
                try {
                    dialog.dismiss()
                } catch (e: Exception) {
                }

                returnToConversationsAfterLogin()

                (fragmentActivity!! as MessengerActivity).accountController.startResyncingAccount()

                val nav = fragmentActivity!!.findViewById<View>(R.id.navigation_view) as NavigationView
                nav.menu.findItem(R.id.drawer_account).setTitle(R.string.menu_account)
            }
        }.start()

        // after a login, lets query the subscription status and write it to their account for them
        fragmentActivity!!.startService(Intent(fragmentActivity!!, SimpleSubscriptionCheckService::class.java))
    }

    private fun cleanAccount() {
        val account = Account
        val dialog = ProgressDialog(fragmentActivity!!)
        dialog.setCancelable(false)
        dialog.isIndeterminate = true
        dialog.setMessage(getString(R.string.preparing_new_account))
        dialog.show()

        Thread {
            DataSource.clearTables(fragmentActivity!!)
            ApiUtils.cleanAccount(account.accountId)

            fragmentActivity?.runOnUiThread {
                try {
                    dialog.dismiss()
                } catch (e: Exception) {
                }

                val login = Intent(fragmentActivity!!, InitialLoadActivity::class.java)
                login.putExtra(InitialLoadActivity.UPLOAD_AFTER_SYNC, true)
                login.putExtra(LoginActivity.ARG_SKIP_LOGIN, true)
                startActivity(login)
                fragmentActivity?.finish()
            }
        }.start()
    }

    private fun redirectToPlayStoreToCancel() {
        if (fragmentActivity != null) {
            AlertDialog.Builder(fragmentActivity!!)
                    .setMessage(R.string.redirect_to_play_store)
                    .setPositiveButton(R.string.play_store) { _: DialogInterface, _: Int ->
                        val intent = Intent(Intent.ACTION_VIEW)
                        intent.data = Uri.parse("https://play.google.com/store/apps/details?id=xyz.klinker.messenger")
                        fragmentActivity?.startActivity(intent)
                    }.show()
        } else {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse("https://play.google.com/store/apps/details?id=xyz.klinker.messenger")
            fragmentActivity?.startActivity(intent)

            Toast.makeText(fragmentActivity, R.string.redirect_to_play_store, Toast.LENGTH_LONG).show()
        }
    }

    private fun returnToConversationsAfterLogin() {
        val holderActivity = activity ?: return

        val nav = holderActivity.findViewById<View>(R.id.navigation_view) as NavigationView?
        nav?.setCheckedItem(R.id.drawer_conversation)

        val account = Account
        if (account.exists() && account.primary) {
            ApiUtils.updateSubscription(account.accountId,
                    account.subscriptionType!!.typeCode, account.subscriptionExpiration)
        }

        SubscriptionExpirationCheckJob.scheduleNextRun(fragmentActivity!!)

        if (holderActivity is MessengerActivity) {
            holderActivity.displayConversations()
            holderActivity.title = StringUtils.titleize(fragmentActivity!!.getString(R.string.app_name))
        } else {
            holderActivity.recreate()
        }
    }

    private fun startLoginActivity(signInOnly: Boolean = false) {
        val intent = Intent(context, LoginActivity::class.java)
        intent.putExtra(LoginActivity.ARG_FORCE_NO_CREATE_ACCOUNT, signInOnly)
        intent.putExtra(LoginActivity.ARG_BACKGROUND_COLOR, Settings.mainColorSet.color)
        intent.putExtra(LoginActivity.ARG_ACCENT_COLOR, Settings.mainColorSet.colorAccent)
        startActivityForResult(intent, SETUP_REQUEST)
    }

    private fun pickSubscription(changingSubscription: Boolean = false) {
        val intent = Intent(fragmentActivity!!, AccountPurchaseActivity::class.java)
        intent.putExtra(AccountPurchaseActivity.ARG_CHANGING_SUBSCRIPTION, changingSubscription)

        startActivityForResult(intent, PURCHASE_REQUEST)
    }

    private fun upgradeTrial() {
        val intent = Intent(fragmentActivity!!, AccountPurchaseActivity::class.java)
        intent.putExtra(AccountPurchaseActivity.ARG_FREE_TRIAL, true)

        startActivityForResult(intent, PURCHASE_REQUEST)
    }

    private fun startTrial() {
        val intent = Intent(fragmentActivity!!, AccountTrialActivity::class.java)
        startActivityForResult(intent, TRIAL_REQUEST)
    }

    private fun purchaseProduct(product: ProductAvailable) {
        billing!!.purchaseItem(fragmentActivity!!, product, object : PurchasedItemCallback {
            override fun onItemPurchased(productId: String) {
                if (fragmentActivity != null) {
                    AnalyticsHelper.accountCompetedPurchase(fragmentActivity!!)
                    AnalyticsHelper.userSubscribed(fragmentActivity!!, productId)
                    Account.setHasPurchased(fragmentActivity!!, true)
                }

                if (Account.accountId == null) {
                    AnalyticsHelper.userSubscribed(fragmentActivity!!, productId)

                    if (product.productId.contains("lifetime")) {
                        Account.updateSubscription(fragmentActivity!!, Account.SubscriptionType.LIFETIME, Date(1))
                    } else {
                        val newExperation = ProductPurchased.getExpiration(product.productId)
                        Account.updateSubscription(fragmentActivity!!, Account.SubscriptionType.SUBSCRIBER, Date(newExperation))
                    }

                    startLoginActivity()
                } else {
                    // they switched their subscription, lets write the new timeout to their account.
                    val newExperation = ProductPurchased.getExpiration(product.productId)
                    val oldSubscription = Account.subscriptionType

                    AnalyticsHelper.userUpgraded(fragmentActivity!!, productId)
                    if (product.productId.contains("lifetime")) {
                        Account.updateSubscription(fragmentActivity!!, Account.SubscriptionType.LIFETIME, Date(newExperation))
                    } else {
                        Account.updateSubscription(fragmentActivity!!, Account.SubscriptionType.SUBSCRIBER, Date(newExperation))
                    }

                    returnToConversationsAfterLogin()

                    if (oldSubscription != Account.SubscriptionType.FREE_TRIAL) {
                        redirectToPlayStoreToCancel()
                    }
                }
            }

            override fun onPurchaseError(message: String) {
                purchaseCancelled(message)
            }
        })
    }

    private fun purchaseCancelled(message: String? = null) {
        AnalyticsHelper.purchaseError(fragmentActivity!!)
        if (message != null) {
            fragmentActivity?.runOnUiThread { Toast.makeText(activity, message, Toast.LENGTH_SHORT).show() }
        }

        if (Account.exists() && Account.subscriptionType == Account.SubscriptionType.FREE_TRIAL && Account.getDaysLeftInTrial() <= 0) {
            promptCancelTrial()
        }
    }

    private fun promptCancelTrial() {
        AlertDialog.Builder(activity!!)
                .setCancelable(false)
                .setMessage(R.string.purchase_cancelled_trial_finished)
                .setPositiveButton(R.string.ok) { _, _ -> deleteAccount() }
                .show()
    }

    companion object {
        val ONBOARDING_REQUEST = 54320
        val SETUP_REQUEST = 54321
        val PURCHASE_REQUEST = 54322
        val TRIAL_REQUEST = 5432

        val RESULT_SIGN_IN = 54323
        val RESULT_START_TRIAL = 54321

        val RESPONSE_START_TRIAL = 101
        val RESPONSE_SKIP_TRIAL_FOR_NOW = 102

        var openTrialUpgradePreference = false
    }
}
