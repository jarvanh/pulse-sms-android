package xyz.klinker.messenger.shared.service.jobs;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

import java.util.Date;

import xyz.klinker.messenger.shared.data.DataSource;
import xyz.klinker.messenger.shared.data.Settings;
import xyz.klinker.messenger.shared.util.TimeUtils;

public class CleanupOldMessagesJob extends BackgroundJob {

    private static final int JOB_ID = 1330;

    @Override
    protected void onRunJob(JobParameters parameters) {
        DataSource source = DataSource.getInstance(this);
        source.open();

        long timeout = Settings.get(this).cleanupMessagesTimeout;
        if (timeout > 0) {
            source.cleanupOldMessages(System.currentTimeMillis() - timeout);
        }

        source.close();

        scheduleNextRun(this);
    }

    public static void scheduleNextRun(Context context) {
        ComponentName component = new ComponentName(context, CleanupOldMessagesJob.class);
        JobInfo.Builder builder = new JobInfo.Builder(JOB_ID, component)
                .setMinimumLatency(TimeUtils.millisUntilHourInTheNextDay(3)) // 3 AM
                .setRequiresCharging(true)
                .setRequiresDeviceIdle(true);

        JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        jobScheduler.schedule(builder.build());
    }
}
