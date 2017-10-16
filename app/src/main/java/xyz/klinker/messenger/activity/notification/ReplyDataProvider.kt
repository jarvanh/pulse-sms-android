package xyz.klinker.messenger.activity.notification

import android.support.v4.app.NotificationManagerCompat
import xyz.klinker.messenger.api.implementation.Account
import xyz.klinker.messenger.api.implementation.ApiUtils
import xyz.klinker.messenger.shared.data.DataSource
import xyz.klinker.messenger.shared.data.MimeType
import xyz.klinker.messenger.shared.data.model.Conversation
import xyz.klinker.messenger.shared.data.model.Message
import xyz.klinker.messenger.shared.service.ReplyService
import xyz.klinker.messenger.shared.util.CursorUtil

class ReplyDataProvider(private val activity: MarshmallowReplyActivity) {

    val conversationId: Long by lazy { activity.intent.getLongExtra(ReplyService.EXTRA_CONVERSATION_ID, -1L) }
    val conversation: Conversation? by lazy { DataSource.getConversation(activity, conversationId) }
    val messages = mutableListOf<Message>()

    fun queryMessageHistory() {
        DataSource.seenConversation(activity, conversationId)

        val cursor = DataSource.getMessages(activity, conversationId)

        if (cursor.moveToLast()) {
            do {
                val message = Message()
                message.fillFromCursor(cursor)

                if (!MimeType.isExpandedMedia(message.mimeType)) {
                    messages.add(message)
                }
            } while (cursor.moveToPrevious() && messages.size < PREV_MESSAGES_TOTAL)
        }

        CursorUtil.closeSilent(cursor)
    }

    fun dismissNotification() {
        NotificationManagerCompat.from(activity).cancel(conversationId.toInt())
        ApiUtils.dismissNotification(Account.accountId,
                Account.deviceId, conversationId)
    }

    companion object {
        val PREV_MESSAGES_TOTAL = 10
        val PREV_MESSAGES_DISPLAYED = 3
    }
}