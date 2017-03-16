package xyz.klinker.messenger.api.implementation.firebase;

import android.content.Context;
import android.util.Log;

import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.messaging.FirebaseMessaging;

import xyz.klinker.messenger.api.implementation.Account;
import xyz.klinker.messenger.api.implementation.ApiUtils;

public class TokenUtil {
    private static final String TAG = "FCMTokenRefresh";

    public static void refreshToken(Context context) {
        Log.v(TAG, "starting refresh");

        final Account account = Account.get(context);
        final String accountId = account.accountId;
        final String token = FirebaseInstanceId.getInstance().getToken();

        Log.v(TAG, "token: " + token);
        if (account.exists() && token != null) {
            Log.v(TAG, "refreshing on server");
            new Thread(() -> new ApiUtils().updateDevice(accountId, Long.parseLong(account.deviceId),
                    null, token)).start();
        }

        FirebaseMessaging.getInstance().subscribeToTopic("feature_flag");
    }
}
