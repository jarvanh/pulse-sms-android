package xyz.klinker.messenger.shared.service.jobs

import android.content.Context
import androidx.work.*
import xyz.klinker.messenger.api.implementation.Account
import xyz.klinker.messenger.api.implementation.ApiUtils

import xyz.klinker.messenger.shared.data.DataSource
import xyz.klinker.messenger.shared.data.model.Conversation
import xyz.klinker.messenger.shared.data.model.Message
import xyz.klinker.messenger.shared.data.model.RetryableRequest
import java.util.concurrent.TimeUnit

/**
 * If some requests fail, they get written in to the retryable_requests table to get retried when the
 * device regains connectivity. This service should read that table and execute any request that are pending.
 *
 * It should be set up to run periodically, but only when the phone has a connection. With the way
 * FirebaseJobDispatcher works, this should force it to run whenever the user goes from a loss in connectivity
 * to regaining connectivity, or shortly after.
 */
class SyncRetryableRequestsWork(private val context: Context, params: WorkerParameters) : Worker(context, params) {

    override fun doWork(): Result {
        val retryables = DataSource.getRetryableRequestsAsList(context)
        DataSource.deleteAllRetryableRequest(context)

        for (retryable in retryables) {
            when (retryable.type) {
                RetryableRequest.TYPE_ADD_MESSAGE -> pushMessage(DataSource.getMessage(context, retryable.dataId))
                RetryableRequest.TYPE_ADD_CONVERSATION -> pushConversation(DataSource.getConversation(context, retryable.dataId))
            }
        }

        return Result.success()
    }

    private fun pushMessage(message: Message?) {
        if (message == null) {
            return
        }

        ApiUtils.addMessage(context, Account.accountId, message.id, message.conversationId, message.type, message.data,
                message.timestamp, message.mimeType, message.read, message.seen, message.from,
                message.color, message.sentDeviceId.toString(), message.simPhoneNumber, Account.encryptor)
    }

    private fun pushConversation(conversation: Conversation?) {
        if (conversation == null) {
            return
        }

        ApiUtils.addConversation(context, Account.accountId, conversation.id, conversation.colors.color,
                conversation.colors.colorDark, conversation.colors.colorLight, conversation.colors.colorAccent,
                conversation.ledColor, conversation.pinned, conversation.read,
                conversation.timestamp, conversation.title, conversation.phoneNumbers,
                conversation.snippet, conversation.ringtoneUri, conversation.idMatcher,
                conversation.mute, conversation.archive, conversation.private,
                conversation.folderId, Account.encryptor, false)
    }

    companion object {

        private const val JOB_ID = "retryable-request-sender"

        fun scheduleNextRun(context: Context?) {
            if (context == null || !Account.exists() || !Account.primary) {
                return
            }

            val work = PeriodicWorkRequest.Builder(SyncRetryableRequestsWork::class.java, 30L, TimeUnit.MINUTES)
                    .build()
            WorkManager.getInstance().enqueueUniquePeriodicWork(JOB_ID, ExistingPeriodicWorkPolicy.KEEP, work)
        }
    }
}
