package xyz.klinker.messenger.util;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.preference.PreferenceManager;

import java.util.Date;

import xyz.klinker.messenger.R;
import xyz.klinker.messenger.activity.MessengerActivity;
import xyz.klinker.messenger.api.implementation.Account;
import xyz.klinker.messenger.data.FeatureFlags;
import xyz.klinker.messenger.service.ContactSyncService;
import xyz.klinker.messenger.service.SignoutService;
import xyz.klinker.messenger.service.SubscriptionExpirationCheckService;

public class UpdateUtils {
    public static void checkForUpdate(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);

        alertToTextFromAnywhere(context, sharedPreferences);

        // i just want to hold this value, in case we need it later when we roll out public.
        sharedPreferences.edit().putBoolean("is_on_pre_release", true).apply();

        int storedAppVersion = sharedPreferences.getInt("app_version", 0);
        int currentAppVersion = getAppVersion(context);

        if (storedAppVersion != currentAppVersion) {
            sharedPreferences.edit().putInt("app_version", currentAppVersion).apply();
            runEveryUpdate(context);
        }
    }

    private static void runEveryUpdate(Context context) {
        ContactSyncService.scheduleNextRun(context);
        SubscriptionExpirationCheckService.scheduleNextRun(context);
        SignoutService.scheduleNextRun(context);
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

    private static void alertToTextFromAnywhere(final Context context, SharedPreferences sharedPrefs) {
        Account account = Account.get(context);

        if (!FeatureFlags.IS_BETA && account.accountId == null && !sharedPrefs.getBoolean("seen_use_anywhere", false)) {
            long installTime = sharedPrefs.getLong("install_time", 0);
            if (installTime != 0 && installTime - new Date().getTime() > TimeUtils.TWO_WEEKS) {
                new AlertDialog.Builder(context)
                        .setTitle(R.string.did_you_know)
                        .setMessage(R.string.use_from_anywhere)
                        .setPositiveButton(R.string.learn_more, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (context instanceof MessengerActivity) {
                                    ((MessengerActivity) context).menuItemClicked(R.id.drawer_account);
                                }
                            }
                        })
                        .setNegativeButton(R.string.dismiss, new DialogInterface.OnClickListener() {
                            @Override public void onClick(DialogInterface dialog, int which) { }
                        }).show();

                sharedPrefs.edit().putBoolean("seen_use_anywhere", true).commit();
            } else if (installTime == 0) {
                sharedPrefs.edit().putLong("install_time", new Date().getTime()).commit();
            }
        }
    }
}
