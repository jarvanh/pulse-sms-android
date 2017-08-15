package xyz.klinker.messenger.fragment.settings;

import android.os.Bundle;
import android.support.v7.app.AppCompatDelegate;

import xyz.klinker.messenger.R;
import xyz.klinker.messenger.api.implementation.Account;
import xyz.klinker.messenger.api.implementation.ApiUtils;
import xyz.klinker.messenger.shared.data.Settings;
import xyz.klinker.messenger.shared.util.ColorUtils;
import xyz.klinker.messenger.view.ColorPreference;

public class ThemeSettingsFragment extends MaterialPreferenceFragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.settings_theme);

        initBaseTheme();
        initFontSize();
        initRounderBubbles();

        initUseGlobalTheme();
        setUpColors();

    }

    @Override
    public void onStop() {
        super.onStop();
        Settings.get(getActivity()).forceUpdate(getActivity());
    }

    private void initBaseTheme() {
        findPreference(getString(R.string.pref_base_theme))
                .setOnPreferenceChangeListener((preference, o) -> {
                    String newValue = (String) o;
                    if (!newValue.equals("day_night") && !newValue.equals("light")) {
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                    } else {
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                    }

                    new ApiUtils().updateBaseTheme(Account.get(getActivity()).accountId,
                            newValue);

                    getActivity().recreate();

                    return true;
                });
    }

    private void initFontSize() {
        findPreference(getString(R.string.pref_font_size))
                .setOnPreferenceChangeListener((preference, o) -> {
                    String size = (String) o;
                    new ApiUtils().updateFontSize(Account.get(getActivity()).accountId,
                            size);

                    return true;
                });
    }

    private void initRounderBubbles() {
        findPreference(getString(R.string.pref_rounder_bubbles))
                .setOnPreferenceChangeListener((preference, o) -> {
                    boolean rounder = (boolean) o;
                    new ApiUtils().updateRounderBubbles(Account.get(getActivity()).accountId,
                            rounder);
                    return true;
                });
    }

    private void initUseGlobalTheme() {
        findPreference(getString(R.string.pref_apply_theme_globally))
                .setOnPreferenceChangeListener((preference, o) -> {
                    boolean global = (boolean) o;
                    new ApiUtils().updateUseGlobalTheme(Account.get(getActivity()).accountId,
                            global);
                    return true;
                });
    }

    private void setUpColors() {
        final ColorPreference preference = (ColorPreference)
                findPreference(getString(R.string.pref_global_primary_color));
        final ColorPreference darkColorPreference = (ColorPreference)
                findPreference(getString(R.string.pref_global_primary_dark_color));
        final ColorPreference accentColorPreference = (ColorPreference)
                findPreference(getString(R.string.pref_global_accent_color));

        preference.setOnPreferenceChangeListener((preference1, o) -> {
            Settings settings = Settings.get(getActivity());

            ColorUtils.animateToolbarColor(getActivity(), settings.mainColorSet.color, (int) o);
            settings.setValue(getActivity(), getString(R.string.pref_global_primary_color), (int) o);
            settings.mainColorSet.color = (int) o;

            new ApiUtils().updatePrimaryThemeColor(Account.get(getActivity()).accountId, (int) o);

            return true;
        });

        preference.setColorSelectedListener(colors -> {
            darkColorPreference.setColor(colors.colorDark);
            accentColorPreference.setColor(colors.colorAccent);
        });

        darkColorPreference.setOnPreferenceChangeListener((preference12, o) -> {
            Settings settings = Settings.get(getActivity());

            ColorUtils.animateStatusBarColor(getActivity(), settings.mainColorSet.colorDark, (int) o);
            settings.setValue(getActivity(), getString(R.string.pref_global_primary_dark_color), (int) o);
            settings.mainColorSet.colorDark = (int) o;

            new ApiUtils().updatePrimaryDarkThemeColor(Account.get(getActivity()).accountId, (int) o);

            return true;
        });

        accentColorPreference.setOnPreferenceChangeListener((preference13, o) -> {
            Settings settings = Settings.get(getActivity());

            settings.setValue(getActivity(), getString(R.string.pref_global_accent_color), (int) o);
            settings.mainColorSet.colorAccent = (int) o;

            new ApiUtils().updateAccentThemeColor(Account.get(getActivity()).accountId, (int) o);

            return true;
        });
    }
}
