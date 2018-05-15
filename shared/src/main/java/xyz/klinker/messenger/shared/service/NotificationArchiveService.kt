package xyz.klinker.messenger.shared.service

import android.content.Intent
import xyz.klinker.messenger.shared.data.DataSource

open class NotificationArchiveService : NotificationMarkReadService() {

    override fun onHandleIntent(intent: Intent?) {
        // mark as read functionality
        super.onHandleIntent(intent)

        val conversationId = intent?.getLongExtra(NotificationMarkReadService.EXTRA_CONVERSATION_ID, -1L) ?: return
        if (conversationId != -1L) {
            DataSource.archiveConversation(this, conversationId)

            // TODO: if this is going to work, I need to be able to remove the conversation from the conversation list
            // once that is done, I can uncomment the "archive" option in the "arrays.xml" file for notification_actions and notification_actions_values
        }
    }

}
