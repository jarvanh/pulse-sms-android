package xyz.klinker.messenger.fragment.settings

import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import xyz.klinker.messenger.R
import xyz.klinker.messenger.api.implementation.Account
import xyz.klinker.messenger.api.implementation.ApiUtils
import xyz.klinker.messenger.shared.data.FeatureFlags
import xyz.klinker.messenger.shared.data.Settings
import xyz.klinker.messenger.shared.service.QuickTextNotificationService

class FeatureSettingsFragment : MaterialPreferenceFragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        addPreferencesFromResource(R.xml.settings_features)
        initSecurePrivateConversations()
        initQuickCompose()
        initDelayedSending()
        initCleanupOldMessages()
        initSignature()
    }

    override fun onStop() {
        super.onStop()
        Settings.forceUpdate(activity)
    }

    private fun initSecurePrivateConversations() {
        val preference = findPreference(getString(R.string.pref_secure_private_conversations))
        preference.setOnPreferenceChangeListener { _, o ->
            val secure = o as Boolean
            ApiUtils.updateSecurePrivateConversations(
                    Account.accountId, secure)
            true
        }

        if (!FeatureFlags.SECURE_PRIVATE) {
            preferenceScreen.removePreference(preference)
        }
    }

    private fun initQuickCompose() {
        val preference = findPreference(getString(R.string.pref_quick_compose))
        preference.setOnPreferenceChangeListener { _, o ->
            val quickCompose = o as Boolean
            ApiUtils.updateQuickCompose(Account.accountId, quickCompose)

            if (quickCompose) {
                QuickTextNotificationService.start(activity)
            } else {
                QuickTextNotificationService.stop(activity)
            }

            true
        }

        if (!FeatureFlags.QUICK_COMPOSE) {
            preferenceScreen.removePreference(preference)
        }
    }

    private fun initDelayedSending() {
        val preference = findPreference(getString(R.string.pref_delayed_sending))
        preference.setOnPreferenceChangeListener { _, o ->
            val delayedSending = o as String
            ApiUtils.updateDelayedSending(
                    Account.accountId, delayedSending)
            true
        }
    }

    private fun initCleanupOldMessages() {
        val preference = findPreference(getString(R.string.pref_cleanup_messages))
        preference.setOnPreferenceChangeListener { _, o ->
            val cleanup = o as String
            ApiUtils.updateCleanupOldMessages(
                    Account.accountId, cleanup)
            true
        }
    }

    private fun initSignature() {
        findPreference(getString(R.string.pref_signature)).setOnPreferenceClickListener {
            val layout = LayoutInflater.from(activity).inflate(R.layout.dialog_edit_text, null, false)
            val editText = layout.findViewById<View>(R.id.edit_text) as EditText
            editText.setHint(R.string.signature)
            editText.setText(Settings.signature)
            editText.setSelection(editText.text.length)

            AlertDialog.Builder(activity)
                    .setView(layout)
                    .setPositiveButton(R.string.save) { _, _ ->
                        val signature = editText.text.toString()
                        Settings.setValue(activity,
                                activity.getString(R.string.pref_signature), signature)
                        if (editText.text.isNotEmpty()) {
                            ApiUtils.updateSignature(Account.accountId,
                                    signature)
                        } else {
                            ApiUtils.updateSignature(Account.accountId, "")
                        }
                    }
                    .setNegativeButton(android.R.string.cancel, null)
                    .show()

            false
        }
    }
}
