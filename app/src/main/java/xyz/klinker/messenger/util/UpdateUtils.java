package xyz.klinker.messenger.util;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.preference.PreferenceManager;
import android.util.Log;

import java.util.Date;

import xyz.klinker.messenger.R;
import xyz.klinker.messenger.activity.MessengerActivity;
import xyz.klinker.messenger.api.implementation.Account;
import xyz.klinker.messenger.data.FeatureFlags;
import xyz.klinker.messenger.service.ContactSyncService;
import xyz.klinker.messenger.service.ForceTokenRefreshService;
import xyz.klinker.messenger.service.SignoutService;
import xyz.klinker.messenger.service.SubscriptionExpirationCheckService;

public class UpdateUtils {

    private static final String TAG = "UpdateUtil";

    private Activity context;

    public UpdateUtils(Activity context) {
        this.context = context;
    }

    public boolean checkForUpdate() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);

        alertToTextFromAnywhere(sharedPreferences);

        if (sharedPreferences.getBoolean("migrate_to_trial", true)) {
            sharedPreferences.edit().putBoolean("migrate_to_trial", false).commit();
            //new BetaTesterMigrationToTrial(context).alertToMigration();
        }

        int storedAppVersion = sharedPreferences.getInt("app_version", 0);
        int currentAppVersion = getAppVersion();

        if (storedAppVersion != currentAppVersion) {
            Log.v(TAG, "new app version");
            sharedPreferences.edit().putInt("app_version", currentAppVersion).apply();
            runEveryUpdate();
            return true;
        } else {
            return false;
        }
    }

    private void runEveryUpdate() {
        ContactSyncService.scheduleNextRun(context);
        SubscriptionExpirationCheckService.scheduleNextRun(context);
        SignoutService.scheduleNextRun(context);

        context.startService(new Intent(context, ForceTokenRefreshService.class));
    }

    private int getAppVersion() {
        try {
            PackageInfo packageInfo = context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0);
            return packageInfo.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            // should never happen
            return -1;
        }
    }

    private void alertToTextFromAnywhere(SharedPreferences sharedPrefs) {
        Account account = Account.get(context);

        if (account.accountId == null && !sharedPrefs.getBoolean("seen_use_anywhere", false)) {
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
