package xyz.klinker.messenger.util;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import xyz.klinker.messenger.activity.MessengerActivity;

public class RedirectToMyAccount extends AppCompatActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        overridePendingTransition(0,0);

        Intent messengerActivity = new Intent(this, MessengerActivity.class);
        messengerActivity.putExtra(MessengerActivity.EXTRA_START_MY_ACCOUNT, true);
        startActivity(messengerActivity);

        overridePendingTransition(0,0);
        finish();
    }
}
