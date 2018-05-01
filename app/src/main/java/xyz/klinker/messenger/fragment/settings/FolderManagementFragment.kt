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
import xyz.klinker.messenger.shared.util.DrawerItemHelper
import xyz.klinker.messenger.shared.util.listener.ColorSelectedListener
import xyz.klinker.messenger.view.ColorPreference

class FolderManagementFragment : MaterialPreferenceFragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        addPreferencesFromResource(R.xml.settings_folder)
    }

    override fun onStop() {
        super.onStop()
        DrawerItemHelper.folders = null
    }

}
