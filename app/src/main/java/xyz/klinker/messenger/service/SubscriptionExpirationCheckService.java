package xyz.klinker.messenger.service;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;

import java.util.Date;

import xyz.klinker.messenger.R;
import xyz.klinker.messenger.activity.MessengerActivity;
import xyz.klinker.messenger.api.implementation.Account;
import xyz.klinker.messenger.data.FeatureFlags;
import xyz.klinker.messenger.util.RedirectToMyAccount;
import xyz.klinker.messenger.util.TimeUtils;

public class SubscriptionExpirationCheckService extends IntentService {

    private static final int REQUEST_CODE = 14;
    public static final int NOTIFICATION_ID = 1004;
    private static final int REQUEST_CODE_EMAIL = 1005;
    private static final int REQUEST_CODE_RENEW = 1006;

    public SubscriptionExpirationCheckService() {
        super("SubscriptionCheckService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Account account = Account.get(this);

        if (account.accountId != null && account.primary) {
            if (isExpired()) {
                makeSignoutNotification();
                //SignoutService.writeSignoutTime(this, new Date().getTime() + TimeUtils.DAY * 2);
            } else {
                scheduleNextRun(this);
            }
        }
    }

    private void makeSignoutNotification() {
        // todo alert the user that they will be signed out in two days since their subscription ended.
        // make an email button since the play store cannot always be trusted to get the right value... ugh

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                .setContentTitle(getString(R.string.no_subscription_found))
                .setContentText(getString(R.string.cancelled_subscription_error))
                .setStyle(new NotificationCompat.BigTextStyle()
                        .setBigContentTitle(getString(R.string.no_subscription_found))
                        .setSummaryText(getString(R.string.cancelled_subscription_error)))
                .setSmallIcon(R.drawable.ic_stat_notify_group)
                .setColor(getResources().getColor(R.color.colorPrimary));

        Intent renew = new Intent(this, RedirectToMyAccount.class);

        String subject = "Pulse Subscription";
        Uri uri = Uri.parse("mailto:luke@klinkerapps.com")
                .buildUpon()
                .appendQueryParameter("subject", subject)
                .build();

        Intent email = new Intent(Intent.ACTION_SENDTO, uri);
        email.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        email.putExtra(Intent.EXTRA_EMAIL, new String[]{"luke@klinkerapps.com"});
        email.putExtra(Intent.EXTRA_SUBJECT, subject);
        email.putExtra(Intent.EXTRA_TEXT, "The Play Store sometimes sucks at determining what you have purchased in the past. Please include the order number of your purchase in this email (which can be found from the Play Store app). I will help you get it worked out!");

        PendingIntent emailPending = PendingIntent.getActivity(this, REQUEST_CODE_EMAIL, email, PendingIntent.FLAG_ONE_SHOT);
        PendingIntent renewPending = PendingIntent.getActivity(this, REQUEST_CODE_RENEW, renew, PendingIntent.FLAG_ONE_SHOT);

        NotificationCompat.Action renewAction = new NotificationCompat.Action(R.drawable.ic_account, getString(R.string.renew), renewPending);
        NotificationCompat.Action emailAction = new NotificationCompat.Action(R.drawable.ic_about, getString(R.string.email), emailPending);

        builder.addAction(renewAction).addAction(emailAction);
        builder.setContentIntent(renewPending);

        NotificationManagerCompat.from(this).notify(NOTIFICATION_ID, builder.build());
    }

    private boolean isExpired() {
        // todo check with the play store to see the status of their subscription
        // if it is active, lets update the subscription expiration date
        // make sure that the
        return true;
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
        if (account.subscriptionType == Account.SubscriptionType.LIFETIME || !account.primary || FeatureFlags.IS_BETA) {
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
