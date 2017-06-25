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

package xyz.klinker.messenger.shared.util

import android.annotation.TargetApi
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import xyz.klinker.messenger.shared.data.pojo.NotificationConversation
import xyz.klinker.messenger.shared.service.NotificationService

/**
 * When creating notifications, a single new conversation will never get the group message key on it.
 * This is because it will mess everything up, since it isn't marked as the group summary and a group summary will get
 * dismissed if it only has one item.
 *
 * This will help us know how many notifications should be updated when a new message comes in.
 *
 * If the user's device cannot read the active notifications (along with the group key), then we just re-notify everything
 * This is fine, and how the app usually worked, but with Notification Channels, it doesn't work as well, since we will
 * get spammed with constant notifications.
 *
 * If there is more than 2 new conversations, then all the logic here has already been taken care of.
 * We can just notify the single new conversation and it will be added to the active group notification
 *
 * If there are two new conversations, then there is the possibility that the original (first) notification
 * doesn't have the required group key. This won't work well, because the first and second notification will
 * now be separated, and not grouped. So we need to update the first conversation notification with the group key
 * so that they can both be combined into the notification group.
 */
object NotificationServiceHelper {

    fun calculateNumberOfNotificationsToProvide(context: Context, conversations: List<NotificationConversation>): Int {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            return conversations.size
        } else if (conversations.size == 2) {
            // add one to the number of active notifications, for the new one that has yet to be notified
            return calculateBasedOnActiveNotifications(context) + 1
        } else {
            return 1
        }
    }

    @TargetApi(Build.VERSION_CODES.N)
    private fun calculateBasedOnActiveNotifications(context: Context): Int {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // get the notifications that are not the group summary
        // get the notifications that don't have the group key for the conversation group
        // return the size, since this is how many will need to be updated with the group key

        // this won't work perfectly, since there is always the chance that there could be other notifications
        // from Pulse, that are active (scheduled message sent, message failed to send, etc)

        return manager.activeNotifications
                .takeWhile { statusBarNotification -> !statusBarNotification.isGroup }
                .takeWhile { statusBarNotification -> statusBarNotification.groupKey != NotificationService.GROUP_KEY_MESSAGES }
                .size
    }
}