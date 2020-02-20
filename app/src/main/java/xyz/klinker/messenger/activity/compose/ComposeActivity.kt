/*
 * Copyright (C) 2020 Luke Klinker
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

package xyz.klinker.messenger.activity.compose

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import xyz.klinker.messenger.R
import xyz.klinker.messenger.activity.main.MainColorController
import xyz.klinker.messenger.api.implementation.Account
import xyz.klinker.messenger.api.implementation.ApiUtils
import xyz.klinker.messenger.shared.data.MmsSettings
import xyz.klinker.messenger.shared.data.Settings
import xyz.klinker.messenger.shared.util.ActivityUtils
import xyz.klinker.messenger.shared.util.ColorUtils
import xyz.klinker.messenger.shared.view.WhitableToolbar

/**
 * Activity to display UI for creating a new conversation.
 */
class ComposeActivity : AppCompatActivity() {

    internal val contactsProvider: ComposeContactsProvider by lazy { ComposeContactsProvider(this) }
    internal val shareHandler: ComposeShareHandler by lazy { ComposeShareHandler(this) }
    internal val vCardSender: ComposeVCardSender by lazy { ComposeVCardSender(this) }
    internal val sender: ComposeSendHelper by lazy { ComposeSendHelper(this) }
    private val intentHandler: ComposeIntentHandler by lazy { ComposeIntentHandler(this) }

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_compose)

        MainColorController(this).configureNavigationBarColor()

        val toolbar = findViewById<View>(R.id.toolbar) as WhitableToolbar
        toolbar.setBackgroundColor(Settings.mainColorSet.color)
        findViewById<View>(R.id.toolbar_holder).setBackgroundColor(
                ActivityUtils.possiblyOverrideColorSelection(this, Settings.mainColorSet.color)
        )

        contactsProvider.contactEntry.setTextColor(toolbar.textColor)

        setSupportActionBar(toolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        title = " "

        ActivityUtils.setStatusBarColor(this, Settings.mainColorSet.colorDark, Settings.mainColorSet.color)
        ActivityUtils.setTaskDescription(this)
        ColorUtils.checkBlackBackground(this)

        sender.setupViews()
        contactsProvider.setupViews()
        intentHandler.handle(intent)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.activity_compose, menu)

        val mobileOnly = menu.findItem(R.id.menu_mobile_only)
        mobileOnly.isChecked = Settings.mobileOnly

        val groupMms = menu.findItem(R.id.menu_group_mms)
        groupMms.isChecked = MmsSettings.groupMMS

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                finish()
                return true
            }
            R.id.menu_mobile_only -> {
                val newValue = !item.isChecked
                item.isChecked = newValue

                Settings.setValue(this, getString(R.string.pref_mobile_only), newValue)
                Settings.forceUpdate(this)

                contactsProvider.toggleMobileOnly(item.isChecked)
                ApiUtils.updateMobileOnly(Account.accountId, newValue)

                return true
            }
            R.id.menu_group_mms -> {
                val newValue = !item.isChecked
                item.isChecked = newValue

                Settings.setValue(this, getString(R.string.pref_group_mms), newValue)
                MmsSettings.forceUpdate(this)

                ApiUtils.updateGroupMMS(Account.accountId, newValue)
                return true
            }
        }

        return false
    }
}
