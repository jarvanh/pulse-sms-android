package xyz.klinker.messenger.fragment.settings;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceGroup;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import xyz.klinker.messenger.R;
import xyz.klinker.messenger.api.implementation.Account;
import xyz.klinker.messenger.api.implementation.ApiUtils;
import xyz.klinker.messenger.data.FeatureFlags;
import xyz.klinker.messenger.data.Settings;

public class FeatureSettingsFragment extends PreferenceFragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.settings_features);
        initSignature();
    }

    @Override
    public void onStop() {
        super.onStop();
        Settings.get(getActivity()).forceUpdate();
    }

    private void initSignature() {
        findPreference(getString(R.string.pref_signature)).setOnPreferenceClickListener(p -> {
            //noinspection AndroidLintInflateParams
            View layout = LayoutInflater.from(getActivity()).inflate(R.layout.dialog_edit_text,
                    null, false);
            final EditText editText = (EditText) layout.findViewById(R.id.edit_text);
            editText.setHint(R.string.signature);
            editText.setText(Settings.get(getActivity()).signature);
            editText.setSelection(editText.getText().length());

            new AlertDialog.Builder(getActivity())
                    .setView(layout)
                    .setPositiveButton(R.string.save, (dialogInterface, i) -> {
                        if (editText.getText().length() > 0) {
                            new ApiUtils().updateSignature(Account.get(getActivity()).accountId,
                                    editText.getText().toString());
                        } else {
                            new ApiUtils().updateSignature(Account.get(getActivity()).accountId, "");
                        }
                    })
                    .setNegativeButton(android.R.string.cancel, null)
                    .show();

            return false;
        });
    }
}
