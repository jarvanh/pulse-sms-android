package xyz.klinker.messenger.shared.service.jobs

import android.annotation.SuppressLint
import android.app.job.JobParameters
import android.app.job.JobService
import android.os.AsyncTask

abstract class BackgroundJob : JobService() {

    internal abstract fun onRunJob(parameters: JobParameters?)

    @SuppressLint("StaticFieldLeak")
    override fun onStartJob(params: JobParameters?): Boolean {
        object : AsyncTask<Void, Void, Void>() {
            override fun doInBackground(vararg args: Void): Void? {
                onRunJob(params)
                return null
            }

            override fun onPostExecute(v: Void?) {
                jobFinished(params, false)
            }
        }.execute()
        return true
    }

    override fun onStopJob(params: JobParameters?): Boolean {
        return false
    }
}
