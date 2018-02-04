package xyz.klinker.messenger.shared.util.vcard

import android.content.Context
import xyz.klinker.messenger.shared.data.model.Message
import xyz.klinker.messenger.shared.util.vcard.parsers.TextAttributeVcardParser

class VcardParserFactory {

    fun getInstances(context: Context, message: Message): List<VcardParser> {
        message.data = VcardReader.readCotactCard(context, message.data!!)
        return buildParsers(context).filter { it.canParse(message) }
    }

    private fun buildParsers(context: Context): List<VcardParser> {
        return listOf(TextAttributeVcardParser(context))
    }

}