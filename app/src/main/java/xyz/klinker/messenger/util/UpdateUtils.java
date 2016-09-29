package xyz.klinker.messenger.util;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.preference.PreferenceManager;

import xyz.klinker.messenger.service.ContactSyncService;

public class UpdateUtils {
    public static void checkForUpdate(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);

        // i just want to hold this value, in case we need it later when we roll out public.
        sharedPreferences.edit().putBoolean("is_on_pre_release", true).apply();

        int storedAppVersion = sharedPreferences.getInt("app_version", 0);
        int currentAppVersion = getAppVersion(context);

        context.startService(new Intent(context, ContactSyncService.class));

        if (storedAppVersion != currentAppVersion) {
            sharedPreferences.edit().putInt("app_version", currentAppVersion).apply();
            runEveryUpdate(context);
        }
    }

    private static void runEveryUpdate(Context context) {
        ContactSyncService.scheduleNextRun(context);
    }

    private static int getAppVersion(Context c) {
        try {
            PackageInfo packageInfo = c.getPackageManager()
                    .getPackageInfo(c.getPackageName(), 0);
            return packageInfo.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            // should never happen
            return -1;
        }
    }
}
