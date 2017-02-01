package xyz.klinker.messenger.shared.service;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.preference.PreferenceManager;
import android.util.Log;

import java.util.Date;
import java.util.List;

import xyz.klinker.messenger.api.implementation.Account;
import xyz.klinker.messenger.api.implementation.ApiUtils;
import xyz.klinker.messenger.shared.util.billing.BillingHelper;
import xyz.klinker.messenger.shared.util.billing.ProductPurchased;

public class SignoutService extends IntentService {

    private static final String TAG = "SignoutService";

    private static final int REQUEST_CODE = 15;

    private BillingHelper billing;

    public SignoutService() {
        super("SignoutService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.v(TAG, "starting signout service.");

        Account account = Account.get(this);
        billing = new BillingHelper(this);

        // Only need to manage this on the primary device
        if (account.exists() && account.primary && account.subscriptionType != Account.SubscriptionType.LIFETIME &&
                account.subscriptionExpiration < new Date().getTime() && isExpired()) {
            Log.v(TAG, "forcing signout due to expired account!");
            final String accountId = account.accountId;

            account.clearAccount();
            new ApiUtils().deleteAccount(accountId);
        } else {
            Log.v(TAG, "account not expired, scheduling the check again.");
            SubscriptionExpirationCheckService.scheduleNextRun(this);
        }

        writeSignoutTime(this, 0);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        billing.destroy();
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
        long signoutTime = PreferenceManager.getDefaultSharedPreferences(context)
                .getLong("account_signout_time", 0L);
        scheduleNextRun(context, signoutTime);
    }

    public static void scheduleNextRun(Context context, long signoutTime) {
        Account account = Account.get(context);

        Intent intent = new Intent(context, SignoutService.class);
        PendingIntent pIntent = PendingIntent.getService(context, REQUEST_CODE, intent, 0);

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(pIntent);

        if (account.accountId == null || account.subscriptionType == Account.SubscriptionType.LIFETIME || !account.primary || signoutTime == 0) {
            return;
        }

        Log.v(TAG, "CURRENT TIME: " + new Date().toString());
        Log.v(TAG, "SCHEDULING NEW SIGNOUT CHECK FOR: " + new Date(signoutTime).toString());
        alarmManager.setExact(AlarmManager.RTC_WAKEUP, signoutTime, pIntent);
    }

    public static void writeSignoutTime(Context context, long signoutTime) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit().putLong("account_signout_time", signoutTime)
                .commit();

        scheduleNextRun(context, signoutTime);
    }

    public static long isScheduled(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getLong("account_signout_time", 0L);
    }
}
