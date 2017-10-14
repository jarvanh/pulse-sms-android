package xyz.klinker.messenger.shared.service

import xyz.klinker.messenger.shared.util.billing.ProductPurchased

class SimpleLifetimeSubscriptionCheckService : SimpleSubscriptionCheckService() {
    override fun handleBestProduct(best: ProductPurchased) {
        if (best.productId == "lifetime") {
            writeLifetimeSubscriber()
        }
    }
}
