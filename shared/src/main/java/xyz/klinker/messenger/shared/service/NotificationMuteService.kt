package xyz.klinker.messenger.shared.service

import android.app.IntentService
import android.content.Intent
import android.support.v4.app.NotificationManagerCompat
import xyz.klinker.messenger.api.implementation.Account
import xyz.klinker.messenger.api.implementation.ApiUtils
import xyz.klinker.messenger.shared.data.DataSource
import xyz.klinker.messenger.shared.receiver.ConversationListUpdatedReceiver
import xyz.klinker.messenger.shared.util.UnreadBadger
import xyz.klinker.messenger.shared.util.closeSilent
import xyz.klinker.messenger.shared.widget.MessengerAppWidgetProvider

class NotificationMuteService : IntentService("NotificationMarkReadService") {

    override fun onHandleIntent(intent: Intent?) {
        val conversationId = intent?.getLongExtra(EXTRA_CONVERSATION_ID, -1) ?: return
        DataSource.readConversation(this, conversationId)
        val conversation = DataSource.getConversation(this, conversationId)

        conversation!!.mute = true
        DataSource.updateConversationSettings(this, conversation, true)

        // cancel the notification we just replied to or
        // if there are no more notifications, cancel the summary as well
        val unseenMessages = DataSource.getUnseenMessages(this)
        if (unseenMessages.count <= 0) {
            NotificationManagerCompat.from(this).cancelAll()
        } else {
            NotificationManagerCompat.from(this).cancel(conversationId.toInt())
        }

        unseenMessages.closeSilent()

        ApiUtils.dismissNotification(Account.accountId, Account.deviceId, conversationId)
        ConversationListUpdatedReceiver.sendBroadcast(this, conversationId, if (conversation == null) "" else conversation.snippet, true)

        UnreadBadger(this).clearCount()
        MessengerAppWidgetProvider.refreshWidget(this)
    }

    companion object {
        val EXTRA_CONVERSATION_ID = "conversation_id"
    }
}
