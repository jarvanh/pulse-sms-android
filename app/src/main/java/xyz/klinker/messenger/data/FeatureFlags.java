package xyz.klinker.messenger.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.VisibleForTesting;

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

    // I saw some people online trimming their data before the decryption
    private static final String FLAG_TRIM_DECRYPTION = "flag_trim_decryption";
    private static final boolean TRIM_DECRYPTION_DEFAULT = false;
    public boolean TRIM_DECRYPTION;

    private Context context;
    private FeatureFlags(final Context context) {
        this.context = context;
        SharedPreferences sharedPrefs = getSharedPrefs();

        TRIM_DECRYPTION = sharedPrefs.getBoolean(FLAG_TRIM_DECRYPTION, TRIM_DECRYPTION_DEFAULT);
    }

    public void updateFlag(String identifier, boolean flag) {
        getSharedPrefs().edit()
                .putBoolean(identifier, flag)
                .apply();

        switch (identifier) {
            case FLAG_TRIM_DECRYPTION:
                TRIM_DECRYPTION = flag;
                break;
        }
    }

    private SharedPreferences getSharedPrefs() {
        return PreferenceManager.getDefaultSharedPreferences(context);
    }
}
