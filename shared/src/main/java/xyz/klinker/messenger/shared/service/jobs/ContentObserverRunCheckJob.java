package xyz.klinker.messenger.shared.service.jobs;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import xyz.klinker.messenger.api.implementation.Account;
import xyz.klinker.messenger.shared.service.ContentObserverService;
import xyz.klinker.messenger.shared.util.TimeUtils;

public class ContentObserverRunCheckJob extends BackgroundJob {

    private static final int JOB_ID = 12;
    private static final long RUN_EVERY = TimeUtils.MINUTE * 15; // 15 mins

    @Override
    protected void onRunJob(JobParameters parameters) {
//        if (!ContentObserverService.IS_RUNNING) {
//            startService(new Intent(this, ContentObserverService.class));
//        }

        scheduleNextRun(this);
    }

    public static void scheduleNextRun(Context context) {
        if (!Account.get(context).primary || Build.VERSION.SDK_INT >= ContentObserverJob.API_LEVEL ||
                ContentObserverService.nougatSamsung()) {
            return;
        }

        ComponentName component = new ComponentName(context, ContentObserverRunCheckJob.class);
        JobInfo.Builder builder = new JobInfo.Builder(JOB_ID, component)
                .setMinimumLatency(RUN_EVERY)
                .setOverrideDeadline(RUN_EVERY + (TimeUtils.MINUTE * 5))
                .setPersisted(true)
                .setRequiresCharging(false)
                .setRequiresDeviceIdle(false);

        JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        jobScheduler.cancel(JOB_ID);
        //jobScheduler.schedule(builder.build());
    }
}
