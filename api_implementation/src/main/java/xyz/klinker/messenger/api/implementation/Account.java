package xyz.klinker.messenger.api.implementation;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.VisibleForTesting;
import android.util.Base64;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import xyz.klinker.messenger.encryption.EncryptionUtils;
import xyz.klinker.messenger.encryption.KeyUtils;

public class Account {

    private static volatile Account account;

    public enum SubscriptionType {
        TRIAL(1), SUBSCRIBER(2), LIFETIME(3);

        public static SubscriptionType findByTypeCode(int code) {
            for (SubscriptionType type : values()) {
                if (type.typeCode == code) {
                    return type;
                }
            }

            return null;
        }

        public int typeCode;
        SubscriptionType(int typeCode) {
            this.typeCode = typeCode;
        }
    }

    private EncryptionUtils encryptionUtils;

    public boolean primary;
    public SubscriptionType subscriptionType;
    public long subscriptionExpiration;
    public String myName;
    public String myPhoneNumber;
    public String deviceId;
    public String accountId;
    public String salt;
    public String passhash;
    public String key;

    /**
     * Gets a new instance (singleton) of Account.
     *
     * @param context the current application context.
     * @return the account instance.
     */
    public static synchronized Account get(Context context) {
        if (account == null) {
            account = new Account(context);
        }

        return account;
    }

    private Account(final Context context) {
        init(context);
    }

    @VisibleForTesting
    protected void init(Context context) {
        SharedPreferences sharedPrefs = getSharedPrefs(context);

        // account info
        this.primary = sharedPrefs.getBoolean(context.getString(R.string.api_pref_primary), false);
        this.subscriptionType = SubscriptionType.findByTypeCode(sharedPrefs.getInt(context.getString(R.string.api_pref_subscription_type), 1));
        this.subscriptionExpiration = sharedPrefs.getLong(context.getString(R.string.api_pref_subscription_expiration), -1);
        this.myName = sharedPrefs.getString(context.getString(R.string.api_pref_my_name), null);
        this.myPhoneNumber = sharedPrefs.getString(context.getString(R.string.api_pref_my_phone_number), null);
        this.deviceId = sharedPrefs.getString(context.getString(R.string.api_pref_device_id), null);
        this.accountId = sharedPrefs.getString(context.getString(R.string.api_pref_account_id), null);
        this.salt = sharedPrefs.getString(context.getString(R.string.api_pref_salt), null);
        this.passhash = sharedPrefs.getString(context.getString(R.string.api_pref_passhash), null);
        this.key = sharedPrefs.getString(context.getString(R.string.api_pref_key), null);

        if (key == null && passhash != null && accountId != null && salt != null) {
            // we have all the requirements to recompute the key,
            // not sure why this wouldn't have worked in the first place..
            recomputeKey(context);
            this.key = sharedPrefs.getString(context.getString(R.string.api_pref_key), null);

            SecretKey secretKey = new SecretKeySpec(Base64.decode(key, Base64.DEFAULT), "AES");
            encryptionUtils = new EncryptionUtils(secretKey);
        } else if (key == null && accountId != null) {
            // we cannot compute the key, uh oh. lets just start up the login activity and grab them...
            // This will do little good if they are on the api utils and trying to send a message or
            // something, or receiving a message. But they will have to re-login sometime I guess
            context.startActivity(new Intent(context, LoginActivity.class));
        } else if (key != null) {
            SecretKey secretKey = new SecretKeySpec(Base64.decode(key, Base64.DEFAULT), "AES");
            encryptionUtils = new EncryptionUtils(secretKey);
        }
    }

    @VisibleForTesting
    protected SharedPreferences getSharedPrefs(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
    }

    public Account forceUpdate(Context context) {
        account = new Account(context);
        return account;
    }

    public EncryptionUtils getEncryptor() {
        return encryptionUtils;
    }

    public void clearAccount(Context context) {
        getSharedPrefs(context).edit()
                .remove(context.getString(R.string.api_pref_account_id))
                .remove(context.getString(R.string.api_pref_salt))
                .remove(context.getString(R.string.api_pref_passhash))
                .remove(context.getString(R.string.api_pref_key))
                .remove(context.getString(R.string.api_pref_subscription_type))
                .remove(context.getString(R.string.api_pref_subscription_expiration))
                .commit();

        init(context);
    }

    public void updateSubscription(Context context, SubscriptionType type, Date expiration) {
        updateSubscription(context, type, expiration == null ? null : expiration.getTime(), true);
    }

    public void updateSubscription(Context context, SubscriptionType type, Long expiration, boolean sendToApi) {
        this.subscriptionType = type;
        this.subscriptionExpiration = expiration;

        getSharedPrefs(context).edit()
                .putInt(context.getString(R.string.api_pref_subscription_type), type == null ? 0 : type.typeCode)
                .putLong(context.getString(R.string.api_pref_subscription_expiration), expiration == null ? 0 : expiration)
                .commit();

        if (sendToApi) {
            new ApiUtils().updateSubscription(accountId, type == null ? null : type.typeCode, expiration);
        }
    }

    public void setName(Context context, String name) {
        this.myName = name;

        getSharedPrefs(context).edit()
                .putString(context.getString(R.string.api_pref_my_name), name)
                .commit();
    }

    public void setPhoneNumber(Context context, String phoneNumber) {
        this.myPhoneNumber = phoneNumber;

        getSharedPrefs(context).edit()
                .putString(context.getString(R.string.api_pref_my_name), phoneNumber)
                .commit();
    }

    public void setPrimary(Context context, boolean primary) {
        this.primary = primary;

        getSharedPrefs(context).edit()
                .putBoolean(context.getString(R.string.api_pref_primary), primary)
                .commit();
    }

    public void setDeviceId(Context context, String deviceId) {
        this.deviceId = deviceId;

        getSharedPrefs(context).edit()
                .putString(context.getString(R.string.api_pref_device_id), deviceId)
                .commit();
    }

    public void recomputeKey(Context context) {
        KeyUtils keyUtils = new KeyUtils();
        SecretKey key = keyUtils.createKey(passhash, accountId, salt);

        String encodedKey = Base64.encodeToString(key.getEncoded(), Base64.DEFAULT);

        getSharedPrefs(context).edit()
                .putString(context.getString(R.string.api_pref_key), encodedKey)
                .commit();
    }

    public boolean exists() {
        return accountId != null && !accountId.isEmpty() && deviceId != null && salt != null && passhash != null
                && key != null;
    }
}
