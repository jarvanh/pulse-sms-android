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

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import xyz.klinker.messenger.R
import xyz.klinker.messenger.activity.main.MainColorController
import xyz.klinker.messenger.fragment.settings.ContactSettingsFragment
import xyz.klinker.messenger.shared.MessengerActivityExtras
import xyz.klinker.messenger.shared.activity.AbstractSettingsActivity
import xyz.klinker.messenger.shared.util.ActivityUtils
import xyz.klinker.messenger.shared.util.ColorUtils

/**
 * Activity for changing contact settings_global.
 */
class ContactSettingsActivity : AbstractSettingsActivity() {

    private val fragment: ContactSettingsFragment by lazy { ContactSettingsFragment.newInstance(
            intent.getLongExtra(EXTRA_CONVERSATION_ID, -1)) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        fragmentManager.beginTransaction()
                .replace(R.id.settings_content, fragment)
                .commit()

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        ColorUtils.checkBlackBackground(this)
        MainColorController(this).configureNavigationBarColor()
    }

    public override fun onStart() {
        super.onStart()
        ActivityUtils.setTaskDescription(this, fragment.conversation.title!!, fragment.conversation.colors.color)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
        }

        return true
    }

    override fun onBackPressed() {
        fragment.saveSettings()

        val intent = Intent(this, MessengerActivity::class.java)
        intent.putExtra(MessengerActivityExtras.EXTRA_CONVERSATION_ID,
                getIntent().getLongExtra(EXTRA_CONVERSATION_ID, -1))
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)

        super.onBackPressed()
    }

    companion object {
        val EXTRA_CONVERSATION_ID = "conversation_id"
    }
}
