package xyz.klinker.messenger.shared.service

import android.app.IntentService
import android.content.Context
import android.content.Intent
import android.support.v4.app.NotificationManagerCompat
import xyz.klinker.messenger.api.implementation.Account
import xyz.klinker.messenger.api.implementation.ApiUtils
import xyz.klinker.messenger.shared.data.DataSource
import xyz.klinker.messenger.shared.receiver.ConversationListUpdatedReceiver
import xyz.klinker.messenger.shared.util.UnreadBadger
import xyz.klinker.messenger.shared.util.closeSilent
import xyz.klinker.messenger.shared.widget.MessengerAppWidgetProvider

open class NotificationMarkReadService : IntentService("NotificationMarkReadService") {

    override fun onHandleIntent(intent: Intent?) {
        handle(intent, this)
    }

    companion object {

        val EXTRA_CONVERSATION_ID = "conversation_id"

        fun handle(intent: Intent?, context: Context) {
            val conversationId = intent?.getLongExtra(EXTRA_CONVERSATION_ID, -1) ?: return
            DataSource.readConversation(context, conversationId)
            val conversation = DataSource.getConversation(context, conversationId)

            // cancel the notification we just replied to or
            // if there are no more notifications, cancel the summary as well
            val unseenMessages = DataSource.getUnseenMessages(context)
            if (unseenMessages.count <= 0) {
                NotificationManagerCompat.from(context).cancelAll()
            } else {
                NotificationManagerCompat.from(context).cancel(conversationId.toInt())
            }

            unseenMessages.closeSilent()

            ApiUtils.dismissNotification(Account.accountId,
                    Account.deviceId,
                    conversationId)

            ConversationListUpdatedReceiver.sendBroadcast(context, conversationId, if (conversation == null) "" else conversation.snippet, true)

            UnreadBadger(context).writeCountFromDatabase()
            MessengerAppWidgetProvider.refreshWidget(context)
        }
    }
}
