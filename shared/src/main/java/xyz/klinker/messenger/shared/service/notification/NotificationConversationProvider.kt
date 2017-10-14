package xyz.klinker.messenger.shared.service.notification

import android.annotation.TargetApi
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.support.v4.app.NotificationCompat
import android.support.v4.app.NotificationManagerCompat
import android.support.v4.app.RemoteInput
import android.text.Html
import android.text.Spanned
import xyz.klinker.messenger.api.implementation.Account
import xyz.klinker.messenger.shared.MessengerActivityExtras
import xyz.klinker.messenger.shared.R
import xyz.klinker.messenger.shared.data.MimeType
import xyz.klinker.messenger.shared.data.Settings
import xyz.klinker.messenger.shared.data.model.Message
import xyz.klinker.messenger.shared.data.pojo.NotificationAction
import xyz.klinker.messenger.shared.data.pojo.NotificationConversation
import xyz.klinker.messenger.shared.data.pojo.NotificationMessage
import xyz.klinker.messenger.shared.data.pojo.VibratePattern
import xyz.klinker.messenger.shared.receiver.NotificationDismissedReceiver
import xyz.klinker.messenger.shared.service.*
import xyz.klinker.messenger.shared.util.*

/**
 * Displays a notification for a single conversation.
 */
class NotificationConversationProvider(private val service: NotificationService, private val ringtoneProvider: NotificationRingtoneProvider, private val summaryProvider: NotificationSummaryProvider) {

    fun giveConversationNotification(conversation: NotificationConversation, conversationIndex: Int, numConversations: Int) {
        var contactImage = ImageUtils.clipToCircle(
                ImageUtils.getBitmap(service, conversation.imageUri))

        try {
            val height = service.resources.getDimension(android.R.dimen.notification_large_icon_height)
            val width = service.resources.getDimension(android.R.dimen.notification_large_icon_width)
            contactImage = Bitmap.createScaledBitmap(contactImage!!, width.toInt(), height.toInt(), true)
        } catch (e: Exception) {
        }

        val vibratePattern = Settings.vibrate
        val shouldVibrate = !shouldAlertOnce(conversation.messages) && conversationIndex == 0
        var defaults = 0
        if (shouldVibrate && vibratePattern === VibratePattern.DEFAULT) {
            defaults = Notification.DEFAULT_VIBRATE
        }

        if (conversation.ledColor == Color.WHITE) {
            defaults = defaults or Notification.DEFAULT_LIGHTS
        }

        val settings = Settings
        val builder = NotificationCompat.Builder(service,
                getNotificationChannel(conversation.id))
                .setSmallIcon(if (!conversation.groupConversation) R.drawable.ic_stat_notify else R.drawable.ic_stat_notify_group)
                .setContentTitle(conversation.title)
                .setAutoCancel(true)
                .setCategory(Notification.CATEGORY_MESSAGE)
                .setColor(if (settings.useGlobalThemeColor) settings.mainColorSet.color else conversation.color)
                .setDefaults(defaults)
                .setLargeIcon(contactImage)
                .setPriority(if (settings.headsUp) Notification.PRIORITY_MAX else Notification.PRIORITY_DEFAULT)
                .setShowWhen(true)
                .setTicker(service.getString(R.string.notification_ticker, conversation.title))
                .setVisibility(Notification.VISIBILITY_PRIVATE)
                .setWhen(conversation.timestamp)

        if (numConversations == 1 && Build.MANUFACTURER.toLowerCase().contains("moto")) {
            // this is necessary for moto's active display, for some reason
            builder.setGroupSummary(true)
        } else {
            builder.setGroupSummary(false)
        }

        if (conversation.ledColor != Color.WHITE) {
            builder.setLights(conversation.ledColor, 1000, 500)
        }

        val sound = ringtoneProvider.getRingtone(conversation.ringtoneUri)
        if (conversationIndex == 0) {
            if (sound != null) {
                builder.setSound(sound)
            }

            if (vibratePattern.pattern != null) {
                builder.setVibrate(vibratePattern.pattern)
            } else if (vibratePattern === VibratePattern.OFF) {
                builder.setVibrate(LongArray(0))
            }
        }

        try {
            if (!conversation.groupConversation) {
                builder.addPerson("tel:" + conversation.phoneNumbers!!)
            }
        } catch (e: Exception) {
        }

        var pictureStyle: NotificationCompat.BigPictureStyle? = null
        var inboxStyle: NotificationCompat.InboxStyle? = null
        var messagingStyle: NotificationCompat.Style? = null

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && settings.historyInNotifications) {
            // build a messaging style notification for Android Nougat
            messagingStyle = NotificationCompat.MessagingStyle(service.getString(R.string.you))

            if (conversation.groupConversation) {
                messagingStyle.conversationTitle = conversation.title
            }

            val source = service.dataSource
            val messages = source.getMessages(service, conversation.id, 10)

            for (i in messages.indices.reversed()) {
                val message = messages[i]

                var from: String? = null
                if (message.type == Message.TYPE_RECEIVED) {
                    // we split it so that we only get the first name,
                    // if there is more than one

                    from = if (message.from != null) {
                        // it is most likely a group message.
                        message.from
                    } else {
                        conversation.title
                    }
                }

                val messageText = when {
                    MimeType.isAudio(message.mimeType!!) -> "<i>" + service.getString(R.string.audio_message) + "</i>"
                    MimeType.isVideo(message.mimeType!!) -> "<i>" + service.getString(R.string.video_message) + "</i>"
                    MimeType.isVcard(message.mimeType!!) -> "<i>" + service.getString(R.string.contact_card) + "</i>"
                    MimeType.isStaticImage(message.mimeType) -> "<i>" + service.getString(R.string.picture_message) + "</i>"
                    message.mimeType == MimeType.IMAGE_GIF -> "<i>" + service.getString(R.string.gif_message) + "</i>"
                    MimeType.isExpandedMedia(message.mimeType) -> "<i>" + service.getString(R.string.media) + "</i>"
                    else -> message.data
                }

                messagingStyle.addMessage(Html.fromHtml(messageText), message.timestamp, from)
            }
        }

        val text = StringBuilder()
        if (conversation.messages.size > 1 && conversation.messages[0].from != null) {
            inboxStyle = NotificationCompat.InboxStyle()

            for ((_, data, mimeType, _, from) in conversation.messages) {
                if (mimeType == MimeType.TEXT_PLAIN) {
                    val line = "<b>$from:</b>  $data"
                    text.append(line)
                    text.append("\n")
                    inboxStyle.addLine(Html.fromHtml(line))
                } else {
                    pictureStyle = NotificationCompat.BigPictureStyle()
                            .bigPicture(ImageUtils.getBitmap(service, data))
                }
            }
        } else {
            for (i in 0 until conversation.messages.size) {
                val (_, data, mimeType, _, from) = conversation.messages[i]

                if (mimeType == MimeType.TEXT_PLAIN) {
                    if (from != null) {
                        text.append("<b>")
                        text.append(from)
                        text.append(":</b>  ")
                        text.append(conversation.messages[i].data)
                        text.append("\n")
                    } else {
                        text.append(conversation.messages[i].data)
                        text.append("<br/>")
                    }
                } else if (MimeType.isStaticImage(mimeType)) {
                    pictureStyle = NotificationCompat.BigPictureStyle()
                            .bigPicture(ImageUtils.getBitmap(service, data))
                }
            }
        }

        var content = text.toString().trim { it <= ' ' }
        if (content.endsWith("<br/>")) {
            content = content.substring(0, content.length - 5)
        }

        if (!conversation.privateNotification) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                builder.setContentText(Html.fromHtml(content, 0))
            } else {
                builder.setContentText(Html.fromHtml(content))
            }

            when {
                pictureStyle != null -> {
                    builder.setStyle(pictureStyle)
                    builder.setContentText(service.getString(R.string.picture_message))
                }
                messagingStyle != null -> builder.setStyle(messagingStyle)
                inboxStyle != null -> builder.setStyle(inboxStyle)
                else -> builder.setStyle(NotificationCompat.BigTextStyle()
                        .bigText(Html.fromHtml(content)))
            }
        }

        val publicVersion = NotificationCompat.Builder(service,
                getNotificationChannel(conversation.id))
                .setSmallIcon(if (!conversation.groupConversation) R.drawable.ic_stat_notify else R.drawable.ic_stat_notify_group)
                .setContentTitle(service.resources.getQuantityString(R.plurals.new_conversations, 1, 1))
                .setContentText(service.resources.getQuantityString(R.plurals.new_messages,
                        conversation.messages.size, conversation.messages.size))
                .setLargeIcon(null)
                .setColor(if (settings.useGlobalThemeColor) settings.mainColorSet.color else conversation.color)
                .setAutoCancel(true)
                .setCategory(Notification.CATEGORY_MESSAGE)
                .setDefaults(defaults)
                .setPriority(if (settings.headsUp) Notification.PRIORITY_MAX else Notification.PRIORITY_DEFAULT)
                .setVisibility(Notification.VISIBILITY_PUBLIC)

        if (conversation.ledColor != Color.WHITE) {
            builder.setLights(conversation.ledColor, 1000, 500)
        }

        if (conversationIndex == 0) {
            if (sound != null) {
                builder.setSound(sound)
            }

            if (vibratePattern.pattern != null) {
                builder.setVibrate(vibratePattern.pattern)
            } else if (vibratePattern === VibratePattern.OFF) {
                builder.setVibrate(LongArray(0))
            }
        }

        if (numConversations == 1 && Build.MANUFACTURER.toLowerCase().contains("moto")) {
            // this is necessary for moto's active display, for some reason
            publicVersion.setGroupSummary(true)
        } else {
            publicVersion.setGroupSummary(false)
        }

        try {
            if (!conversation.groupConversation) {
                publicVersion.addPerson("tel:" + conversation.phoneNumbers!!)
            } else {
                for (number in conversation.phoneNumbers!!.split(", ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()) {
                    publicVersion.addPerson("tel:" + number)
                }
            }
        } catch (e: Exception) {
        }

        builder.setPublicVersion(publicVersion.build())


        // one thing to keep in mind here... my adding only a wearable extender to the notification,
        // will the action be shown on phones or only on wear devices? If it is shown on phones, is
        // it only shown on Nougat+ where these actions can be accepted?
        val remoteInput = RemoteInput.Builder(ReplyService.EXTRA_REPLY)
                .setLabel(service.getString(R.string.reply_to, conversation.title))
                .setChoices(service.resources.getStringArray(R.array.reply_choices))
                .setAllowFreeFormInput(true)
                .build()


        // Android wear extender (add a second page with message history
        val secondPageStyle = NotificationCompat.BigTextStyle()
        secondPageStyle.setBigContentTitle(conversation.title)
                .bigText(getWearableSecondPageConversation(conversation))
        val wear = NotificationCompat.Builder(service, getNotificationChannel(conversation.id))
                .setStyle(secondPageStyle)

        val wearableExtender = NotificationCompat.WearableExtender().addPage(wear.build())

        val actionExtender = NotificationCompat.Action.WearableExtender()
                .setHintLaunchesActivity(true)
                .setHintDisplayActionInline(true)

        val pendingReply: PendingIntent
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && !NotificationConstants.DEBUG_QUICK_REPLY) {
            // with Android N, we only need to show the the reply service intent through the wearable extender
            val reply = Intent(service, ReplyService::class.java)
            reply.putExtra(ReplyService.EXTRA_CONVERSATION_ID, conversation.id)
            pendingReply = PendingIntent.getService(service, conversation.id.toInt(), reply, PendingIntent.FLAG_UPDATE_CURRENT)

            val action = NotificationCompat.Action.Builder(R.drawable.ic_reply_white,
                    service.getString(R.string.reply), pendingReply)
                    .addRemoteInput(remoteInput)
                    .setAllowGeneratedReplies(true)
                    .extend(actionExtender)
                    .build()

            if (!conversation.privateNotification && settings.notificationActions.contains(NotificationAction.REPLY)) {
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
                    conversation.id.toInt(), reply, PendingIntent.FLAG_UPDATE_CURRENT)

            if (NotificationConstants.DEBUG_QUICK_REPLY) {
                // if we are debugging, the assumption is that we are on android N, we have to be stop showing
                // the remote input or else it will keep using the direct reply
                val action = NotificationCompat.Action.Builder(R.drawable.ic_reply_dark,
                        service.getString(R.string.reply), pendingReply)
                        .extend(actionExtender)
                        .setAllowGeneratedReplies(true)
                        .build()

                if (!conversation.privateNotification && settings.notificationActions.contains(NotificationAction.REPLY)) {
                    builder.addAction(action)
                }

                action.icon = R.drawable.ic_reply_white
                wearableExtender.addAction(action)
            } else {
                val action = NotificationCompat.Action.Builder(R.drawable.ic_reply_dark,
                        service.getString(R.string.reply), pendingReply)
                        .build()

                if (!conversation.privateNotification && settings.notificationActions.contains(NotificationAction.REPLY)) {
                    builder.addAction(action)
                }

                val wearReply = Intent(service, ReplyService::class.java)
                val extras = Bundle()
                extras.putLong(ReplyService.EXTRA_CONVERSATION_ID, conversation.id)
                wearReply.putExtras(extras)
                val wearPendingReply = PendingIntent.getService(service,
                        conversation.id.toInt() + 1, wearReply, PendingIntent.FLAG_UPDATE_CURRENT)

                val wearAction = NotificationCompat.Action.Builder(R.drawable.ic_reply_white,
                        service.getString(R.string.reply), wearPendingReply)
                        .addRemoteInput(remoteInput)
                        .extend(actionExtender)
                        .build()

                wearableExtender.addAction(wearAction)
            }
        }

        if (!conversation.groupConversation && settings.notificationActions.contains(NotificationAction.CALL)
                && (!Account.exists() || Account.primary)) {
            val call = Intent(service, NotificationCallService::class.java)
            call.putExtra(NotificationMarkReadService.EXTRA_CONVERSATION_ID, conversation.id)
            call.putExtra(NotificationCallService.EXTRA_PHONE_NUMBER, conversation.phoneNumbers)
            val callPending = PendingIntent.getService(service, conversation.id.toInt(),
                    call, PendingIntent.FLAG_UPDATE_CURRENT)

            builder.addAction(NotificationCompat.Action(R.drawable.ic_call_dark, service.getString(R.string.call), callPending))
        }

        val deleteMessage = Intent(service, NotificationDeleteService::class.java)
        deleteMessage.putExtra(NotificationDeleteService.EXTRA_CONVERSATION_ID, conversation.id)
        deleteMessage.putExtra(NotificationDeleteService.EXTRA_MESSAGE_ID, conversation.unseenMessageId)
        val pendingDeleteMessage = PendingIntent.getService(service, conversation.id.toInt(),
                deleteMessage, PendingIntent.FLAG_UPDATE_CURRENT)

        if (settings.notificationActions.contains(NotificationAction.DELETE)) {
            builder.addAction(NotificationCompat.Action(R.drawable.ic_delete_dark, service.getString(R.string.delete), pendingDeleteMessage))
        }

        val read = Intent(service, NotificationMarkReadService::class.java)
        read.putExtra(NotificationMarkReadService.EXTRA_CONVERSATION_ID, conversation.id)
        val pendingRead = PendingIntent.getService(service, conversation.id.toInt(),
                read, PendingIntent.FLAG_UPDATE_CURRENT)

        if (settings.notificationActions.contains(NotificationAction.READ)) {
            builder.addAction(NotificationCompat.Action(R.drawable.ic_done_dark, service.getString(R.string.read), pendingRead))
        }

        wearableExtender.addAction(NotificationCompat.Action(R.drawable.ic_done_white, service.getString(R.string.read), pendingRead))
        wearableExtender.addAction(NotificationCompat.Action(R.drawable.ic_delete_white, service.getString(R.string.delete), pendingDeleteMessage))

        val delete = Intent(service, NotificationDismissedReceiver::class.java)
        delete.putExtra(NotificationDismissedService.EXTRA_CONVERSATION_ID, conversation.id)
        val pendingDelete = PendingIntent.getBroadcast(service, conversation.id.toInt(),
                delete, PendingIntent.FLAG_UPDATE_CURRENT)

        val open = ActivityUtils.buildForComponent(ActivityUtils.MESSENGER_ACTIVITY)
        open.putExtra(MessengerActivityExtras.EXTRA_CONVERSATION_ID, conversation.id)
        open.putExtra(MessengerActivityExtras.EXTRA_FROM_NOTIFICATION, true)
        open.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        val pendingOpen = PendingIntent.getActivity(service,
                conversation.id.toInt(), open, PendingIntent.FLAG_UPDATE_CURRENT)

        builder.setDeleteIntent(pendingDelete)
        builder.setContentIntent(pendingOpen)

        val carReply = Intent().addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
                .setAction("xyz.klinker.messenger.CAR_REPLY")
                .putExtra(ReplyService.EXTRA_CONVERSATION_ID, conversation.id)
                .setPackage("xyz.klinker.messenger")
        val pendingCarReply = PendingIntent.getBroadcast(service, conversation.id.toInt(),
                carReply, PendingIntent.FLAG_UPDATE_CURRENT)

        val carRead = Intent().addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
                .setAction("xyz.klinker.messenger.CAR_READ")
                .putExtra(NotificationMarkReadService.EXTRA_CONVERSATION_ID, conversation.id)
                .setPackage("xyz.klinker.messenger")
        val pendingCarRead = PendingIntent.getBroadcast(service, conversation.id.toInt(),
                carRead, PendingIntent.FLAG_UPDATE_CURRENT)

        // Android Auto extender
        val car = NotificationCompat.CarExtender.UnreadConversation.Builder(conversation.title)
                .setReadPendingIntent(pendingCarRead)
                .setReplyAction(pendingCarReply, remoteInput)
                .setLatestTimestamp(conversation.timestamp)

        for ((_, data, mimeType) in conversation.messages) {
            if (mimeType == MimeType.TEXT_PLAIN) {
                car.addMessage(data)
            } else {
                car.addMessage(service.getString(R.string.new_mms_message))
            }
        }

        // apply the extenders to the notification
        builder.extend(NotificationCompat.CarExtender().setUnreadConversation(car.build()))
        builder.extend(wearableExtender)

        if (NotificationConstants.CONVERSATION_ID_OPEN == conversation.id) {
            // skip this notification since we are already on the conversation.
            summaryProvider.skipSummary = true
        } else {
            NotificationManagerCompat.from(service).notify(conversation.id.toInt(), builder.build())
        }
    }

    /**
     * If the user is getting spammed by the same person over and over again, we don't want to immediately
     * give a vibrate or ringtone again.
     *
     * The 'onlyAlertOnce" flag on the notification builder means that it will not give a vibrate or sound
     * if the notification is already active.
     *
     * @param messages the messages in this conversation
     * @return true if the latest two messages are less than 1 min apart, or there is only one message. False
     * if the latest messages are more than 1 min apart, so that it will ring again.
     */
    fun shouldAlertOnce(messages: List<NotificationMessage>): Boolean {
        if (messages.size > 1) {
            val (_, _, _, timestamp) = messages[messages.size - 2]
            val (_, _, _, timestamp1) = messages[messages.size - 1]

            if (Math.abs(timestamp - timestamp1) > TimeUtils.SECOND * 30) {
                return false
            }
        }

        // default to true
        return true
    }

    private fun getWearableSecondPageConversation(conversation: NotificationConversation): Spanned {
        val source = service.dataSource
        val messages = source.getMessages(service, conversation.id, 10)

        val you = service.getString(R.string.you)
        val builder = StringBuilder()

        for (message in messages) {
            var messageText = ""
            if (MimeType.isAudio(message.mimeType!!)) {
                messageText += "<i>" + service.getString(R.string.audio_message) + "</i>"
            } else if (MimeType.isVideo(message.mimeType!!)) {
                messageText += "<i>" + service.getString(R.string.video_message) + "</i>"
            } else if (MimeType.isVcard(message.mimeType!!)) {
                messageText += "<i>" + service.getString(R.string.contact_card) + "</i>"
            } else if (MimeType.isStaticImage(message.mimeType)) {
                messageText += "<i>" + service.getString(R.string.picture_message) + "</i>"
            } else if (message.mimeType == MimeType.IMAGE_GIF) {
                messageText += "<i>" + service.getString(R.string.gif_message) + "</i>"
            } else if (MimeType.isExpandedMedia(message.mimeType)) {
                messageText += "<i>" + service.getString(R.string.media) + "</i>"
            } else {
                messageText += message.data
            }

            if (message.type == Message.TYPE_RECEIVED) {
                if (message.from != null) {
                    builder.append("<b>" + message.from + "</b>  " + messageText + "<br>")
                } else {
                    builder.append("<b>" + conversation.title + "</b>  " + messageText + "<br>")
                }
            } else {
                builder.append("<b>$you</b>  $messageText<br>")
            }

        }

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Html.fromHtml(builder.toString(), 0)
        } else {
            Html.fromHtml(builder.toString())
        }
    }

    @TargetApi(Build.VERSION_CODES.O)
    private fun getNotificationChannel(conversationId: Long): String {
        if (!AndroidVersionUtil.isAndroidO) {
            return NotificationUtils.DEFAULT_CONVERSATION_CHANNEL_ID
        }

        val manager = service.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        return if (manager.getNotificationChannel(conversationId.toString() + "") != null) {
            conversationId.toString() + ""
        } else {
            NotificationUtils.DEFAULT_CONVERSATION_CHANNEL_ID
        }
    }
}