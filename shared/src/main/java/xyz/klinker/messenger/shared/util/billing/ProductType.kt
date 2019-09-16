package xyz.klinker.messenger.shared.util.billing

enum class ProductType private constructor(val identifier: String) {
    SINGLE_PURCHASE("inapp"), SUBSCRIPTION("subs");

    val availableProductIds: Array<String>
        get() = if (this == SINGLE_PURCHASE) {
            arrayOf("lifetime2")
        } else {
            arrayOf("subscription_one_month_no_trial", "subscription_three_months_no_trial", "subscription_one_year_no_trial")
        }
}
