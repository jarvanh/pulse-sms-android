package xyz.klinker.messenger.shared.util;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import xyz.klinker.messenger.shared.MessengerActivityExtras;

public class RedirectToMyAccount extends AppCompatActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        overridePendingTransition(0,0);

        Intent messengerActivity = ActivityUtils.buildForComponent(ActivityUtils.MESSENGER_ACTIVITY);
        messengerActivity.putExtra(MessengerActivityExtras.INSTANCE.getEXTRA_START_MY_ACCOUNT(), true);
        startActivity(messengerActivity);

        overridePendingTransition(0,0);
        finish();
    }
}
