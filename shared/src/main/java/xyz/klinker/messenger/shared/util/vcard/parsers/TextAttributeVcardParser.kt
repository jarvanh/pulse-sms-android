package xyz.klinker.messenger.shared.util.vcard.parsers

import android.content.Context
import android.util.Log
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
            Log.v("pulse_vcard", line)

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
            line.contains(EMAIL) -> readEmail(line)
            line.contains(URL) -> readUrl(line)
            else -> throw IllegalArgumentException("this line shouldn't have made it here: $line")
        }
    }

    private fun readFullName(line: String): String {
        return line.replace(FULL_NAME, "")
    }

    private fun readPhoneNumber(line: String): String {
        return try {
            when {
                line.contains("TYPE=") -> { // Pulse's style of vCard
                    val type = line.substring(line.indexOf("=") + 1, line.indexOf(","))
                    val number = line.substring(line.indexOf("VOICE:") + "VOICE:".length)
                    "${StringUtils.titleize(type)}: $number"
                }
                else -> {
                    val line = line.replace("TEL;", "").replace("TEL:", "")
                            .replace("PREF:", "").replace("PREF;", "")

                    val type = when {
                        line.contains(":") -> line.substring(0, line.indexOf(":"))
                        line.contains(";") -> line.substring(0, line.indexOf(";"))
                        else -> "Phone"
                    }

                    val number = when {
                        line.contains("CELL") -> line.substring(line.indexOf("CELL") + "CELL:".length)
                        line.contains("WORK") -> line.substring(line.indexOf("WORK") + "WORK:".length)
                        line.contains("HOME") -> line.substring(line.indexOf("HOME") + "HOME:".length)
                        line.contains("VOICE") -> line.substring(line.indexOf("VOICE") + "VOICE:".length)
                        else -> throw IllegalArgumentException("can't parse this line: $line")
                    }

                    "${StringUtils.titleize(type)}: $number"
                }
            }

        } catch (e: Exception) {
            ""
        }
    }

    private fun readEmail(line: String): String {
        return try {
            val line = line.replace("EMAIL;", "").replace("EMAIL:", "")
                    .replace("PREF;", "").replace("PREF:", "")

            val type = when {
                line.contains(":") -> line.substring(0, line.indexOf(":"))
                line.contains(";") -> line.substring(0, line.indexOf(";"))
                else -> "Email"
            }

            val address = when {
                line.contains("HOME") -> line.substring(line.indexOf("HOME") + "HOME:".length)
                line.contains("WORK") -> line.substring(line.indexOf("WORK") + "WORK:".length)
                !line.contains(":") -> line
                else -> throw IllegalArgumentException("can't parse this line: $line")
            }

            "${StringUtils.titleize(type)}: $address"
        } catch (e: Exception) {
            ""
        }
    }

    private fun readUrl(line: String): String {
        return try {
            val url = line.replace("URL:", "")
            "URL: $url"
        } catch (e: Exception) {
            ""
        }
    }

    companion object {
        private const val START_LINE = "BEGIN:VCARD"
        private const val END_LINE = "END:VCARD"

        private const val FULL_NAME = "FN:"
        private const val PHONE_NUMBER = "TEL;"
        private const val EMAIL = "EMAIL"
        private const val URL = "URL:"

        private val PARSABLE_LINES = listOf(FULL_NAME, PHONE_NUMBER, EMAIL, URL)
    }
}