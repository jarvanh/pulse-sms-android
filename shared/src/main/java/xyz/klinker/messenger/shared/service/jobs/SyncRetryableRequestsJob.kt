package xyz.klinker.messenger.shared.service.jobs

import android.content.Context
import android.util.Log
import com.firebase.jobdispatcher.*
import xyz.klinker.messenger.api.implementation.Account
import xyz.klinker.messenger.api.implementation.ApiUtils

import xyz.klinker.messenger.shared.data.DataSource
import xyz.klinker.messenger.shared.data.FeatureFlags
import xyz.klinker.messenger.shared.data.model.Conversation
import xyz.klinker.messenger.shared.data.model.Message
import xyz.klinker.messenger.shared.data.model.RetryableRequest
import xyz.klinker.messenger.shared.receiver.MessageListUpdatedReceiver
import xyz.klinker.messenger.shared.util.TimeUtils

/**
 * If some requests fail, they get written in to the retryable_requests table to get retried when the
 * device regains connectivity. This service should read that table and execute any request that are pending.
 *
 * It should be set up to run periodically, but only when the phone has a connection. With the way
 * FirebaseJobDispatcher works, this should force it to run whenever the user goes from a loss in connectivity
 * to regaining connectivity, or shortly after.
 */
class SyncRetryableRequestsJob : SimpleJobService() {

    override fun onRunJob(job: JobParameters?): Int {
        val retryables = DataSource.getRetryableRequestsAsList(this)
        DataSource.deleteAllRetryableRequest(this)

        for (retryable in retryables) {
            when (retryable.type) {
                RetryableRequest.TYPE_ADD_MESSAGE -> pushMessage(DataSource.getMessage(this, retryable.dataId))
                RetryableRequest.TYPE_ADD_CONVERSATION -> pushConversation(DataSource.getConversation(this, retryable.dataId))
            }
        }

        scheduleNextRun(this)
        return 0
    }

    private fun pushMessage(message: Message?) {
        if (message == null) {
            return
        }

        ApiUtils.addMessage(this, Account.accountId, message.id, message.conversationId, message.type, message.data,
                message.timestamp, message.mimeType, message.read, message.seen, message.from,
                message.color, message.sentDeviceId.toString(), message.simPhoneNumber, Account.encryptor)
    }

    private fun pushConversation(conversation: Conversation?) {
        if (conversation == null) {
            return
        }

        ApiUtils.addConversation(this, Account.accountId, conversation.id, conversation.colors.color,
                conversation.colors.colorDark, conversation.colors.colorLight, conversation.colors.colorAccent,
                conversation.ledColor, conversation.pinned, conversation.read,
                conversation.timestamp, conversation.title, conversation.phoneNumbers,
                conversation.snippet, conversation.ringtoneUri, conversation.idMatcher,
                conversation.mute, conversation.archive, conversation.privateNotifications,
                conversation.folderId, Account.encryptor, false)
    }

    companion object {

        private const val JOB_ID = "retryable-request-sender"
        private const val TWENTY_MINS = 60 * 20 // seconds
        private const val THIRTY_MINS = 60 * 30 // seconds

        fun scheduleNextRun(context: Context?) {
            if (context == null || !Account.exists() || !Account.primary || !FeatureFlags.RETRY_FAILED_REQUESTS) {
                return
            }

            val dispatcher = FirebaseJobDispatcher(GooglePlayDriver(context))
            val myJob = dispatcher.newJobBuilder()
                    .setService(SyncRetryableRequestsJob::class.java)
                    .setTag(JOB_ID)
                    .setRecurring(true)
                    .setLifetime(Lifetime.FOREVER)
                    .setTrigger(Trigger.executionWindow(TWENTY_MINS, THIRTY_MINS))
                    .setConstraints(Constraint.ON_ANY_NETWORK)
                    .setReplaceCurrent(true)
                    .build()

            dispatcher.mustSchedule(myJob)
        }
    }
}
