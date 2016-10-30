package xyz.klinker.messenger.util.billing;

import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.Test;

import static org.junit.Assert.*;

public class ProductPurchasedTest {

    @Test
    public void lifetimeIsBest() {
        assertThat(createProduct("lifetime").isBetterThan(createProduct("subscription_one_year")), Matchers.is(true));
        assertThat(createProduct("lifetime").isBetterThan(createProduct("subscription_three_months")), Matchers.is(true));
        assertThat(createProduct("lifetime").isBetterThan(createProduct("subscription_one_month")), Matchers.is(true));
        assertThat(createProduct("lifetime").isBetterThan(createProduct("test")), Matchers.is(true));
        assertThat(createProduct("lifetime").isBetterThan(createProduct("lifetime")), Matchers.is(true));
    }

    @Test
    public void oneMonthIsWorst() {
        assertThat(createProduct("subscription_one_month").isBetterThan(createProduct("lifetime")), Matchers.is(false));
        assertThat(createProduct("subscription_one_month").isBetterThan(createProduct("subscription_one_year")), Matchers.is(false));
        assertThat(createProduct("subscription_one_month").isBetterThan(createProduct("subscription_three_months")), Matchers.is(false));
        assertThat(createProduct("subscription_one_month").isBetterThan(createProduct("subscription_one_month")), Matchers.is(true));
        assertThat(createProduct("subscription_one_month").isBetterThan(createProduct("test")), Matchers.is(true));
    }

    @Test
    public void oneYearIsGood() {
        assertThat(createProduct("subscription_one_year").isBetterThan(createProduct("lifetime")), Matchers.is(false));
        assertThat(createProduct("subscription_one_year").isBetterThan(createProduct("subscription_one_year")), Matchers.is(true));
        assertThat(createProduct("subscription_one_year").isBetterThan(createProduct("subscription_three_months")), Matchers.is(true));
        assertThat(createProduct("subscription_one_year").isBetterThan(createProduct("subscription_one_month")), Matchers.is(true));
        assertThat(createProduct("subscription_one_year").isBetterThan(createProduct("test")), Matchers.is(true));
    }

    @Test
    public void threeMonthsIsOk() {
        assertThat(createProduct("subscription_three_months").isBetterThan(createProduct("lifetime")), Matchers.is(false));
        assertThat(createProduct("subscription_three_months").isBetterThan(createProduct("subscription_one_year")), Matchers.is(false));
        assertThat(createProduct("subscription_three_months").isBetterThan(createProduct("subscription_three_months")), Matchers.is(true));
        assertThat(createProduct("subscription_three_months").isBetterThan(createProduct("subscription_one_month")), Matchers.is(true));
        assertThat(createProduct("subscription_three_months").isBetterThan(createProduct("test")), Matchers.is(true));
    }

    private ProductPurchased createProduct(String id) {
        return new ProductPurchased(null, id);
    }
}