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
import xyz.klinker.messenger.shared.util.SendUtils
import xyz.klinker.messenger.shared.util.autoreply.AutoReplyParser
import xyz.klinker.messenger.shared.util.autoreply.AutoReplyParserFactory
import xyz.klinker.messenger.shared.util.media.MediaMessageParserFactory
import xyz.klinker.messenger.shared.util.media.MediaParser
import xyz.klinker.messenger.shared.util.media.parsers.ArticleParser

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

        val conversationId = intent.getLongExtra(EXTRA_CONVERSATION_ID, -1L)
        val text = intent.getStringExtra(EXTRA_BODY_TEXT)
        val phoneNumber = intent.getStringExtra(EXTRA_PHONE_NUMBER)

        if (conversationId == -1L || text == null || phoneNumber == null) {
            stopForeground(true)
            return
        }

        val parsers = createParsers(this, phoneNumber, text)
        val conversation = DataSource.getConversation(this, conversationId)

        if (parsers.isEmpty() || conversation == null) {
            stopForeground(true)
            return
        }

        parsers.forEach {
            val message = it.parse(conversationId)
            if (message != null) {
                DataSource.insertMessage(this, message, conversationId, true)
                MessageListUpdatedReceiver.sendBroadcast(this, conversationId, message.data, message.type)
                SendUtils().send(this, message.data!!, conversation.phoneNumbers!!)
            }
        }


        if (AndroidVersionUtil.isAndroidO) {
            stopForeground(true)
        }
    }

    @SuppressLint("NewApi", "MayBeConstant")
    companion object {
        fun start(context: Context, conversationId: Long, phoneNumber: String, text: String) {
            val parser = Intent(context, AutoReplyParserService::class.java)
            parser.putExtra(AutoReplyParserService.EXTRA_CONVERSATION_ID, conversationId)
            parser.putExtra(AutoReplyParserService.EXTRA_BODY_TEXT, text.trim { it <= ' ' })
            parser.putExtra(AutoReplyParserService.EXTRA_PHONE_NUMBER, phoneNumber.trim { it <= ' ' })

            if (AndroidVersionUtil.isAndroidO) {
                context.startForegroundService(parser)
            } else {
                context.startService(parser)
            }
        }

        private val AUTO_REPLY_PARSE_FOREGROUND_ID = 1339

        val EXTRA_CONVERSATION_ID = "conversation_id"
        val EXTRA_BODY_TEXT = "body_text"
        val EXTRA_PHONE_NUMBER = "phone_number"

        fun createParsers(context: Context, phoneNumber: String, text: String): List<AutoReplyParser> {
            return AutoReplyParserFactory().getInstances(context, phoneNumber, text)
        }
    }
}
