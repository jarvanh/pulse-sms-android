package xyz.klinker.messenger.shared.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

import xyz.klinker.messenger.shared.service.NotificationDismissedService

class NotificationDismissedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val conversationId = intent.getLongExtra(NotificationDismissedService.EXTRA_CONVERSATION_ID, 0L)

        val serviceIntent = Intent(context, NotificationDismissedService::class.java)
        serviceIntent.putExtra(NotificationDismissedService.EXTRA_CONVERSATION_ID, conversationId)

        Thread { NotificationDismissedService.handle(serviceIntent, context) }.start()
    }
}
