package xyz.klinker.messenger.shared.util.media

import android.content.Context
import android.support.annotation.VisibleForTesting

import java.util.regex.Matcher
import java.util.regex.Pattern

import xyz.klinker.messenger.api.implementation.Account
import xyz.klinker.messenger.shared.data.model.Message

abstract class MediaParser(protected var context: Context?) {
    protected var matchedText: String? = null

    protected abstract val patternMatcher: Pattern
    protected abstract val ignoreMatcher: String?
    protected abstract val mimeType: String
    protected abstract fun buildBody(matchedText: String?): String?

    open fun canParse(message: Message): Boolean {
        val text = message.data!!
        val matcher = patternMatcher.matcher(text)
        if (matcher.find()) {
            matchedText = matcher.group(0)
        }

        return matchedText != null && (ignoreMatcher == null || ignoreMatcher != null && !Pattern.compile(ignoreMatcher).matcher(text).find())
    }

    fun parse(forMessage: Message): Message? {
        val message = Message()
        message.conversationId = forMessage.conversationId
        message.timestamp = forMessage.timestamp + 1L
        message.type = Message.TYPE_MEDIA
        message.read = false
        message.seen = false
        message.mimeType = mimeType
        message.data = buildBody(matchedText)
        message.sentDeviceId = -1L

        matchedText = null

        return if (message.data == null) null else message
    }
}
