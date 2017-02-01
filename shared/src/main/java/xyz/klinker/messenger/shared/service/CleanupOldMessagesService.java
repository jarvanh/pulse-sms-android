package xyz.klinker.messenger.shared.service;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import java.util.Date;

import xyz.klinker.messenger.shared.data.DataSource;
import xyz.klinker.messenger.shared.data.Settings;

public class CleanupOldMessagesService extends IntentService {

    public CleanupOldMessagesService() {
        super("CleanupOldMessages");
    }

    private static final int REQUEST_CODE = 1330;
    private static final long RUN_EVERY = 1000 * 60 * 60 * 24; // 1 day

    @Override
    protected void onHandleIntent(Intent intent) {
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
        Intent intent = new Intent(context, CleanupOldMessagesService.class);
        PendingIntent pIntent = PendingIntent.getService(context, REQUEST_CODE,
                intent, PendingIntent.FLAG_UPDATE_CURRENT);

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        long currentTime = new Date().getTime();

        alarmManager.cancel(pIntent);
        alarmManager.set(AlarmManager.RTC_WAKEUP, currentTime + RUN_EVERY, pIntent);
    }
}
