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

                    ApiUtils.INSTANCE.updateBaseTheme(Account.INSTANCE.getAccountId(),
                            newValue);

                    getActivity().recreate();

                    return true;
                });
    }

    private void initFontSize() {
        findPreference(getString(R.string.pref_font_size))
                .setOnPreferenceChangeListener((preference, o) -> {
                    String size = (String) o;
                    ApiUtils.INSTANCE.updateFontSize(Account.INSTANCE.getAccountId(),
                            size);

                    return true;
                });
    }

    private void initRounderBubbles() {
        findPreference(getString(R.string.pref_rounder_bubbles))
                .setOnPreferenceChangeListener((preference, o) -> {
                    boolean rounder = (boolean) o;
                    ApiUtils.INSTANCE.updateRounderBubbles(Account.INSTANCE.getAccountId(),
                            rounder);
                    return true;
                });
    }

    private void initUseGlobalTheme() {
        findPreference(getString(R.string.pref_apply_theme_globally))
                .setOnPreferenceChangeListener((preference, o) -> {
                    boolean global = (boolean) o;
                    ApiUtils.INSTANCE.updateUseGlobalTheme(Account.INSTANCE.getAccountId(),
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

            ColorUtils.INSTANCE.animateToolbarColor(getActivity(), Settings.INSTANCE.getMainColorSet().getColor(), (int) o);
            settings.setValue(getActivity(), getString(R.string.pref_global_primary_color), (int) o);
            Settings.INSTANCE.getMainColorSet().setColor((int) o);

            ApiUtils.INSTANCE.updatePrimaryThemeColor(Account.INSTANCE.getAccountId(), (int) o);

            return true;
        });

        preference.setColorSelectedListener(colors -> {
            darkColorPreference.setColor(colors.getColorDark());
            accentColorPreference.setColor(colors.getColorAccent());
        });

        darkColorPreference.setOnPreferenceChangeListener((preference12, o) -> {
            Settings settings = Settings.get(getActivity());

            ColorUtils.INSTANCE.animateStatusBarColor(getActivity(), Settings.INSTANCE.getMainColorSet().getColorDark(), (int) o);
            settings.setValue(getActivity(), getString(R.string.pref_global_primary_dark_color), (int) o);
            Settings.INSTANCE.getMainColorSet().setColorDark((int) o);

            ApiUtils.INSTANCE.updatePrimaryDarkThemeColor(Account.INSTANCE.getAccountId(), (int) o);

            return true;
        });

        accentColorPreference.setOnPreferenceChangeListener((preference13, o) -> {
            Settings settings = Settings.get(getActivity());

            settings.setValue(getActivity(), getString(R.string.pref_global_accent_color), (int) o);
            Settings.INSTANCE.getMainColorSet().setColorAccent((int) o);

            ApiUtils.INSTANCE.updateAccentThemeColor(Account.INSTANCE.getAccountId(), (int) o);

            return true;
        });
    }
}
