package xyz.klinker.messenger.service;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import java.util.Date;

public class CleanupOldMessagesService extends IntentService {

    public CleanupOldMessagesService() {
        super("CleanupOldMessages");
    }

    private static final int REQUEST_CODE = 1330;
    private static final long RUN_EVERY = 1000 * 60 * 60 * 24; // 1 day

    @Override
    protected void onHandleIntent(Intent intent) {

    }

    public static void scheduleNextRun(Context context) {
        Intent intent = new Intent(context, CleanupOldMessagesService.class);
        PendingIntent pIntent = PendingIntent.getService(context, REQUEST_CODE,
                intent, PendingIntent.FLAG_UPDATE_CURRENT);

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        long currentTime = new Date().getTime();

        alarmManager.cancel(pIntent);
        alarmManager.set(AlarmManager.RTC_WAKEUP, currentTime + RUN_EVERY, pIntent);
    }
}
