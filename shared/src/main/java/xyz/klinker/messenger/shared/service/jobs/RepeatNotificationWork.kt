package xyz.klinker.messenger.shared.service.jobs

import android.content.Context
import androidx.work.*
import xyz.klinker.messenger.api.implementation.Account
import xyz.klinker.messenger.shared.service.notification.Notifier
import java.util.concurrent.TimeUnit

class RepeatNotificationWork(private val context: Context, params: WorkerParameters) : Worker(context, params) {

    override fun doWork(): Result {
        if (!firstRun) {
            Notifier(context).notify(null, true)
        }

        firstRun = false
        return Result.success()
    }

    companion object {

        // periodic work is executed immediately, which we don't really want in this case. That would produce a duplicate notification
        private var firstRun = true

        private const val JOB_ID = "repeat-notifications"

        fun scheduleNextRun(context: Context, timeout: Long) {
            if (Account.exists() && !Account.primary) {
                return
            }

            firstRun = true

            // tried it with the normal one time work request and it didn't work well. The queue got too large, since we weren't using unique work
            val work = PeriodicWorkRequest.Builder(RepeatNotificationWork::class.java, timeout, TimeUnit.MILLISECONDS)
                    .build()
            WorkManager.getInstance().enqueueUniquePeriodicWork(JOB_ID, ExistingPeriodicWorkPolicy.REPLACE, work)
        }

        fun cancel(context: Context) {
            WorkManager.getInstance().cancelUniqueWork(JOB_ID)
        }
    }

}
