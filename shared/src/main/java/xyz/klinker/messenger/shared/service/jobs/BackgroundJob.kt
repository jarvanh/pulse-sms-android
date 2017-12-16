package xyz.klinker.messenger.shared.service.jobs

import android.app.job.JobParameters
import android.app.job.JobService

abstract class BackgroundJob : JobService() {

    internal abstract fun onRunJob(parameters: JobParameters?)

    override fun onStartJob(params: JobParameters?): Boolean {
        Thread {
            onRunJob(params)
            jobFinished(params, false)
        }.start()
        return true
    }

    override fun onStopJob(params: JobParameters?): Boolean {
        return false
    }
}
