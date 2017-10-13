package xyz.klinker.messenger.shared.data

import xyz.klinker.messenger.shared.data.model.Conversation

class IdMatcher(val fiveLetter: String, val sevenLetter: String, val sevenLetterNoFormatting: String,
                val eightLetter: String, val default: String, val tenLetter: String) {

    val allMatchers = arrayOf(fiveLetter, sevenLetter, sevenLetterNoFormatting, eightLetter, default, tenLetter)

    // Remember to update DataSourceTest#findConversationByNumbers
    val whereClause = Conversation.COLUMN_ID_MATCHER + "=? OR " + Conversation.COLUMN_ID_MATCHER + "=? OR " +
                Conversation.COLUMN_ID_MATCHER + "=? OR " + Conversation.COLUMN_ID_MATCHER + "=? OR " +
                Conversation.COLUMN_ID_MATCHER + "=? OR " + Conversation.COLUMN_ID_MATCHER + "=?"
}
