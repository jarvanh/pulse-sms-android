package xyz.klinker.messenger.service;

import android.app.IntentService;
import android.content.Intent;

import xyz.klinker.messenger.api.implementation.firebase.TokenUtil;

public class ForceTokenRefreshService extends IntentService {


    public ForceTokenRefreshService() {
        super("FCMTokenService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        TokenUtil.refreshToken(this);
    }
}
