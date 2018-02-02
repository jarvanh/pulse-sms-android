package xyz.klinker.messenger.shared.util.autoreply

import android.content.Context
import xyz.klinker.messenger.shared.data.MimeType
import xyz.klinker.messenger.shared.data.model.AutoReply
import xyz.klinker.messenger.shared.data.model.Message

abstract class AutoReplyParser(protected var context: Context?, protected val reply: AutoReply) {

    abstract fun canParse(phoneNumber: String, text: String): Boolean

    fun parse(conversationId: Long): Message? {
        val message = Message()
        message.conversationId = conversationId
        message.timestamp = System.currentTimeMillis()
        message.type = Message.TYPE_SENDING
        message.read = false
        message.seen = false
        message.mimeType = MimeType.TEXT_PLAIN
        message.data = reply.response
        message.sentDeviceId = -1L

        return if (message.data == null) null else message
    }
}
