package xyz.klinker.messenger.shared.util.billing;

public interface PurchasedItemCallback {
    void onItemPurchased(String productId);
    void onPurchaseError(String message);
}
