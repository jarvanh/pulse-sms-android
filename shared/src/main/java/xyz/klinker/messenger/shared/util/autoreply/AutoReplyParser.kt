package xyz.klinker.messenger.shared.util.autoreply

import android.content.Context
import xyz.klinker.messenger.api.implementation.Account
import xyz.klinker.messenger.shared.data.MimeType
import xyz.klinker.messenger.shared.data.model.AutoReply
import xyz.klinker.messenger.shared.data.model.Conversation
import xyz.klinker.messenger.shared.data.model.Message

abstract class AutoReplyParser(protected var context: Context?, protected val reply: AutoReply) {

    abstract fun canParse(conversation: Conversation, message: Message): Boolean

    fun parse(forMessage: Message): Message? {
        val message = Message()
        message.conversationId = forMessage.conversationId
        message.timestamp = forMessage.timestamp + 1
        message.type = Message.TYPE_SENDING
        message.read = false
        message.seen = false
        message.mimeType = MimeType.TEXT_PLAIN
        message.data = reply.response
        message.sentDeviceId =  if (Account.exists()) Account.deviceId!!.toLong() else -1L

        return if (message.data == null) null else message
    }
}
