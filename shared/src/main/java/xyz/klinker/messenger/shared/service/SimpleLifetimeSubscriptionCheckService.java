package xyz.klinker.messenger.shared.service;

import xyz.klinker.messenger.shared.util.billing.ProductPurchased;

public class SimpleLifetimeSubscriptionCheckService extends SimpleSubscriptionCheckService {
    @Override
    protected void handleBestProduct(ProductPurchased best) {
        if (best.getProductId().equals("lifetime")) {
            writeLifetimeSubscriber();
        }
    }
}
