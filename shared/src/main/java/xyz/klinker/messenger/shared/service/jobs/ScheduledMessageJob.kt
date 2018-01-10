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
import android.support.v4.app.NotificationCompat
import android.support.v4.app.NotificationManagerCompat
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
import android.os.Build
import android.os.Build.VERSION.SDK_INT

/**
 * Service responsible for sending scheduled message that are coming up, removing that message
 * from the database and then scheduling the next one.
 */
class ScheduledMessageJob : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null) {
            return
        }

        val source = DataSource
        val messages = source.getScheduledMessages(context)

        if (messages.moveToFirst()) {
            do {
                val timestamp = messages.getLong(
                        messages.getColumnIndex(ScheduledMessage.COLUMN_TIMESTAMP))

                // if message scheduled to be sent in less than a half hour in the future,
                // or more than 60 in the past
                if (timestamp > System.currentTimeMillis() - TimeUtils.DAY && timestamp < System.currentTimeMillis() + TimeUtils.HOUR / 2) {
                    val message = ScheduledMessage()
                    message.fillFromCursor(messages)

                    // delete, insert and send
                    source.deleteScheduledMessage(context, message.id)
                    val conversationId = source.insertSentMessage(message.to!!, message.data!!, message.mimeType!!, context)
                    val conversation = source.getConversation(context, conversationId)

                    SendUtils(conversation?.simSubscriptionId)
                            .send(context, message.data!!, message.to!!)

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
                } else if (timestamp < System.currentTimeMillis() - TimeUtils.HOUR) {
                    val message = ScheduledMessage()
                    message.fillFromCursor(messages)

                    source.deleteScheduledMessage(context, message.id)
                }
            } while (messages.moveToNext())
        }

        messages.closeSilent()

        context.sendBroadcast(Intent(BROADCAST_SCHEDULED_SENT))
        ScheduledMessageJob.scheduleNextRun(context)
    }

    companion object {

        val BROADCAST_SCHEDULED_SENT = "xyz.klinker.messenger.SENT_SCHEDULED_MESSAGE"

        fun scheduleNextRun(context: Context, source: DataSource = DataSource) {
            val account = Account
            if (account.exists() && !account.primary) {
                // if they have an online account, we only want scheduled messages to go through the phone
                return
            }

            val messages = source.getScheduledMessagesAsList(context)
                    .sortedBy { it.timestamp }

            if (messages.isNotEmpty()) {
                val intent = Intent(context, ScheduledMessageJob::class.java)
                val pendingIntent = PendingIntent.getBroadcast(context, 0, intent, 0)
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
