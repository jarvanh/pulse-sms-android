package xyz.klinker.messenger.shared.util.vcard

import android.content.Context
import xyz.klinker.messenger.shared.data.model.Message

class VcardParserFactory {

    fun getInstances(context: Context, message: Message): List<VcardParser> {
        return buildParsers(context).filter { it.canParse(message) }
    }

    private fun buildParsers(context: Context): List<VcardParser> {
        return listOf()
    }

}