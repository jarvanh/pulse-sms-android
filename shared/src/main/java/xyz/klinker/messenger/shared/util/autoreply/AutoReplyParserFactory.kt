package xyz.klinker.messenger.shared.util.autoreply

import android.content.Context
import xyz.klinker.messenger.shared.data.DataSource
import xyz.klinker.messenger.shared.data.model.AutoReply
import xyz.klinker.messenger.shared.util.autoreply.parsers.ContactReplyParser
import xyz.klinker.messenger.shared.util.autoreply.parsers.DrivingReplyParser
import xyz.klinker.messenger.shared.util.autoreply.parsers.KeywordReplyParser
import xyz.klinker.messenger.shared.util.autoreply.parsers.VacationReplyParser

class AutoReplyParserFactory {

    fun getInstance(context: Context, fromPhoneNumber: String, messageText: String): AutoReplyParser? {
        return buildParsers(context).firstOrNull { it.canParse(fromPhoneNumber, messageText) }
    }

    private fun buildParsers(context: Context): List<AutoReplyParser> {
        val parsers = DataSource.getAutoRepliesAsList(context)
                .filter { it.response!!.isNotBlank() }
                .mapNotNull { mapToParser(context, it) }

        val driving = parsers.filter { it is DrivingReplyParser }
        if (driving.isNotEmpty()) {
            return driving
        }

        val vacation = parsers.filter { it is VacationReplyParser }
        if (vacation.isNotEmpty()) {
            return vacation
        }

        return parsers
    }

    private fun mapToParser(context: Context, reply: AutoReply): AutoReplyParser? {
        return when (reply.type) {
            AutoReply.TYPE_VACATION -> VacationReplyParser(context, reply)
            AutoReply.TYPE_DRIVING -> DrivingReplyParser(context, reply)
            AutoReply.TYPE_CONTACT -> ContactReplyParser(context, reply)
            AutoReply.TYPE_KEYWORD -> KeywordReplyParser(context, reply)
            else -> null
        }
    }
}