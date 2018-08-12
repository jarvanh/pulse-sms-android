package xyz.klinker.messenger.api.implementation

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.preference.PreferenceManager
import android.util.Base64

import java.util.Date

import javax.crypto.spec.SecretKeySpec

import xyz.klinker.messenger.encryption.EncryptionUtils
import xyz.klinker.messenger.encryption.KeyUtils

@SuppressLint("ApplySharedPref")
object Account {

    enum class SubscriptionType constructor(var typeCode: Int) {
        TRIAL(1), SUBSCRIBER(2), LIFETIME(3), FREE_TRIAL(4), FINISHED_FREE_TRIAL_WITH_NO_ACCOUNT_SETUP(5);

        companion object {
            fun findByTypeCode(code: Int): SubscriptionType? {
                return values().firstOrNull { it.typeCode == code }
            }
        }
    }

    var encryptor: EncryptionUtils? = null
        private set

    var primary: Boolean = false
    var trialStartTime: Long = 0
    var subscriptionType: SubscriptionType? = null
    var subscriptionExpiration: Long = 0
    var myName: String? = null
    var myPhoneNumber: String? = null
    var deviceId: String? = null
    var accountId: String? = null
    var salt: String? = null
    var passhash: String? = null
    var key: String? = null

    var hasPurchased: Boolean = false

    fun init(context: Context) {
        val sharedPrefs = getSharedPrefs(context)

        // account info
        this.primary = sharedPrefs.getBoolean(context.getString(R.string.api_pref_primary), false)
        this.subscriptionType = SubscriptionType.findByTypeCode(sharedPrefs.getInt(context.getString(R.string.api_pref_subscription_type), 1))
        this.subscriptionExpiration = sharedPrefs.getLong(context.getString(R.string.api_pref_subscription_expiration), -1)
        this.trialStartTime = sharedPrefs.getLong(context.getString(R.string.api_pref_trial_start), -1)
        this.myName = sharedPrefs.getString(context.getString(R.string.api_pref_my_name), null)
        this.myPhoneNumber = sharedPrefs.getString(context.getString(R.string.api_pref_my_phone_number), null)
        this.deviceId = sharedPrefs.getString(context.getString(R.string.api_pref_device_id), null)
        this.accountId = sharedPrefs.getString(context.getString(R.string.api_pref_account_id), null)
        this.salt = sharedPrefs.getString(context.getString(R.string.api_pref_salt), null)
        this.passhash = sharedPrefs.getString(context.getString(R.string.api_pref_passhash), null)
        this.key = sharedPrefs.getString(context.getString(R.string.api_pref_key), null)

        this.hasPurchased = sharedPrefs.getBoolean(context.getString(R.string.api_pref_has_purchased), false)

        if (key == null && passhash != null && accountId != null && salt != null) {
            // we have all the requirements to recompute the key,
            // not sure why this wouldn't have worked in the first place..
            recomputeKey(context)
            this.key = sharedPrefs.getString(context.getString(R.string.api_pref_key), null)

            val secretKey = SecretKeySpec(Base64.decode(key, Base64.DEFAULT), "AES")
            encryptor = EncryptionUtils(secretKey)
        } else if (key == null && accountId != null) {
            // we cannot compute the key, uh oh. lets just start up the login activity and grab them...
            // This will do little good if they are on the api utils and trying to send a message or
            // something, or receiving a message. But they will have to re-login sometime I guess
            context.startActivity(Intent(context, LoginActivity::class.java))
        } else if (key != null) {
            val secretKey = SecretKeySpec(Base64.decode(key, Base64.DEFAULT), "AES")
            encryptor = EncryptionUtils(secretKey)
        }

        val application = context.applicationContext
        if (application is AccountInvalidator) {
            application.onAccountInvalidated(this)
        }
    }

    fun getSharedPrefs(context: Context): SharedPreferences {
        return PreferenceManager.getDefaultSharedPreferences(context.applicationContext)
    }

    fun forceUpdate(context: Context): Account {
        init(context)
        return this
    }

    fun clearAccount(context: Context) {
        getSharedPrefs(context).edit()
                .remove(context.getString(R.string.api_pref_account_id))
                .remove(context.getString(R.string.api_pref_salt))
                .remove(context.getString(R.string.api_pref_passhash))
                .remove(context.getString(R.string.api_pref_key))
                .remove(context.getString(R.string.api_pref_subscription_type))
                .remove(context.getString(R.string.api_pref_subscription_expiration))
                .commit()

        init(context)
    }

    fun updateSubscription(context: Context, type: SubscriptionType, expiration: Date?) {
        updateSubscription(context, type, expiration?.time, true)
    }

    fun updateSubscription(context: Context, type: SubscriptionType?, expiration: Long?, sendToApi: Boolean) {
        this.subscriptionType = type
        this.subscriptionExpiration = expiration!!

        getSharedPrefs(context).edit()
                .putInt(context.getString(R.string.api_pref_subscription_type), type?.typeCode ?: 0)
                .putLong(context.getString(R.string.api_pref_subscription_expiration), expiration)
                .commit()

        if (sendToApi) {
            ApiUtils.updateSubscription(accountId, type?.typeCode, expiration)
        }
    }

    fun setName(context: Context, name: String?) {
        this.myName = name

        getSharedPrefs(context).edit()
                .putString(context.getString(R.string.api_pref_my_name), name)
                .commit()
    }

    fun setPhoneNumber(context: Context, phoneNumber: String?) {
        this.myPhoneNumber = phoneNumber

        getSharedPrefs(context).edit()
                .putString(context.getString(R.string.api_pref_my_name), phoneNumber)
                .commit()
    }

    fun setPrimary(context: Context, primary: Boolean) {
        this.primary = primary

        getSharedPrefs(context).edit()
                .putBoolean(context.getString(R.string.api_pref_primary), primary)
                .commit()
    }

    fun setDeviceId(context: Context, deviceId: String?) {
        this.deviceId = deviceId

        getSharedPrefs(context).edit()
                .putString(context.getString(R.string.api_pref_device_id), deviceId)
                .commit()
    }

    fun setHasPurchased(context: Context, hasPurchased: Boolean) {
        this.hasPurchased = hasPurchased

        getSharedPrefs(context).edit()
                .putBoolean(context.getString(R.string.api_pref_has_purchased), hasPurchased)
                .commit()
    }

    fun recomputeKey(context: Context) {
        val keyUtils = KeyUtils()
        val key = keyUtils.createKey(passhash, accountId, salt)

        val encodedKey = Base64.encodeToString(key.encoded, Base64.DEFAULT)

        getSharedPrefs(context).edit()
                .putString(context.getString(R.string.api_pref_key), encodedKey)
                .commit()
    }

    fun exists(): Boolean {
        return accountId != null && !accountId!!.isEmpty() && deviceId != null && salt != null && passhash != null
                && key != null
    }

    fun getDaysLeftInTrial(): Int {
        return if (subscriptionType == SubscriptionType.FREE_TRIAL) {
            val timeInTrial = Date().time - trialStartTime
            val timeLeftInTrial = (1000 * 60 * 60 * 24 * 7) - timeInTrial
            val timeInDays = (timeLeftInTrial / (1000 * 60 * 60 * 24)) + 1
            timeInDays.toInt()
        } else {
            0
        }
    }
}
