package xyz.klinker.messenger.util.billing;

public interface PurchasedItemCallback {
    void onItemPurchased(String productId);
    void onPurchaseError(String message);
}
