package xyz.klinker.messenger.shared.service.jobs

import android.app.PendingIntent
import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import android.util.Log
import xyz.klinker.messenger.api.implementation.Account
import xyz.klinker.messenger.shared.R
import xyz.klinker.messenger.shared.data.ColorSet
import xyz.klinker.messenger.shared.data.Settings
import xyz.klinker.messenger.shared.util.RedirectToMyAccount
import xyz.klinker.messenger.shared.util.TimeUtils
import xyz.klinker.messenger.shared.util.billing.BillingHelper
import xyz.klinker.messenger.shared.util.billing.ProductPurchased
import java.util.*

class SubscriptionExpirationCheckJob : BackgroundJob() {

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

    override fun onRunJob(parameters: JobParameters?) {
        billing = BillingHelper(this)

        if (Account.exists() && Account.primary && Account.subscriptionType !== Account.SubscriptionType.LIFETIME) {
            Log.v(TAG, "checking for expiration")
            if (isExpired) {
                Log.v(TAG, "service is expired")
                makeSignoutNotification()
                SignoutJob.writeSignoutTime(this, Date().time + TimeUtils.DAY * 2)
            } else {
                Log.v(TAG, "not expired, scheduling the next refresh")
                scheduleNextRun(this)
                SignoutJob.writeSignoutTime(this, 0)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (billing != null) {
            billing!!.destroy()
        }
    }

    private fun makeSignoutNotification() {
        val sharedPreferences = Settings.getSharedPrefs(this)
        if (sharedPreferences.getBoolean("seen_subscription_expired_notification", false)) {
            return
        } else {
            sharedPreferences.edit().putBoolean("seen_subscription_expired_notification", true).apply()
        }

        val builder = NotificationCompat.Builder(this)
                .setContentTitle(getString(R.string.no_subscription_found))
                .setContentText(getString(R.string.cancelled_subscription_error))
                .setStyle(NotificationCompat.BigTextStyle()
                        .setBigContentTitle(getString(R.string.no_subscription_found))
                        .setSummaryText(getString(R.string.cancelled_subscription_error)))
                .setSmallIcon(R.drawable.ic_stat_notify_group)
                .setColor(ColorSet.DEFAULT(this).color)

        val renew = Intent(this, RedirectToMyAccount::class.java)

        val subject = "Pulse Subscription"
        val uri = Uri.parse("mailto:pulsesmsapp@gmail.com")
                .buildUpon()
                .appendQueryParameter("subject", subject)
                .build()

        val email = Intent(Intent.ACTION_SENDTO, uri)
        email.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        email.putExtra(Intent.EXTRA_EMAIL, arrayOf("pulsesmsapp@gmail.com"))
        email.putExtra(Intent.EXTRA_SUBJECT, subject)
        email.putExtra(Intent.EXTRA_TEXT, "The Play Store sometimes sucks at determining what you have purchased in the past. Please include the order number of your purchase in this email (which can be found from the Play Store app). I will help you get it worked out!")

        val emailPending = PendingIntent.getActivity(this, REQUEST_CODE_EMAIL, email, PendingIntent.FLAG_UPDATE_CURRENT)
        val renewPending = PendingIntent.getActivity(this, REQUEST_CODE_RENEW, renew, PendingIntent.FLAG_UPDATE_CURRENT)

        val renewAction = NotificationCompat.Action(R.drawable.ic_account, getString(R.string.renew), renewPending)
        val emailAction = NotificationCompat.Action(R.drawable.ic_about, getString(R.string.email), emailPending)

        builder.addAction(renewAction).addAction(emailAction)
        builder.setContentIntent(renewPending)

        NotificationManagerCompat.from(this).notify(NOTIFICATION_ID, builder.build())
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

        private val TAG = "SubscriptionCheck"

        private val JOB_ID = 14
        val NOTIFICATION_ID = 1004
        private val REQUEST_CODE_EMAIL = 1005
        private val REQUEST_CODE_RENEW = 1006

        fun scheduleNextRun(context: Context) {
            val account = Account
            val jobScheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler

            val currentTime = Date().time
            val expiration = account.subscriptionExpiration + TimeUtils.DAY

            val component = ComponentName(context, SubscriptionExpirationCheckJob::class.java)
            val builder = JobInfo.Builder(JOB_ID, component)
                    .setMinimumLatency(expiration - currentTime)
                    .setRequiresCharging(false)
                    .setRequiresDeviceIdle(false)

//            if (account.accountId == null || account.subscriptionType === Account.SubscriptionType.LIFETIME || !account.primary) {
//                jobScheduler.cancel(JOB_ID)
//            } else {
//                Log.v(TAG, "CURRENT TIME: " + Date().toString())
//                Log.v(TAG, "SCHEDULING NEW SIGNOUT CHECK FOR: " + Date(expiration).toString())
//
//                jobScheduler.schedule(builder.build())
//            }
        }
    }
}
