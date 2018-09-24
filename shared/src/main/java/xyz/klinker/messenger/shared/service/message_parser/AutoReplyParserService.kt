package xyz.klinker.messenger.shared.service.message_parser

import android.annotation.SuppressLint
import android.app.IntentService
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import xyz.klinker.messenger.shared.R
import xyz.klinker.messenger.shared.data.ColorSet
import xyz.klinker.messenger.shared.data.DataSource
import xyz.klinker.messenger.shared.data.model.Conversation
import xyz.klinker.messenger.shared.data.model.Message
import xyz.klinker.messenger.shared.receiver.MessageListUpdatedReceiver
import xyz.klinker.messenger.shared.util.AndroidVersionUtil
import xyz.klinker.messenger.shared.util.NotificationUtils
import xyz.klinker.messenger.shared.util.SendUtils
import xyz.klinker.messenger.shared.util.autoreply.AutoReplyParser
import xyz.klinker.messenger.shared.util.autoreply.AutoReplyParserFactory

class AutoReplyParserService : IntentService("AutoReplyParserService") {

    override fun onHandleIntent(intent: Intent?) {
        if (AndroidVersionUtil.isAndroidO) {
            val notification = NotificationCompat.Builder(this,
                    NotificationUtils.SILENT_BACKGROUND_CHANNEL_ID)
                    .setContentTitle(getString(R.string.auto_reply_parse_text))
                    .setSmallIcon(R.drawable.ic_stat_notify_group)
                    .setProgress(0, 0, true)
                    .setLocalOnly(true)
                    .setColor(ColorSet.DEFAULT(this).color)
                    .setOngoing(true)
                    .setPriority(NotificationCompat.PRIORITY_MIN)
                    .build()
            startForeground(AUTO_REPLY_PARSE_FOREGROUND_ID, notification)
        }

        if (intent == null) {
            stopForeground(true)
            return
        }

        val messageId = intent.getLongExtra(EXTRA_MESSAGE_ID, -1L)
        val message = DataSource.getMessage(this, messageId)
        if (message == null) {
            stopForeground(true)
            return
        }

        val conversation = DataSource.getConversation(this, message.conversationId)
        if (conversation == null) {
            stopForeground(true)
            return
        }

        val parsers = createParsers(this, conversation, message)
        if (parsers.isEmpty()) {
            stopForeground(true)
            return
        }

        parsers.forEach {
            val parsedMessage = it.parse(message)
            if (parsedMessage != null) {
                DataSource.insertMessage(this, parsedMessage, conversation.id, true)
                MessageListUpdatedReceiver.sendBroadcast(this, conversation.id, message.data, message.type)
                SendUtils().send(this, parsedMessage.data!!, conversation.phoneNumbers!!)
            }
        }

        if (AndroidVersionUtil.isAndroidO) {
            stopForeground(true)
        }
    }

    @SuppressLint("NewApi", "MayBeConstant")
    companion object {
        fun start(context: Context, message: Message) {
            val parser = Intent(context, AutoReplyParserService::class.java)
            parser.putExtra(EXTRA_MESSAGE_ID, message.id)

            if (AndroidVersionUtil.isAndroidO) {
                context.startForegroundService(parser)
            } else {
                context.startService(parser)
            }
        }

        private val AUTO_REPLY_PARSE_FOREGROUND_ID = 1339

        val EXTRA_MESSAGE_ID = "message_id"

        fun createParsers(context: Context, conversation: Conversation, message: Message): List<AutoReplyParser> {
            return AutoReplyParserFactory().getInstances(context, conversation, message)
        }
    }
}
