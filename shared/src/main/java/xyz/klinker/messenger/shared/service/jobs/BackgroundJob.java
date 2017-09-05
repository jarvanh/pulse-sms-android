package xyz.klinker.messenger.shared.service.jobs;

import android.annotation.SuppressLint;
import android.app.job.JobParameters;
import android.app.job.JobService;
import android.os.AsyncTask;

public abstract class BackgroundJob extends JobService {

    abstract void onRunJob(JobParameters parameters);

    @Override
    @SuppressLint("StaticFieldLeak")
    public final boolean onStartJob(final JobParameters params) {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... args) {
                onRunJob(params);
                return null;
            }

            @Override
            protected void onPostExecute(Void v) {
                jobFinished(params, false);
            }
        }.execute();

        return true;
    }

    @Override
    public final boolean onStopJob(JobParameters params) {
        return false;
    }
}
