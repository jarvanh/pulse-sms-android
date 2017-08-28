package xyz.klinker.messenger.utils;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.preference.PreferenceManager;
import android.util.Log;

import xyz.klinker.messenger.api.implementation.Account;
import xyz.klinker.messenger.api.implementation.firebase.ScheduledTokenRefreshService;
import xyz.klinker.messenger.shared.R;
import xyz.klinker.messenger.shared.data.ColorSet;
import xyz.klinker.messenger.shared.data.MmsSettings;
import xyz.klinker.messenger.shared.data.Settings;
import xyz.klinker.messenger.shared.service.ContactResyncService;
import xyz.klinker.messenger.shared.service.ForceTokenRefreshService;
import xyz.klinker.messenger.shared.service.jobs.CleanupOldMessagesJob;
import xyz.klinker.messenger.shared.service.jobs.ContactSyncJob;
import xyz.klinker.messenger.shared.service.jobs.ScheduledMessageJob;
import xyz.klinker.messenger.shared.service.jobs.SignoutJob;
import xyz.klinker.messenger.shared.service.jobs.SubscriptionExpirationCheckJob;

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

        int storedAppVersion = sharedPreferences.getInt("app_version", 0);

        if (sharedPreferences.getBoolean("v2.5.0.1", true)) {
            String colorSetName = sharedPreferences.getString(context.getString(R.string.pref_global_color_theme), "default");
            ColorSet legacyGlobalTheme = ColorSet.getFromString(context, colorSetName);

            sharedPreferences.edit()
                    .putBoolean("v2.5.0.1", false)
                    .putInt(context.getString(R.string.pref_global_primary_color), legacyGlobalTheme.color)
                    .putInt(context.getString(R.string.pref_global_primary_dark_color), legacyGlobalTheme.colorDark)
                    .putInt(context.getString(R.string.pref_global_primary_light_color), legacyGlobalTheme.colorLight)
                    .putInt(context.getString(R.string.pref_global_accent_color), legacyGlobalTheme.colorAccent)
                    .putBoolean(context.getString(R.string.pref_apply_theme_globally), !colorSetName.equals("default"))
                    .commit();

            settings.forceUpdate(context);
        }

        if (sharedPreferences.getBoolean("v2.5.4.2", true)) {
            if (storedAppVersion != 0) {
                context.startService(new Intent(context, ContactResyncService.class));
            }

            sharedPreferences.edit()
                    .putBoolean("v2.5.4.2", false)
                    .commit();
        }

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
