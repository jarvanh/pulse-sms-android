package xyz.klinker.messenger.shared.service.jobs

import android.content.Context
import androidx.work.*
import xyz.klinker.messenger.shared.data.DataSource
import xyz.klinker.messenger.shared.data.Settings
import xyz.klinker.messenger.shared.util.TimeUtils
import java.util.concurrent.TimeUnit

class CleanupOldMessagesWork(private val context: Context, params: WorkerParameters) : Worker(context, params) {

    override fun doWork(): Result {
        val timeout = Settings.cleanupMessagesTimeout
        if (timeout > 0) {
            DataSource.cleanupOldMessages(context, TimeUtils.now - timeout)
        }

        scheduleNextRun(context)
        return Result.success()
    }

    companion object {

        fun scheduleNextRun(context: Context) {
            val time = TimeUtils.millisUntilHourInTheNextDay(3)
            val work = OneTimeWorkRequest.Builder(CleanupOldMessagesWork::class.java)
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
