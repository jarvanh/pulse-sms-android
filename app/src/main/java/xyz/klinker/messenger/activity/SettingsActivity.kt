/*
 * Copyright (C) 2017 Luke Klinker
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package xyz.klinker.messenger.activity

import android.app.Fragment
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import xyz.klinker.messenger.R
import xyz.klinker.messenger.activity.main.MainColorController
import xyz.klinker.messenger.fragment.settings.FeatureSettingsFragment
import xyz.klinker.messenger.fragment.settings.GlobalSettingsFragment
import xyz.klinker.messenger.fragment.settings.MmsConfigurationFragment
import xyz.klinker.messenger.fragment.settings.ThemeSettingsFragment
import xyz.klinker.messenger.shared.activity.AbstractSettingsActivity
import xyz.klinker.messenger.shared.data.MmsSettings
import xyz.klinker.messenger.shared.data.Settings
import xyz.klinker.messenger.shared.util.ActivityUtils
import xyz.klinker.messenger.shared.util.ColorUtils
import xyz.klinker.messenger.shared.util.StringUtils

class SettingsActivity : AbstractSettingsActivity() {

    private var globalFragment: GlobalSettingsFragment? = null
    private var featureFragment: FeatureSettingsFragment? = null
    private var mmsFragment: MmsConfigurationFragment? = null
    private var themeFragment: ThemeSettingsFragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val type = intent.getIntExtra(ARG_SETTINGS_TYPE, TYPE_GLOBAL)
        when (type) {
            TYPE_GLOBAL -> {
                globalFragment = GlobalSettingsFragment()
                title = StringUtils.titleize(getString(R.string.menu_settings))
                startFragment(globalFragment!!)
            }
            TYPE_FEATURE -> {
                featureFragment = FeatureSettingsFragment()
                title = StringUtils.titleize(getString(R.string.menu_feature_settings))
                startFragment(featureFragment!!)
            }
            TYPE_MMS -> {
                mmsFragment = MmsConfigurationFragment()
                setTitle(R.string.menu_mms_configuration)
                startFragment(mmsFragment!!)
            }
            TYPE_THEME -> {
                themeFragment = ThemeSettingsFragment()
                title = getString(R.string.theme_settings_redirect)
                startFragment(themeFragment!!)
            }
        }

        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        // sets it to teal if there is no color selected
        toolbar?.setBackgroundColor(Settings.mainColorSet.color)
        ActivityUtils.setStatusBarColor(this, Settings.mainColorSet.colorDark)

        ColorUtils.checkBlackBackground(this)
        MainColorController(this).configureNavigationBarColor()
    }

    private fun startFragment(fragment: Fragment) {
        fragmentManager.beginTransaction()
                .replace(R.id.settings_content, fragment)
                .commit()
    }

    public override fun onStart() {
        super.onStart()

        toolbar?.setBackgroundColor(Settings.mainColorSet.color)
        ActivityUtils.setStatusBarColor(this, Settings.mainColorSet.colorDark)

        ActivityUtils.setTaskDescription(this)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        if (globalFragment != null) {
            val inflater = menuInflater
            inflater.inflate(R.menu.global_settings, menu)
        }

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
        }

        return true
    }

    override fun onBackPressed() {
        Settings.forceUpdate(this)

        if (mmsFragment == null && themeFragment == null) {
            val intent = Intent(this, MessengerActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        } else {
            MmsSettings.forceUpdate(this)
        }

        super.onBackPressed()
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        globalFragment!!.onActivityResult(requestCode, resultCode, data)
    }

    companion object {

        private val ARG_SETTINGS_TYPE = "arg_settings_type"
        private val TYPE_GLOBAL = 1
        private val TYPE_FEATURE = 2
        private val TYPE_MMS = 3
        private val TYPE_THEME = 4

        fun startGlobalSettings(context: Context) {
            startWithExtra(context, TYPE_GLOBAL)
        }

        fun startFeatureSettings(context: Context) {
            startWithExtra(context, TYPE_FEATURE)
        }

        fun startMmsSettings(context: Context) {
            startWithExtra(context, TYPE_MMS)
        }

        fun startThemeSettings(context: Context) {
            startWithExtra(context, TYPE_THEME)
        }

        private fun startWithExtra(context: Context, type: Int) {
            val intent = Intent(context, SettingsActivity::class.java)
            intent.putExtra(ARG_SETTINGS_TYPE, type)
            context.startActivity(intent)
        }
    }

}
