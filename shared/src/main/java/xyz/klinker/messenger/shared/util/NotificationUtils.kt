package xyz.klinker.messenger.shared.util

import android.annotation.TargetApi
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationChannelGroup
import android.app.NotificationManager
import android.content.Context
import android.graphics.Color
import android.os.Build
import xyz.klinker.messenger.shared.R
import xyz.klinker.messenger.shared.data.DataSource
import xyz.klinker.messenger.shared.data.Settings
import xyz.klinker.messenger.shared.data.model.Conversation

object NotificationUtils {

    const val DEFAULT_CONVERSATION_CHANNEL_ID = "default-conversation-channel"
    const val SILENT_CONVERSATION_CHANNEL_ID = "silent-conversation-channel"
    const val QUICK_TEXT_CHANNEL_ID = "quick-text"
    const val SILENT_BACKGROUND_CHANNEL_ID = "silent-background-services"
    const val ACCOUNT_ACTIVITY_CHANNEL_ID = "account-activity-channel"

    fun cancelGroupedNotificationWithNoContent(context: Context?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && context != null) {
            Thread {
                try {
                    Thread.sleep(100)
                } catch (e: Exception) {
                }

                val cursor = DataSource.getUnseenMessages(context)
                if (cursor.count == 0) {
                    // all messages are seen
                    val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

                    try {
                        manager.cancelAll()
                    } catch (e: SecurityException) {
                        // not the right permissions. Changed user account maybe
                    }
                }

                cursor.closeSilent()
            }.start()
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
    private fun createDefaultChannel(context: Context) {
        if (!AndroidVersionUtil.isAndroidO) {
            return
        }

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val defaultChannel = NotificationChannel(DEFAULT_CONVERSATION_CHANNEL_ID,
                context.getString(R.string.default_notifications_channel), NotificationManager.IMPORTANCE_HIGH)
        defaultChannel.description = context.getString(R.string.default_notifications_channel_description)
        defaultChannel.group = "conversations"
        defaultChannel.lightColor = Color.WHITE
        defaultChannel.enableLights(true)
        defaultChannel.setBypassDnd(false)
        defaultChannel.setShowBadge(true)
        defaultChannel.enableVibration(true)
        manager.createNotificationChannel(defaultChannel)
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
    private fun createSilentConversationChannel(context: Context) {
        if (!AndroidVersionUtil.isAndroidO) {
            return
        }

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val silentConversations = NotificationChannel(SILENT_CONVERSATION_CHANNEL_ID,
                context.getString(R.string.silent_conversations_channel), NotificationManager.IMPORTANCE_LOW)
        silentConversations.group = "conversations"
        silentConversations.setShowBadge(true)
        silentConversations.enableLights(true)
        silentConversations.enableVibration(false)
        manager.createNotificationChannel(silentConversations)
    }

    @TargetApi(Build.VERSION_CODES.O)
    private fun createSilentBackgroundChannel(context: Context) {
        if (!AndroidVersionUtil.isAndroidO) {
            return
        }

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val silentBackground = NotificationChannel(SILENT_BACKGROUND_CHANNEL_ID,
                context.getString(R.string.silent_background_services), NotificationManager.IMPORTANCE_MIN)
        silentBackground.setShowBadge(false)
        silentBackground.enableLights(false)
        silentBackground.enableVibration(false)
        manager.createNotificationChannel(silentBackground)
    }

    @TargetApi(Build.VERSION_CODES.O)
    private fun createAccountActivityChannel(context: Context) {
        if (!AndroidVersionUtil.isAndroidO) {
            return
        }

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val silentBackground = NotificationChannel(ACCOUNT_ACTIVITY_CHANNEL_ID,
                context.getString(R.string.account_activity_notifications), NotificationManager.IMPORTANCE_LOW)
        silentBackground.setShowBadge(false)
        silentBackground.enableLights(false)
        silentBackground.enableVibration(false)
        manager.createNotificationChannel(silentBackground)
    }

    @TargetApi(Build.VERSION_CODES.O)
    fun createNotificationChannels(context: Context) {
        if (!AndroidVersionUtil.isAndroidO) {
            return
        }

        deleteOldChannels(context)

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // notification channel group for conversations
        val conversationsGroup = NotificationChannelGroup("conversations", context.getString(R.string.conversations))
        manager.createNotificationChannelGroup(conversationsGroup)

        // channels to place the notifications in
        createDefaultChannel(context)
        createQuickTextChannel(context)
        createSilentConversationChannel(context)
        createSilentBackgroundChannel(context)
        createAccountActivityChannel(context)
    }

    @TargetApi(Build.VERSION_CODES.O)
    private fun createChannel(context: Context, conversation: Conversation): NotificationChannel {
        val settings = Settings

        val channel = NotificationChannel(conversation.id.toString() + "", conversation.title, NotificationManager.IMPORTANCE_HIGH)
        channel.group = "conversations"
        channel.enableLights(true)
        channel.lightColor = Color.WHITE
        channel.setBypassDnd(false)
        channel.setShowBadge(true)
        channel.enableVibration(true)
        channel.lockscreenVisibility = if (conversation.private)
            Notification.VISIBILITY_PRIVATE else Notification.VISIBILITY_PUBLIC

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
    fun deleteOldChannels(context: Context) {
        if (!AndroidVersionUtil.isAndroidO) {
            return
        }

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.deleteNotificationChannel("background-service")
        manager.deleteNotificationChannel("general")
        manager.deleteNotificationChannel("media-parsing")
        manager.deleteNotificationChannel("status-notifications")
        manager.deleteNotificationChannel("test-notifications")
        manager.deleteNotificationChannel("failed-messages")
        manager.deleteNotificationChannel("message-group-summary")
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
