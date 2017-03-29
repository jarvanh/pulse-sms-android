package xyz.klinker.messenger.fragment.settings;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.codekidlabs.storagechooser.StorageChooser;

import xyz.klinker.messenger.R;
import xyz.klinker.messenger.api.implementation.Account;
import xyz.klinker.messenger.api.implementation.ApiUtils;
import xyz.klinker.messenger.shared.data.MmsSettings;
import xyz.klinker.messenger.shared.data.Settings;

public class MmsConfigurationFragment extends PreferenceFragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.settings_mms);
        initConvertToMMS();
        initMaxImageSize();
        initGroupMMS();
        initAutoSaveMedia();
        initDownloadLocation();

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
                    String convert = (String) o;
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

    private void initDownloadLocation() {
        findPreference(getString(R.string.pref_mms_save_location))
                .setOnPreferenceClickListener(preference -> {

                    if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE)
                            == PackageManager.PERMISSION_GRANTED) {
                        showStorageChooser();
                    } else {
                        if (getActivity() != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            getActivity().requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, -1);
                        } else {
                            showStorageChooser();
                        }
                    }

                    return false;
                });
    }

    private void showStorageChooser() {
        StorageChooser chooser = new StorageChooser.Builder()
                .withActivity(getActivity())
                .withFragmentManager(((AppCompatActivity) getActivity()).getSupportFragmentManager())
                .allowCustomPath(true)
                .setType(StorageChooser.DIRECTORY_CHOOSER)
                .actionSave(true)
                .withPreference(MmsSettings.get(getActivity()).getSharedPrefs(getActivity()))
                .build();

        chooser.show();
        chooser.setOnSelectListener(s -> { });
    }

    private void initOverrideSystemSettings() {
        findPreference(getString(R.string.pref_override_system_apn))
                .setOnPreferenceChangeListener((preference, o) -> {
                    boolean override = (boolean) o;
                    new ApiUtils().updateOverrideSystemApn(Account.get(getActivity()).accountId,
                            override);

                    if (override) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !android.provider.Settings.System.canWrite(getActivity())) {
                            new AlertDialog.Builder(getActivity())
                                    .setMessage(com.klinker.android.send_message.R.string.write_settings_permission)
                                    .setPositiveButton(com.klinker.android.send_message.R.string.ok, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            Intent intent = new Intent(android.provider.Settings.ACTION_MANAGE_WRITE_SETTINGS);
                                            intent.setData(Uri.parse("package:" + getActivity().getPackageName()));
                                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                                            try {
                                                startActivity(intent);
                                            } catch (Exception e) {
                                                Log.e("MainActivity", "error starting permission intent", e);
                                            }
                                        }
                                    })
                                    .show();
                        }
                    }

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
