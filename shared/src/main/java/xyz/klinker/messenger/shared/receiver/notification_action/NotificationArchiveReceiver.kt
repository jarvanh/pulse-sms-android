package xyz.klinker.messenger.shared.receiver.notification_action

import android.content.Context
import android.content.Intent
import xyz.klinker.messenger.shared.data.DataSource

open class NotificationArchiveReceiver : xyz.klinker.messenger.shared.receiver.notification_action.NotificationMarkReadReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        super.onReceive(context, intent)
        // mark as read functionality

        if (context == null || intent == null) {
            return
        }

        Thread {
            val conversationId = intent.getLongExtra(xyz.klinker.messenger.shared.receiver.notification_action.NotificationMarkReadReceiver.Companion.EXTRA_CONVERSATION_ID, -1L)
            if (conversationId != -1L) {
                DataSource.archiveConversation(context, conversationId)

                // TODO: if this is going to work, I need to be able to remove the conversation from the conversation list
                // once that is done, I can uncomment the "archive" option in the "arrays.xml" file for notification_actions and notification_actions_values
            }
        }.start()

    }

}
