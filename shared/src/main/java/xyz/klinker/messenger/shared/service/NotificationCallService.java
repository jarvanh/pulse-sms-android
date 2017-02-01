package xyz.klinker.messenger.shared.service;

import android.content.Intent;
import android.net.Uri;

public class NotificationCallService extends NotificationMarkReadService {

    public static final String EXTRA_PHONE_NUMBER = "extra_phone_number";

    @Override
    protected void onHandleIntent(Intent intent) {
        // mark as read functionality
        super.onHandleIntent(intent);

        String phoneNumber = intent.getStringExtra(EXTRA_PHONE_NUMBER);

        if (phoneNumber != null) {
            Intent call = new Intent(Intent.ACTION_DIAL);
            call.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            call.setData(Uri.parse("tel:" + phoneNumber));

            startActivity(call);
        }
    }
}
