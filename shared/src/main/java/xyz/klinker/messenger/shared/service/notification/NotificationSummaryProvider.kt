package xyz.klinker.messenger.shared.service.notification

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import android.text.Html
import xyz.klinker.messenger.shared.R
import xyz.klinker.messenger.shared.data.Settings
import xyz.klinker.messenger.shared.data.pojo.NotificationConversation
import xyz.klinker.messenger.shared.service.NotificationDismissedService
import xyz.klinker.messenger.shared.util.ActivityUtils
import xyz.klinker.messenger.shared.util.NotificationUtils

/**
 * Displays a summary notification for all conversations using the rows returned by each
 * individual notification.
 */
class NotificationSummaryProvider(private val service: Context) {

    var skipSummary = false

    fun giveSummaryNotification(conversations: List<NotificationConversation>, rows: List<String>) {
        if (skipSummary) {
            return
        }

        val title = service.resources.getQuantityString(R.plurals.new_conversations, conversations.size, conversations.size)
        val summary = buildSummary(conversations)
        val style = buildStyle(rows)
                .setBigContentTitle(title)
                .setSummaryText(summary)

        val notification = buildNotification(conversations[0].id, title, summary)
                .setPublicVersion(buildPublicNotification(conversations[0].id, title).build())
                .setWhen(conversations[conversations.size - 1].timestamp)
                .setStyle(style)

        NotificationManagerCompat.from(service).notify(NotificationConstants.SUMMARY_ID, applyPendingIntents(notification).build())
    }

    private fun buildSummary(conversations: List<NotificationConversation>): String {
        val summaryBuilder = StringBuilder()
        for (i in conversations.indices) {
            if (conversations[i].privateNotification) {
                summaryBuilder.append(service.getString(R.string.new_message))
            } else {
                summaryBuilder.append(conversations[i].title)
            }

            summaryBuilder.append(", ")
        }

        var summary = summaryBuilder.toString()
        if (summary.endsWith(", ")) {
            summary = summary.substring(0, summary.length - 2)
        }

        return summary
    }

    private fun buildStyle(rows: List<String>): NotificationCompat.InboxStyle {
        val style = NotificationCompat.InboxStyle()

        for (row in rows) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    style.addLine(Html.fromHtml(row, 0))
                } else {
                    style.addLine(Html.fromHtml(row))
                }
            } catch (t: Throwable) {
                // there was a motorola device running api 24, but was on 6.0.1? WTF?
                // so catch the throwable instead of checking the api version
                style.addLine(Html.fromHtml(row))
            }
        }

        return style
    }

    private fun buildNotification(firstConversationId: Long, title: String, summary: String) =
            buildCommonNotification(firstConversationId, title)
                    .setContentText(summary)
                    .setShowWhen(true)
                    .setTicker(title)
                    .setVisibility(Notification.VISIBILITY_PRIVATE)

    private fun buildPublicNotification(firstConversationId: Long, title: String) =
            buildCommonNotification(firstConversationId, title)
                    .setVisibility(Notification.VISIBILITY_PUBLIC)

    private fun buildCommonNotification(firstConversationId: Long, title: String) = NotificationCompat.Builder(service,
                NotificationUtils.SILENT_CONVERSATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_notify_group)
            .setContentTitle(title)
            .setGroup(NotificationConstants.GROUP_KEY_MESSAGES)
            .setGroupSummary(true)
            .setAutoCancel(true)
            .setCategory(Notification.CATEGORY_MESSAGE)
            .setColor(Settings.mainColorSet.color)
            .setPriority(if (Settings.headsUp) Notification.PRIORITY_MAX else Notification.PRIORITY_DEFAULT)


    private fun applyPendingIntents(builder: NotificationCompat.Builder): NotificationCompat.Builder {
        val delete = Intent(service, NotificationDismissedService::class.java)
        val pendingDelete = PendingIntent.getService(service, 0,
                delete, PendingIntent.FLAG_UPDATE_CURRENT)

        val open = ActivityUtils.buildForComponent(ActivityUtils.MESSENGER_ACTIVITY)
        open.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        val pendingOpen = PendingIntent.getActivity(service, 0,
                open, PendingIntent.FLAG_UPDATE_CURRENT)

        builder.setDeleteIntent(pendingDelete)
        builder.setContentIntent(pendingOpen)

        return builder
    }
}