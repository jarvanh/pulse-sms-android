package xyz.klinker.messenger.shared.service.jobs

import android.content.Context
import android.util.Log
import com.firebase.jobdispatcher.*
import xyz.klinker.messenger.api.implementation.Account
import xyz.klinker.messenger.api.implementation.ApiUtils

import xyz.klinker.messenger.shared.data.DataSource
import xyz.klinker.messenger.shared.data.FeatureFlags
import xyz.klinker.messenger.shared.data.Settings
import xyz.klinker.messenger.shared.data.model.Conversation
import xyz.klinker.messenger.shared.data.model.Message
import xyz.klinker.messenger.shared.data.model.RetryableRequest
import xyz.klinker.messenger.shared.receiver.MessageListUpdatedReceiver
import xyz.klinker.messenger.shared.service.QuickComposeNotificationService
import xyz.klinker.messenger.shared.util.TimeUtils

class RepostQuickComposeNotification : SimpleJobService() {

    override fun onRunJob(job: JobParameters?): Int {
        if (Settings.quickCompose) {
            QuickComposeNotificationService.start(this)
        } else {
            QuickComposeNotificationService.stop(this)
        }

        scheduleNextRun(this)
        return 0
    }

    companion object {

        private const val JOB_ID = "quick-compose-reposter"
        private const val TWENTY_MINS = 60 * 20 // seconds
        private const val THIRTY_MINS = 60 * 30 // seconds

        fun scheduleNextRun(context: Context?) {
            val dispatcher = FirebaseJobDispatcher(GooglePlayDriver(context))

            if (!Settings.quickCompose) {
                dispatcher.cancel(JOB_ID)
                return
            }

            val myJob = dispatcher.newJobBuilder()
                    .setService(RepostQuickComposeNotification::class.java)
                    .setTag(JOB_ID)
                    .setRecurring(true)
                    .setLifetime(Lifetime.FOREVER)
                    .setTrigger(Trigger.executionWindow(TWENTY_MINS, THIRTY_MINS))
                    .setReplaceCurrent(true)
                    .build()

            dispatcher.mustSchedule(myJob)
        }
    }
}
