package xyz.klinker.messenger.shared.service.jobs

import android.content.Context
import androidx.work.*
import xyz.klinker.messenger.shared.data.Settings
import xyz.klinker.messenger.shared.service.QuickComposeNotificationService
import java.util.concurrent.TimeUnit

class RepostQuickComposeNotificationWork(private val context: Context, params: WorkerParameters) : Worker(context, params) {

    override fun doWork(): Result {
        if (Settings.quickCompose) {
            QuickComposeNotificationService.start(context)
        } else {
            WorkManager.getInstance().cancelUniqueWork(JOB_ID)
            QuickComposeNotificationService.stop(context)
        }

        return Result.success()
    }

    companion object {

        private const val JOB_ID = "quick-compose-reposter"

        fun scheduleNextRun(context: Context?) {
            if (context == null) {
                return
            }

            if (!Settings.quickCompose) {
                WorkManager.getInstance().cancelUniqueWork(JOB_ID)
                return
            }

            val work = PeriodicWorkRequest.Builder(RepostQuickComposeNotificationWork::class.java, 30L, TimeUnit.MINUTES)
                    .build()
            WorkManager.getInstance().enqueueUniquePeriodicWork(JOB_ID, ExistingPeriodicWorkPolicy.KEEP, work)
        }
    }
}
