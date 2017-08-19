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
import android.content.SharedPreferences;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;

import java.util.Date;
import java.util.List;

import xyz.klinker.messenger.shared.R;
import xyz.klinker.messenger.api.implementation.Account;
import xyz.klinker.messenger.shared.data.ColorSet;
import xyz.klinker.messenger.shared.data.Settings;
import xyz.klinker.messenger.shared.util.RedirectToMyAccount;
import xyz.klinker.messenger.shared.util.TimeUtils;
import xyz.klinker.messenger.shared.util.billing.BillingHelper;
import xyz.klinker.messenger.shared.util.billing.ProductPurchased;

public class SubscriptionExpirationCheckJob extends BackgroundJob {

    private static final String TAG = "SubscriptionCheck";

    private static final int JOB_ID = 14;
    public static final int NOTIFICATION_ID = 1004;
    private static final int REQUEST_CODE_EMAIL = 1005;
    private static final int REQUEST_CODE_RENEW = 1006;

    private BillingHelper billing;

    @Override
    protected void onRunJob(JobParameters parameters) {
        Log.v(TAG, "starting subscription check service.");

        Account account = Account.get(this);

        billing = new BillingHelper(this);

        if (account.exists()&& account.primary && account.subscriptionType != Account.SubscriptionType.LIFETIME) {
            Log.v(TAG, "checking for expiration");
            if (isExpired()) {
                Log.v(TAG, "service is expired");
                makeSignoutNotification();
                SignoutJob.writeSignoutTime(this, new Date().getTime() + (TimeUtils.DAY * 2));
            } else {
                Log.v(TAG, "not expired, scheduling the next refresh");
                scheduleNextRun(this);
                SignoutJob.writeSignoutTime(this, 0);
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (billing != null) {
            billing.destroy();
        }
    }

    private void makeSignoutNotification() {
        SharedPreferences sharedPreferences = Settings.get(this).getSharedPrefs(this);
        if (sharedPreferences.getBoolean("seen_subscription_expired_notification", false)) {
            return;
        } else {
            sharedPreferences.edit().putBoolean("seen_subscription_expired_notification", true).apply();
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                .setContentTitle(getString(R.string.no_subscription_found))
                .setContentText(getString(R.string.cancelled_subscription_error))
                .setStyle(new NotificationCompat.BigTextStyle()
                        .setBigContentTitle(getString(R.string.no_subscription_found))
                        .setSummaryText(getString(R.string.cancelled_subscription_error)))
                .setSmallIcon(R.drawable.ic_stat_notify_group)
                .setColor(ColorSet.DEFAULT(this).color);

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

        PendingIntent emailPending = PendingIntent.getActivity(this, REQUEST_CODE_EMAIL, email, PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent renewPending = PendingIntent.getActivity(this, REQUEST_CODE_RENEW, renew, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Action renewAction = new NotificationCompat.Action(R.drawable.ic_account, getString(R.string.renew), renewPending);
        NotificationCompat.Action emailAction = new NotificationCompat.Action(R.drawable.ic_about, getString(R.string.email), emailPending);

        builder.addAction(renewAction).addAction(emailAction);
        builder.setContentIntent(renewPending);

        NotificationManagerCompat.from(this).notify(NOTIFICATION_ID, builder.build());
    }

    private boolean isExpired() {
        List<ProductPurchased> purchasedList = billing.queryAllPurchasedProducts();

        if (purchasedList.size() > 0) {
            ProductPurchased best = getBestProduct(purchasedList);

            if (best.getProductId().equals("lifetime")) {
                writeLifetimeSubscriber();
            } else {
                writeNewExpirationToAccount(new Date().getTime() + best.getExperation());
            }

            return false;
        } else {
            return true;
        }
    }

    private void writeLifetimeSubscriber() {
        Account account = Account.get(this);
        account.updateSubscription(this,
                Account.SubscriptionType.LIFETIME, 1L, true);
    }

    private void writeNewExpirationToAccount(long time) {
        Account account = Account.get(this);
        account.updateSubscription(this,
                Account.SubscriptionType.SUBSCRIBER, time, true);
    }

    private ProductPurchased getBestProduct(List<ProductPurchased> products) {
        ProductPurchased best = products.get(0);

        for (ProductPurchased productPurchased : products) {
            if (productPurchased.isBetterThan(best)) {
                best = productPurchased;
            }
        }

        return best;
    }

    public static void scheduleNextRun(Context context) {
        Account account = Account.get(context);
        JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);

        long currentTime = new Date().getTime();
        long expiration = account.subscriptionExpiration + TimeUtils.DAY;

        ComponentName component = new ComponentName(context, SubscriptionExpirationCheckJob.class);
        JobInfo.Builder builder = new JobInfo.Builder(JOB_ID, component)
                .setMinimumLatency(expiration - currentTime)
                .setRequiresCharging(false)
                .setRequiresDeviceIdle(false);

        if (account.accountId == null || account.subscriptionType == Account.SubscriptionType.LIFETIME || !account.primary) {
            jobScheduler.cancel(JOB_ID);
        } else {
            Log.v(TAG, "CURRENT TIME: " + new Date().toString());
            Log.v(TAG, "SCHEDULING NEW SIGNOUT CHECK FOR: " + new Date(expiration).toString());

            jobScheduler.schedule(builder.build());
        }
    }
}
