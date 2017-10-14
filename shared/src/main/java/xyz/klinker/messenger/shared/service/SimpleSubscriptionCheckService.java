package xyz.klinker.messenger.shared.service;

import android.app.IntentService;
import android.content.Intent;

import java.util.Date;
import java.util.List;

import xyz.klinker.messenger.api.implementation.Account;
import xyz.klinker.messenger.shared.util.billing.BillingHelper;
import xyz.klinker.messenger.shared.util.billing.ProductPurchased;

/**
 * Just checks to see if a user has a subscripiton, if they do, write it to the database,
 * if they don't, just let it be.
 */
public class SimpleSubscriptionCheckService extends IntentService {

    private BillingHelper billing;

    public SimpleSubscriptionCheckService() {
        super("SimpleSubscriptionCheckService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        billing = new BillingHelper(this);

        if (Account.INSTANCE.getAccountId() == null || !Account.INSTANCE.getPrimary()) {
            return;
        }

        List<ProductPurchased> purchasedList = billing.queryAllPurchasedProducts();

        if (purchasedList.size() > 0) {
            ProductPurchased best = getBestProduct(purchasedList);

            handleBestProduct(best);
        }
    }

    protected void handleBestProduct(ProductPurchased best) {
        if (best.getProductId().equals("lifetime")) {
            writeLifetimeSubscriber();
        } else {
            writeNewExpirationToAccount(new Date().getTime() + best.getExpiration());
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        billing.destroy();
    }

    protected void writeLifetimeSubscriber() {
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
}
