package xyz.klinker.messenger.shared.service

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent

open class NotificationCopyOtpService : NotificationMarkReadService() {

    override fun onHandleIntent(intent: Intent?) {
        // mark as read functionality
        super.onHandleIntent(intent)

        val otp = intent?.getStringExtra(EXTRA_PASSWORD) ?: return
        val clip = ClipData.newPlainText("one_time_password", otp)
        (getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager?)?.primaryClip = clip
    }

    companion object {
        const val EXTRA_PASSWORD = "extra_password"
    }

}
