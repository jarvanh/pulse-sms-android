package xyz.klinker.messenger.shared.service.jobs;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.os.AsyncTask;

public abstract class BackgroundJob extends JobService {

    abstract void onRunJob(JobParameters parameters);

    @Override
    public boolean onStartJob(final JobParameters params) {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... args) {
                onRunJob(params);
                return null;
            }
        }.execute();

        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        return false;
    }
}
