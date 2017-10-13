package xyz.klinker.messenger.shared.service.jobs;

import android.annotation.SuppressLint;
import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import java.util.Date;

import xyz.klinker.messenger.api.implementation.Account;
import xyz.klinker.messenger.shared.service.NotificationService;
import xyz.klinker.messenger.shared.util.AndroidVersionUtil;
import xyz.klinker.messenger.shared.util.TimeUtils;

public class RepeatNotificationJob extends BackgroundJob {

    public static final int JOB_ID = 1224;

    @Override
    @SuppressLint("NewApi")
    void onRunJob(JobParameters parameters) {
        Intent intent = new Intent(this, NotificationService.class);
        if (!AndroidVersionUtil.INSTANCE.isAndroidO()) {
            startService(intent);
        } else {
            intent.putExtra(NotificationService.EXTRA_FOREGROUND, true);
            startForegroundService(intent);
        }
    }

    public static void scheduleNextRun(Context context, long nextRun) {
        long currentTime = new Date().getTime();
        long timeout = nextRun - currentTime;
        JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);

        if (!Account.INSTANCE.exists() || (Account.INSTANCE.exists() && Account.INSTANCE.getPrimary())) {
            ComponentName component = new ComponentName(context, RepeatNotificationJob.class);
            JobInfo.Builder builder = new JobInfo.Builder(JOB_ID, component)
                    .setMinimumLatency(timeout)
                    .setOverrideDeadline(timeout + (TimeUtils.SECOND * 15))
                    .setRequiresCharging(false)
                    .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                    .setRequiresDeviceIdle(false);
            if (currentTime < nextRun) {
                jobScheduler.schedule(builder.build());
            } else {
                jobScheduler.cancel(JOB_ID);
            }
        }
    }
}
