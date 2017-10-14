package xyz.klinker.messenger.shared.util.billing

import android.os.Bundle

import org.json.JSONObject

import java.util.ArrayList

class ProductAvailable(val type: ProductType, val productId: String, val price: String) {
    companion object {
        fun createFromBundle(type: ProductType, bundle: Bundle): List<ProductAvailable> {
            val list = ArrayList<ProductAvailable>()
            val response = bundle.getInt("RESPONSE_CODE")

            // BILLING_RESPONSE_OK
            if (response == 0) {
                val responseList = bundle.getStringArrayList("DETAILS_LIST")

                for (thisResponse in responseList!!) {
                    try {
                        val `object` = JSONObject(thisResponse)
                        val productId = `object`.getString("productId")
                        val price = `object`.getString("price")

                        list.add(ProductAvailable(type, productId, price))
                    } catch (e: Exception) {
                    }

                }
            }

            return list
        }

        fun createLifetime(): ProductAvailable {
            return ProductAvailable(ProductType.SINGLE_PURCHASE, "lifetime", "$10.99")
        }

        fun createYearly(): ProductAvailable {
            return ProductAvailable(ProductType.SUBSCRIPTION, "subscription_one_year", "$5.99")
        }

        fun createThreeMonth(): ProductAvailable {
            return ProductAvailable(ProductType.SUBSCRIPTION, "subscription_three_months", "$1.99")
        }

        fun createMonthly(): ProductAvailable {
            return ProductAvailable(ProductType.SUBSCRIPTION, "subscription_one_month", "$0.99")
        }
    }
}
