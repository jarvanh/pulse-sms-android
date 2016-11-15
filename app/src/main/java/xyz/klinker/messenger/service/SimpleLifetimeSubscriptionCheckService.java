package xyz.klinker.messenger.service;

import xyz.klinker.messenger.util.billing.ProductPurchased;

public class SimpleLifetimeSubscriptionCheckService extends SimpleSubscriptionCheckService {
    @Override
    protected void handleBestProduct(ProductPurchased best) {
        if (best.getProductId().equals("lifetime")) {
            writeLifetimeSubscriber();
        }
    }
}
