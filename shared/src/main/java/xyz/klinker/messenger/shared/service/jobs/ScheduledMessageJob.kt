/*
 * Copyright (C) 2017 Luke Klinker
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package xyz.klinker.messenger.shared.service.jobs

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import android.text.Html
import android.util.Log
import xyz.klinker.messenger.api.implementation.Account
import xyz.klinker.messenger.shared.R
import xyz.klinker.messenger.shared.data.ColorSet
import xyz.klinker.messenger.shared.data.DataSource
import xyz.klinker.messenger.shared.data.model.Message
import xyz.klinker.messenger.shared.data.model.ScheduledMessage
import xyz.klinker.messenger.shared.util.*
import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.net.Uri
import android.os.Build
import android.os.Build.VERSION.SDK_INT
import xyz.klinker.messenger.shared.data.MimeType
import java.util.*

/**
 * Service responsible for sending scheduled message that are coming up, removing that message
 * from the database and then scheduling the next one.
 */
class ScheduledMessageJob : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || TimeUtils.now - ScheduledMessageJob.lastRun < (20 * TimeUtils.SECOND)) {
            return
        } else {
            ScheduledMessageJob.lastRun = TimeUtils.now
        }

        val source = DataSource
        val messages = source.getScheduledMessages(context)

        if (messages.moveToFirst()) {
            do {
                val timestamp = messages.getLong(
                        messages.getColumnIndex(ScheduledMessage.COLUMN_TIMESTAMP))

                // if message scheduled to be sent in less than a half hour in the future,
                // or more than 60 in the past
                if (timestamp > TimeUtils.now - TimeUtils.DAY && timestamp < TimeUtils.now + TimeUtils.MINUTE * 15) {
                    val message = ScheduledMessage()
                    message.fillFromCursor(messages)

                    // delete, insert and send
                    source.deleteScheduledMessage(context, message.id)
                    handleRepeat(context, message)

                    val conversationId = source.insertSentMessage(message.to!!, message.data!!, message.mimeType!!, context)
                    val conversation = source.getConversation(context, conversationId)

                    if (message.mimeType == MimeType.TEXT_PLAIN) {
                        SendUtils(conversation?.simSubscriptionId).send(context, message.data!!, message.to!!)
                    } else {
                        SendUtils(conversation?.simSubscriptionId).send(context, "", message.to!!, Uri.parse(message.data!!), message.mimeType!!)
                    }

                    // display a notification
                    val body = "<b>" + message.title + ": </b>" + message.data
                    val open = ActivityUtils.buildForComponent(ActivityUtils.MESSENGER_ACTIVITY)
                    open.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    val pendingOpen = PendingIntent.getActivity(context, 0, open,
                            PendingIntent.FLAG_UPDATE_CURRENT)

                    val notification = NotificationCompat.Builder(context)
                            .setSmallIcon(R.drawable.ic_stat_notify)
                            .setContentTitle(context.getString(R.string.scheduled_message_sent))
                            .setContentText(Html.fromHtml(body))
                            .setColor(ColorSet.DEFAULT(context).color)
                            .setAutoCancel(true)
                            .setContentIntent(pendingOpen)
                            .build()
                    NotificationManagerCompat.from(context)
                            .notify(5555 + message.id.toInt(), notification)

                    try {
                        // delete the INFO message, if applicable.
                        val conversationMessages = DataSource.getMessages(context, conversationId)

                        if (conversationMessages.moveToFirst()) {
                            val mess = Message()
                            mess.fillFromCursor(conversationMessages)
                            if (mess.type == Message.TYPE_INFO) {
                                DataSource.deleteMessage(context, mess.id)
                            }
                        }

                        CursorUtil.closeSilent(conversationMessages)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                    Log.v("scheduled message", "message was sent and notification given")
                } else if (timestamp < TimeUtils.now - TimeUtils.HOUR) {
                    val message = ScheduledMessage()
                    message.fillFromCursor(messages)

                    source.deleteScheduledMessage(context, message.id)
                    handleRepeat(context, message)
                }
            } while (messages.moveToNext())
        }

        messages.closeSilent()

        context.sendBroadcast(Intent(BROADCAST_SCHEDULED_SENT))
        ScheduledMessageJob.scheduleNextRun(context)
    }

    private fun handleRepeat(context: Context, message: ScheduledMessage) {
        if (message.repeat == ScheduledMessage.REPEAT_NEVER) {
            return
        }

        when (message.repeat) {
            ScheduledMessage.REPEAT_DAILY -> message.timestamp += TimeUtils.DAY
            ScheduledMessage.REPEAT_WEEKLY -> message.timestamp += TimeUtils.WEEK
            ScheduledMessage.REPEAT_MONTHLY -> {
                val calendar = Calendar.getInstance()
                calendar.timeInMillis = message.timestamp
                calendar.set(Calendar.MONTH, calendar.get(Calendar.MONTH) + 1)

                message.timestamp = calendar.timeInMillis
            }
            ScheduledMessage.REPEAT_YEARLY -> {
                val calendar = Calendar.getInstance()
                calendar.timeInMillis = message.timestamp
                calendar.set(Calendar.YEAR, calendar.get(Calendar.YEAR) + 1)

                message.timestamp = calendar.timeInMillis
            }
        }

        DataSource.insertScheduledMessage(context, message, true)
    }

    companion object {

        private var lastRun = 0L
        const val BROADCAST_SCHEDULED_SENT = "xyz.klinker.messenger.SENT_SCHEDULED_MESSAGE"

        fun scheduleNextRun(context: Context, source: DataSource = DataSource) {
            val account = Account
            if (account.exists() && !account.primary) {
                // if they have an online account, we only want scheduled messages to go through the phone
                return
            }

            val messages = source.getScheduledMessagesAsList(context).sortedBy { it.timestamp }
            if (messages.isNotEmpty()) {
                val intent = Intent(context, ScheduledMessageJob::class.java)
                val pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
                setAlarm(context, messages[0].timestamp, pendingIntent)

                Log.v("scheduled message", "new message scheduled")
            } else {
                Log.v("scheduled message", "no more scheduled messages")
            }
        }

        private fun setAlarm(context: Context, time: Long, pendingIntent: PendingIntent) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.cancel(pendingIntent)

            if (Build.VERSION_CODES.KITKAT <= SDK_INT && SDK_INT < Build.VERSION_CODES.M) {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, time, pendingIntent)
            } else if (SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time, pendingIntent)
            }
        }
    }
}
