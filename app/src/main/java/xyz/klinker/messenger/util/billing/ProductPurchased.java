package xyz.klinker.messenger.util.billing;

import android.os.Bundle;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import xyz.klinker.messenger.util.TimeUtils;

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

    public boolean isBetterThan(ProductPurchased other) {
        if (productId.equals("lifetime")) {
            return true;
        } else if (other.getProductId().equals("lifetime")) {
            return false;
        } else if (productId.contains("one_year")) {
            if (other.getProductId().equals("lifetime")) {
                return false;
            } else {
                return true;
            }
        } else if (productId.contains("three_months")) {
            if (other.getProductId().equals("lifetime") || other.getProductId().contains("one_year")) {
                return false;
            } else {
                return true;
            }
        } else {
            if (other.getProductId().equals("lifetime") || other.getProductId().contains("one_year") || other.getProductId().contains("three_months")) {
                return false;
            } else {
                return true;
            }
        }
    }

    public long getExperation() {
        if (productId.contains("one_year")) {
            return TimeUtils.DAY * 365;
        } else if (productId.contains("three_months")) {
            return TimeUtils.DAY * 93;
        } else if (productId.contains("one_month")) {
            return TimeUtils.DAY * 31;
        }

        return 1L;
    }

}
