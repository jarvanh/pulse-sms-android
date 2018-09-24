package xyz.klinker.messenger.activity.notification

import android.content.Intent
import androidx.core.app.RemoteInput
import xyz.klinker.messenger.shared.service.ReplyService

class ReplyWearableHandler(private val activity: MarshmallowReplyActivity) {

    fun reply(): Boolean {
        val remoteInput = RemoteInput.getResultsFromIntent(activity.intent)
        if (remoteInput != null) {
            val replyService = Intent(activity, ReplyService::class.java)
            replyService.putExtras(activity.intent)

            activity.startService(replyService)
            return true
        }

        return false
    }
}