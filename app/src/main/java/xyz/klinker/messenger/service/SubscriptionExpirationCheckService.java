package xyz.klinker.messenger.service;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;

import java.util.Date;
import java.util.List;

import xyz.klinker.messenger.R;
import xyz.klinker.messenger.activity.MessengerActivity;
import xyz.klinker.messenger.api.implementation.Account;
import xyz.klinker.messenger.data.FeatureFlags;
import xyz.klinker.messenger.util.RedirectToMyAccount;
import xyz.klinker.messenger.util.TimeUtils;
import xyz.klinker.messenger.util.billing.BillingHelper;
import xyz.klinker.messenger.util.billing.ProductPurchased;

public class SubscriptionExpirationCheckService extends IntentService {

    private static final String TAG = "SubscriptionCheck";

    private static final int REQUEST_CODE = 14;
    public static final int NOTIFICATION_ID = 1004;
    private static final int REQUEST_CODE_EMAIL = 1005;
    private static final int REQUEST_CODE_RENEW = 1006;

    private BillingHelper billing;

    public SubscriptionExpirationCheckService() {
        super("SubscriptionCheckService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.v(TAG, "starting subscription check service.");

        Account account = Account.get(this);

        billing = new BillingHelper(this);

        if (account.accountId != null && account.primary && account.subscriptionType != Account.SubscriptionType.LIFETIME) {
            Log.v(TAG, "checking for expiration");
            if (isExpired()) {
                Log.v(TAG, "service is expired");
                makeSignoutNotification();
                SignoutService.writeSignoutTime(this, new Date().getTime() + (TimeUtils.DAY * 2));
            } else {
                Log.v(TAG, "not expired, scheduling the next refresh");
                scheduleNextRun(this);
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        billing.destroy();
    }

    private void makeSignoutNotification() {
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
        account.updateSubscription(
                Account.SubscriptionType.LIFETIME, 1L, true);
    }

    private void writeNewExpirationToAccount(long time) {
        Account account = Account.get(this);
        account.updateSubscription(
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

        Intent intent = new Intent(context, SubscriptionExpirationCheckService.class);
        PendingIntent pIntent = PendingIntent.getService(context, REQUEST_CODE, intent, 0);

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(pIntent);

        if (account.accountId == null || account.subscriptionType == Account.SubscriptionType.LIFETIME || !account.primary) {
            return;
        }

        long expiration = account.subscriptionExpiration + TimeUtils.DAY;

        Log.v(TAG, "CURRENT TIME: " + new Date().toString());
        Log.v(TAG, "SCHEDULING NEW SUBSCRIPTION CHECK FOR: " + new Date(expiration).toString());
        alarmManager.setExact(AlarmManager.RTC_WAKEUP, expiration, pIntent);
    }
}
