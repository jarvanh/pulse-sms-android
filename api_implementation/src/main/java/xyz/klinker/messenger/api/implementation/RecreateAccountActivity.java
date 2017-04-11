package xyz.klinker.messenger.api.implementation;

import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;

public class RecreateAccountActivity extends LoginActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Account account = Account.get(this);
        account.clearAccount();

        signup();
    }

    @Override
    protected void close() {
        super.close();

        final Intent uploadService = new Intent();
        uploadService.setComponent(new ComponentName("xyz.klinker.messenger",
                "xyz.klinker.messenger.shared" + ".service.ApiUploadService"));
        startService(uploadService);
    }
}
