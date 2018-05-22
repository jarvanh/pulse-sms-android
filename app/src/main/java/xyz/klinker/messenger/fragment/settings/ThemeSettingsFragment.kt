package xyz.klinker.messenger.fragment.settings

import android.os.Bundle
import android.preference.PreferenceCategory
import android.support.v7.app.AppCompatDelegate

import xyz.klinker.messenger.R
import xyz.klinker.messenger.api.implementation.Account
import xyz.klinker.messenger.api.implementation.ApiUtils
import xyz.klinker.messenger.shared.data.ColorSet
import xyz.klinker.messenger.shared.data.FeatureFlags
import xyz.klinker.messenger.shared.data.Settings
import xyz.klinker.messenger.shared.util.AndroidVersionUtil
import xyz.klinker.messenger.shared.util.ColorUtils
import xyz.klinker.messenger.shared.util.listener.ColorSelectedListener
import xyz.klinker.messenger.view.ColorPreference

class ThemeSettingsFragment : MaterialPreferenceFragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        addPreferencesFromResource(R.xml.settings_theme)

        initBaseTheme()
        initAdjustableNavBar()
        initFontSize()
        initRounderBubbles()

        initUseGlobalTheme()
        setUpColors()

    }

    override fun onStop() {
        super.onStop()
        Settings.forceUpdate(activity)
    }

    private fun initBaseTheme() {
        findPreference(getString(R.string.pref_base_theme))
                .setOnPreferenceChangeListener { _, o ->
                    val newValue = o as String
                    if (newValue != "day_night" && newValue != "light") {
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                    } else {
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                    }

                    ApiUtils.updateBaseTheme(Account.accountId, newValue)
                    activity.recreate()

                    true
                }
    }

    private fun initAdjustableNavBar() {
        findPreference(getString(R.string.pref_adjustable_nav_bar))
                .setOnPreferenceChangeListener { _, o ->
                    val adjustable = o as Boolean
                    ApiUtils.updateAdjustableNavBar(Account.accountId, adjustable)

                    activity.recreate()
                    true
                }

        // TODO: Remove all this at some point. We aren't going to use this preference anymore
//        if (!AndroidVersionUtil.isAndroidO_MR1) {
            val prefCategory = findPreference(getString(R.string.pref_general_category)) as PreferenceCategory
            prefCategory.removePreference(findPreference(getString(R.string.pref_adjustable_nav_bar)))
//        }
    }

    private fun initFontSize() {
        findPreference(getString(R.string.pref_font_size))
                .setOnPreferenceChangeListener { _, o ->
                    val size = o as String
                    ApiUtils.updateFontSize(Account.accountId, size)

                    true
                }
    }

    private fun initRounderBubbles() {
        findPreference(getString(R.string.pref_rounder_bubbles))
                .setOnPreferenceChangeListener { _, o ->
                    val rounder = o as Boolean
                    ApiUtils.updateRounderBubbles(Account.accountId, rounder)
                    true
                }

        // TODO: Remove all this at some point. We aren't going to use this preference anymore
        val prefCategory = findPreference(getString(R.string.pref_general_category)) as PreferenceCategory
        prefCategory.removePreference(findPreference(getString(R.string.pref_rounder_bubbles)))
    }

    private fun initUseGlobalTheme() {
        findPreference(getString(R.string.pref_apply_theme_globally))
                .setOnPreferenceChangeListener { _, o ->
                    val global = o as Boolean
                    ApiUtils.updateUseGlobalTheme(Account.accountId, global)

                    true
                }
    }

    private fun setUpColors() {
        val preference = findPreference(getString(R.string.pref_global_primary_color)) as ColorPreference
        val darkColorPreference = findPreference(getString(R.string.pref_global_primary_dark_color)) as ColorPreference
        val accentColorPreference = findPreference(getString(R.string.pref_global_accent_color)) as ColorPreference

        preference.setOnPreferenceChangeListener { _, o ->
            ColorUtils.animateToolbarColor(activity, Settings.mainColorSet.color, o as Int)
            Settings.setValue(activity, getString(R.string.pref_global_primary_color), o)
            Settings.mainColorSet.color = o

            ApiUtils.updatePrimaryThemeColor(Account.accountId, o)
            true
        }

        preference.setColorSelectedListener(object : ColorSelectedListener {
            override fun onColorSelected(colors: ColorSet) {
                darkColorPreference.setColor(colors.colorDark)
                accentColorPreference.setColor(colors.colorAccent)
            }
        })

        darkColorPreference.setOnPreferenceChangeListener { _, o ->
            ColorUtils.animateStatusBarColor(activity, Settings.mainColorSet.colorDark, o as Int)
            Settings.setValue(activity, getString(R.string.pref_global_primary_dark_color), o)
            Settings.mainColorSet.colorDark = o

            ApiUtils.updatePrimaryDarkThemeColor(Account.accountId, o)
            true
        }

        accentColorPreference.setOnPreferenceChangeListener { _, o ->
            Settings.setValue(activity, getString(R.string.pref_global_accent_color), o as Int)
            Settings.mainColorSet.colorAccent = o

            ApiUtils.updateAccentThemeColor(Account.accountId, o)
            true
        }
    }
}
