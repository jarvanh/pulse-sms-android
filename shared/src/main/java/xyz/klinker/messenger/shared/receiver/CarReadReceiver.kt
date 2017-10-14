package xyz.klinker.messenger.shared.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

import xyz.klinker.messenger.shared.service.NotificationMarkReadService

class CarReadReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        NotificationMarkReadService.handle(intent, context)
    }
}
