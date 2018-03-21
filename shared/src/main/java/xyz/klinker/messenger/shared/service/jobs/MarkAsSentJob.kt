package xyz.klinker.messenger.shared.service.jobs

import android.content.Context
import android.util.Log
import com.firebase.jobdispatcher.*
import xyz.klinker.messenger.api.implementation.Account

import xyz.klinker.messenger.shared.data.DataSource
import xyz.klinker.messenger.shared.data.model.Message
import xyz.klinker.messenger.shared.receiver.MessageListUpdatedReceiver
import xyz.klinker.messenger.shared.util.TimeUtils

/**
 * Some devices don't seem to ever get messages marked as sent and I don't really know why.
 * This job can be started to mark old messages that are "sending" as sent.
 */
class MarkAsSentJob : SimpleJobService() {

    override fun onRunJob(job: JobParameters?): Int {
        if (Account.exists() && !Account.primary) {
            return 0
        }

        val messages = DataSource.getNewerSendingMessagesAsList(
                this, TimeUtils.now - TimeUtils.MINUTE * 5)

        for (message in messages) {
            Log.v("MarkAsSentJob", "marking as read: " + message.id)

            DataSource.updateMessageType(this, message.id, Message.TYPE_SENT)
            MessageListUpdatedReceiver.sendBroadcast(this, message.conversationId)
        }

        return 0
    }

    companion object {

        private val JOB_ID = "mark-as-sent"
        private val MESSAGE_SENDING_TIMEOUT = TimeUtils.MINUTE.toInt() / 1000

        fun scheduleNextRun(context: Context?, messageId: Long?) {
            if (context == null || (Account.exists() && !Account.primary) || messageId == null) {
                return
            }

            val dispatcher = FirebaseJobDispatcher(GooglePlayDriver(context))
            val myJob = dispatcher.newJobBuilder()
                    .setService(MarkAsSentJob::class.java)
                    .setTag(JOB_ID)
                    .setRecurring(false)
                    .setLifetime(Lifetime.FOREVER)
                    .setTrigger(Trigger.executionWindow(MESSAGE_SENDING_TIMEOUT / 2, MESSAGE_SENDING_TIMEOUT))
                    .setReplaceCurrent(true)
                    .build()

            dispatcher.mustSchedule(myJob)
        }
    }
}
