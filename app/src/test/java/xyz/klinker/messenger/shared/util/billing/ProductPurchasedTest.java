package xyz.klinker.messenger.shared.util.billing;

import org.hamcrest.CoreMatchers;
import org.junit.Test;

import static org.junit.Assert.*;

public class ProductPurchasedTest {

    @Test
    public void lifetimeIsBest() {
        assertThat(createProduct("lifetime").isBetterThan(createProduct("subscription_one_year")), CoreMatchers.is(true));
        assertThat(createProduct("lifetime").isBetterThan(createProduct("subscription_three_months")), CoreMatchers.is(true));
        assertThat(createProduct("lifetime").isBetterThan(createProduct("subscription_one_month")), CoreMatchers.is(true));
        assertThat(createProduct("lifetime").isBetterThan(createProduct("test")), CoreMatchers.is(true));
        assertThat(createProduct("lifetime").isBetterThan(createProduct("lifetime")), CoreMatchers.is(true));
    }

    @Test
    public void oneMonthIsWorst() {
        assertThat(createProduct("subscription_one_month").isBetterThan(createProduct("lifetime")), CoreMatchers.is(false));
        assertThat(createProduct("subscription_one_month").isBetterThan(createProduct("subscription_one_year")), CoreMatchers.is(false));
        assertThat(createProduct("subscription_one_month").isBetterThan(createProduct("subscription_three_months")), CoreMatchers.is(false));
        assertThat(createProduct("subscription_one_month").isBetterThan(createProduct("subscription_one_month")), CoreMatchers.is(true));
        assertThat(createProduct("subscription_one_month").isBetterThan(createProduct("test")), CoreMatchers.is(true));
    }

    @Test
    public void oneYearIsGood() {
        assertThat(createProduct("subscription_one_year").isBetterThan(createProduct("lifetime")), CoreMatchers.is(false));
        assertThat(createProduct("subscription_one_year").isBetterThan(createProduct("subscription_one_year")), CoreMatchers.is(true));
        assertThat(createProduct("subscription_one_year").isBetterThan(createProduct("subscription_three_months")), CoreMatchers.is(true));
        assertThat(createProduct("subscription_one_year").isBetterThan(createProduct("subscription_one_month")), CoreMatchers.is(true));
        assertThat(createProduct("subscription_one_year").isBetterThan(createProduct("test")), CoreMatchers.is(true));
    }

    @Test
    public void threeMonthsIsOk() {
        assertThat(createProduct("subscription_three_months").isBetterThan(createProduct("lifetime")), CoreMatchers.is(false));
        assertThat(createProduct("subscription_three_months").isBetterThan(createProduct("subscription_one_year")), CoreMatchers.is(false));
        assertThat(createProduct("subscription_three_months").isBetterThan(createProduct("subscription_three_months")), CoreMatchers.is(true));
        assertThat(createProduct("subscription_three_months").isBetterThan(createProduct("subscription_one_month")), CoreMatchers.is(true));
        assertThat(createProduct("subscription_three_months").isBetterThan(createProduct("test")), CoreMatchers.is(true));
    }

    private ProductPurchased createProduct(String id) {
        return new ProductPurchased(null, id);
    }
}