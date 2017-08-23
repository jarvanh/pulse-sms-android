package xyz.klinker.messenger.api.implementation;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.VisibleForTesting;
import android.util.Base64;

import java.util.Date;

import javax.crypto.SecretKey;

import xyz.klinker.messenger.api.entity.LoginResponse;
import xyz.klinker.messenger.api.entity.SignupResponse;
import xyz.klinker.messenger.encryption.EncryptionUtils;
import xyz.klinker.messenger.encryption.KeyUtils;

public class AccountEncryptionCreator {

    private Context context;
    private SharedPreferences sharedPrefs;
    private String password;

    public AccountEncryptionCreator(Context context, String password) {
        this.context = context;
        this.password = password;

        sharedPrefs = getSharedPrefs(context);
    }

    @VisibleForTesting
    protected SharedPreferences getSharedPrefs(Context context) {
        return PreferenceManager
                .getDefaultSharedPreferences(context.getApplicationContext());
    }

    public EncryptionUtils createAccountEncryptionFromLogin(LoginResponse loginResponse) {
        getSharedPrefs(context).edit()
                .putInt("global_primary_color", loginResponse.color)
                .putInt("global_primary_dark_color", loginResponse.colorDark)
                .putInt("global_primary_light_color", loginResponse.colorLight)
                .putInt("global_accent_color", loginResponse.colorAccent)
                .putBoolean("apply_theme_globally", loginResponse.useGlobalTheme)
                .putBoolean("rounder_bubbles", loginResponse.rounderBubbles)
                .putString("base_theme", loginResponse.baseTheme)
                .apply();

        return createEncryptorParams(loginResponse.name, loginResponse.phoneNumber,
                loginResponse.accountId, loginResponse.salt1, loginResponse.salt2);
    }

    public EncryptionUtils createAccountEncryptionFromSignup(String name, String phone, SignupResponse signupResponse) {
        return createEncryptorParams(name, phone,
                signupResponse.accountId, signupResponse.salt1, signupResponse.salt2);
    }

    private EncryptionUtils createEncryptorParams(String name, String phone, String accountId,
                                                   String salt1, String salt2) {
        KeyUtils keyUtils = new KeyUtils();
        String hash = keyUtils.hashPassword(password, salt2);
        SecretKey key = keyUtils.createKey(hash, accountId, salt1);

        return createAccount(name, phone, accountId, salt1, hash, key);
    }

    private EncryptionUtils createAccount(String name, String phone, String accountId, String salt1,
                                            String passhash, SecretKey key) {
        sharedPrefs.edit()
                .putInt(context.getString(R.string.api_pref_subscription_type), Account.get(context).subscriptionType == Account.SubscriptionType.LIFETIME ?
                        Account.SubscriptionType.LIFETIME.typeCode : Account.SubscriptionType.TRIAL.typeCode)
                .putLong(context.getString(R.string.api_pref_subscription_expiration), getTrialEnd())
                .putString(context.getString(R.string.api_pref_my_name), name)
                .putString(context.getString(R.string.api_pref_my_phone_number), phone)
                .putString(context.getString(R.string.api_pref_account_id), accountId)
                .putString(context.getString(R.string.api_pref_salt), salt1)
                .putString(context.getString(R.string.api_pref_passhash), passhash)
                .putString(context.getString(R.string.api_pref_key),
                        Base64.encodeToString(key.getEncoded(), Base64.DEFAULT))
                .commit();

        Account account = Account.get(context);
        account = account.forceUpdate(context);
        return account.getEncryptor();
    }

    private long getTrialEnd() {
        // 25 hours per day, just to give them a little extra wiggle room
        long sevenDays = 1000 * 60 * 60 * 25 * 7;
        return new Date().getTime() + sevenDays;
    }
}
