package xyz.klinker.messenger.shared.service.jobs

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.firebase.jobdispatcher.JobParameters
import com.firebase.jobdispatcher.SimpleJobService
import xyz.klinker.messenger.shared.util.TimeUtils
import com.firebase.jobdispatcher.GooglePlayDriver
import com.firebase.jobdispatcher.FirebaseJobDispatcher
import com.firebase.jobdispatcher.Trigger
import com.firebase.jobdispatcher.Lifetime
import xyz.klinker.messenger.api.implementation.Account
import xyz.klinker.messenger.api.implementation.firebase.AnalyticsHelper
import xyz.klinker.messenger.shared.R
import xyz.klinker.messenger.shared.data.Settings
import xyz.klinker.messenger.shared.util.NotificationUtils
import xyz.klinker.messenger.shared.util.RedirectToMyAccount

class FreeTrialNotifierJob : SimpleJobService() {

    override fun onRunJob(job: JobParameters): Int {
        if (Account.exists() && Account.subscriptionType == Account.SubscriptionType.FREE_TRIAL) {
            val daysLeft = Account.getDaysLeftInTrial()
            AnalyticsHelper.accountTrialDay(this, daysLeft)
            when (daysLeft) {
//                7 -> notifyDaysLeft(7)
//                6 -> notifyDaysLeft(6)
//                5 -> notifyDaysLeft(5)
                4 -> notifyDaysLeft(4)
                2 -> notifyDaysLeft(2)
                1 -> notifyLastDay()
                0 -> notifyExpired()
            }
        }

        scheduleNextRun(this)
        return 0
    }

    private fun notifyDaysLeft(left: Int) {
        val notification = NotificationCompat.Builder(this, NotificationUtils.ACCOUNT_ACTIVITY_CHANNEL_ID)
                .setContentTitle(getString(R.string.notification_trial_title))
                .setSmallIcon(R.drawable.ic_stat_notify_group)
                .setLocalOnly(true)
                .setColor(Settings.mainColorSet.color)
                .setAutoCancel(true)
                .setContentText(getString(R.string.notification_days_remaining_in_trial, left.toString()))

        val upgrade = Intent(this, RedirectToMyAccount::class.java)
        val upgradePending = PendingIntent.getActivity(this, 1006, upgrade, PendingIntent.FLAG_UPDATE_CURRENT)
        notification.setContentIntent(upgradePending)

        NotificationManagerCompat.from(this).notify(667443, notification.build())
    }

    private fun notifyLastDay() {
        val notification = NotificationCompat.Builder(this, NotificationUtils.ACCOUNT_ACTIVITY_CHANNEL_ID)
                .setContentTitle(getString(R.string.notification_trial_title))
                .setSmallIcon(R.drawable.ic_stat_notify_group)
                .setLocalOnly(true)
                .setColor(Settings.mainColorSet.color)
                .setAutoCancel(true)
                .setContentText(getString(R.string.notification_last_day_of_trial))

        val upgrade = Intent(this, RedirectToMyAccount::class.java)
        val upgradePending = PendingIntent.getActivity(this, 1006, upgrade, PendingIntent.FLAG_UPDATE_CURRENT)
        notification.setContentIntent(upgradePending)

        NotificationManagerCompat.from(this).notify(667443, notification.build())
    }

    private fun notifyExpired() {
        val notification = NotificationCompat.Builder(this, NotificationUtils.ACCOUNT_ACTIVITY_CHANNEL_ID)
                .setContentTitle(getString(R.string.notification_trial_title))
                .setSmallIcon(R.drawable.ic_stat_notify_group)
                .setLocalOnly(true)
                .setColor(Settings.mainColorSet.color)
                .setAutoCancel(true)
                .setContentText(getString(R.string.notification_trial_expired))

        val upgrade = Intent(this, RedirectToMyAccount::class.java)
        val upgradePending = PendingIntent.getActivity(this, 1006, upgrade, PendingIntent.FLAG_UPDATE_CURRENT)
        notification.setContentIntent(upgradePending)

        NotificationManagerCompat.from(this).notify(667443, notification.build())
    }

    companion object {
        private const val JOB_ID = "free-trial-notifier"

        fun scheduleNextRun(context: Context) {
            val dispatcher = FirebaseJobDispatcher(GooglePlayDriver(context))
            val time = (TimeUtils.millisUntilHourInTheNextDay(14).toLong() / 1000).toInt() // 2:00 PM
            val myJob = dispatcher.newJobBuilder()
                    .setService(FreeTrialNotifierJob::class.java)
                    .setTag(JOB_ID)
                    .setRecurring(true)
                    .setLifetime(Lifetime.FOREVER)
                    .setTrigger(Trigger.executionWindow(time, time + (TimeUtils.MINUTE.toInt() / 1000 * 5)))
                    .setReplaceCurrent(true)
                    .build()

            dispatcher.mustSchedule(myJob)
        }
    }
}
