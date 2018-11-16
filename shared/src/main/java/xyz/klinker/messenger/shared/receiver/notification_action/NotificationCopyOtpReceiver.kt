package xyz.klinker.messenger.shared.receiver.notification_action

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent

open class NotificationCopyOtpReceiver : xyz.klinker.messenger.shared.receiver.notification_action.NotificationMarkReadReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        super.onReceive(context, intent)
        // mark as read functionality

        if (context == null || intent == null) {
            return
        }

        val otp = intent.getStringExtra(xyz.klinker.messenger.shared.receiver.notification_action.NotificationCopyOtpReceiver.Companion.EXTRA_PASSWORD) ?: return
        val clip = ClipData.newPlainText("one_time_password", otp)
        (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager?)?.primaryClip = clip
    }

    companion object {
        const val EXTRA_PASSWORD = "extra_password"
    }

}
