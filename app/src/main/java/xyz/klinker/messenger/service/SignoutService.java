package xyz.klinker.messenger.service;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.preference.PreferenceManager;

import java.util.Date;

import xyz.klinker.messenger.api.implementation.Account;
import xyz.klinker.messenger.api.implementation.ApiUtils;

public class SignoutService extends IntentService {

    private static final int REQUEST_CODE = 15;

    public SignoutService() {
        super("SignoutService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Account account = Account.get(this);

        // Only need to manage this on the primary device
        if (account.primary) {
            new ApiUtils().deleteAccount(account.accountId);
        }

        writeSignoutTime(this, 0);
    }

    public static void scheduleNextRun(Context context) {
        long signoutTime = PreferenceManager.getDefaultSharedPreferences(context)
                .getLong("account_signout_time", 0L);
        scheduleNextRun(context, signoutTime);
    }

    public static void scheduleNextRun(Context context, long signoutTime) {
        Intent intent = new Intent(context, ContactSyncService.class);
        PendingIntent pIntent = PendingIntent.getService(context, REQUEST_CODE,
                intent, PendingIntent.FLAG_UPDATE_CURRENT);

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(pIntent);

        if (signoutTime > new Date().getTime() && signoutTime != 0) {
            alarmManager.set(AlarmManager.RTC_WAKEUP, signoutTime, pIntent);
        }
    }

    public static void writeSignoutTime(Context context, long signoutTime) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit().putLong("account_signout_time", signoutTime)
                .commit();

        scheduleNextRun(context, signoutTime);
    }
}
