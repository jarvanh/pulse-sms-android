package xyz.klinker.messenger.shared.util.billing;

import android.os.Bundle;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class ProductAvailable {

    public static List<ProductAvailable> createFromBundle(ProductType type, Bundle bundle) {
        List<ProductAvailable> list = new ArrayList<>();
        int response = bundle.getInt("RESPONSE_CODE");

        // BILLING_RESPONSE_OK
        if (response == 0) {
            ArrayList<String> responseList = bundle.getStringArrayList("DETAILS_LIST");

            for (String thisResponse : responseList) {
                try {
                    JSONObject object = new JSONObject(thisResponse);
                    String productId = object.getString("productId");
                    String price = object.getString("price");

                    list.add(new ProductAvailable(type, productId, price));
                } catch (Exception e) { }
            }
        }

        return list;
    }

    private ProductType type;
    private String productId;
    private String price;

    public ProductAvailable(ProductType type, String productId, String price) {
        this.type = type;
        this.productId = productId;
        this.price = price;
    }

    public ProductType getType() {
        return type;
    }

    public String getProductId() {
        return productId;
    }

    public String getPrice() {
        return price;
    }

    public static ProductAvailable createLifetime() {
        return new ProductAvailable(ProductType.SINGLE_PURCHASE, "lifetime", "$10.99");
    }

    public static ProductAvailable createYearly() {
        return new ProductAvailable(ProductType.SUBSCRIPTION, "subscription_one_year", "$5.99");
    }

    public static ProductAvailable createThreeMonth() {
        return new ProductAvailable(ProductType.SUBSCRIPTION, "subscription_three_months", "$1.99");
    }

    public static ProductAvailable createMonthly() {
        return new ProductAvailable(ProductType.SUBSCRIPTION, "subscription_one_month", "$0.99");
    }
}
