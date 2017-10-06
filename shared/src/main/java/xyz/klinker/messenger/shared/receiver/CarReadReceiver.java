package xyz.klinker.messenger.shared.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import xyz.klinker.messenger.shared.service.NotificationMarkReadService;

public class CarReadReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        NotificationMarkReadService.handle(intent, context);
    }
}
