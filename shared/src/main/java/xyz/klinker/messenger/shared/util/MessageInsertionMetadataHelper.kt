package xyz.klinker.messenger.shared.util

import android.content.Context
import xyz.klinker.messenger.api.implementation.Account
import xyz.klinker.messenger.shared.data.DataSource
import xyz.klinker.messenger.shared.data.FeatureFlags
import xyz.klinker.messenger.shared.data.MimeType
import xyz.klinker.messenger.shared.data.model.Conversation
import xyz.klinker.messenger.shared.data.model.Message
import xyz.klinker.messenger.shared.service.message_parser.AutoReplyParserService
import xyz.klinker.messenger.shared.service.message_parser.MediaParserService

class MessageInsertionMetadataHelper(private val context: Context) {

    private fun shouldProcessOnThisDevice(): Boolean {
        return !Account.exists() || Account.primary
    }

    fun process(message: Message) {
        if (!shouldProcessOnThisDevice()) {
            return
        }

        val conversation = try {
            DataSource.getConversation(context, message.conversationId)
        } catch (e: Exception) { null }

        if (conversation != null) {
            process(message, conversation)
        }
    }

    private fun process(message: Message, conversation: Conversation) {
        if (message.mimeType == MimeType.TEXT_PLAIN && canProcessMedia(message)) {
            MediaParserService.start(context, message)
        }

        if (message.type == Message.TYPE_RECEIVED && canProcessAutoReply(message, conversation)) {
            AutoReplyParserService.start(context, message)
        }

        if (FeatureFlags.VCARD_PREVIEWS && MimeType.isVcard(message.mimeType!!)) {

        }
    }

    private fun canProcessMedia(message: Message) =
        MediaParserService.createParser(context, message) != null

    private fun canProcessAutoReply(message: Message, conversation: Conversation) =
            AutoReplyParserService.createParsers(context, conversation, message).isNotEmpty()

}