package xyz.klinker.messenger.activity.compose

import xyz.klinker.messenger.R
import xyz.klinker.messenger.shared.data.DataSource
import xyz.klinker.messenger.shared.data.MimeType
import xyz.klinker.messenger.shared.data.model.Message
import xyz.klinker.messenger.shared.service.MessengerChooserTargetService

class ComposeShareHandler(private val activity: ComposeActivity) {

    fun apply(mimeType: String, data: String) {
        val phoneNumbers = activity.contactsProvider.getPhoneNumberFromContactEntry()
        apply(mimeType, data, phoneNumbers)
    }

    fun apply(mimeType: String, data: String?, phoneNumbers: String) {
        var conversationId = DataSource.findConversationId(activity, phoneNumbers)

        if (conversationId == null) {
            val message = Message()
            message.type = Message.TYPE_INFO
            message.data = activity.getString(R.string.no_messages_with_contact)
            message.timestamp = System.currentTimeMillis()
            message.mimeType = MimeType.TEXT_PLAIN
            message.read = true
            message.seen = true
            message.sentDeviceId = -1

            conversationId = DataSource.insertMessage(message, phoneNumbers, activity)
        }

        if (data != null) {
            DataSource.insertDraft(activity, conversationId, data, mimeType)
        }

        activity.sender.showConversation(conversationId)
    }

    fun directShare(data: String?, mimeType: String, isVcard: Boolean = false) {
        val conversationId = activity.intent.extras.getLong(MessengerChooserTargetService.EXTRA_CONVO_ID)

        if (isVcard) {
            activity.vCardSender.send(mimeType, data!!, conversationId)
        } else {
            if (data != null) {
                DataSource.insertDraft(activity, conversationId, data, mimeType)
            }

            activity.sender.showConversation(conversationId)
        }
    }
}