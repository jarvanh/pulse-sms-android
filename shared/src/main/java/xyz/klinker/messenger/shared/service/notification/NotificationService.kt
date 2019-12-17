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

package xyz.klinker.messenger.shared.service.notification

import android.app.IntentService
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.ml.naturallanguage.FirebaseNaturalLanguage
import xyz.klinker.messenger.shared.data.DataSource
import xyz.klinker.messenger.shared.data.Settings
import xyz.klinker.messenger.shared.data.pojo.NotificationAction
import xyz.klinker.messenger.shared.service.jobs.RepeatNotificationJob
import xyz.klinker.messenger.shared.service.notification.conversation.NotificationConversationProvider
import xyz.klinker.messenger.shared.util.MockableDataSourceWrapper
import xyz.klinker.messenger.shared.util.TimeUtils
import xyz.klinker.messenger.shared.widget.MessengerAppWidgetProvider
import java.util.*

/**
 * Service for displaying notifications to the user based on which conversations have not been
 * seen yet.
 *
 * I used pseudocode here: http://blog.danlew.net/2017/02/07/correctly-handling-bundled-android-notifications/
 */
class NotificationService : IntentService("NotificationService") {
    private val foreground = NotificationForegroundController(this)

    override fun onHandleIntent(intent: Intent?) {
        foreground.show(intent)
        Notifier(this).notify(intent)
        foreground.hide()
    }
}

class Notifier(private val context: Context) {

    private val query = NotificationUnreadConversationQuery(context)
    private val ringtoneProvider = NotificationRingtoneProvider(context)
    private val summaryNotifier = NotificationSummaryProvider(context)
    private val conversationNotifier = NotificationConversationProvider(context, ringtoneProvider, summaryNotifier)

    private val dataSource: MockableDataSourceWrapper
        get() = MockableDataSourceWrapper(DataSource)

    fun notify(intent: Intent? = null) {
        try {
            val snoozeTil = Settings.snooze
            if (snoozeTil > TimeUtils.now) {
                return
            }

            val conversations = query.getUnseenConversations(dataSource).filter { !it.mute }

            if (conversations.isNotEmpty()) {
                if (conversations.size > 1) {
                    val rows = conversations.mapTo(ArrayList()) { "<b>" + it.title + "</b>  " + it.snippet }
                    summaryNotifier.giveSummaryNotification(conversations, rows)
                }

                val numberToNotify = NotificationServiceHelper.calculateNumberOfNotificationsToProvide(context, conversations)
                for (i in 0 until if (numberToNotify < conversations.size) numberToNotify else conversations.size) {
                    val conversation = conversations[i]

                    try {
                        if (Settings.notificationActions.contains(NotificationAction.SMART_REPLY)) {
                            val smartReply = FirebaseNaturalLanguage.getInstance().smartReply
                            smartReply.suggestReplies(conversation.getFirebaseSmartReplyConversation().asReversed())
                                    .addOnSuccessListener { result ->
                                        val suggestions = result.suggestions/*.filter { it.confidence > 0.2 }*/
                                        conversationNotifier.giveConversationNotification(conversation, i, conversations.size, suggestions)
                                    }.addOnFailureListener {
                                        conversationNotifier.giveConversationNotification(conversation, i, conversations.size)
                                    }
                        } else {
                            conversationNotifier.giveConversationNotification(conversation, i, conversations.size)
                        }
                    } catch (e: Throwable) {
                        conversationNotifier.giveConversationNotification(conversation, i, conversations.size)
                    }
                }

                if (conversations.size == 1) {
                    NotificationManagerCompat.from(context).cancel(NotificationConstants.SUMMARY_ID)
                }

                if (Settings.repeatNotifications != -1L) {
                    RepeatNotificationJob.scheduleNextRun(context, Settings.repeatNotifications)
                }

                if (Settings.wakeScreen) {
                    try {
                        Thread.sleep(600)
                    } catch (e: Exception) {
                    }

                    val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
                    val wl = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP, "pulse:new-notification")
                    wl.acquire(5000)
                }
            }

            MessengerAppWidgetProvider.refreshWidget(context)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
