package xyz.klinker.messenger.activity.compose

import android.net.Uri
import xyz.klinker.messenger.shared.data.DataSource
import xyz.klinker.messenger.shared.util.CursorUtil
import xyz.klinker.messenger.shared.util.SendUtils

class ComposeVCardSender(private val activity: ComposeActivity) {

    fun send(mimeType: String, data: String) {
        val phoneNumbers = activity.contactsProvider.getPhoneNumberFromContactEntry()
        send(mimeType, data, phoneNumbers)
    }

    fun send(mimeType: String, data: String, conversationId: Long) {
        val conversation = DataSource.getConversation(activity, conversationId)

        val uri = SendUtils(conversation!!.simSubscriptionId)
                .send(activity, "", conversation.phoneNumbers!!, Uri.parse(data), mimeType)
        val cursor = DataSource.searchMessages(activity, data)

        if (cursor != null && cursor.moveToFirst()) {
            DataSource.updateMessageData(activity, cursor.getLong(0), uri!!.toString())
        }

        CursorUtil.closeSilent(cursor)
        activity.finish()
    }

    private fun send(mimeType: String, data: String, phoneNumbers: String) {
        val conversationId = DataSource.insertSentMessage(phoneNumbers, data, mimeType, activity)
        send(mimeType, data, conversationId)
    }

}