package xyz.klinker.messenger.shared.service.jobs;

import android.annotation.SuppressLint;
import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.preference.PreferenceManager;
import android.util.Log;

import java.util.Date;
import java.util.List;

import xyz.klinker.messenger.api.implementation.Account;
import xyz.klinker.messenger.shared.util.billing.BillingHelper;
import xyz.klinker.messenger.shared.util.billing.ProductPurchased;

public class SignoutJob extends BackgroundJob {

    private static final String TAG = "SignoutJob";

    private static final int JOB_ID = 15;

    private BillingHelper billing;

    @Override
    protected void onRunJob(JobParameters parameters) {
        Log.v(TAG, "starting signout service.");

        Account account = Account.INSTANCE;
        billing = new BillingHelper(this);

        // Only need to manage this on the primary device
        if (account.exists() && account.getPrimary() && account.getSubscriptionType() != Account.SubscriptionType.LIFETIME &&
                account.getSubscriptionExpiration() < new Date().getTime() && isExpired()) {
            Log.v(TAG, "forcing signout due to expired account!");
            final String accountId = account.getAccountId();

            //account.clearAccount();
            //ApiUtils.INSTANCE.deleteAccount(accountId);
        } else {
            Log.v(TAG, "account not expired, scheduling the check again.");
            SubscriptionExpirationCheckJob.scheduleNextRun(this);
        }

        writeSignoutTime(this, 0);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (billing != null) {
            billing.destroy();
        }
    }

    private boolean isExpired() {
        List<ProductPurchased> purchasedList = billing.queryAllPurchasedProducts();

        if (purchasedList.size() > 0) {
            ProductPurchased best = getBestProduct(purchasedList);

            if (best.getProductId().equals("lifetime")) {
                writeLifetimeSubscriber();
            } else {
                writeNewExpirationToAccount(new Date().getTime() + best.getExpiration());
            }

            return false;
        } else {
            return true;
        }
    }

    private void writeLifetimeSubscriber() {
        Account account = Account.INSTANCE;
        account.updateSubscription(this,
                Account.SubscriptionType.LIFETIME, 1L, true);
    }

    private void writeNewExpirationToAccount(long time) {
        Account account = Account.INSTANCE;
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
        long signoutTime = PreferenceManager.getDefaultSharedPreferences(context)
                .getLong("account_signout_time", 0L);
        scheduleNextRun(context, signoutTime);
    }

    public static void scheduleNextRun(Context context, long signoutTime) {
        long currentTime = new Date().getTime();
        Account account = Account.INSTANCE;
        JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);

        ComponentName component = new ComponentName(context, SignoutJob.class);
        JobInfo.Builder builder = new JobInfo.Builder(JOB_ID, component)
                .setMinimumLatency(signoutTime - currentTime)
                .setRequiresCharging(false)
                .setRequiresDeviceIdle(false);

        if (account.getAccountId() == null || account.getSubscriptionType() == Account.SubscriptionType.LIFETIME || !account.getPrimary() || signoutTime == 0) {
            jobScheduler.cancel(JOB_ID);
        } else {
            Log.v(TAG, "CURRENT TIME: " + new Date().toString());
            Log.v(TAG, "SCHEDULING NEW SIGNOUT CHECK FOR: " + new Date(signoutTime).toString());

            jobScheduler.schedule(builder.build());
        }
    }

    @SuppressLint("ApplySharedPref")
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
