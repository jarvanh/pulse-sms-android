package xyz.klinker.messenger.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.annotation.VisibleForTesting;
import android.support.compat.BuildConfig;

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


    private static final String FLAG_TRIM_DECRYPTION = "flag_trim_decryption";
    public boolean TRIM_DECRYPTION;

    private static final String FLAG_MESSAGING_STYLE_NOTIFICATIONS = "flag_messaging_notifications";
    public boolean MESSAGING_STYLE_NOTIFICATIONS;

    private Context context;
    private FeatureFlags(final Context context) {
        this.context = context;
        SharedPreferences sharedPrefs = getSharedPrefs();

        TRIM_DECRYPTION = sharedPrefs.getBoolean(FLAG_TRIM_DECRYPTION, getDefault());
        MESSAGING_STYLE_NOTIFICATIONS = sharedPrefs.getBoolean(FLAG_MESSAGING_STYLE_NOTIFICATIONS, getDefault());
    }

    public void updateFlag(String identifier, boolean flag) {
        getSharedPrefs().edit()
                .putBoolean(identifier, flag)
                .apply();

        switch (identifier) {
            case FLAG_TRIM_DECRYPTION:
                TRIM_DECRYPTION = flag;
                break;
            case FLAG_MESSAGING_STYLE_NOTIFICATIONS:
                MESSAGING_STYLE_NOTIFICATIONS = flag;
                break;
        }
    }

    private SharedPreferences getSharedPrefs() {
        return PreferenceManager.getDefaultSharedPreferences(context);
    }

    private boolean getDefault() {
        return BuildConfig.DEBUG;
    }
}
