package xyz.klinker.messenger.shared.service.jobs;

import android.annotation.TargetApi;
import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.os.Build;
import android.provider.Telephony;

import xyz.klinker.messenger.shared.service.ContentObserverService;

@TargetApi(ContentObserverJob.API_LEVEL)
public class ContentObserverJob extends BackgroundJob {

    public static final int API_LEVEL = Build.VERSION_CODES.N;
    private static final int JOB_ID = 2254;

    @Override
    void onRunJob(JobParameters parameters) {
        ContentObserverService.SmsContentObserver.processLastMessage(this);
        scheduleNextRun(this);
    }

    public static void scheduleNextRun(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return;
        }
        
        ComponentName component = new ComponentName(context, ContentObserverJob.class);
        JobInfo.Builder builder = new JobInfo.Builder(JOB_ID, component)
                .addTriggerContentUri(new JobInfo.TriggerContentUri(Telephony.MmsSms.CONTENT_URI, 0));

        JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        jobScheduler.schedule(builder.build());
    }
}
