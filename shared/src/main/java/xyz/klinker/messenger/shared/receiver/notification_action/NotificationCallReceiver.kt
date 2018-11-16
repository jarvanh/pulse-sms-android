package xyz.klinker.messenger.shared.receiver.notification_action

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri

class NotificationCallReceiver : xyz.klinker.messenger.shared.receiver.notification_action.NotificationMarkReadReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        super.onReceive(context, intent)
        // mark as read functionality

        if (context == null || intent == null) {
            return
        }

        val phoneNumber = intent.getStringExtra(xyz.klinker.messenger.shared.receiver.notification_action.NotificationCallReceiver.Companion.EXTRA_PHONE_NUMBER)
        if (phoneNumber != null) {
            val call = Intent(Intent.ACTION_DIAL)
            call.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            call.data = Uri.parse("tel:$phoneNumber")

            try {
                context.startActivity(call)
            } catch (e: ActivityNotFoundException) {
                // no call activity
            }

        }
    }

    companion object {
        const val EXTRA_PHONE_NUMBER = "extra_phone_number"
    }
}
