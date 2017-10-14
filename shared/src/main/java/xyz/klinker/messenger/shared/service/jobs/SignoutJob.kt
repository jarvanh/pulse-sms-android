package xyz.klinker.messenger.shared.service.jobs

import android.annotation.SuppressLint
import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import android.preference.PreferenceManager
import android.util.Log
import xyz.klinker.messenger.api.implementation.Account
import xyz.klinker.messenger.shared.util.billing.BillingHelper
import xyz.klinker.messenger.shared.util.billing.ProductPurchased
import java.util.*

class SignoutJob : BackgroundJob() {

    private var billing: BillingHelper? = null

    private val isExpired: Boolean
        get() {
            val purchasedList = billing!!.queryAllPurchasedProducts()

            return if (purchasedList.size > 0) {
                val best = getBestProduct(purchasedList)

                if (best.productId == "lifetime") {
                    writeLifetimeSubscriber()
                } else {
                    writeNewExpirationToAccount(Date().time + best.expiration)
                }

                false
            } else {
                true
            }
        }

    override fun onRunJob(parameters: JobParameters) {
//        billing = BillingHelper(this)
//
//        // Only need to manage this on the primary device
//        if (Account.exists() && Account.primary && Account.subscriptionType !== Account.SubscriptionType.LIFETIME &&
//                Account.subscriptionExpiration < Date().time && isExpired) {
//            Log.v(TAG, "forcing signout due to expired account!")
//
//            Account.clearAccount(this)
//            ApiUtils.deleteAccount(Account.accountId)
//        } else {
//            Log.v(TAG, "account not expired, scheduling the check again.")
//            SubscriptionExpirationCheckJob.scheduleNextRun(this)
//        }
//
//        writeSignoutTime(this, 0)
    }

    override fun onDestroy() {
        super.onDestroy()

        if (billing != null) {
            billing!!.destroy()
        }
    }

    private fun writeLifetimeSubscriber() {
        val account = Account
        account.updateSubscription(this,
                Account.SubscriptionType.LIFETIME, 1L, true)
    }

    private fun writeNewExpirationToAccount(time: Long) {
        val account = Account
        account.updateSubscription(this,
                Account.SubscriptionType.SUBSCRIBER, time, true)
    }

    private fun getBestProduct(products: List<ProductPurchased>): ProductPurchased {
        var best = products[0]
        products.asSequence()
                .filter { it.isBetterThan(best) }
                .forEach { best = it }
        return best
    }

    companion object {

        private val TAG = "SignoutJob"

        private val JOB_ID = 15

        fun scheduleNextRun(context: Context) {
            val signoutTime = PreferenceManager.getDefaultSharedPreferences(context)
                    .getLong("account_signout_time", 0L)
            scheduleNextRun(context, signoutTime)
        }

        fun scheduleNextRun(context: Context, signoutTime: Long) {
            val currentTime = Date().time
            val account = Account
            val jobScheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler

            val component = ComponentName(context, SignoutJob::class.java)
            val builder = JobInfo.Builder(JOB_ID, component)
                    .setMinimumLatency(signoutTime - currentTime)
                    .setRequiresCharging(false)
                    .setRequiresDeviceIdle(false)

            if (account.accountId == null || account.subscriptionType === Account.SubscriptionType.LIFETIME || !account.primary || signoutTime == 0L) {
                jobScheduler.cancel(JOB_ID)
            } else {
                Log.v(TAG, "CURRENT TIME: " + Date().toString())
                Log.v(TAG, "SCHEDULING NEW SIGNOUT CHECK FOR: " + Date(signoutTime).toString())

                jobScheduler.schedule(builder.build())
            }
        }

        @SuppressLint("ApplySharedPref")
        fun writeSignoutTime(context: Context, signoutTime: Long) {
            PreferenceManager.getDefaultSharedPreferences(context)
                    .edit().putLong("account_signout_time", signoutTime)
                    .commit()

            scheduleNextRun(context, signoutTime)
        }

        fun isScheduled(context: Context): Long {
            return PreferenceManager.getDefaultSharedPreferences(context)
                    .getLong("account_signout_time", 0L)
        }
    }
}
