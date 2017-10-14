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
import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.content.ComponentName
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
import xyz.klinker.messenger.shared.data.model.ScheduledMessage
import xyz.klinker.messenger.shared.util.*
import java.util.*

/**
 * Service responsible for sending scheduled message that are coming up, removing that message
 * from the database and then scheduling the next one.
 */
class ScheduledMessageJob : BackgroundJob() {

    override fun onRunJob(parameters: JobParameters?) {
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

                    Log.v("scheduled message", "message was sent and notification given")
                }
            } while (messages.moveToNext())
        }

        messages.closeSilent()

        sendBroadcast(Intent(BROADCAST_SCHEDULED_SENT))
        scheduleNextRun(this)
    }

    companion object {

        val BROADCAST_SCHEDULED_SENT = "xyz.klinker.messenger.SENT_SCHEDULED_MESSAGE"
        private val JOB_ID = 5424

        @JvmOverloads
        fun scheduleNextRun(context: Context, source: DataSource = DataSource) {
            val account = Account
            if (account.exists() && !account.primary) {
                // if they have an online account, we only want scheduled messages to go through the phone
                return
            }

            val messages = source.getScheduledMessagesAsList(context)
            if (messages.isNotEmpty()) {
                val timeout = messages[0].timestamp - Date().time

                val component = ComponentName(context, ScheduledMessageJob::class.java)
                val builder = JobInfo.Builder(JOB_ID, component)
                        .setMinimumLatency(timeout)
                        .setOverrideDeadline(timeout + TimeUtils.MINUTE)
                        .setRequiresCharging(false)
                        .setRequiresDeviceIdle(false)

                val jobScheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
                jobScheduler.schedule(builder.build())

                Log.v("scheduled message", "new message scheduled")
            } else {
                Log.v("scheduled message", "no more scheduled messages")
            }
        }
    }
}
