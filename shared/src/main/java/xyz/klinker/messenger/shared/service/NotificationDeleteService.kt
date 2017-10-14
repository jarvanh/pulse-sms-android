package xyz.klinker.messenger.shared.service

import android.app.IntentService
import android.content.Intent
import android.support.v4.app.NotificationManagerCompat
import xyz.klinker.messenger.api.implementation.Account
import xyz.klinker.messenger.api.implementation.ApiUtils
import xyz.klinker.messenger.shared.data.DataSource
import xyz.klinker.messenger.shared.data.MimeType
import xyz.klinker.messenger.shared.data.model.Message
import xyz.klinker.messenger.shared.receiver.ConversationListUpdatedReceiver
import xyz.klinker.messenger.shared.util.CursorUtil
import xyz.klinker.messenger.shared.util.UnreadBadger
import xyz.klinker.messenger.shared.widget.MessengerAppWidgetProvider

class NotificationDeleteService : IntentService("NotificationDeleteService") {

    override fun onHandleIntent(intent: Intent?) {
        val messageId = intent!!.getLongExtra(EXTRA_MESSAGE_ID, -1)
        val conversationId = intent.getLongExtra(EXTRA_CONVERSATION_ID, -1)

        DataSource.deleteMessage(this, messageId)

        val messages = DataSource.getMessages(this, conversationId, 1)
        var latest: Message? = null

        if (messages.size == 1) {
            latest = messages[0]
        }

        if (latest == null) {
            DataSource.deleteConversation(this, conversationId)
        } else if (latest.mimeType == MimeType.TEXT_PLAIN) {
            DataSource.updateConversation(this, conversationId, true, latest.timestamp, latest.data, latest.mimeType, false)
        }

        // cancel the notification we just replied to or
        // if there are no more notifications, cancel the summary as well
        val unseenMessages = DataSource.getUnseenMessages(this)
        if (unseenMessages.count <= 0) {
            NotificationManagerCompat.from(this).cancelAll()
        } else {
            NotificationManagerCompat.from(this).cancel(conversationId.toInt())
        }

        CursorUtil.closeSilent(unseenMessages)

        ApiUtils.dismissNotification(Account.accountId,
                Account.deviceId,
                conversationId)

        ConversationListUpdatedReceiver.sendBroadcast(this, conversationId,
                if (latest != null && latest.mimeType == MimeType.TEXT_PLAIN) latest.data else "",
                true)

        UnreadBadger(this).writeCountFromDatabase()
        MessengerAppWidgetProvider.refreshWidget(this)
    }

    companion object {
        val EXTRA_CONVERSATION_ID = "conversation_id"
        val EXTRA_MESSAGE_ID = "message_id"
    }
}
