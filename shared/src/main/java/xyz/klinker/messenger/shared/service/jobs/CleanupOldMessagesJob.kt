package xyz.klinker.messenger.shared.service.jobs

import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import xyz.klinker.messenger.shared.data.DataSource
import xyz.klinker.messenger.shared.data.Settings
import xyz.klinker.messenger.shared.util.TimeUtils

class CleanupOldMessagesJob : BackgroundJob() {

    override fun onRunJob(parameters: JobParameters) {
        val timeout = Settings.cleanupMessagesTimeout
        if (timeout > 0) {
            DataSource.cleanupOldMessages(this, System.currentTimeMillis() - timeout)
        }

        scheduleNextRun(this)
    }

    companion object {
        private val JOB_ID = 1330

        fun scheduleNextRun(context: Context) {
            val component = ComponentName(context, CleanupOldMessagesJob::class.java)
            val builder = JobInfo.Builder(JOB_ID, component)
                    .setMinimumLatency(TimeUtils.millisUntilHourInTheNextDay(3).toLong()) // 3 AM
                    .setRequiresCharging(true)
                    .setRequiresDeviceIdle(true)

            val jobScheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
            jobScheduler.schedule(builder.build())
        }
    }
}
