package xyz.klinker.messenger.service;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import java.util.Date;

import xyz.klinker.messenger.api.implementation.Account;
import xyz.klinker.messenger.util.TimeUtils;

public class SubscriptionExpirationCheckService extends IntentService {

    private static final int REQUEST_CODE = 14;

    public SubscriptionExpirationCheckService() {
        super("SubscriptionCheckService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Account account = Account.get(this);

        if (account.accountId != null && account.primary) {
            if (isExpired()) {
                makeSignoutNotification();
                SignoutService.writeSignoutTime(this, new Date().getTime() + TimeUtils.DAY * 2);
            } else {
                scheduleNextRun(this);
            }
        }
    }

    private void makeSignoutNotification() {
        // todo alert the user that they will be signed out in two days since their subscription ended.
        // make an email button since the play store cannot always be trusted to get the right value... ugh
    }

    private boolean isExpired() {
        // todo check with the play store to see the status of their subscription
        // if it is active, lets update the subscription expiration date
        // make sure that the
        return false;
    }

    private void writeNewExpirationToAccount(Date expiration) {
        Account account = Account.get(this);

        if (account.subscriptionType != Account.SubscriptionType.LIFETIME) {
            account.updateSubscription(
                    Account.SubscriptionType.SUBSCRIBER, expiration.getTime(), true);
        }
    }

    public static void scheduleNextRun(Context context) {
        Account account = Account.get(context);
        if (account.subscriptionType == Account.SubscriptionType.LIFETIME || !account.primary) {
            return;
        }

        long expiration = account.subscriptionExpiration;

        Intent intent = new Intent(context, ContactSyncService.class);
        PendingIntent pIntent = PendingIntent.getService(context, REQUEST_CODE,
                intent, PendingIntent.FLAG_UPDATE_CURRENT);

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(pIntent);

        if (expiration < new Date().getTime() || expiration == 0) {
            // set it up one day after the subscription end time, just to give a bit of padding.
            alarmManager.set(AlarmManager.RTC_WAKEUP, Account.get(context).subscriptionExpiration + TimeUtils.DAY, pIntent);
        }
    }
}
