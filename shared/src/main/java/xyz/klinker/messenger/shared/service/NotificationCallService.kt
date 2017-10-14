package xyz.klinker.messenger.shared.service

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri

class NotificationCallService : NotificationMarkReadService() {

    override fun onHandleIntent(intent: Intent?) {
        // mark as read functionality
        super.onHandleIntent(intent)

        val phoneNumber = intent!!.getStringExtra(EXTRA_PHONE_NUMBER)

        if (phoneNumber != null) {
            val call = Intent(Intent.ACTION_DIAL)
            call.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            call.data = Uri.parse("tel:" + phoneNumber)

            try {
                startActivity(call)
            } catch (e: ActivityNotFoundException) {
                // no call activity
            }

        }
    }

    companion object {
        val EXTRA_PHONE_NUMBER = "extra_phone_number"
    }
}
