package xyz.klinker.messenger.activity.share

import xyz.klinker.messenger.R
import xyz.klinker.messenger.api.implementation.Account
import xyz.klinker.messenger.shared.data.DataSource
import xyz.klinker.messenger.shared.data.MimeType
import xyz.klinker.messenger.shared.data.model.Message
import xyz.klinker.messenger.shared.receiver.ConversationListUpdatedReceiver
import xyz.klinker.messenger.shared.receiver.MessageListUpdatedReceiver
import xyz.klinker.messenger.shared.util.DualSimUtils
import xyz.klinker.messenger.shared.util.PhoneNumberUtils
import xyz.klinker.messenger.shared.util.SendUtils
import xyz.klinker.messenger.shared.widget.MessengerAppWidgetProvider

class ShareSender(private val page: QuickSharePage) {

    fun sendMessage() {
        val message = createMessage(page.messageEntry.text.toString().trim { it <= ' ' })
        val phoneNumbers = page.contactEntry.recipients
                .joinToString(", ") { PhoneNumberUtils.clearFormatting(it.entry.destination) }

        val conversationId = DataSource.insertMessage(message, phoneNumbers, page.context)
        DataSource.readConversation(page.context, conversationId)
        val conversation = DataSource.getConversation(page.activity, conversationId) ?: return

        Thread {
            SendUtils(conversation.simSubscriptionId).send(page.context, message.data!!, conversation.phoneNumbers!!)
            //MarkAsSentJob.scheduleNextRun(page.context, messageId)
        }.start()

        ConversationListUpdatedReceiver.sendBroadcast(page.context, conversationId, page.context.getString(R.string.you) + ": " + message.data, true)
        MessageListUpdatedReceiver.sendBroadcast(page.context, conversationId)
        MessengerAppWidgetProvider.refreshWidget(page.context)
    }

    private fun createMessage(text: String): Message {
        val message = Message()
        message.type = Message.TYPE_SENDING
        message.data = text
        message.timestamp = System.currentTimeMillis()
        message.mimeType = MimeType.TEXT_PLAIN
        message.read = true
        message.seen = true
        message.simPhoneNumber = DualSimUtils.defaultPhoneNumber
        message.sentDeviceId = if (Account.exists()) Account.deviceId!!.toLong() else -1L

        return message
    }
}