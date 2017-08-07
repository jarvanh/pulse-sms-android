package xyz.klinker.messenger.utils;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.util.Log;
import android.view.View;

import java.util.Date;

import xyz.klinker.messenger.shared.R;
import xyz.klinker.messenger.activity.MessengerActivity;
import xyz.klinker.messenger.api.implementation.Account;
import xyz.klinker.messenger.api.implementation.firebase.ScheduledTokenRefreshService;
import xyz.klinker.messenger.shared.data.MmsSettings;
import xyz.klinker.messenger.shared.data.Settings;
import xyz.klinker.messenger.shared.service.jobs.CleanupOldMessagesJob;
import xyz.klinker.messenger.shared.service.jobs.ContactSyncJob;
import xyz.klinker.messenger.shared.service.jobs.ContentObserverRunCheckJob;
import xyz.klinker.messenger.shared.service.ForceTokenRefreshService;
import xyz.klinker.messenger.shared.service.jobs.ScheduledMessageJob;
import xyz.klinker.messenger.shared.service.jobs.SignoutJob;
import xyz.klinker.messenger.shared.service.jobs.SubscriptionExpirationCheckJob;
import xyz.klinker.messenger.shared.util.NotificationUtils;
import xyz.klinker.messenger.shared.util.TimeUtils;

public class UpdateUtils {

    private static final String TAG = "UpdateUtil";

    private Activity context;

    public UpdateUtils(Activity context) {
        this.context = context;
    }

    public boolean checkForUpdate() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);

        Account account = Account.get(context);
        Settings settings = Settings.get(context);
        MmsSettings mmsSettings = MmsSettings.get(context);

        if (sharedPreferences.getBoolean("v2.4.9", true)) {
            sharedPreferences.edit()
                    .putBoolean("v2.4.9", false)
                    .apply();

            NotificationUtils.deleteAllChannels(context);
            NotificationUtils.createNotificationChannels(context);
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
        CleanupOldMessagesJob.scheduleNextRun(context);
        ScheduledMessageJob.scheduleNextRun(context);
        ContentObserverRunCheckJob.scheduleNextRun(context);
        ContactSyncJob.scheduleNextRun(context);
        SubscriptionExpirationCheckJob.scheduleNextRun(context);
        SignoutJob.scheduleNextRun(context);
        ScheduledTokenRefreshService.scheduleNextRun(context);

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
}
