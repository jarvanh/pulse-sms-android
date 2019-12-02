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

package xyz.klinker.messenger.shared.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

import xyz.klinker.messenger.api.implementation.firebase.ScheduledTokenRefreshService
import xyz.klinker.messenger.shared.data.Settings
import xyz.klinker.messenger.shared.service.QuickComposeNotificationService
import xyz.klinker.messenger.shared.service.jobs.*

/**
 * Receiver for when boot has completed. This will be responsible for starting up the content
 * observer service and scheduling any messages that are to be sent in the future.
 */
class BootCompletedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        try {
            if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
                ScheduledMessageJob.scheduleNextRun(context)
                CleanupOldMessagesWork.scheduleNextRun(context)
                FreeTrialNotifierWork.scheduleNextRun(context)
                ContactSyncWork.scheduleNextRun(context)
                SubscriptionExpirationCheckJob.scheduleNextRun(context)
                SignoutJob.scheduleNextRun(context)
                ScheduledTokenRefreshService.scheduleNextRun(context)
                SyncRetryableRequestsWork.scheduleNextRun(context)
                RepostQuickComposeNotificationWork.scheduleNextRun(context)

                if (Settings.quickCompose) QuickComposeNotificationService.start(context)
            }
        } catch (e: IllegalStateException) {
            e.printStackTrace()
        }

    }

}
