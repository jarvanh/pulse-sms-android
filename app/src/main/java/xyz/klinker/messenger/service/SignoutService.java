package xyz.klinker.messenger.service;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.preference.PreferenceManager;

import java.util.Date;
import java.util.List;

import xyz.klinker.messenger.api.implementation.Account;
import xyz.klinker.messenger.api.implementation.ApiUtils;
import xyz.klinker.messenger.data.FeatureFlags;
import xyz.klinker.messenger.util.billing.BillingHelper;
import xyz.klinker.messenger.util.billing.ProductPurchased;

public class SignoutService extends IntentService {

    private static final int REQUEST_CODE = 15;

    private BillingHelper billing;

    public SignoutService() {
        super("SignoutService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Account account = Account.get(this);
        billing = new BillingHelper(this);

        // Only need to manage this on the primary device
        if (account.primary && account.subscriptionExpiration < new Date().getTime() && isExpired()) {
            final String accountId = account.accountId;

            account.clearAccount();
            new ApiUtils().deleteAccount(accountId);
        } else {
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
        if (account.subscriptionType == Account.SubscriptionType.LIFETIME|| !account.primary) {
            return;
        }

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
