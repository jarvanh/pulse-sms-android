package xyz.klinker.messenger.api.implementation;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Base64;

import javax.crypto.SecretKey;

import xyz.klinker.messenger.api.entity.LoginResponse;
import xyz.klinker.messenger.encryption.EncryptionUtils;
import xyz.klinker.messenger.encryption.KeyUtils;

public class AccountEncryptionCreator {

    private Context context;
    private SharedPreferences sharedPrefs;
    private String password;

    public AccountEncryptionCreator(Context context, String password) {
        this.context = context;
        this.password = password;

        sharedPrefs = PreferenceManager
                .getDefaultSharedPreferences(context.getApplicationContext());
    }

    public EncryptionUtils createAccountEncryption(LoginResponse loginResponse) {
        KeyUtils keyUtils = new KeyUtils();
        String hash = keyUtils.hashPassword(password, loginResponse.salt2);
        SecretKey key = keyUtils.createKey(hash, loginResponse.accountId, loginResponse.salt1);

        sharedPrefs.edit()
                .putString("my_name", loginResponse.name)
                .putString("my_phone_number", loginResponse.phoneNumber)
                .putString("account_id", loginResponse.accountId)
                .putString("salt", loginResponse.salt1)
                .putString("passhash", hash)
                .putString("key", Base64.encodeToString(key.getEncoded(), Base64.DEFAULT))
                .commit();

        return Account.get(context).getEncryptor();
    }
}
