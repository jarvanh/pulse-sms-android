package xyz.klinker.messenger.shared.service

import android.annotation.SuppressLint
import android.app.IntentService
import android.content.Context
import android.content.Intent
import android.support.v4.app.NotificationCompat
import xyz.klinker.messenger.shared.R
import xyz.klinker.messenger.shared.data.ColorSet
import xyz.klinker.messenger.shared.data.DataSource
import xyz.klinker.messenger.shared.data.Settings
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

        val conversationId = intent.getLongExtra(EXTRA_CONVERSATION_ID, -1L)
        val text = intent.getStringExtra(EXTRA_BODY_TEXT)

        if (conversationId == -1L || text == null) {
            stopForeground(true)
            return
        }

        val parser = createParser(this, text)
        if (parser == null || !Settings.internalBrowser && parser is ArticleParser) {
            stopForeground(true)
            return
        }

        val message = parser.parse(conversationId)
        if (message != null) {
            DataSource.insertMessage(this, message, conversationId, true)
            MessageListUpdatedReceiver.sendBroadcast(this, conversationId, message.data, message.type)
        }

        if (AndroidVersionUtil.isAndroidO) {
            stopForeground(true)
        }
    }

    companion object {

        @SuppressLint("NewApi")
        fun start(context: Context, conversationId: Long, text: String) {
            val mediaParser = Intent(context, MediaParserService::class.java)
            mediaParser.putExtra(MediaParserService.EXTRA_CONVERSATION_ID, conversationId)
            mediaParser.putExtra(MediaParserService.EXTRA_BODY_TEXT, text.trim { it <= ' ' })

            if (AndroidVersionUtil.isAndroidO) {
                context.startForegroundService(mediaParser)
            } else {
                context.startService(mediaParser)
            }
        }

        private val MEDIA_PARSE_FOREGROUND_ID = 1334

        val EXTRA_CONVERSATION_ID = "conversation_id"
        val EXTRA_BODY_TEXT = "body_text"

        fun createParser(context: Context, text: String): MediaParser? {
            return MediaMessageParserFactory().getInstance(context, text)
        }
    }
}
