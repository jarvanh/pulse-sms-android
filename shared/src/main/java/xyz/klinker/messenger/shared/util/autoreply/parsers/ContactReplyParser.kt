package xyz.klinker.messenger.shared.util.autoreply.parsers

import android.content.Context
import xyz.klinker.messenger.shared.data.model.AutoReply
import xyz.klinker.messenger.shared.data.model.Conversation
import xyz.klinker.messenger.shared.data.model.Message
import xyz.klinker.messenger.shared.util.SmsMmsUtils
import xyz.klinker.messenger.shared.util.autoreply.AutoReplyParser

class ContactReplyParser(context: Context?, reply: AutoReply) : AutoReplyParser(context, reply) {

    override fun canParse(conversation: Conversation, message: Message): Boolean {
        if (reply.pattern == null) {
            return false
        }

        val autoReplyIdMatcher = SmsMmsUtils.createIdMatcher(reply.pattern!!)
        val receivedMessageMatcher = SmsMmsUtils.createIdMatcher(conversation.phoneNumbers!!)

        return autoReplyIdMatcher.default == receivedMessageMatcher.default
    }

}
