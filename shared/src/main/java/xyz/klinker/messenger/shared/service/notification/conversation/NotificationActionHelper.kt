package xyz.klinker.messenger.shared.service.notification.conversation

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.core.app.NotificationCompat
import androidx.core.app.RemoteInput
import xyz.klinker.messenger.api.implementation.Account
import xyz.klinker.messenger.shared.MessengerActivityExtras
import xyz.klinker.messenger.shared.R
import xyz.klinker.messenger.shared.data.Settings
import xyz.klinker.messenger.shared.data.pojo.NotificationAction
import xyz.klinker.messenger.shared.data.pojo.NotificationConversation
import xyz.klinker.messenger.shared.receiver.*
import xyz.klinker.messenger.shared.receiver.notification_action.*
import xyz.klinker.messenger.shared.receiver.notification_action.NotificationDismissedReceiver
import xyz.klinker.messenger.shared.service.*
import xyz.klinker.messenger.shared.service.notification.NotificationConstants
import xyz.klinker.messenger.shared.util.ActivityUtils
import xyz.klinker.messenger.shared.util.AndroidVersionUtil

class NotificationActionHelper(private val service: Context) {

    fun addReplyAction(builder: NotificationCompat.Builder, wearableExtender: NotificationCompat.WearableExtender, remoteInput: RemoteInput, conversation: NotificationConversation) {
        val actionExtender = NotificationCompat.Action.WearableExtender()
                .setHintLaunchesActivity(true)
                .setHintDisplayActionInline(true)

        val pendingReply: PendingIntent
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && !NotificationConstants.DEBUG_QUICK_REPLY) {
            // with Android N, we only need to show the the reply service intent through the wearable extender
            val reply = Intent(service, ReplyService::class.java)
            reply.putExtra(ReplyService.EXTRA_CONVERSATION_ID, conversation.id)
            pendingReply = PendingIntent.getService(service, conversation.id.toInt() - 1, reply, PendingIntent.FLAG_UPDATE_CURRENT)

            val action = NotificationCompat.Action.Builder(R.drawable.ic_reply_white,
                    service.getString(R.string.reply), pendingReply)
                    .addRemoteInput(remoteInput)
                    .setAllowGeneratedReplies(true)
                    .extend(actionExtender)
                    .build()

            if (!conversation.privateNotification && Settings.notificationActions.contains(NotificationAction.REPLY)) {
                builder.addAction(action)
            }

            wearableExtender.addAction(action)
        } else {
            // on older versions, we have to show the reply activity button as an action and add the remote input to it
            // this will allow it to be used on android wear (we will have to handle this from the activity)
            // as well as have a reply quick action button.
            val reply = ActivityUtils.buildForComponent(ActivityUtils.NOTIFICATION_REPLY)
            reply.putExtra(ReplyService.EXTRA_CONVERSATION_ID, conversation.id)
            reply.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            pendingReply = PendingIntent.getActivity(service,
                    conversation.id.toInt() - 1, reply, PendingIntent.FLAG_UPDATE_CURRENT)

            if (NotificationConstants.DEBUG_QUICK_REPLY) {
                // if we are debugging, the assumption is that we are on android N, we have to be stop showing
                // the remote input or else it will keep using the direct reply
                val action = NotificationCompat.Action.Builder(R.drawable.ic_reply_dark,
                        service.getString(R.string.reply), pendingReply)
                        .extend(actionExtender)
                        .setAllowGeneratedReplies(true)
                        .build()

                if (!conversation.privateNotification && Settings.notificationActions.contains(NotificationAction.REPLY)) {
                    builder.addAction(action)
                }

                action.icon = R.drawable.ic_reply_white
                wearableExtender.addAction(action)
            } else {
                val action = NotificationCompat.Action.Builder(R.drawable.ic_reply_dark,
                        service.getString(R.string.reply), pendingReply)
                        .build()

                if (!conversation.privateNotification && Settings.notificationActions.contains(NotificationAction.REPLY)) {
                    builder.addAction(action)
                }

                val wearReply = Intent(service, ReplyService::class.java)
                val extras = Bundle()
                extras.putLong(ReplyService.EXTRA_CONVERSATION_ID, conversation.id)
                wearReply.putExtras(extras)
                val wearPendingReply = PendingIntent.getService(service,
                        conversation.id.toInt() - 2, wearReply, PendingIntent.FLAG_UPDATE_CURRENT)

                val wearAction = NotificationCompat.Action.Builder(R.drawable.ic_reply_white,
                        service.getString(R.string.reply), wearPendingReply)
                        .addRemoteInput(remoteInput)
                        .extend(actionExtender)
                        .build()

                wearableExtender.addAction(wearAction)
            }
        }
    }

    fun addOtpAction(builder: NotificationCompat.Builder, otp: String, conversationId: Long) {
        val copy = Intent(service, xyz.klinker.messenger.shared.receiver.notification_action.NotificationCopyOtpReceiver::class.java)
        copy.putExtra(xyz.klinker.messenger.shared.receiver.notification_action.NotificationCopyOtpReceiver.EXTRA_PASSWORD, otp)
        copy.putExtra(xyz.klinker.messenger.shared.receiver.notification_action.NotificationMarkReadReceiver.EXTRA_CONVERSATION_ID, conversationId)
        val pendingCopy = PendingIntent.getBroadcast(service, conversationId.toInt() + 5,
                copy, PendingIntent.FLAG_CANCEL_CURRENT)

        builder.addAction(NotificationCompat.Action(R.drawable.ic_copy_dark, service.getString(R.string.copy_otp) + " $otp", pendingCopy))
    }

    fun addNonReplyActions(builder: NotificationCompat.Builder, wearableExtender: NotificationCompat.WearableExtender, conversation: NotificationConversation) {
        if (!conversation.groupConversation && Settings.notificationActions.contains(NotificationAction.CALL)
                && (!Account.exists() || Account.primary)) {
            val call = Intent(service, xyz.klinker.messenger.shared.receiver.notification_action.NotificationCallReceiver::class.java)
            call.putExtra(xyz.klinker.messenger.shared.receiver.notification_action.NotificationMarkReadReceiver.EXTRA_CONVERSATION_ID, conversation.id)
            call.putExtra(xyz.klinker.messenger.shared.receiver.notification_action.NotificationCallReceiver.EXTRA_PHONE_NUMBER, conversation.phoneNumbers)
            val callPending = PendingIntent.getBroadcast(service, conversation.id.toInt() + 1,
                    call, PendingIntent.FLAG_CANCEL_CURRENT)

            builder.addAction(NotificationCompat.Action(R.drawable.ic_call_dark, service.getString(R.string.call), callPending))
        }

        val deleteMessage = Intent(service, xyz.klinker.messenger.shared.receiver.notification_action.NotificationDeleteReceiver::class.java)
        deleteMessage.putExtra(xyz.klinker.messenger.shared.receiver.notification_action.NotificationDeleteReceiver.EXTRA_CONVERSATION_ID, conversation.id)
        deleteMessage.putExtra(xyz.klinker.messenger.shared.receiver.notification_action.NotificationDeleteReceiver.EXTRA_MESSAGE_ID, conversation.unseenMessageId)
        val pendingDeleteMessage = PendingIntent.getBroadcast(service, conversation.id.toInt() + 2,
                deleteMessage, PendingIntent.FLAG_CANCEL_CURRENT)

        if (Settings.notificationActions.contains(NotificationAction.DELETE)) {
            builder.addAction(NotificationCompat.Action(R.drawable.ic_delete_dark, service.getString(R.string.delete), pendingDeleteMessage))
            wearableExtender.addAction(NotificationCompat.Action(R.drawable.ic_delete_white, service.getString(R.string.delete), pendingDeleteMessage))
        }

        val read = Intent(service, xyz.klinker.messenger.shared.receiver.notification_action.NotificationMarkReadReceiver::class.java)
        read.putExtra(xyz.klinker.messenger.shared.receiver.notification_action.NotificationMarkReadReceiver.EXTRA_CONVERSATION_ID, conversation.id)
        val pendingRead = PendingIntent.getBroadcast(service, conversation.id.toInt() + 3,
                read, PendingIntent.FLAG_CANCEL_CURRENT)

        if (Settings.notificationActions.contains(NotificationAction.READ)) {
            builder.addAction(NotificationCompat.Action(R.drawable.ic_done_dark, service.getString(if (AndroidVersionUtil.isAndroidN) R.string.mark_as_read else R.string.read), pendingRead))
            wearableExtender.addAction(NotificationCompat.Action(R.drawable.ic_done_white, service.getString(if (AndroidVersionUtil.isAndroidN) R.string.mark_as_read else R.string.read), pendingRead))
        }

        val mute = Intent(service, xyz.klinker.messenger.shared.receiver.notification_action.NotificationMuteReceiver::class.java)
        mute.putExtra(xyz.klinker.messenger.shared.receiver.notification_action.NotificationMarkReadReceiver.EXTRA_CONVERSATION_ID, conversation.id)
        val pendingMute = PendingIntent.getBroadcast(service, conversation.id.toInt() + 4,
                mute, PendingIntent.FLAG_CANCEL_CURRENT)

        if (Settings.notificationActions.contains(NotificationAction.MUTE)) {
            builder.addAction(NotificationCompat.Action(R.drawable.ic_mute_dark, service.getString(R.string.mute), pendingMute))
            wearableExtender.addAction(NotificationCompat.Action(R.drawable.ic_mute_white, service.getString(R.string.mute), pendingMute))
        }

        val archive = Intent(service, xyz.klinker.messenger.shared.receiver.notification_action.NotificationArchiveReceiver::class.java)
        archive.putExtra(xyz.klinker.messenger.shared.receiver.notification_action.NotificationMarkReadReceiver.EXTRA_CONVERSATION_ID, conversation.id)
        val pendingArchive = PendingIntent.getBroadcast(service, conversation.id.toInt() + 6,
                archive, PendingIntent.FLAG_CANCEL_CURRENT)

        if (Settings.notificationActions.contains(NotificationAction.ARCHIVE)) {
            builder.addAction(NotificationCompat.Action(R.drawable.ic_archive_dark, service.getString(R.string.menu_archive_conversation), pendingArchive))
            wearableExtender.addAction(NotificationCompat.Action(R.drawable.ic_archive_light, service.getString(R.string.menu_archive_conversation), pendingArchive))
        }

    }

    fun addContentIntents(builder: NotificationCompat.Builder, conversation: NotificationConversation) {
        val delete = Intent(service, NotificationDismissedReceiver::class.java)
        delete.putExtra(xyz.klinker.messenger.shared.service.NotificationDismissedReceiver.EXTRA_CONVERSATION_ID, conversation.id)
        val pendingDelete = PendingIntent.getBroadcast(service, conversation.id.toInt(),
                delete, PendingIntent.FLAG_CANCEL_CURRENT)

        val open = ActivityUtils.buildForComponent(ActivityUtils.MESSENGER_ACTIVITY)
        open.putExtra(MessengerActivityExtras.EXTRA_CONVERSATION_ID, conversation.id)
        open.putExtra(MessengerActivityExtras.EXTRA_FROM_NOTIFICATION, true)
        if (conversation.privateNotification) {
            open.putExtra(MessengerActivityExtras.EXTRA_OPEN_PRIVATE, true)
        }

        open.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        val pendingOpen = PendingIntent.getActivity(service,
                conversation.id.toInt(), open, PendingIntent.FLAG_UPDATE_CURRENT)

        builder.setDeleteIntent(pendingDelete)
        builder.setContentIntent(pendingOpen)
    }
}