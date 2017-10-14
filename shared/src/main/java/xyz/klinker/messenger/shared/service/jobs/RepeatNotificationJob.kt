package xyz.klinker.messenger.shared.service.jobs

import android.annotation.SuppressLint
import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import xyz.klinker.messenger.api.implementation.Account
import xyz.klinker.messenger.shared.service.notification.NotificationConstants
import xyz.klinker.messenger.shared.service.notification.NotificationService
import xyz.klinker.messenger.shared.util.AndroidVersionUtil
import xyz.klinker.messenger.shared.util.TimeUtils
import java.util.*

class RepeatNotificationJob : BackgroundJob() {

    @SuppressLint("NewApi")
    override fun onRunJob(parameters: JobParameters?) {
        val intent = Intent(this, NotificationService::class.java)
        if (!AndroidVersionUtil.isAndroidO) {
            startService(intent)
        } else {
            intent.putExtra(NotificationConstants.EXTRA_FOREGROUND, true)
            startForegroundService(intent)
        }
    }

    companion object {
        private val JOB_ID = 1224

        fun scheduleNextRun(context: Context, nextRun: Long) {
            val currentTime = Date().time
            val timeout = nextRun - currentTime
            val jobScheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler

            if (!Account.exists() || Account.exists() && Account.primary) {
                val component = ComponentName(context, RepeatNotificationJob::class.java)
                val builder = JobInfo.Builder(JOB_ID, component)
                        .setMinimumLatency(timeout)
                        .setOverrideDeadline(timeout + TimeUtils.SECOND * 15)
                        .setRequiresCharging(false)
                        .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                        .setRequiresDeviceIdle(false)
                if (currentTime < nextRun) {
                    jobScheduler.schedule(builder.build())
                } else {
                    jobScheduler.cancel(JOB_ID)
                }
            }
        }
    }
}
