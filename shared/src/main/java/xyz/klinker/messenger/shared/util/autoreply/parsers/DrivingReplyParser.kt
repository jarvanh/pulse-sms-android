package xyz.klinker.messenger.shared.util.autoreply.parsers

import android.content.Context
import xyz.klinker.messenger.shared.data.Settings
import xyz.klinker.messenger.shared.data.model.AutoReply
import xyz.klinker.messenger.shared.util.autoreply.AutoReplyParser

class DrivingReplyParser(context: Context?, reply: AutoReply) : AutoReplyParser(context, reply) {

    override fun canParse(phoneNumber: String, text: String) = Settings.drivingMode

}
