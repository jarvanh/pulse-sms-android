package xyz.klinker.messenger.api.implementation;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.VisibleForTesting;
import android.util.Base64;

import java.nio.charset.StandardCharsets;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import xyz.klinker.messenger.encryption.EncryptionUtils;
import xyz.klinker.messenger.encryption.KeyUtils;

public class Account {

    private static volatile Account account;

    private Context context;
    private EncryptionUtils encryptionUtils;

    public boolean primary;
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
        this.context = context;
        SharedPreferences sharedPrefs = getSharedPrefs();

        // account info
        this.primary = sharedPrefs.getBoolean(context.getString(R.string.api_pref_primary), false);
        this.myName = sharedPrefs.getString(context.getString(R.string.api_pref_my_name), null);
        this.myPhoneNumber = sharedPrefs.getString(context.getString(R.string.api_pref_my_phone_number), null);
        this.deviceId = sharedPrefs.getString(context.getString(R.string.api_pref_device_id), null);
        this.accountId = sharedPrefs.getString(context.getString(R.string.api_pref_account_id), null);
        this.salt = sharedPrefs.getString(context.getString(R.string.api_pref_salt), null);
        this.passhash = sharedPrefs.getString(context.getString(R.string.api_pref_passhash), null);
        this.key = sharedPrefs.getString(context.getString(R.string.api_pref_key),
                Base64.encodeToString("no key yet.".getBytes(StandardCharsets.UTF_8), Base64.DEFAULT));

        SecretKey secretKey = new SecretKeySpec(Base64.decode(key, Base64.DEFAULT), "AES");
        encryptionUtils = new EncryptionUtils(secretKey);
    }

    @VisibleForTesting
    protected SharedPreferences getSharedPrefs() {
        return PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
    }

    public void forceUpdate(Context context) {
        account = null;
        account = new Account(context);
    }

    public EncryptionUtils getEncryptor() {
        return encryptionUtils;
    }

    public void clearAccount() {
        getSharedPrefs().edit()
                .remove(context.getString(R.string.api_pref_account_id))
                .remove(context.getString(R.string.api_pref_salt))
                .remove(context.getString(R.string.api_pref_passhash))
                .remove(context.getString(R.string.api_pref_key))
                .commit();

        init(context);
    }

    public void setName(String name) {
        this.myName = name;

        getSharedPrefs().edit()
                .putString(context.getString(R.string.api_pref_my_name), name)
                .commit();
    }

    public void setPhoneNumber(String phoneNumber) {
        this.myPhoneNumber = phoneNumber;

        getSharedPrefs().edit()
                .putString(context.getString(R.string.api_pref_my_name), phoneNumber)
                .commit();
    }

    public void setPrimary(boolean primary) {
        this.primary = primary;

        getSharedPrefs().edit()
                .putBoolean(context.getString(R.string.api_pref_primary), primary)
                .commit();
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;

        getSharedPrefs().edit()
                .putString(context.getString(R.string.api_pref_device_id), deviceId)
                .commit();
    }

    public void recomputeKey() {
        KeyUtils keyUtils = new KeyUtils();
        SecretKey key = keyUtils.createKey(passhash, accountId, salt);

        String encodedKey = Base64.encodeToString(key.getEncoded(), Base64.DEFAULT);

        getSharedPrefs().edit()
                .putString(context.getString(R.string.api_pref_key), encodedKey)
                .commit();
    }
}