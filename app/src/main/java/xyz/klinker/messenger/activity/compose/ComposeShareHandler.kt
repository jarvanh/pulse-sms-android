package xyz.klinker.messenger.activity.compose

import xyz.klinker.messenger.R
import xyz.klinker.messenger.shared.data.DataSource
import xyz.klinker.messenger.shared.data.MimeType
import xyz.klinker.messenger.shared.data.model.Message
import xyz.klinker.messenger.shared.service.MessengerChooserTargetService
import xyz.klinker.messenger.shared.util.TimeUtils

data class ShareData(val mimeType: String, val data: String)

class ComposeShareHandler(private val activity: ComposeActivity) {

    fun apply(data: ShareData) {
        apply(listOf(data))
    }

    fun apply(data: List<ShareData>) {
        val phoneNumbers = activity.contactsProvider.getPhoneNumberFromContactEntry()
        apply(data, phoneNumbers)
    }

    fun apply(data: ShareData, phoneNumbers: String) {
        apply(listOf(data), phoneNumbers)
    }

    fun apply(data: List<ShareData>, phoneNumbers: String) {
        var conversationId = DataSource.findConversationId(activity, phoneNumbers)

        if (conversationId == null) {
            val message = Message()
            message.type = Message.TYPE_INFO
            message.data = activity.getString(R.string.no_messages_with_contact)
            message.timestamp = TimeUtils.now
            message.mimeType = MimeType.TEXT_PLAIN
            message.read = true
            message.seen = true
            message.sentDeviceId = -1

            conversationId = DataSource.insertMessage(message, phoneNumbers, activity)
        }

        data.forEach {
            DataSource.insertDraft(activity, conversationId, it.data, it.mimeType)
        }

        activity.sender.showConversation(conversationId)
    }

    fun directShare(data: ShareData, isVcard: Boolean = false) {
        directShare(listOf(data), isVcard)
    }

    fun directShare(data: List<ShareData>, isVcard: Boolean = false) {
        val conversationId = activity.intent.extras.getLong(MessengerChooserTargetService.EXTRA_CONVO_ID)
        directShare(data, conversationId, isVcard)
    }

    fun directShare(data: List<ShareData>, conversationId: Long, isVcard: Boolean = false) {
        if (isVcard && data.isNotEmpty()) {
            activity.vCardSender.send(data[0].mimeType, data[0].data, conversationId)
        } else {
            data.forEach {
                DataSource.insertDraft(activity, conversationId, it.data, it.mimeType)
            }

            activity.sender.showConversation(conversationId)
        }
    }
}