package xyz.klinker.messenger.shared.service.jobs

import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import android.os.PersistableBundle
import android.util.Log
import xyz.klinker.messenger.api.implementation.Account

import xyz.klinker.messenger.shared.data.DataSource
import xyz.klinker.messenger.shared.data.model.Message
import xyz.klinker.messenger.shared.receiver.MessageListUpdatedReceiver
import xyz.klinker.messenger.shared.util.TimeUtils
import java.util.*

/**
 * Some devices don't seem to ever get messages marked as sent and I don't really know why.
 * This job can be started to mark old messages that are "sending" as sent.
 */
class MarkAsSentJob : BackgroundJob() {

    override fun onRunJob(parameters: JobParameters?) {
        val messages = DataSource.getNewerSendingMessagesAsList(
                this,
                System.currentTimeMillis() - TimeUtils.MINUTE * 5)

        for (message in messages) {
            Log.v("MarkAsSentJob", "marking as read: " + message.id)

            DataSource.updateMessageType(this, message.id, Message.TYPE_SENT)
            MessageListUpdatedReceiver.sendBroadcast(this, message.conversationId)
        }
    }

    companion object {

        private val JOB_ID = 9
        private val MESSAGE_SENDING_TIMEOUT = TimeUtils.MINUTE
        private val EXTRA_MESSAGE_ID = "extra_message_id"

        fun scheduleNextRun(context: Context?, messageId: Long?) {
            if (context == null || (Account.exists() && !Account.primary) || messageId == null) {
                return
            }

            val bundle = PersistableBundle()
            bundle.putLong(EXTRA_MESSAGE_ID, messageId)

            val component = ComponentName(context, MarkAsSentJob::class.java)
            val builder = JobInfo.Builder(JOB_ID, component)
                    .setMinimumLatency(MESSAGE_SENDING_TIMEOUT / 2)
                    .setOverrideDeadline(MESSAGE_SENDING_TIMEOUT)
                    .setExtras(bundle)
                    .setPersisted(true)
                    .setRequiresCharging(false)
                    .setRequiresDeviceIdle(false)

            val jobScheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
            jobScheduler.cancel(JOB_ID)
            jobScheduler.schedule(builder.build())
        }
    }
}
