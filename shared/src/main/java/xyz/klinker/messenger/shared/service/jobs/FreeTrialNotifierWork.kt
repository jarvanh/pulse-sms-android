package xyz.klinker.messenger.shared.service.jobs

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import xyz.klinker.messenger.shared.util.TimeUtils
import xyz.klinker.messenger.api.implementation.Account
import xyz.klinker.messenger.api.implementation.firebase.AnalyticsHelper
import xyz.klinker.messenger.shared.R
import xyz.klinker.messenger.shared.data.Settings
import xyz.klinker.messenger.shared.util.NotificationUtils
import xyz.klinker.messenger.shared.util.RedirectToMyAccount
import java.util.concurrent.TimeUnit

class FreeTrialNotifierWork(private val context: Context, params: WorkerParameters) : Worker(context, params) {

    override fun doWork(): Result {
        if (Account.exists() && Account.subscriptionType == Account.SubscriptionType.FREE_TRIAL) {
            val daysLeft = Account.getDaysLeftInTrial()
            AnalyticsHelper.accountTrialDay(context, daysLeft)
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

        scheduleNextRun(context)
        return Result.success()
    }

    private fun notifyDaysLeft(left: Int) {
        val notification = NotificationCompat.Builder(context, NotificationUtils.ACCOUNT_ACTIVITY_CHANNEL_ID)
                .setContentTitle(context.getString(R.string.notification_trial_title))
                .setSmallIcon(R.drawable.ic_stat_notify_group)
                .setLocalOnly(true)
                .setColor(Settings.mainColorSet.color)
                .setAutoCancel(true)
                .setContentText(context.getString(R.string.notification_days_remaining_in_trial, left.toString()))

        val upgrade = Intent(context, RedirectToMyAccount::class.java)
        val upgradePending = PendingIntent.getActivity(context, 1006, upgrade, PendingIntent.FLAG_UPDATE_CURRENT)
        notification.setContentIntent(upgradePending)

        NotificationManagerCompat.from(context).notify(667443, notification.build())
    }

    private fun notifyLastDay() {
        val notification = NotificationCompat.Builder(context, NotificationUtils.ACCOUNT_ACTIVITY_CHANNEL_ID)
                .setContentTitle(context.getString(R.string.notification_trial_title))
                .setSmallIcon(R.drawable.ic_stat_notify_group)
                .setLocalOnly(true)
                .setColor(Settings.mainColorSet.color)
                .setAutoCancel(true)
                .setContentText(context.getString(R.string.notification_last_day_of_trial))

        val upgrade = Intent(context, RedirectToMyAccount::class.java)
        val upgradePending = PendingIntent.getActivity(context, 1006, upgrade, PendingIntent.FLAG_UPDATE_CURRENT)
        notification.setContentIntent(upgradePending)

        NotificationManagerCompat.from(context).notify(667443, notification.build())
    }

    private fun notifyExpired() {
        val notification = NotificationCompat.Builder(context, NotificationUtils.ACCOUNT_ACTIVITY_CHANNEL_ID)
                .setContentTitle(context.getString(R.string.notification_trial_title))
                .setSmallIcon(R.drawable.ic_stat_notify_group)
                .setLocalOnly(true)
                .setColor(Settings.mainColorSet.color)
                .setAutoCancel(true)
                .setContentText(context.getString(R.string.notification_trial_expired))

        val upgrade = Intent(context, RedirectToMyAccount::class.java)
        val upgradePending = PendingIntent.getActivity(context, 1006, upgrade, PendingIntent.FLAG_UPDATE_CURRENT)
        notification.setContentIntent(upgradePending)

        NotificationManagerCompat.from(context).notify(667443, notification.build())
    }

    companion object {

        fun scheduleNextRun(context: Context) {
            val time = TimeUtils.millisUntilHourInTheNextDay(14)
            val work = OneTimeWorkRequest.Builder(FreeTrialNotifierWork::class.java)
                    .setInitialDelay(time, TimeUnit.MILLISECONDS)
                    .build()
            try {
                WorkManager.getInstance().enqueue(work)
            } catch (e: Exception) {
                // can't schedule more than 100 unique tasks?
            }
        }
    }
}
