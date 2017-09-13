package xyz.klinker.messenger.shared.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.annotation.VisibleForTesting;

import com.codekidlabs.storagechooser.utils.DiskUtil;

import java.io.File;

import xyz.klinker.messenger.shared.R;

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
    public boolean groupMMS;
    public boolean autoSaveMedia;
    public boolean overrideSystemAPN;
    public String saveDirectory;

    public String mmscUrl;
    public String mmsProxy;
    public String mmsPort;
    public String userAgent;
    public String userAgentProfileUrl;
    public String userAgentProfileTagName;

    public int numberOfMessagesBeforeMms;
    public long maxImageSize;

    private MmsSettings(final Context context) {
        init(context);
    }

    @VisibleForTesting
    protected void init(Context context) {
        SharedPreferences sharedPrefs = getSharedPrefs(context);

        this.groupMMS = sharedPrefs.getBoolean(context.getString(R.string.pref_group_mms), true);
        this.autoSaveMedia = sharedPrefs.getBoolean(context.getString(R.string.pref_auto_save_media), false);
        this.overrideSystemAPN = sharedPrefs.getBoolean(context.getString(R.string.pref_override_system_apn), false);
        this.saveDirectory = sharedPrefs.getString(DiskUtil.SC_PREFERENCE_KEY,
                new File(Environment.getExternalStorageDirectory(), "Download").getPath());

        this.mmscUrl = sharedPrefs.getString(context.getString(R.string.pref_mmsc_url), "");
        this.mmsProxy = sharedPrefs.getString(context.getString(R.string.pref_mms_proxy), "");
        this.mmsPort = sharedPrefs.getString(context.getString(R.string.pref_mms_port), "");
        this.userAgent = sharedPrefs.getString(context.getString(R.string.pref_user_agent), "Android-Mms/2.0");
        this.userAgentProfileUrl = sharedPrefs.getString(context.getString(R.string.pref_user_agent_profile_url), "");
        this.userAgentProfileTagName = sharedPrefs.getString(context.getString(R.string.pref_user_agent_profile_tag), "x-wap-profile");

        String sizeString = sharedPrefs.getString(context.getString(R.string.pref_mms_size), "500_kb");
        switch (sizeString) {
            case "100_kb":
                this.maxImageSize = 100 * 1024;
                break;
            case "300_kb":
                this.maxImageSize = 300 * 1024;
                break;
            case "500_kb":
                this.maxImageSize = 500 * 1024;
                break;
            case "700_kb":
                this.maxImageSize = 700 * 1024;
                break;
            case "1_mb":
                this.maxImageSize = 900 * 1024;
                break;
            case "2_mb":
                this.maxImageSize = 2000 * 1024;
                break;
            case "3_mb":
                this.maxImageSize = 3000 * 1024;
                break;
            case "5_mb":
                this.maxImageSize = 5000 * 1024;
                break;
            case "10_mb":
                this.maxImageSize = 10000 * 1024;
                break;
            default:
                this.maxImageSize = 500 * 1024;
                break;
        }

        String convertToMmsAfterXMessages = sharedPrefs.getString(context.getString(R.string.pref_convert_to_mms), "3");
        if (convertToMmsAfterXMessages.equals("0")) {
            this.convertLongMessagesToMMS = false;
            this.numberOfMessagesBeforeMms = -1;
        } else {
            this.convertLongMessagesToMMS = true;
            this.numberOfMessagesBeforeMms = Integer.parseInt(convertToMmsAfterXMessages);
        }
    }

    /**
     * Forces a reload of all MMS Settings data.
     */
    public void forceUpdate(Context context) {
        init(context);
    }

    public SharedPreferences getSharedPrefs(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context);
    }
}
