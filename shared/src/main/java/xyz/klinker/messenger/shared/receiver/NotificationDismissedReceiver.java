package xyz.klinker.messenger.shared.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import xyz.klinker.messenger.shared.service.NotificationDismissedService;

public class NotificationDismissedReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        long conversationId = intent.getLongExtra(NotificationDismissedService.EXTRA_CONVERSATION_ID, 0L);

        Intent serviceIntent = new Intent(context, NotificationDismissedService.class);
        serviceIntent.putExtra(NotificationDismissedService.EXTRA_CONVERSATION_ID, conversationId);

        context.startService(serviceIntent);
    }
}
