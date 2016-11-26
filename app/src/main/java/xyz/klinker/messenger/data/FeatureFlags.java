package xyz.klinker.messenger.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.annotation.VisibleForTesting;
import android.support.compat.BuildConfig;

import xyz.klinker.messenger.R;

/**
 * We can use these for new features or if we want to test something quick and don't know if it
 * is going to work. These are great for quick changes. Say we have something that could cause force
 * closes or that we aren't sure users will like. We don't have to go through the play store to be
 * able to change it.
 *
 * These flags should continuously be updated. After we know the flag is no longer necessary,
 * we can remove any code here and any flag implementation to stick with the true (or false) implementation
 * of the flag.
 */
public class FeatureFlags {
    // region static initialization
    private static volatile FeatureFlags featureFlags;
    public static synchronized FeatureFlags get(Context context) {
        if (featureFlags == null) {
            featureFlags = new FeatureFlags(context);
        }

        return featureFlags;
    }
    //endregion
    // region feature flag strings
    private static final String FLAG_MESSAGING_STYLE_NOTIFICATIONS = "messaging_notifications";
    private static final String FLAG_ANDROID_WEAR_SECOND_PAGE = "wear_second_page";
    private static final String FLAG_NO_NOTIFICATION_WHEN_CONVO_OPEN = "hold_notification";
    private static final String FLAG_REORDER_CONVERSATIONS_WHEN_NEW_MESSAGE_ARRIVES = "reorder_conversations";
    private static final String FLAG_TURN_DOWN_CONTENT_OBSERVER_TIMEOUT = "content_observer_timeout";
    private static final String FLAG_REMOVE_MESSAGE_LIST_DRAWER = "remove_message_drawer";
    private static final String FLAG_ARTICLE_ENHANCER = "flag_article_enhancer";
    private static final String FLAG_FEATURE_SETTINGS = "flag_feature_settings";
    private static final String FLAG_SECURE_PRIVATE = "flag_secure_private";
    private static final String FLAG_QUICK_COMPOSE = "flag_quick_compose";
    private static final String FLAG_DELAYED_SENDING = "flag_delayed_sending";
    private static final String FLAG_CLEANUP_OLD = "flag_cleanup_old";
    // endregion

    private static final String[] ALWAYS_ON_FLAGS = new String[] {
            // none yet
    };

    // ADDING FEATURE FLAGS:
    // 1. Add the static identifiers and flag name right below here.
    // 2. Set up the flag in the constructor
    // 3. Add the switch case for the flag in the updateFlag method


    // step 1
    //public boolean MESSAGING_STYLE_NOTIFICATIONS;
    public boolean CONTENT_OBSERVER_TIMEOUT;
    public boolean ARTICLE_ENHANCER;
    public boolean FEATURE_SETTINGS;
    public boolean SECURE_PRIVATE;
    public boolean QUICK_COMPOSE;
    public boolean DELAYED_SENDING;
    public boolean CLEANUP_OLD_MESSAGES;

    private Context context;
    private FeatureFlags(final Context context) {
        this.context = context;
        SharedPreferences sharedPrefs = getSharedPrefs();

        // step 2
        //MESSAGING_STYLE_NOTIFICATIONS = getValue(sharedPrefs, FLAG_MESSAGING_STYLE_NOTIFICATIONS);
        CONTENT_OBSERVER_TIMEOUT = getValue(sharedPrefs, FLAG_TURN_DOWN_CONTENT_OBSERVER_TIMEOUT);
        ARTICLE_ENHANCER = getValue(sharedPrefs, FLAG_ARTICLE_ENHANCER);
        FEATURE_SETTINGS = getValue(sharedPrefs, FLAG_FEATURE_SETTINGS);
        SECURE_PRIVATE = getValue(sharedPrefs, FLAG_SECURE_PRIVATE);
        QUICK_COMPOSE = getValue(sharedPrefs, FLAG_QUICK_COMPOSE);
        DELAYED_SENDING = getValue(sharedPrefs, FLAG_DELAYED_SENDING);
        CLEANUP_OLD_MESSAGES = getValue(sharedPrefs, FLAG_CLEANUP_OLD);
    }

    public void updateFlag(String identifier, boolean flag) {
        getSharedPrefs().edit()
                .putBoolean(identifier, flag)
                .apply();

        switch (identifier) {
            // step 3
            /*case FLAG_MESSAGING_STYLE_NOTIFICATIONS:
                MESSAGING_STYLE_NOTIFICATIONS = flag;
                break;*/
            case FLAG_TURN_DOWN_CONTENT_OBSERVER_TIMEOUT:
                CONTENT_OBSERVER_TIMEOUT = flag;
                break;
            case FLAG_ARTICLE_ENHANCER:
                ARTICLE_ENHANCER= flag;
                break;
            case FLAG_FEATURE_SETTINGS:
                FEATURE_SETTINGS = flag;
                break;
            case FLAG_SECURE_PRIVATE:
                SECURE_PRIVATE = flag;
                break;
            case FLAG_DELAYED_SENDING:
                DELAYED_SENDING = flag;
                break;
            case FLAG_QUICK_COMPOSE:
                QUICK_COMPOSE= flag;
                break;
            case FLAG_CLEANUP_OLD:
                CLEANUP_OLD_MESSAGES = flag;
                break;
        }
    }

    private boolean getValue(SharedPreferences sharedPrefs, String key) {
        if (context.getResources().getBoolean(R.bool.feature_flag_default)) {
            return true;
        } else {
            return sharedPrefs.getBoolean(key, alwaysOn(key));
        }
    }

    private SharedPreferences getSharedPrefs() {
        return PreferenceManager.getDefaultSharedPreferences(context);
    }

    private boolean alwaysOn(String key) {
        for (String s : ALWAYS_ON_FLAGS) {
            if (key.equals(s)) {
                return true;
            }
        }

        return false;
    }
}
