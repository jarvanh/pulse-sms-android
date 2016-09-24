package xyz.klinker.messenger.service;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import java.util.Date;

public class ContentObserverRunCheckService extends IntentService {

    private static final int REQUEST_CODE = 12;
    private static final long RUN_EVERY = 1000 * 60 * 10; // 10 mins

    public ContentObserverRunCheckService() {
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
        Intent intent = new Intent(context, ContentObserverRunCheckService.class);
        PendingIntent pIntent = PendingIntent.getService(context, REQUEST_CODE,
                intent, PendingIntent.FLAG_UPDATE_CURRENT);

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        long currentTime = new Date().getTime();

        alarmManager.cancel(pIntent);
        alarmManager.set(AlarmManager.RTC_WAKEUP, currentTime + RUN_EVERY, pIntent);
    }
}
