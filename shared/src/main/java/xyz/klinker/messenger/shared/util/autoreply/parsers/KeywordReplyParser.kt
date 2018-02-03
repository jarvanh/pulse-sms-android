package xyz.klinker.messenger.shared.util.autoreply.parsers

import android.content.Context
import xyz.klinker.messenger.shared.data.MimeType
import xyz.klinker.messenger.shared.data.model.AutoReply
import xyz.klinker.messenger.shared.data.model.Conversation
import xyz.klinker.messenger.shared.data.model.Message
import xyz.klinker.messenger.shared.util.autoreply.AutoReplyParser

class KeywordReplyParser(context: Context?, reply: AutoReply) : AutoReplyParser(context, reply) {

    override fun canParse(conversation: Conversation, message: Message): Boolean {
        if (reply.pattern == null || message.mimeType != MimeType.TEXT_PLAIN) {
            return false
        }

        return message.data!!.toLowerCase().contains(reply.pattern!!.toLowerCase())
    }

}