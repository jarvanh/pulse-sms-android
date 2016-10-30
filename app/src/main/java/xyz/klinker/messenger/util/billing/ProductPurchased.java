package xyz.klinker.messenger.util.billing;

import android.os.Bundle;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class ProductPurchased {

    public static List<ProductPurchased> createFromBundle(ProductType type, Bundle bundle) {
        List<ProductPurchased> list = new ArrayList<>();
        int response = bundle.getInt("RESPONSE_CODE");

        // BILLING_RESPONSE_OK
        if (response == 0) {
            ArrayList<String> ownedProductIds =
                    bundle.getStringArrayList("INAPP_PURCHASE_ITEM_LIST");

            for (String thisResponse : ownedProductIds) {
                list.add(new ProductPurchased(type, thisResponse));
            }
        }

        return list;
    }

    private ProductType type;
    private String productId;

    public ProductPurchased(ProductType type, String productId) {
        this.type = type;
        this.productId = productId;
    }

    public ProductType getType() {
        return type;
    }

    public String getProductId() {
        return productId;
    }

}
