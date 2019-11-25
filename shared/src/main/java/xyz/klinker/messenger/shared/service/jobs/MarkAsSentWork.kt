package xyz.klinker.messenger.shared.service.jobs

import android.content.Context
import androidx.work.*
import xyz.klinker.messenger.api.implementation.Account
import xyz.klinker.messenger.shared.receiver.SmsSentReceiver
import java.util.concurrent.TimeUnit

/**
 * Some devices don't seem to ever get messages marked as sent and I don't really know why.
 * This job can be started to mark old messages that are "sending" as sent.
 */
class MarkAsSentWork(private val context: Context, params: WorkerParameters) : Worker(context, params) {

    override fun doWork(): Result {
        if (Account.exists() && !Account.primary) {
            return Result.success()
        }

        SmsSentReceiver.markLatestAsRead(context)
        return Result.success()
    }

    companion object {

        private const val JOB_ID = "mark-as-sent"

        fun scheduleNextRun(context: Context?) {
            if (context == null || (Account.exists() && !Account.primary)) {
                return
            }

            val work = OneTimeWorkRequest.Builder(MarkAsSentWork::class.java)
                    .setInitialDelay(30, TimeUnit.SECONDS)
                    .build()
            WorkManager.getInstance().enqueueUniqueWork(JOB_ID, ExistingWorkPolicy.REPLACE, work)
        }
    }
}
