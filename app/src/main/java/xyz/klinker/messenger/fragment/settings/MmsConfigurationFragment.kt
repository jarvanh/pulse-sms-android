package xyz.klinker.messenger.fragment.settings

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.util.Log

import com.codekidlabs.storagechooser.StorageChooser

import xyz.klinker.messenger.R
import xyz.klinker.messenger.api.implementation.Account
import xyz.klinker.messenger.api.implementation.ApiUtils
import xyz.klinker.messenger.shared.data.MmsSettings
import xyz.klinker.messenger.shared.data.Settings

class MmsConfigurationFragment : MaterialPreferenceFragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        addPreferencesFromResource(R.xml.settings_mms)
        initConvertToMMS()
        initMaxImageSize()
        initGroupMMS()
        initAutoSaveMedia()
        initDownloadLocation()

        // MMS APN settings
        initOverrideSystemSettings()
        initMmsc()
        initProxy()
        initPort()
        initUserAgent()
        initUserAgentProfileUrl()
        initUserAgentProfileTagName()
    }

    override fun onStop() {
        super.onStop()
        Settings.forceUpdate(activity)
    }

    private fun initConvertToMMS() {
        findPreference(getString(R.string.pref_convert_to_mms))
                .setOnPreferenceChangeListener { _, o ->
                    val convert = o as String
                    ApiUtils.updateConvertToMMS(Account.accountId,
                            convert)
                    true
                }
    }

    private fun initMaxImageSize() {
        findPreference(getString(R.string.pref_mms_size))
                .setOnPreferenceChangeListener { _, o ->
                    val size = o as String
                    ApiUtils.updateMmsSize(Account.accountId,
                            size)
                    true
                }
    }

    private fun initGroupMMS() {
        findPreference(getString(R.string.pref_group_mms))
                .setOnPreferenceChangeListener { _, o ->
                    val group = o as Boolean
                    ApiUtils.updateGroupMMS(Account.accountId,
                            group)
                    true
                }
    }

    private fun initAutoSaveMedia() {
        findPreference(getString(R.string.pref_auto_save_media))
                .setOnPreferenceChangeListener { _, o ->
                    val save = o as Boolean
                    ApiUtils.updateAutoSaveMedia(Account.accountId,
                            save)
                    true
                }
    }

    private fun initDownloadLocation() {
        findPreference(getString(R.string.pref_mms_save_location))
                .setOnPreferenceClickListener { _ ->

                    if (ContextCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                        showStorageChooser()
                    } else {
                        if (activity != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            activity.requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 10)
                        } else {
                            showStorageChooser()
                        }
                    }

                    false
                }
    }

    private fun showStorageChooser() {
        val chooser = StorageChooser.Builder()
                .withActivity(activity)
                .withFragmentManager((activity as AppCompatActivity).fragmentManager)
                .allowCustomPath(true)
                .setType(StorageChooser.DIRECTORY_CHOOSER)
                .actionSave(true)
                .skipOverview(true)
                .withPreference(MmsSettings.getSharedPrefs(activity))
                .build()

        chooser.show()
        chooser.setOnSelectListener { _ -> }
    }

    private fun initOverrideSystemSettings() {
        findPreference(getString(R.string.pref_override_system_apn))
                .setOnPreferenceChangeListener { _, o ->
                    val override = o as Boolean
                    ApiUtils.updateOverrideSystemApn(Account.accountId,
                            override)

                    if (override) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !android.provider.Settings.System.canWrite(activity)) {
                            AlertDialog.Builder(activity)
                                    .setMessage(com.klinker.android.send_message.R.string.write_settings_permission)
                                    .setPositiveButton(com.klinker.android.send_message.R.string.ok) { _, _ ->
                                        val intent = Intent(android.provider.Settings.ACTION_MANAGE_WRITE_SETTINGS)
                                        intent.data = Uri.parse("package:" + activity.packageName)
                                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

                                        try {
                                            startActivity(intent)
                                        } catch (e: Exception) {
                                            Log.e("MainActivity", "error starting permission intent", e)
                                        }
                                    }
                                    .show()
                        }
                    }

                    true
                }
    }

    private fun initMmsc() {
        findPreference(getString(R.string.pref_mmsc_url)).setOnPreferenceChangeListener { _, o ->
            val mmsc = o as String
            ApiUtils.updateMmscUrl(Account.accountId,
                    mmsc)
            true
        }
    }

    private fun initProxy() {
        findPreference(getString(R.string.pref_mms_proxy)).setOnPreferenceChangeListener { _, o ->
            val proxy = o as String
            ApiUtils.updateMmsProxy(Account.accountId,
                    proxy)
            true
        }
    }

    private fun initPort() {
        findPreference(getString(R.string.pref_mms_port)).setOnPreferenceChangeListener { _, o ->
            val port = o as String
            ApiUtils.updateMmsPort(Account.accountId,
                    port)
            true
        }
    }

    private fun initUserAgent() {
        findPreference(getString(R.string.pref_user_agent)).setOnPreferenceChangeListener { _, o ->
            val userAgent = o as String
            ApiUtils.updateUserAgent(Account.accountId,
                    userAgent)
            true
        }
    }

    private fun initUserAgentProfileUrl() {
        findPreference(getString(R.string.pref_user_agent_profile_url)).setOnPreferenceChangeListener { _, o ->
            val uaProfileUrl = o as String
            ApiUtils.updateUserAgentProfileUrl(Account.accountId,
                    uaProfileUrl)
            true
        }
    }

    private fun initUserAgentProfileTagName() {
        findPreference(getString(R.string.pref_user_agent_profile_tag)).setOnPreferenceChangeListener { _, o ->
            val uaProfileTagName = o as String
            ApiUtils.updateUserAgentProfileTagName(Account.accountId,
                    uaProfileTagName)
            true
        }
    }
}
