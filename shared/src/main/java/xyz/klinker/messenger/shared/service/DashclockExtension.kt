package xyz.klinker.messenger.shared.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import com.google.android.apps.dashclock.api.DashClockExtension
import com.google.android.apps.dashclock.api.ExtensionData
import xyz.klinker.messenger.shared.R
import xyz.klinker.messenger.shared.data.DataSource
import xyz.klinker.messenger.shared.data.model.Conversation
import xyz.klinker.messenger.shared.util.ActivityUtils
import xyz.klinker.messenger.shared.widget.MessengerAppWidgetProvider

// similar to:
// https://github.com/romannurik/dashclock/blob/master/main/src/main/java/com/google/android/apps/dashclock/phone/SmsExtension.java
class DashclockExtension : DashClockExtension() {

    var update: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            onUpdateData(0)
        }
    }

    public override fun onInitialize(isReconnect: Boolean) {
        super.onInitialize(isReconnect)

        try {
            unregisterReceiver(update)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val filter = IntentFilter()
        filter.addAction(MessengerAppWidgetProvider.REFRESH_ACTION)
        registerReceiver(update, filter)
    }

    override fun onUpdateData(reason: Int) {
        val conversations = DataSource.getUnreadConversationsAsList(this)
        publishUpdate(ExtensionData()
                .visible(conversations.isNotEmpty())
                .icon(R.drawable.ic_stat_notify_group)
                .status(getStatus(conversations))
                .expandedTitle(getExpandedStatus(conversations))
                .expandedBody(getBody(conversations))
                .clickIntent(getIntent(conversations)))
    }

    private fun getStatus(conversations: List<Conversation>): String {
        return resources.getQuantityString(R.plurals.new_conversations,
                conversations.size, conversations.size)
    }

    private fun getExpandedStatus(conversations: List<Conversation>): String? {
        return if (conversations.size == 1) {
            conversations[0].title
        } else {
            resources.getQuantityString(R.plurals.new_conversations,
                    conversations.size, conversations.size)
        }
    }

    private fun getBody(conversations: List<Conversation>) = when {
        conversations.size == 1 -> conversations[0].snippet
        conversations.size > 1 -> {
            val builder = StringBuilder(conversations[0].title)
            for (i in 1 until conversations.size) {
                builder.append(", ").append(conversations[i].title)
            }
            builder.toString()
        }
        else -> ""
    }

    private fun getIntent(conversations: List<Conversation>): Intent {
        val intent = ActivityUtils.buildForComponent(ActivityUtils.MESSENGER_ACTIVITY)

        if (conversations.size == 1) {
            intent.data = Uri.parse("https://messenger.klinkerapps.com/" + conversations[0].id)
        }

        return intent
    }
}