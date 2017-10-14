package xyz.klinker.messenger.shared.util.billing

enum class ProductType private constructor(val identifier: String) {
    SINGLE_PURCHASE("inapp"), SUBSCRIPTION("subs");

    val availableProductIds: Array<String>
        get() = if (this == SINGLE_PURCHASE) {
            arrayOf("lifetime")
        } else {
            arrayOf("subscription_one_month", "subscription_three_months", "subscription_one_year")
        }
}
