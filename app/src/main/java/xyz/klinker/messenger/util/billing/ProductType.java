package xyz.klinker.messenger.util.billing;

public enum ProductType {
    SINGLE_PURCHASE("inapp"), SUBSCRIPTION("subs");

    private String identifier;

    ProductType(String identifier) {
        this.identifier = identifier;
    }

    public String getIdentifier() {
        return identifier;
    }

    public String[] getAvailableProductIds() {
        if (this == SINGLE_PURCHASE) {
            return new String[] {
                    "lifetime"
            };
        } else {
            return new String[] {
                    "subscription_one_month",
                    "subscription_three_months",
                    "subscription_one_year"
            };
        }
    }
}
