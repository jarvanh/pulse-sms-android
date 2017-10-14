package xyz.klinker.messenger.shared.util.billing

interface PurchasedItemCallback {
    fun onItemPurchased(productId: String)
    fun onPurchaseError(message: String)
}
