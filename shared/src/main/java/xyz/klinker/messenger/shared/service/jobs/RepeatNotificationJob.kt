package xyz.klinker.messenger.shared.service.jobs

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import com.firebase.jobdispatcher.*
import xyz.klinker.messenger.api.implementation.Account
import xyz.klinker.messenger.shared.service.notification.NotificationConstants
import xyz.klinker.messenger.shared.service.notification.NotificationService
import xyz.klinker.messenger.shared.util.AndroidVersionUtil
import xyz.klinker.messenger.shared.util.TimeUtils
import java.util.*

class RepeatNotificationJob : JobService() {

    override fun onStopJob(job: JobParameters?): Boolean {
        return false
    }

    @SuppressLint("NewApi")
    override fun onStartJob(job: JobParameters?): Boolean {
        val intent = Intent(this, NotificationService::class.java)
        if (!AndroidVersionUtil.isAndroidO) {
            startService(intent)
        } else {
            intent.putExtra(NotificationConstants.EXTRA_FOREGROUND, true)
            startForegroundService(intent)
        }

        return false
    }

    companion object {
        private val JOB_ID = "repeat-notifications"

        fun scheduleNextRun(context: Context, nextRun: Long) {
            val dispatcher = FirebaseJobDispatcher(GooglePlayDriver(context))

            val currentTime = Date().time
            val timeout = (nextRun - currentTime).toInt() / 1000

            if (!Account.exists() || Account.exists() && Account.primary) {
                val myJob = dispatcher.newJobBuilder()
                        .setService(RepeatNotificationJob::class.java)
                        .setTag(JOB_ID)
                        .setRecurring(false)
                        .setLifetime(Lifetime.FOREVER)
                        .setTrigger(Trigger.executionWindow(timeout, timeout + TimeUtils.SECOND.toInt() * 15 / 1000))
                        .setReplaceCurrent(true)
                        .build()

                if (currentTime < nextRun) {
                    dispatcher.mustSchedule(myJob)
                } else {
                    dispatcher.cancel(JOB_ID)
                }
            }
        }
    }
}
