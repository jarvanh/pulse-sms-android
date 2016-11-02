package xyz.klinker.messenger.util.billing;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;

import xyz.klinker.messenger.R;

public class ProductAvailableDetailed extends ProductAvailable {

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
