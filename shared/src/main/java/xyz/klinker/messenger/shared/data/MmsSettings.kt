package xyz.klinker.messenger.shared.data

import android.content.Context
import android.content.SharedPreferences
import android.os.Environment
import android.preference.PreferenceManager

import com.codekidlabs.storagechooser.utils.DiskUtil

import java.io.File

import xyz.klinker.messenger.shared.R

object MmsSettings {

    var convertLongMessagesToMMS: Boolean = false
    var groupMMS: Boolean = false
    var readReceipts: Boolean = false
    var autoSaveMedia: Boolean = false
    var overrideSystemAPN: Boolean = false
    var saveDirectory: String? = null

    var mmscUrl: String? = null
    var mmsProxy: String? = null
    var mmsPort: String? = null
    var userAgent: String? = null
    var userAgentProfileUrl: String? = null
    var userAgentProfileTagName: String? = null

    var numberOfMessagesBeforeMms: Int = 0
    var maxImageSize: Long = 0

    fun init(context: Context) {
        val sharedPrefs = getSharedPrefs(context)

        this.groupMMS = sharedPrefs.getBoolean(context.getString(R.string.pref_group_mms), true)
        this.readReceipts = sharedPrefs.getBoolean(context.getString(R.string.pref_mms_read_receipts), false)
        this.autoSaveMedia = sharedPrefs.getBoolean(context.getString(R.string.pref_auto_save_media), false)
        this.overrideSystemAPN = sharedPrefs.getBoolean(context.getString(R.string.pref_override_system_apn), false)
        this.saveDirectory = sharedPrefs.getString(DiskUtil.SC_PREFERENCE_KEY,
                File(Environment.getExternalStorageDirectory(), "Pictures/Pulse").path)

        this.mmscUrl = sharedPrefs.getString(context.getString(R.string.pref_mmsc_url), "")
        this.mmsProxy = sharedPrefs.getString(context.getString(R.string.pref_mms_proxy), "")
        this.mmsPort = sharedPrefs.getString(context.getString(R.string.pref_mms_port), "")
        this.userAgent = sharedPrefs.getString(context.getString(R.string.pref_user_agent), "Android-Mms/2.0")
        this.userAgentProfileUrl = sharedPrefs.getString(context.getString(R.string.pref_user_agent_profile_url), "")
        this.userAgentProfileTagName = sharedPrefs.getString(context.getString(R.string.pref_user_agent_profile_tag), "x-wap-profile")

        when (sharedPrefs.getString(context.getString(R.string.pref_mms_size), "500_kb")) {
            "100_kb" -> this.maxImageSize = (100 * 1024).toLong()
            "300_kb" -> this.maxImageSize = (300 * 1024).toLong()
            "500_kb" -> this.maxImageSize = (500 * 1024).toLong()
            "700_kb" -> this.maxImageSize = (700 * 1024).toLong()
            "1_mb" -> this.maxImageSize = (900 * 1024).toLong()
            "2_mb" -> this.maxImageSize = (2000 * 1024).toLong()
            "3_mb" -> this.maxImageSize = (3000 * 1024).toLong()
            "5_mb" -> this.maxImageSize = (5000 * 1024).toLong()
            "10_mb" -> this.maxImageSize = (10000 * 1024).toLong()
            else -> this.maxImageSize = (500 * 1024).toLong()
        }

        val convertToMmsAfterXMessages = sharedPrefs.getString(context.getString(R.string.pref_convert_to_mms), "3")
        if (convertToMmsAfterXMessages == "0") {
            this.convertLongMessagesToMMS = false
            this.numberOfMessagesBeforeMms = -1
        } else {
            this.convertLongMessagesToMMS = true
            this.numberOfMessagesBeforeMms = Integer.parseInt(convertToMmsAfterXMessages!!)
        }
    }

    /**
     * Forces a reload of all MMS Settings data.
     */
    fun forceUpdate(context: Context) {
        init(context)
    }

    fun getSharedPrefs(context: Context): SharedPreferences {
        return PreferenceManager.getDefaultSharedPreferences(context)
    }
}
