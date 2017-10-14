package xyz.klinker.messenger.shared.util.media

import android.content.Context

import xyz.klinker.messenger.shared.util.media.parsers.ArticleParser
import xyz.klinker.messenger.shared.util.media.parsers.YoutubeParser

/**
 * Create an instance that can be used to parse media from the message text
 */
class MediaMessageParserFactory {

    fun getInstance(context: Context, messageText: String): MediaParser? {
        return buildParsers(context).firstOrNull { it.canParse(messageText) }
    }

    private fun buildParsers(context: Context): Array<MediaParser> {
        return arrayOf(YoutubeParser(context), ArticleParser(context))
    }
}
