package xyz.klinker.messenger.shared.service.jobs

import android.content.Context
import com.firebase.jobdispatcher.*
import xyz.klinker.messenger.api.implementation.Account
import xyz.klinker.messenger.shared.service.notification.Notifier
import xyz.klinker.messenger.shared.util.TimeUtils
import java.util.*

class RepeatNotificationJob : SimpleJobService() {

    override fun onRunJob(job: JobParameters?): Int {
        Notifier(this).notify()
        return 0
    }

    companion object {
        private val JOB_ID = "repeat-notifications"

        fun scheduleNextRun(context: Context, nextRun: Long) {
            val dispatcher = FirebaseJobDispatcher(GooglePlayDriver(context))

            if (nextRun == 0L) {
                dispatcher.cancel(JOB_ID)
                return
            }
            
            val currentTime = Date().time
            val timeout = (nextRun - currentTime).toInt() / 1000

            if (!Account.exists() || Account.exists() && Account.primary) {
                try {
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
                } catch (e: Throwable) {
                    // can't schedule for less than 0
                }
            }
        }
    }
}
