package xyz.klinker.messenger.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class UpdateUtils {
    public static void checkForUpdate(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);

        // i just want to hold this value, in case we need it later when we roll out public.
        sharedPreferences.edit().putBoolean("is_on_pre_release", true).apply();
    }
}
