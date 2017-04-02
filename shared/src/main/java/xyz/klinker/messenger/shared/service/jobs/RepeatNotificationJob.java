package xyz.klinker.messenger.shared.service.jobs;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;

import java.util.Date;

import xyz.klinker.messenger.api.implementation.Account;
import xyz.klinker.messenger.shared.util.TimeUtils;

public class RepeatNotificationJob extends JobService {

    public static final int JOB_ID = 1224;

    @Override
    public boolean onStartJob(JobParameters params) {
        return false;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        return false;
    }

    public static void scheduleNextRun(Context context, long nextRun) {
        long currentTime = new Date().getTime();
        long timeout = nextRun - currentTime;
        JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);

        if (!Account.get(context).exists() || (Account.get(context).exists() && Account.get(context).primary)) {
            ComponentName component = new ComponentName(context, RepeatNotificationJob.class);
            JobInfo.Builder builder = new JobInfo.Builder(JOB_ID, component)
                    .setMinimumLatency(timeout)
                    .setOverrideDeadline(timeout + TimeUtils.MINUTE)
                    .setRequiresCharging(false)
                    .setRequiresDeviceIdle(false);

            if (currentTime < nextRun) {
                jobScheduler.schedule(builder.build());
            } else {
                jobScheduler.cancel(JOB_ID);
            }
        }
    }
}
