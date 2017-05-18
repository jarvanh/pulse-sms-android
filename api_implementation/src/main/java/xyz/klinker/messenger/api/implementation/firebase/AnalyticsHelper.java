package xyz.klinker.messenger.api.implementation.firebase;

import android.content.Context;
import android.os.Bundle;

import com.google.firebase.analytics.FirebaseAnalytics;

public class AnalyticsHelper {

    private static void logEvent(Context context, String event) {
        logEvent(context, event, new Bundle());
    }

    private static void logEvent(Context context, String event, Bundle bundle) {
        FirebaseAnalytics.getInstance(context).logEvent(event, bundle);
    }


    // Purchase events
    private static final String PURCHASE_ERROR = "purchase_error";
    private static final String PURCHASE_LIFETIME = "lifetime";
    private static final String PURCHASE_YEAR = "subscription_one_year";
    private static final String PURCHASE_THREE_MONTH = "subscription_three_months";
    private static final String PURCHASE_MONTH = "subscription_one_month";

    public static void userSubscribed(Context context, String subscription) {
        logEvent(context, subscription);
    }

    public static void purchaseError(Context context) {
        logEvent(context, PURCHASE_ERROR);
    }


    // Initial setup events
    private static final String TUTORIAL_STARTED = "TUTORIAL_STARTED";
    private static final String TUTORIAL_SKIPPED = "TUTORIAL_SKIPPED";
    private static final String TUTORIAL_FINISHED = "TUTORIAL_FINISHED";
    private static final String IMPORT_FINISHED = "IMPORT_FINISHED";

    public static void tutorialStarted(Context context) {
        logEvent(context, TUTORIAL_STARTED);
    }

    public static void tutorialSkipped(Context context) {
        logEvent(context, TUTORIAL_SKIPPED);
    }

    public static void tutorialFinished(Context context) {
        logEvent(context, TUTORIAL_FINISHED);
    }

    public static void importFinished(Context context, long importTime) {
        Bundle bundle = new Bundle();
        bundle.putLong("import_time", importTime);

        logEvent(context, IMPORT_FINISHED, bundle);
    }


    // Create account events
    private static final String ACCOUNT_TUTORIAL_STARTED = "ACCOUNT_TUTORIAL_STARTED";
    private static final String ACCOUNT_TUTORIAL_FINISHED = "ACCOUNT_TUTORIAL_FINISHED";
    private static final String ACCOUNT_SELECTED_PURCHASE = "ACCOUNT_SELECTED_PURCHASE";
    private static final String ACCOUNT_COMPLETED_PURCHASE = "ACCOUNT_COMPLETED_PURCHASE";
    private static final String ACCOUNT_SIGNED_UP = "ACCOUNT_SIGNED_UP";
    private static final String ACCOUNT_LOGGED_IN = "ACCOUNT_LOGGED_IN";

    public static void accountTutorialStarted(Context context) {
        logEvent(context, ACCOUNT_TUTORIAL_STARTED);
    }

    public static void accountTutorialFinished(Context context) {
        logEvent(context, ACCOUNT_TUTORIAL_FINISHED);
    }

    public static void accountSelectedPurchase(Context context) {
        logEvent(context, ACCOUNT_SELECTED_PURCHASE);
    }

    public static void accountCompetedPurchase(Context context) {
        logEvent(context, ACCOUNT_COMPLETED_PURCHASE);
    }

    public static void accountSignedUp(Context context) {
        logEvent(context, ACCOUNT_SIGNED_UP);
    }

    public static void accountLoggedIn(Context context) {
        logEvent(context, ACCOUNT_LOGGED_IN);
    }
}
