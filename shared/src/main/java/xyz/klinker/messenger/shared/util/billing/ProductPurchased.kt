package xyz.klinker.messenger.shared.util.billing

import android.os.Bundle

import java.util.ArrayList

import xyz.klinker.messenger.shared.util.TimeUtils

class ProductPurchased(val type: ProductType?, val productId: String) {

    val expiration: Long
        get() = getExpiration(this.productId)

    fun isBetterThan(other: ProductPurchased) = when {
        productId.contains("lifetime") -> true
        other.productId.contains("lifetime") -> false
        productId.contains("one_year") -> !other.productId.contains("lifetime")
        productId.contains("three_months") -> !(other.productId.contains("lifetime") || other.productId.contains("one_year"))
        else -> !(other.productId.contains("lifetime") || other.productId.contains("one_year") || other.productId.contains("three_months"))
    }

    companion object {

        fun createFromBundle(type: ProductType, bundle: Bundle): List<ProductPurchased> {
            val list = ArrayList<ProductPurchased>()
            val response = bundle.getInt("RESPONSE_CODE")

            // BILLING_RESPONSE_OK
            if (response == 0) {
                bundle.getStringArrayList("INAPP_PURCHASE_ITEM_LIST")?.mapTo(list) { ProductPurchased(type, it) }
            }

            return list
        }

        fun getExpiration(productId: String) = when {
            productId.contains("one_year") -> TimeUtils.DAY * 365
            productId.contains("three_months") -> TimeUtils.DAY * 93
            productId.contains("one_month") -> TimeUtils.DAY * 31
            else -> 1L
        }
    }

}
