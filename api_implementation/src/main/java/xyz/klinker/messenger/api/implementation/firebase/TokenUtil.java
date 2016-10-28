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

        Account account = Account.get(context);
        String accountId = account.accountId;
        String token = FirebaseInstanceId.getInstance().getToken();

        Log.v(TAG, "token: " + token);
        if (accountId != null && account.deviceId != null && token != null) {
            Log.v(TAG, "refreshing on server");
            new ApiUtils().updateDevice(accountId, Long.parseLong(account.deviceId),
                    null, token);
        }

        FirebaseMessaging.getInstance().subscribeToTopic("feature_flag");
    }
}
