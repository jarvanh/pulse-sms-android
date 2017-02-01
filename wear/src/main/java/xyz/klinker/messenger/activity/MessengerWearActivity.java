package xyz.klinker.messenger.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import xyz.klinker.messenger.api.implementation.Account;

public class MessengerWearActivity extends Activity {

    private TextView code;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Account account = Account.get(this);
        if (account.accountId == null) {
            startActivity(new Intent(this, InitialLoadWearActivity.class));
            finish();
            return;
        }
    }
}
