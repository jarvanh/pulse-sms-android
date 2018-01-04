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
import com.firebase.jobdispatcher.*
import xyz.klinker.messenger.api.implementation.Account
import xyz.klinker.messenger.shared.R
import xyz.klinker.messenger.shared.data.ColorSet
import xyz.klinker.messenger.shared.data.DataSource
import xyz.klinker.messenger.shared.data.model.Message
import xyz.klinker.messenger.shared.data.model.ScheduledMessage
import xyz.klinker.messenger.shared.util.*
import java.util.*

/**
 * Service responsible for sending scheduled message that are coming up, removing that message
 * from the database and then scheduling the next one.
 */
class ScheduledMessageJob : SimpleJobService() {

    override fun onRunJob(job: JobParameters?): Int {
        val source = DataSource
        val messages = source.getScheduledMessages(this)

        if (messages.moveToFirst()) {
            do {
                val timestamp = messages.getLong(
                        messages.getColumnIndex(ScheduledMessage.COLUMN_TIMESTAMP))

                // if message scheduled to be sent in less than 5 mins in the future,
                // or more than 60 in the past
                if (timestamp > System.currentTimeMillis() - TimeUtils.HOUR && timestamp < System.currentTimeMillis() + TimeUtils.MINUTE * 5) {
                    val message = ScheduledMessage()
                    message.fillFromCursor(messages)

                    // delete, insert and send
                    source.deleteScheduledMessage(this, message.id)
                    val conversationId = source.insertSentMessage(message.to!!, message.data!!, message.mimeType!!, this)
                    val conversation = source.getConversation(this, conversationId)

                    SendUtils(conversation?.simSubscriptionId)
                            .send(this, message.data!!, message.to!!)

                    // display a notification
                    val body = "<b>" + message.title + ": </b>" + message.data
                    val open = ActivityUtils.buildForComponent(ActivityUtils.MESSENGER_ACTIVITY)
                    open.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    val pendingOpen = PendingIntent.getActivity(this, 0, open,
                            PendingIntent.FLAG_UPDATE_CURRENT)

                    val notification = NotificationCompat.Builder(this)
                            .setSmallIcon(R.drawable.ic_stat_notify)
                            .setContentTitle(getString(R.string.scheduled_message_sent))
                            .setContentText(Html.fromHtml(body))
                            .setColor(ColorSet.DEFAULT(this).color)
                            .setAutoCancel(true)
                            .setContentIntent(pendingOpen)
                            .build()
                    NotificationManagerCompat.from(this)
                            .notify(5555 + message.id.toInt(), notification)

                    try {
                        val conversationMessages = DataSource.getMessages(this, conversationId)

                        if (conversationMessages.moveToFirst()) {
                            val mess = Message()
                            mess.fillFromCursor(conversationMessages)
                            if (mess.type == Message.TYPE_INFO) {
                                DataSource.deleteMessage(this, mess.id)
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

                    source.deleteScheduledMessage(this, message.id)
                }
            } while (messages.moveToNext())
        }

        messages.closeSilent()

        sendBroadcast(Intent(BROADCAST_SCHEDULED_SENT))
        scheduleNextRun(this)

        return 0
    }

    companion object {

        val BROADCAST_SCHEDULED_SENT = "xyz.klinker.messenger.SENT_SCHEDULED_MESSAGE"
        private val JOB_ID = "scheduled-message-job"

        fun scheduleNextRun(context: Context, source: DataSource = DataSource) {
            val account = Account
            if (account.exists() && !account.primary) {
                // if they have an online account, we only want scheduled messages to go through the phone
                return
            }

            val messages = source.getScheduledMessagesAsList(context)
                    .sortedBy { it.timestamp }

            if (messages.isNotEmpty()) {
                var timeout = (messages[0].timestamp - Date().time).toInt() / 1000
                if (timeout < 0) {
                    timeout = 0
                }

                val dispatcher = FirebaseJobDispatcher(GooglePlayDriver(context))
                val myJob = dispatcher.newJobBuilder()
                        .setService(ScheduledMessageJob::class.java)
                        .setTag(JOB_ID)
                        .setRecurring(false)
                        .setLifetime(Lifetime.FOREVER)
                        .setTrigger(Trigger.executionWindow(timeout, timeout + (TimeUtils.MINUTE.toInt() / 1000)))
                        .setReplaceCurrent(true)
                        .build()

                dispatcher.mustSchedule(myJob)

                Log.v("scheduled message", "new message scheduled")
            } else {
                Log.v("scheduled message", "no more scheduled messages")
            }
        }
    }
}
