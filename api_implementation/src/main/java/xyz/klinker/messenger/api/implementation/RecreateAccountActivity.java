package xyz.klinker.messenger.api.implementation;

import android.content.ComponentName;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;

import java.util.Date;

public class RecreateAccountActivity extends LoginActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Account account = Account.INSTANCE;
        account.clearAccount(this);

        signup();
    }

    @Override
    protected void close() {
        super.close();

        Account.INSTANCE.updateSubscription(this, Account.SubscriptionType.SUBSCRIBER, new Date().getTime(), true);

        final Intent uploadService = new Intent();
        uploadService.setComponent(new ComponentName("xyz.klinker.messenger",
                "xyz.klinker.messenger.shared" + ".service.ApiUploadService"));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(uploadService);
        } else {
            startService(uploadService);
        }
    }
}
