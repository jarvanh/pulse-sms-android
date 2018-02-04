package xyz.klinker.messenger.shared.util.vcard.parsers

import android.content.Context
import xyz.klinker.messenger.shared.data.MimeType
import xyz.klinker.messenger.shared.data.model.Message
import xyz.klinker.messenger.shared.util.StringUtils
import xyz.klinker.messenger.shared.util.vcard.VcardParser

class TextAttributeVcardParser(context: Context) : VcardParser(context) {

    private var data: String? = null

    override fun canParse(message: Message) = getData(message).isNotEmpty()
    override fun getMimeType(message: Message) = MimeType.TEXT_PLAIN

    override fun getData(message: Message): String {
        if (data != null) {
            return data!!
        }

        val lines = message.data!!.split("\n")
        val cards = mutableListOf<MutableList<String>>()

        for (line in lines) {
            if (line.contains(START_LINE)) {
                cards.add(mutableListOf())
            }

            if (cards.isNotEmpty()) {
                cards[cards.size - 1].add(line)
            }
        }

        data = cards.joinToString("\n\n") { getReadableLines(it) }

        return data!!
    }

    private fun getReadableLines(card: List<String>): String {
        return card.filter { line -> PARSABLE_LINES.firstOrNull { line.contains(it) } != null }
                .map { readLine(it) }
                .filter { it.isNotEmpty() }
                .joinToString("\n")
    }

    private fun readLine(line: String): String {
        return when {
            line.contains(FULL_NAME) -> readFullName(line)
            line.contains(PHONE_NUMBER) -> readPhoneNumber(line)
            else -> throw IllegalArgumentException("this line shouldn't have made it here: $line")
        }
    }

    private fun readFullName(line: String): String {
        return line.replace(FULL_NAME, "")
    }

    private fun readPhoneNumber(line: String): String {
        return try {
            val type = line.substring(line.indexOf("=") + 1, line.indexOf(","))
            val number = line.substring(line.indexOf("VOICE:") + "VOICE:".length)

            "${StringUtils.titleize(type)}: $number"
        } catch (e: Exception) {
            ""
        }
    }

    companion object {
        private const val START_LINE = "BEGIN:VCARD"
        private const val END_LINE = "END:VCARD"

        private const val FULL_NAME = "FN:"
        private const val PHONE_NUMBER = "TEL;"

        private val PARSABLE_LINES = listOf(FULL_NAME, PHONE_NUMBER)
    }
}