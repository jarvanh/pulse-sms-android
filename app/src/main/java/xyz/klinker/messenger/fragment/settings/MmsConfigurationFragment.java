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
}
