package xyz.klinker.messenger.api.implementation.firebase;

import android.content.Context;
import android.os.Bundle;

import com.google.firebase.analytics.FirebaseAnalytics;

public class AnalyticsHelper {

    private static void logEvent(Context context, String event) {
        logEvent(context, event, new Bundle());
    }

    private static void logEvent(Context context, String event, Bundle bundle) {
        if (context != null) {
            FirebaseAnalytics.getInstance(context).logEvent(event, bundle);
        }
    }


    // Purchase events
    private static final String PURCHASE_ERROR = "purchase_error";
    private static final String PURCHASE_LIFETIME = "lifetime";
    private static final String PURCHASE_YEAR = "subscription_one_year";
    private static final String PURCHASE_THREE_MONTH = "subscription_three_months";
    private static final String PURCHASE_MONTH = "subscription_one_month";

    public static void userSubscribed(Context context, String subscription) {
        logEvent(context, "FIRST_TIME_SUBSCRIPTION_" + subscription);
    }

    public static void userUpgraded(Context context, String subscription) {
        logEvent(context, "USER_UPGRADED_SUBSCRIPTION_" + subscription);
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
    private static final String ACCOUNT_SIGN_IN_INSTEAD_OF_PURCHASE = "ACCOUNT_SIGN_IN_INSTEAD_OF_PURCHASE";
    private static final String ACCOUNT_COMPLETED_PURCHASE = "ACCOUNT_COMPLETED_PURCHASE";
    private static final String ACCOUNT_SIGNED_UP = "ACCOUNT_SIGNED_UP";
    private static final String ACCOUNT_LOGGED_IN = "ACCOUNT_LOGGED_IN";
    private static final String ACCOUNT_START_FREE_TRIAL_TUTORIAL = "ACCOUNT_START_FREE_TRIAL_TUTORIAL";
    private static final String ACCOUNT_ACCEPT_FREE_TRIAL = "ACCOUNT_ACCEPT_FREE_TRIAL";
    private static final String ACCOUNT_EXPIRED_FREE_TRIAL = "ACCOUNT_EXPIRED_FREE_TRIAL";
    private static final String ACCOUNT_TRIAL_DAY = "TRIAL_DAY_";
    private static final String ACCOUNT_FREE_TRIAL_UPGRADE_DIALOG_SHOWN = "ACCOUNT_FREE_TRIAL_UPGRADE_DIALOG_SHOWN";
    private static final String ACCOUNT_FREE_TRIAL_UPGRADE_DIALOG_UPGRADE_CLICKED = "ACCOUNT_FREE_TRIAL_UPGRADED";
    private static final String ACCOUNT_FREE_TRIAL_UPGRADE_DIALOG_CANCEL_CLICKED = "ACCOUNT_FREE_TRIAL_CANCELED";
    private static final String ACCOUNT_RESTORED_SUB_TO_TRIAL = "ACCOUNT_RESTORE_SUB_TO_TRIAL";

    public static void accountTutorialStarted(Context context) {
        logEvent(context, ACCOUNT_TUTORIAL_STARTED);
    }

    public static void accountTutorialFinished(Context context) {
        logEvent(context, ACCOUNT_TUTORIAL_FINISHED);
    }

    public static void accountSelectedPurchase(Context context) {
        logEvent(context, ACCOUNT_SELECTED_PURCHASE);
    }

    public static void accountSignInInsteadOfPurchase(Context context) {
        logEvent(context, ACCOUNT_SIGN_IN_INSTEAD_OF_PURCHASE);
    }

    public static void accountCompetedPurchase(Context context) {
        logEvent(context, ACCOUNT_COMPLETED_PURCHASE);
    }

    public static void accountStartTrialTutorial(Context context) {
        logEvent(context, ACCOUNT_START_FREE_TRIAL_TUTORIAL);
    }

    public static void accountAcceptFreeTrial(Context context) {
        logEvent(context, ACCOUNT_ACCEPT_FREE_TRIAL);
    }

    public static void accountExpiredFreeTrial(Context context) {
        logEvent(context, ACCOUNT_EXPIRED_FREE_TRIAL);
    }

    public static void accountTrialDay(Context context, int daysLeftInTrial) {
        logEvent(context, ACCOUNT_TRIAL_DAY + daysLeftInTrial);
    }

    public static void accountFreeTrialUpgradeDialogShown(Context context) {
        logEvent(context, ACCOUNT_FREE_TRIAL_UPGRADE_DIALOG_SHOWN);
    }

    public static void accountFreeTrialUpgradeDialogUpgradeClicked(Context context) {
        logEvent(context, ACCOUNT_FREE_TRIAL_UPGRADE_DIALOG_UPGRADE_CLICKED);
    }

    public static void accountFreeTrialUpgradeDialogCancelClicked(Context context) {
        logEvent(context, ACCOUNT_FREE_TRIAL_UPGRADE_DIALOG_CANCEL_CLICKED);
    }

    public static void accountRestoreSubToTrial(Context context) {
        logEvent(context, ACCOUNT_RESTORED_SUB_TO_TRIAL);
    }

    public static void accountSignedUp(Context context) {
        logEvent(context, ACCOUNT_SIGNED_UP);
    }

    public static void accountLoggedIn(Context context) {
        logEvent(context, ACCOUNT_LOGGED_IN);
    }


    // Track events from clicks on the card that is a header in the conversation list
    private static final String CONVO_LIST_CARD_SHOWN = "CONVO_LIST_CARD_SHOWN";
    private static final String CONVO_LIST_CARD_TRY_IT = "CONVO_LIST_TRY_IT";
    private static final String CONVO_LIST_CARD_NOT_NOW = "CONVO_LIST_NOT_NOW";

    public static void convoListCardShown(Context context) {
        logEvent(context, CONVO_LIST_CARD_SHOWN);
    }

    public static void convoListTryIt(Context context) {
        logEvent(context, CONVO_LIST_CARD_TRY_IT);
    }

    public static void convoListNotNow(Context context) {
        logEvent(context, CONVO_LIST_CARD_NOT_NOW);
    }


    // Rate it events
    private static final String RATE_IT_PROMPT_SHOWN = "RATE_IT_SHOWN";
    private static final String RATE_IT_CLICKED = "RATE_IT_CLICKED";

    public static void rateItPromptShown(Context context) {
        logEvent(context, RATE_IT_PROMPT_SHOWN);
    }

    public static void rateItClicked(Context context) {
        logEvent(context, RATE_IT_CLICKED);
    }


    // Other Events
    private static final String FAILED_TO_SAVE_SMS = "FAILED_TO_SAVE_SMS";
    private static final String RECEIVED_DUPLICATE_SMS = "RECEIVED_DUPLICATE_SMS";
    private static final String UPDATING_FCM_TOKEN = "UPDATING_FCM_TOKEN";
    private static final String CAUGHT_EXCEPTION = "CAUGHT_EXCEPTION";

    public static void failedToSaveSms(Context context, String error) {
        Bundle bundle = new Bundle();
        bundle.putString("error", error);

        logEvent(context, FAILED_TO_SAVE_SMS, bundle);
    }

    public static void receivedDuplicateSms(Context context) {
        logEvent(context, RECEIVED_DUPLICATE_SMS);
    }

    public static void updatingFcmToken(Context context) {
        logEvent(context, UPDATING_FCM_TOKEN);
    }

    public static void caughtForceClose(Context context, String message, Throwable e) {
        Bundle bundle = new Bundle();
        bundle.putString("message", message);
        bundle.putString("error", e.getMessage());

        logEvent(context, CAUGHT_EXCEPTION, bundle);
    }

}
