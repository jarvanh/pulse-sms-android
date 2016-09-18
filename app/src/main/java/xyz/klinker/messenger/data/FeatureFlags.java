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

    // ADDING FEATURE FLAGS:
    // 1. Add the static identifiers and flag name right below here.
    // 2. Set up the flag in the constructor
    // 3. Add the switch case for the flag in the updateFlag method


    private static final String FLAG_MESSAGING_STYLE_NOTIFICATIONS = "messaging_notifications";
    public boolean MESSAGING_STYLE_NOTIFICATIONS;

    private static final String FLAG_ANDROID_WEAR_SECOND_PAGE = "wear_second_page";
    public boolean ANDROID_WEAR_SECOND_PAGE;

    private Context context;
    private FeatureFlags(final Context context) {
        this.context = context;
        SharedPreferences sharedPrefs = getSharedPrefs();

        MESSAGING_STYLE_NOTIFICATIONS = getValue(sharedPrefs, FLAG_MESSAGING_STYLE_NOTIFICATIONS);
        ANDROID_WEAR_SECOND_PAGE = getValue(sharedPrefs, FLAG_ANDROID_WEAR_SECOND_PAGE);
    }

    public void updateFlag(String identifier, boolean flag) {
        getSharedPrefs().edit()
                .putBoolean(identifier, flag)
                .apply();

        switch (identifier) {
            case FLAG_MESSAGING_STYLE_NOTIFICATIONS:
                MESSAGING_STYLE_NOTIFICATIONS = flag;
                break;
            case FLAG_ANDROID_WEAR_SECOND_PAGE:
                ANDROID_WEAR_SECOND_PAGE = flag;
                break;
        }
    }

    private boolean getValue(SharedPreferences sharedPrefs, String key) {
        if (context.getResources().getBoolean(R.bool.feature_flag_default)) {
            return true;
        } else {
            return sharedPrefs.getBoolean(key, false);
        }
    }

    private SharedPreferences getSharedPrefs() {
        return PreferenceManager.getDefaultSharedPreferences(context);
    }
}
