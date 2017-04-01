package xyz.klinker.messenger.shared.service.jobs;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

import java.util.Date;

import xyz.klinker.messenger.api.implementation.Account;
import xyz.klinker.messenger.shared.service.ContentObserverService;
import xyz.klinker.messenger.shared.util.TimeUtils;

public class ContentObserverRunCheckJob extends IntentService {

    private static final int JOB_ID = 12;
    private static final long RUN_EVERY = 1000 * 60 * 15; // 15 mins

    public ContentObserverRunCheckJob() {
        super("ContentObserverRunCheck");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (!ContentObserverService.IS_RUNNING) {
            startService(new Intent(this, ContentObserverService.class));
        }

        scheduleNextRun(this);
    }

    public static void scheduleNextRun(Context context) {
        if (!Account.get(context).primary) {
            return;
        }

        ComponentName component = new ComponentName(context, ContentObserverRunCheckJob.class);
        JobInfo.Builder builder = new JobInfo.Builder(JOB_ID, component)
                .setPeriodic(RUN_EVERY)
                .setPersisted(true)
                .setRequiresCharging(false)
                .setRequiresDeviceIdle(true);

        JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        jobScheduler.schedule(builder.build());
    }
}
