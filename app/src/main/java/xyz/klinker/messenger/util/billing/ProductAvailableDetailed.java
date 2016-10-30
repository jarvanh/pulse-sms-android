package xyz.klinker.messenger.util.billing;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;

import xyz.klinker.messenger.R;

public class ProductAvailableDetailed extends ProductAvailable {

    public static List<ProductAvailableDetailed> getAllAvailableProducts(Context context) {
        List<ProductAvailableDetailed> availables = new ArrayList<>();

        availables.add(new ProductAvailableDetailed(ProductType.SINGLE_PURCHASE, "lifetime", "$10.99",
                context.getString(R.string.lifetime_access), context.getString(R.string.lifetime_access_summary)));

        availables.add(new ProductAvailableDetailed(ProductType.SUBSCRIPTION, "subscription_one_year", "$5.99",
                context.getString(R.string.one_year_subscription), null));

        availables.add(new ProductAvailableDetailed(ProductType.SUBSCRIPTION, "subscription_three_months", "$1.99",
                context.getString(R.string.three_month_subscription), null));

        availables.add(new ProductAvailableDetailed(ProductType.SUBSCRIPTION, "subscription_one_month", "$0.99",
                context.getString(R.string.one_month_subscription), null));

        return availables;
    }

    private String title;
    private String description;

    public ProductAvailableDetailed(ProductType type, String productId, String price, String title, String description) {
        super(type, productId, price);

        this.title = title;
        this.description = description;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }
}
