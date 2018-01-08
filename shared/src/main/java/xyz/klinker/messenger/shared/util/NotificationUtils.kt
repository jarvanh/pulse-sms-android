package xyz.klinker.messenger.shared.util

import android.annotation.TargetApi
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationChannelGroup
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.os.Build
import xyz.klinker.messenger.shared.R
import xyz.klinker.messenger.shared.data.Settings
import xyz.klinker.messenger.shared.data.model.Conversation
import xyz.klinker.messenger.shared.service.notification.NotificationRingtoneProvider
import xyz.klinker.messenger.shared.service.notification.NotificationService

object NotificationUtils {

    val DEFAULT_CONVERSATION_CHANNEL_ID = "default-conversation-channel"
    val MESSAGE_GROUP_SUMMARY_CHANNEL_ID = "message-group-summary"
    val FAILED_MESSAGES_CHANNEL_ID = "failed-messages"
    val QUICK_TEXT_CHANNEL_ID = "quick-text"
    val TEST_NOTIFICATIONS_CHANNEL_ID = "test-notifications"
    val STATUS_NOTIFICATIONS_CHANNEL_ID = "status-notifications"
    val MEDIA_PARSE_CHANNEL_ID = "media-parsing"
    val GENERAL_CHANNEL_ID = "general"
    val BACKGROUND_SERVICE_CHANNEL_ID = "background-service"

    fun cancelGroupedNotificationWithNoContent(context: Context?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && context != null) {
            val map = mutableMapOf<String, Int>()

            val manager = context.getSystemService(
                    Context.NOTIFICATION_SERVICE) as NotificationManager

            val notifications = manager.activeNotifications

            for (notification in notifications) {
                var keyString = notification.groupKey
                if (keyString.contains("|g:")) { // this is a grouped notification
                    keyString = keyString.substring(keyString.indexOf("|g:") + 3, keyString.length)

                    if (map.containsKey(keyString)) {
                        map.put(keyString, map[keyString]!! + 1)
                    } else {
                        map.put(keyString, 1)
                    }
                }
            }

            val it = map.entries.iterator()
            while (it.hasNext()) {
                val pair = it.next() as Map.Entry<String, Int>
                val key = pair.key
                val value = pair.value

                if (value == 1) {
                    for (notification in notifications) {
                        var keyString = notification.groupKey
                        if (keyString.contains("|g:")) { // this is a grouped notification
                            keyString = keyString.substring(keyString.indexOf("|g:") + 3, keyString.length)

                            if (key == keyString) {
                                manager.cancel(notification.id)
                                break
                            }
                        }
                    }
                }

                it.remove()
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.O)
    fun createNotificationChannel(context: Context, conversation: Conversation) {
        if (!AndroidVersionUtil.isAndroidO) {
            return
        }

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = createChannel(context, conversation)
        manager.createNotificationChannel(channel)
    }

    @TargetApi(Build.VERSION_CODES.O)
    fun createDefaultChannel(context: Context) {
        if (!AndroidVersionUtil.isAndroidO) {
            return
        }

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val defaultChannel = NotificationChannel(DEFAULT_CONVERSATION_CHANNEL_ID,
                context.getString(R.string.default_notifications_channel),
                if (Settings.headsUp) NotificationManager.IMPORTANCE_HIGH else NotificationManager.IMPORTANCE_DEFAULT)
        defaultChannel.description = context.getString(R.string.default_notifications_channel_description)
        defaultChannel.group = "conversations"
        manager.createNotificationChannel(defaultChannel)
    }

    @TargetApi(Build.VERSION_CODES.O)
    fun createTestChannel(context: Context) {
        if (!AndroidVersionUtil.isAndroidO) {
            return
        }

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val testChannel = NotificationChannel(TEST_NOTIFICATIONS_CHANNEL_ID,
                context.getString(R.string.test_notifications_channel),
                if (Settings.headsUp) NotificationManager.IMPORTANCE_HIGH else NotificationManager.IMPORTANCE_DEFAULT)
        manager.createNotificationChannel(testChannel)
    }

    @TargetApi(Build.VERSION_CODES.O)
    private fun createStatusChannel(context: Context) {
        if (!AndroidVersionUtil.isAndroidO) {
            return
        }

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val statusChannel = NotificationChannel(STATUS_NOTIFICATIONS_CHANNEL_ID,
                context.getString(R.string.status_notifications_channel), NotificationManager.IMPORTANCE_DEFAULT)
        manager.createNotificationChannel(statusChannel)
    }

    @TargetApi(Build.VERSION_CODES.O)
    fun createBackgroundServiceChannel(context: Context) {
        if (!AndroidVersionUtil.isAndroidO) {
            return
        }

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val statusChannel = NotificationChannel(BACKGROUND_SERVICE_CHANNEL_ID,
                context.getString(R.string.background_service_channel), NotificationManager.IMPORTANCE_MIN)
        manager.createNotificationChannel(statusChannel)
    }

    @TargetApi(Build.VERSION_CODES.O)
    private fun createMediaParseChannel(context: Context) {
        if (!AndroidVersionUtil.isAndroidO) {
            return
        }

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val statusChannel = NotificationChannel(MEDIA_PARSE_CHANNEL_ID,
                context.getString(R.string.status_notifications_channel), NotificationManager.IMPORTANCE_MIN)
        manager.createNotificationChannel(statusChannel)
    }

    @TargetApi(Build.VERSION_CODES.O)
    private fun createGeneralChannel(context: Context) {
        if (!AndroidVersionUtil.isAndroidO) {
            return
        }

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val statusChannel = NotificationChannel(GENERAL_CHANNEL_ID,
                context.getString(R.string.general_notifications_channel), NotificationManager.IMPORTANCE_MIN)
        manager.createNotificationChannel(statusChannel)
    }

    @TargetApi(Build.VERSION_CODES.O)
    private fun createFailedMessageChannel(context: Context) {
        if (!AndroidVersionUtil.isAndroidO) {
            return
        }

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val testChannel = NotificationChannel(FAILED_MESSAGES_CHANNEL_ID,
                context.getString(R.string.failed_messages_channel),
                if (Settings.headsUp) NotificationManager.IMPORTANCE_HIGH else NotificationManager.IMPORTANCE_DEFAULT)
        manager.createNotificationChannel(testChannel)
    }

    @TargetApi(Build.VERSION_CODES.O)
    private fun createQuickTextChannel(context: Context) {
        if (!AndroidVersionUtil.isAndroidO) {
            return
        }

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val testChannel = NotificationChannel(QUICK_TEXT_CHANNEL_ID,
                context.getString(R.string.quick_text_channel), NotificationManager.IMPORTANCE_MIN)
        testChannel.setShowBadge(false)
        testChannel.enableLights(false)
        testChannel.enableVibration(false)
        manager.createNotificationChannel(testChannel)
    }

    @TargetApi(Build.VERSION_CODES.O)
    private fun createMessageGroupChannel(context: Context) {
        if (!AndroidVersionUtil.isAndroidO) {
            return
        }

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val messageGroupChannel = NotificationChannel(MESSAGE_GROUP_SUMMARY_CHANNEL_ID,
                context.getString(R.string.group_summary_notifications), NotificationManager.IMPORTANCE_LOW)
        manager.createNotificationChannel(messageGroupChannel)
    }

    @TargetApi(Build.VERSION_CODES.O)
    fun createNotificationChannels(context: Context) {
        if (!AndroidVersionUtil.isAndroidO) {
            return
        }

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // notification channel group for conversations
        val conversationsGroup = NotificationChannelGroup("conversations",
                context.getString(R.string.conversations))
        manager.createNotificationChannelGroup(conversationsGroup)

        // channels to place the notifications in
        createDefaultChannel(context)
        createTestChannel(context)
        createStatusChannel(context)
        createBackgroundServiceChannel(context)
        createFailedMessageChannel(context)
        createMessageGroupChannel(context)
        createMediaParseChannel(context)
        createQuickTextChannel(context)
        createGeneralChannel(context)
    }

    @TargetApi(Build.VERSION_CODES.O)
    private fun createChannel(context: Context, conversation: Conversation): NotificationChannel {
        val settings = Settings

        val channel = NotificationChannel(conversation.id.toString() + "", conversation.title,
                if (settings.headsUp) NotificationManager.IMPORTANCE_HIGH else NotificationManager.IMPORTANCE_DEFAULT)
        channel.group = "conversations"
        channel.enableLights(true)
        channel.lightColor = conversation.ledColor
        channel.setBypassDnd(false)
        channel.setShowBadge(true)
        channel.vibrationPattern = settings.vibrate.pattern
        channel.lockscreenVisibility = if (conversation.privateNotifications)
            Notification.VISIBILITY_PRIVATE
        else
            Notification.VISIBILITY_PUBLIC

        val ringtone = NotificationRingtoneProvider(context).getRingtone(conversation.ringtoneUri)
        if (ringtone != null) {
            channel.setSound(ringtone, AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION).build())
        }

        return channel
    }

    @TargetApi(Build.VERSION_CODES.O)
    fun deleteChannel(context: Context, conversationId: Long) {
        if (!AndroidVersionUtil.isAndroidO) {
            return
        }

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.deleteNotificationChannel(conversationId.toString() + "")
    }

    @TargetApi(Build.VERSION_CODES.O)
    fun deleteAllChannels(context: Context) {
        if (!AndroidVersionUtil.isAndroidO) {
            return
        }

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channels = manager.notificationChannels
        for (channel in channels) {
            manager.deleteNotificationChannel(channel.id)
        }
    }

}
