package xyz.klinker.messenger.shared.service

import android.app.IntentService
import android.content.Intent

import java.util.Date

import xyz.klinker.messenger.api.implementation.Account
import xyz.klinker.messenger.shared.util.billing.BillingHelper
import xyz.klinker.messenger.shared.util.billing.ProductPurchased

/**
 * Just checks to see if a user has a subscripiton, if they do, write it to the database,
 * if they don't, just let it be.
 */
open class SimpleSubscriptionCheckService : IntentService("SimpleSubscriptionCheckService") {

    private var billing: BillingHelper? = null

    override fun onHandleIntent(intent: Intent?) {
        billing = BillingHelper(this)

        if (Account.accountId == null || !Account.primary) {
            return
        }

        val purchasedList = billing!!.queryAllPurchasedProducts()

        if (purchasedList.size > 0) {
            val best = getBestProduct(purchasedList)

            handleBestProduct(best)
        }
    }

    protected open fun handleBestProduct(best: ProductPurchased) {
        if (best.productId == "lifetime") {
            writeLifetimeSubscriber()
        } else {
            writeNewExpirationToAccount(Date().time + best.expiration)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        billing!!.destroy()
    }

    protected fun writeLifetimeSubscriber() {
        val account = Account
        account.updateSubscription(this,
                Account.SubscriptionType.LIFETIME, 1L, true)
    }

    private fun writeNewExpirationToAccount(time: Long) {
        val account = Account
        account.updateSubscription(this,
                Account.SubscriptionType.SUBSCRIBER, time, true)
    }

    private fun getBestProduct(products: List<ProductPurchased>): ProductPurchased {
        var best = products[0]

        products.asSequence()
                .filter { it.isBetterThan(best) }
                .forEach { best = it }

        return best
    }
}
