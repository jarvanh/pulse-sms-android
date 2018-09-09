package xyz.klinker.messenger.shared.service.message_parser

import android.annotation.SuppressLint
import android.app.IntentService
import android.content.Context
import android.content.Intent
import android.support.v4.app.NotificationCompat
import xyz.klinker.messenger.shared.R
import xyz.klinker.messenger.shared.data.ColorSet
import xyz.klinker.messenger.shared.data.DataSource
import xyz.klinker.messenger.shared.data.Settings
import xyz.klinker.messenger.shared.data.model.Message
import xyz.klinker.messenger.shared.receiver.MessageListUpdatedReceiver
import xyz.klinker.messenger.shared.util.AndroidVersionUtil
import xyz.klinker.messenger.shared.util.NotificationUtils
import xyz.klinker.messenger.shared.util.media.MediaMessageParserFactory
import xyz.klinker.messenger.shared.util.media.MediaParser
import xyz.klinker.messenger.shared.util.media.parsers.ArticleParser

class MediaParserService : IntentService("MediaParserService") {

    override fun onHandleIntent(intent: Intent?) {
        if (AndroidVersionUtil.isAndroidO) {
            val notification = NotificationCompat.Builder(this,
                    NotificationUtils.SILENT_BACKGROUND_CHANNEL_ID)
                    .setContentTitle(getString(R.string.media_parse_text))
                    .setSmallIcon(R.drawable.ic_stat_notify_group)
                    .setProgress(0, 0, true)
                    .setLocalOnly(true)
                    .setColor(ColorSet.DEFAULT(this).color)
                    .setOngoing(true)
                    .setPriority(NotificationCompat.PRIORITY_MIN)
                    .build()
            startForeground(MEDIA_PARSE_FOREGROUND_ID, notification)
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

        val parser = createParser(this, message)
        if (parser == null || (!Settings.internalBrowser && parser is ArticleParser)) {
            stopForeground(true)
            return
        }

        val parsedMessage = parser.parse(message)
        if (parsedMessage != null) {
            DataSource.insertMessage(this, parsedMessage, message.conversationId, true)
            MessageListUpdatedReceiver.sendBroadcast(this, message.conversationId, parsedMessage.data, parsedMessage.type)
        }

        if (AndroidVersionUtil.isAndroidO) {
            stopForeground(true)
        }
    }

    companion object {

        @SuppressLint("NewApi")
        fun start(context: Context, message: Message) {
            val mediaParser = Intent(context, MediaParserService::class.java)
            mediaParser.putExtra(EXTRA_MESSAGE_ID, message.id)

            if (AndroidVersionUtil.isAndroidO) {
                context.startForegroundService(mediaParser)
            } else {
                context.startService(mediaParser)
            }
        }

        private const val MEDIA_PARSE_FOREGROUND_ID = 1334

        const val EXTRA_MESSAGE_ID = "message_id"

        fun createParser(context: Context, message: Message): MediaParser? {
            return MediaMessageParserFactory().getInstance(context, message)
        }
    }
}
