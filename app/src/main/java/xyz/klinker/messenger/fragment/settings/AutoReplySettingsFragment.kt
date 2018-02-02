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

package xyz.klinker.messenger.fragment.settings

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.preference.*
import android.support.annotation.RequiresApi
import android.support.v4.app.FragmentActivity
import android.text.InputType
import android.util.Log
import xyz.klinker.messenger.R
import xyz.klinker.messenger.activity.compose.ComposeActivity
import xyz.klinker.messenger.activity.compose.ComposeConstants
import xyz.klinker.messenger.api.implementation.Account
import xyz.klinker.messenger.api.implementation.ApiUtils
import xyz.klinker.messenger.shared.activity.AbstractSettingsActivity
import xyz.klinker.messenger.shared.data.ColorSet
import xyz.klinker.messenger.shared.data.DataSource
import xyz.klinker.messenger.shared.data.FeatureFlags
import xyz.klinker.messenger.shared.data.Settings
import xyz.klinker.messenger.shared.data.model.AutoReply
import xyz.klinker.messenger.shared.data.model.Conversation
import xyz.klinker.messenger.shared.util.*
import xyz.klinker.messenger.shared.util.listener.ColorSelectedListener
import xyz.klinker.messenger.view.ColorPreference

/**
 * Fragment for modifying contact preferences. This includes pinning, changing colors, changing
 * ringtone, and changing the group name.
 */
class AutoReplySettingsFragment : MaterialPreferenceFragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        addPreferencesFromResource(R.xml.settings_auto_reply)

        fillAutoReplies()

        initAddNewReply()
        initDrivingMode()
        initVacationMode()
    }

    private fun fillAutoReplies() {

    }

    private fun initAddNewReply() {

    }

    private fun initDrivingMode() {
        val toggle = findPreference(getString(R.string.pref_driving_mode))
        toggle.setOnPreferenceChangeListener { _, o ->
            val enabled = o as Boolean
            ApiUtils.enableDrivingMode(Account.accountId, enabled)
            true
        }

        val responseEntry = findPreference(getString(R.string.pref_driving_mode_editable))
        responseEntry.setOnPreferenceChangeListener { _, o ->
            val response = o as String

            ApiUtils.updateDrivingModeText(Account.accountId, response)
            updateDatabaseReply(AutoReply.TYPE_DRIVING, response)

            true
        }
    }

    private fun initVacationMode() {
        val toggle = findPreference(getString(R.string.pref_vacation_mode))
        toggle.setOnPreferenceChangeListener { _, o ->
            val enabled = o as Boolean
            ApiUtils.enableVacationMode(Account.accountId, enabled)
            true
        }

        val responseEntry = findPreference(getString(R.string.pref_vacation_mode_editable))
        responseEntry.setOnPreferenceChangeListener { _, o ->
            val response = o as String

            ApiUtils.updateVacationModeText(Account.accountId, response)
            updateDatabaseReply(AutoReply.TYPE_VACATION, response)

            true
        }
    }

    private fun updateDatabaseReply(type: String, response: String) {
        val databaseReply = DataSource.getAutoRepliesAsList(activity).firstOrNull { it.type == type }

        if (databaseReply != null) {
            databaseReply.response = response
            DataSource.updateAutoReply(activity, databaseReply, true)
        } else {
            val reply = AutoReply()
            reply.pattern = ""
            reply.response = response
            reply.type = type
            DataSource.insertAutoReply(activity, reply, true)
        }
    }

    companion object {
        fun newInstance(): AutoReplySettingsFragment {
            val fragment = AutoReplySettingsFragment()
            val args = Bundle()
            fragment.arguments = args

            return fragment
        }
    }
}
