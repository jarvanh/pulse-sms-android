package xyz.klinker.messenger.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.VisibleForTesting;

import xyz.klinker.messenger.R;

public class MmsSettings {

    /**
     * Gets a new instance (singleton) of MmsSettings.
     *
     * @param context the current application context.
     * @return the settings_mms instance.
     */
    public static synchronized MmsSettings get(Context context) {
        if (settings == null) {
            settings = new MmsSettings(context);
        }

        return settings;
    }

    private static volatile MmsSettings settings;

    public boolean convertLongMessagesToMMS;
    public boolean overrideSystemAPN;

    public String mmscUrl;
    public String mmsProxy;
    public String mmsPort;
    public String userAgent;
    public String userAgentProfileUrl;
    public String userAgentProfileTagName;

    public long maxImageSize;

    private MmsSettings(final Context context) {
        init(context);
    }

    @VisibleForTesting
    protected void init(Context context) {
        SharedPreferences sharedPrefs = getSharedPrefs(context);

        this.convertLongMessagesToMMS = sharedPrefs.getBoolean(context.getString(R.string.pref_convert_to_mms), true);
        this.overrideSystemAPN = sharedPrefs.getBoolean(context.getString(R.string.pref_override_system_apn), false);

        this.mmscUrl = sharedPrefs.getString(context.getString(R.string.pref_mmsc_url), "");
        this.mmsProxy = sharedPrefs.getString(context.getString(R.string.pref_mms_proxy), "");
        this.mmsPort = sharedPrefs.getString(context.getString(R.string.pref_mms_port), "");
        this.userAgent = sharedPrefs.getString(context.getString(R.string.pref_user_agent), "Android-Mms/2.0");
        this.userAgentProfileUrl = sharedPrefs.getString(context.getString(R.string.pref_user_agent_profile_url), "");
        this.userAgentProfileTagName = sharedPrefs.getString(context.getString(R.string.pref_user_agent_profile_tag), "x-wap-profile");

        String sizeString = sharedPrefs.getString(context.getString(R.string.pref_mms_size), "1_mb");
        switch (sizeString) {
            case "100_kb":
                this.maxImageSize = 100 * 1024;
                break;
            case "300_kb":
                this.maxImageSize = 300 * 1024;
                break;
            case "500_kb":
                this.maxImageSize = 800 * 1024;
                break;
            case "700_kb":
                this.maxImageSize = 700 * 1024;
                break;
            case "1_mb":
                this.maxImageSize = 900 * 1024;
                break;
            case "5_mb":
                // make it 5 mb, don't actually really want it to be unlimited...
                this.maxImageSize = 5000 * 1024;
            default:
                this.maxImageSize = 900 * 1024;
                break;
        }
    }

    @VisibleForTesting
    public SharedPreferences getSharedPrefs(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context);
    }
}
