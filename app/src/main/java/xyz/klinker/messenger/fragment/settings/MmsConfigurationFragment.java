package xyz.klinker.messenger.fragment.settings;

import android.os.Bundle;
import android.preference.PreferenceFragment;

import xyz.klinker.messenger.R;
import xyz.klinker.messenger.api.implementation.Account;
import xyz.klinker.messenger.api.implementation.ApiUtils;
import xyz.klinker.messenger.data.Settings;

public class MmsConfigurationFragment extends PreferenceFragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.settings_mms);
        initConvertToMMS();
        initMaxImageSize();
        initGroupMMS();
        initAutoSaveMedia();

        // MMS APN settings
        initOverrideSystemSettings();
        initMmsc();
        initProxy();
        initPort();
        initUserAgent();
        initUserAgentProfileUrl();
        initUserAgentProfileTagName();
    }

    @Override
    public void onStop() {
        super.onStop();
        Settings.get(getActivity()).forceUpdate();
    }


    private void initConvertToMMS() {
        findPreference(getString(R.string.pref_convert_to_mms))
                .setOnPreferenceChangeListener((preference, o) -> {
                    boolean convert = (boolean) o;
                    new ApiUtils().updateConvertToMMS(Account.get(getActivity()).accountId,
                            convert);
                    return true;
                });
    }

    private void initMaxImageSize() {
        findPreference(getString(R.string.pref_mms_size))
                .setOnPreferenceChangeListener((preference, o) -> {
                    String size = (String) o;
                    new ApiUtils().updateMmsSize(Account.get(getActivity()).accountId,
                            size);
                    return true;
                });
    }

    private void initGroupMMS() {
        findPreference(getString(R.string.pref_group_mms))
                .setOnPreferenceChangeListener((preference, o) -> {
                    boolean group = (boolean) o;
                    new ApiUtils().updateGroupMMS(Account.get(getActivity()).accountId,
                            group);
                    return true;
                });
    }

    private void initAutoSaveMedia() {
        findPreference(getString(R.string.pref_auto_save_media))
                .setOnPreferenceChangeListener((preference, o) -> {
                    boolean save = (boolean) o;
                    new ApiUtils().updateAutoSaveMedia(Account.get(getActivity()).accountId,
                            save);
                    return true;
                });
    }

    private void initOverrideSystemSettings() {
        findPreference(getString(R.string.pref_override_system_apn))
                .setOnPreferenceChangeListener((preference, o) -> {
                    boolean override = (boolean) o;
                    new ApiUtils().updateOverrideSystemApn(Account.get(getActivity()).accountId,
                            override);
                    return true;
                });
    }

    private void initMmsc() {
        findPreference(getString(R.string.pref_mmsc_url))
                .setOnPreferenceChangeListener((preference, o) -> {
                    String mmsc = (String) o;
                    new ApiUtils().updateMmscUrl(Account.get(getActivity()).accountId,
                            mmsc);
                    return true;
                });
    }

    private void initProxy() {
        findPreference(getString(R.string.pref_mms_proxy))
                .setOnPreferenceChangeListener((preference, o) -> {
                    String proxy = (String) o;
                    new ApiUtils().updateMmsProxy(Account.get(getActivity()).accountId,
                            proxy);
                    return true;
                });
    }

    private void initPort() {
        findPreference(getString(R.string.pref_mms_port))
                .setOnPreferenceChangeListener((preference, o) -> {
                    String port = (String) o;
                    new ApiUtils().updateMmsPort(Account.get(getActivity()).accountId,
                            port);
                    return true;
                });
    }

    private void initUserAgent() {
        findPreference(getString(R.string.pref_user_agent))
                .setOnPreferenceChangeListener((preference, o) -> {
                    String userAgent = (String) o;
                    new ApiUtils().updateUserAgent(Account.get(getActivity()).accountId,
                            userAgent);
                    return true;
                });
    }

    private void initUserAgentProfileUrl() {
        findPreference(getString(R.string.pref_user_agent_profile_url))
                .setOnPreferenceChangeListener((preference, o) -> {
                    String uaProfileUrl = (String) o;
                    new ApiUtils().updateUserAgentProfileUrl(Account.get(getActivity()).accountId,
                            uaProfileUrl);
                    return true;
                });
    }

    private void initUserAgentProfileTagName() {
        findPreference(getString(R.string.pref_user_agent_profile_tag))
                .setOnPreferenceChangeListener((preference, o) -> {
                    String uaProfileTagName = (String) o;
                    new ApiUtils().updateUserAgentProfileTagName(Account.get(getActivity()).accountId,
                            uaProfileTagName);
                    return true;
                });
    }
}
