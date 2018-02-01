package xyz.klinker.messenger.shared.util.autoreply.parsers

import android.content.Context
import xyz.klinker.messenger.shared.data.model.AutoReply
import xyz.klinker.messenger.shared.util.SmsMmsUtils
import xyz.klinker.messenger.shared.util.autoreply.AutoReplyParser

class ContactReplyParser(context: Context?, reply: AutoReply) : AutoReplyParser(context, reply) {

    override fun canParse(phoneNumber: String, text: String): Boolean {
        if (reply.pattern == null) {
            return false
        }

        val autoReplyIdMatcher = SmsMmsUtils.createIdMatcher(reply.pattern!!)
        val receivedMessageMatcher = SmsMmsUtils.createIdMatcher(phoneNumber)

        return autoReplyIdMatcher.default == receivedMessageMatcher.default
    }

}
