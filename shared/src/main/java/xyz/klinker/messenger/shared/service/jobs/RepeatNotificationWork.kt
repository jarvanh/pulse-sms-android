package xyz.klinker.messenger.shared.service.jobs

import android.content.Context
import androidx.work.*
import xyz.klinker.messenger.api.implementation.Account
import xyz.klinker.messenger.shared.service.notification.Notifier
import java.util.concurrent.TimeUnit

class RepeatNotificationWork(private val context: Context, params: WorkerParameters) : Worker(context, params) {

    override fun doWork(): Result {
        Notifier(context).notify()
        return Result.success()
    }

    companion object {

        fun scheduleNextRun(context: Context, timeout: Long) {
            if (Account.exists() && !Account.primary) {
                return
            }

            val work = OneTimeWorkRequest.Builder(RepeatNotificationWork::class.java)
                    .setInitialDelay(timeout, TimeUnit.MILLISECONDS)
                    .build()

            WorkManager.getInstance().enqueue(work)
        }
    }

}
