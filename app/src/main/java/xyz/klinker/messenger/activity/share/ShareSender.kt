package xyz.klinker.messenger.activity.share

import android.net.Uri
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

    fun sendMessage(): Boolean {
        if (page.messageEntry.text.isEmpty() && page.mediaData == null) {
            return false
        }

        var conversationId: Long? = null

        val messageText = page.messageEntry.text.toString().trim { it <= ' ' }
        val phoneNumbers = page.contactEntry.recipients
                .joinToString(", ") { PhoneNumberUtils.clearFormatting(it.entry.destination) }

        if (messageText.isNotEmpty()) {
            val textMessage = createMessage(messageText)
            conversationId = DataSource.insertMessage(textMessage, phoneNumbers, page.context)
        }

        if (page.mediaData != null) {
            val imageMessage = createMessage(page.mediaData!!)
            imageMessage.mimeType = page.mimeType!!
            conversationId = DataSource.insertMessage(imageMessage, phoneNumbers, page.context)
        }

        DataSource.readConversation(page.context, conversationId!!)
        val conversation = DataSource.getConversation(page.activity, conversationId) ?: return false

        Thread {
            if (page.mediaData != null) {
                SendUtils(conversation.simSubscriptionId).send(page.context, messageText, phoneNumbers, Uri.parse(page.mediaData!!), page.mimeType!!)
            } else {
                SendUtils(conversation.simSubscriptionId).send(page.context, messageText, phoneNumbers)
            }

            //MarkAsSentJob.scheduleNextRun(page.context, messageId)
        }.start()

        ConversationListUpdatedReceiver.sendBroadcast(page.context, conversation.id, page.context.getString(R.string.you) + ": " + messageText, true)
        MessageListUpdatedReceiver.sendBroadcast(page.context, conversation.id)
        MessengerAppWidgetProvider.refreshWidget(page.context)

        return true
    }

    private fun createMessage(text: String, mimeType: String = MimeType.TEXT_PLAIN): Message {
        val message = Message()
        message.type = Message.TYPE_SENDING
        message.data = text
        message.timestamp = System.currentTimeMillis()
        message.mimeType = mimeType
        message.read = true
        message.seen = true
        message.simPhoneNumber = if (DualSimUtils.availableSims.size > 1) DualSimUtils.defaultPhoneNumber else null
        message.sentDeviceId = if (Account.exists()) Account.deviceId!!.toLong() else -1L

        return message
    }
}