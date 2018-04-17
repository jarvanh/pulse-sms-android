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

import android.app.AlertDialog
import android.os.Build
import android.os.Bundle
import android.preference.*
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.MultiAutoCompleteTextView
import com.android.ex.chips.BaseRecipientAdapter
import com.android.ex.chips.RecipientEditTextView
import xyz.klinker.messenger.R
import xyz.klinker.messenger.api.implementation.Account
import xyz.klinker.messenger.api.implementation.ApiUtils
import xyz.klinker.messenger.service.DrivingModeQuickSettingTile
import xyz.klinker.messenger.shared.data.DataSource
import xyz.klinker.messenger.shared.data.Settings
import xyz.klinker.messenger.shared.data.model.AutoReply
import xyz.klinker.messenger.shared.util.AndroidVersionUtil

/**
 * Fragment for modifying contact preferences. This includes pinning, changing colors, changing
 * ringtone, and changing the group name.
 */
class AutoReplySettingsFragment : MaterialPreferenceFragment() {

    private val repliesPrefGroup: PreferenceGroup by lazy { findPreference(getString(R.string.pref_auto_replies_group)) as PreferenceGroup }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        addPreferencesFromResource(R.xml.settings_auto_reply)

        fillAutoRepliesList()
        initDrivingMode()
        initVacationMode()
    }

    private fun fillAutoRepliesList() {
        val createNewReply = Preference(activity)
        createNewReply.setTitle(R.string.create_auto_reply)
        createNewReply.setSummary(R.string.auto_reply_top_pref_summary)
        createNewReply.setOnPreferenceClickListener {
            createNewAutoReply()
            true
        }

        repliesPrefGroup.removeAll()
        repliesPrefGroup.addPreference(createNewReply)

        DataSource.getAutoRepliesAsList(activity)
                .filter { it.type != AutoReply.TYPE_DRIVING && it.type != AutoReply.TYPE_VACATION }
                .forEach {
                    val pref = createPreference(it)
                    repliesPrefGroup.addPreference(pref)
                }
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

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                DrivingModeQuickSettingTile.updateState(activity)
            }

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

        if (response.isEmpty() && databaseReply != null) {
            DataSource.deleteAutoReply(activity, databaseReply.id, true)
        } else if (databaseReply != null) {
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

    private fun createNewAutoReply() {
        AlertDialog.Builder(activity)
                .setMessage(R.string.which_auto_reply_type)
                .setPositiveButton(R.string.keyword) { _, _ -> showKeywordCreator() }
                .setNegativeButton(R.string.contact) { _, _ -> showContactCreator() }
                .show()
    }

    private fun showKeywordCreator() {
        val layout = LayoutInflater.from(activity).inflate(R.layout.dialog_keyword_auto_reply, null, false)
        val patternEditText = layout.findViewById<View>(R.id.keyword) as EditText
        patternEditText.setHint(R.string.keyword)
        val responseEditText = layout.findViewById<View>(R.id.response) as EditText
        responseEditText.setHint(R.string.response)

        AlertDialog.Builder(activity)
                .setView(layout)
                .setPositiveButton(R.string.save) { _, _ ->
                    if (patternEditText.text.isEmpty() || responseEditText.text.isBlank()) {
                        return@setPositiveButton
                    }

                    val keyword = patternEditText.text.toString()
                    val response = responseEditText.text.toString()

                    createAndShowReply(AutoReply.TYPE_KEYWORD, keyword, response)
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
    }

    private fun showContactCreator() {
        val layout = LayoutInflater.from(activity).inflate(R.layout.dialog_contact_auto_reply, null, false)
        val contactEditText = layout.findViewById<View>(R.id.contact) as RecipientEditTextView
        contactEditText.setHint(R.string.contact)
        val responseEditText = layout.findViewById<View>(R.id.response) as EditText
        responseEditText.setHint(R.string.response)

        val adapter = BaseRecipientAdapter(BaseRecipientAdapter.QUERY_TYPE_PHONE, context)
        adapter.isShowMobileOnly = Settings.mobileOnly

        contactEditText.setTokenizer(MultiAutoCompleteTextView.CommaTokenizer())
        contactEditText.highlightColor = Settings.mainColorSet.colorAccent
        contactEditText.setAdapter(adapter)
        contactEditText.maxChips = 1

        AlertDialog.Builder(activity)
                .setView(layout)
                .setPositiveButton(R.string.save) { _, _ ->
                    if (contactEditText.recipients.isEmpty() || responseEditText.text.isBlank()) {
                        return@setPositiveButton
                    }

                    val contact = contactEditText.recipients[0].entry.destination
                    val response = responseEditText.text.toString()

                    createAndShowReply(AutoReply.TYPE_CONTACT, contact, response)
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
    }

    private fun createAndShowReply(type: String, pattern: String, response: String) {
        val reply = AutoReply()
        reply.id = DataSource.generateId()
        reply.pattern = pattern
        reply.response = response
        reply.type = type

        DataSource.insertAutoReply(activity, reply, true)

        val pref = createPreference(reply)
        repliesPrefGroup.addPreference(pref)
    }

    private fun createPreference(reply: AutoReply): Preference {
        val pref = Preference(activity)
        pref.title = reply.response
        pref.summary = "${getStringFromAutoReplyType(reply)}: ${reply.pattern}"

        pref.setOnPreferenceClickListener {
            AlertDialog.Builder(activity)
                    .setMessage(R.string.delete_auto_reply)
                    .setPositiveButton(R.string.api_yes) { _, _ ->
                        DataSource.deleteAutoReply(activity, reply.id, true)
                        fillAutoRepliesList()
                    }.setNegativeButton(R.string.no) { _, _ -> }
                    .show()
            true
        }

        return pref
    }

    private fun getStringFromAutoReplyType(reply: AutoReply): String {
        return when (reply.type) {
            AutoReply.TYPE_KEYWORD -> getString(R.string.keyword)
            AutoReply.TYPE_CONTACT -> getString(R.string.contact)
            AutoReply.TYPE_VACATION -> getString(R.string.vacation_mode)
            AutoReply.TYPE_DRIVING -> getString(R.string.driving_mode)
            else -> throw IllegalArgumentException("cannot get string for type: ${reply.type}")
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
