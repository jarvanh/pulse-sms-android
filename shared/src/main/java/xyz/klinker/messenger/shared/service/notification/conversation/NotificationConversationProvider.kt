package xyz.klinker.messenger.shared.service.notification.conversation

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Build
import android.support.v4.app.NotificationCompat
import android.support.v4.app.NotificationManagerCompat
import android.support.v4.app.RemoteInput
import android.text.Html
import android.util.Log
import xyz.klinker.messenger.shared.R
import xyz.klinker.messenger.shared.data.DataSource
import xyz.klinker.messenger.shared.data.MimeType
import xyz.klinker.messenger.shared.data.Settings
import xyz.klinker.messenger.shared.data.model.Message
import xyz.klinker.messenger.shared.data.pojo.NotificationConversation
import xyz.klinker.messenger.shared.data.pojo.NotificationMessage
import xyz.klinker.messenger.shared.data.pojo.VibratePattern
import xyz.klinker.messenger.shared.service.ReplyService
import xyz.klinker.messenger.shared.service.notification.*
import xyz.klinker.messenger.shared.util.*

/**
 * Displays a notification for a single conversation.
 */
@Suppress("DEPRECATION")
@SuppressLint("NewApi")
class NotificationConversationProvider(private val service: Context, private val ringtoneProvider: NotificationRingtoneProvider, private val summaryProvider: NotificationSummaryProvider) {

    private val actionHelper = NotificationActionHelper(service)
    private val carHelper = NotificationCarHelper(service)
    private val wearableHelper = NotificationWearableHelper(service, this)

    fun giveConversationNotification(conversation: NotificationConversation, conversationIndex: Int, numConversations: Int): Notification {
        val publicVersion = preparePublicBuilder(conversation, conversationIndex)
                .setDefaults(buildNotificationDefaults(conversation, conversationIndex))
                .setGroupSummary(numConversations == 1 && Build.MANUFACTURER.toLowerCase().contains("moto"))
                .setGroup(if (numConversations > 1) NotificationConstants.GROUP_KEY_MESSAGES else null)
                .applyLightsSoundAndVibrate(conversation, conversationIndex)
                .addPerson(conversation)

        val builder = prepareBuilder(conversation, conversationIndex)
                .setDefaults(buildNotificationDefaults(conversation, conversationIndex))
                .setLargeIcon(buildContactImage(conversation))
                .setGroupSummary(numConversations == 1 && Build.MANUFACTURER.toLowerCase().contains("moto"))
                .setGroup(if (numConversations > 1) NotificationConstants.GROUP_KEY_MESSAGES else null)
                .applyLightsSoundAndVibrate(conversation, conversationIndex)
                .applyStyle(conversation)
                .setPublicVersion(publicVersion.build())

        val remoteInput = RemoteInput.Builder(ReplyService.EXTRA_REPLY)
                .setLabel(service.getString(R.string.reply_to, conversation.title))
                .setChoices(service.resources.getStringArray(R.array.reply_choices))
                .setAllowFreeFormInput(true)
                .build()

        val wearableExtender = wearableHelper.buildExtender(conversation)

        actionHelper.addReplyAction(builder, wearableExtender, remoteInput, conversation)
        actionHelper.addNonReplyActions(builder, wearableExtender, conversation)
        actionHelper.addContentIntents(builder, conversation)

        // apply the extenders to the notification
        builder.extend(carHelper.buildExtender(conversation, remoteInput))
        builder.extend(wearableExtender)

        val notification = builder.build()

        if (NotificationConstants.CONVERSATION_ID_OPEN == conversation.id) {
            // skip this notification since we are already on the conversation.
            summaryProvider.skipSummary = true
        } else {
            NotificationManagerCompat.from(service).notify(conversation.id.toInt(), notification)
        }

        return notification
    }

    private fun prepareCommonBuilder(conversation: NotificationConversation, conversationIndex: Int) = NotificationCompat.Builder(service,
            if (conversationIndex == 0) getNotificationChannel(conversation.id) else NotificationUtils.SILENT_CONVERSATION_CHANNEL_ID)
            .setSmallIcon(if (!conversation.groupConversation) R.drawable.ic_stat_notify else R.drawable.ic_stat_notify_group)
            .setAutoCancel(true)
            .setColor(if (Settings.useGlobalThemeColor) Settings.mainColorSet.color else conversation.color)
            .setPriority(if (Settings.headsUp) Notification.PRIORITY_MAX else Notification.PRIORITY_DEFAULT)
            .setCategory(Notification.CATEGORY_MESSAGE)
            .addPerson(conversation)

    private fun prepareBuilder(conversation: NotificationConversation, conversationIndex: Int) =
            prepareCommonBuilder(conversation, conversationIndex)
            .setContentTitle(conversation.title)
            .setShowWhen(true)
            .setTicker(service.getString(R.string.notification_ticker, conversation.title))
            .setVisibility(Notification.VISIBILITY_PRIVATE)
            .setWhen(if (AndroidVersionUtil.isAndroidO) TimeUtils.now else  conversation.timestamp)

    private fun preparePublicBuilder(conversation: NotificationConversation, conversationIndex: Int) =
            prepareCommonBuilder(conversation, conversationIndex)
            .setContentTitle(service.resources.getQuantityString(R.plurals.new_conversations, 1, 1))
            .setContentText(service.resources.getQuantityString(R.plurals.new_messages, conversation.messages.size, conversation.messages.size))
            .setVisibility(Notification.VISIBILITY_PUBLIC)

    private fun buildContactImage(conversation: NotificationConversation): Bitmap? {
        var contactImage = ImageUtils.clipToCircle(ImageUtils.getBitmap(service, conversation.imageUri))

        try {
            val height = service.resources.getDimension(android.R.dimen.notification_large_icon_height)
            val width = service.resources.getDimension(android.R.dimen.notification_large_icon_width)
            contactImage = Bitmap.createScaledBitmap(contactImage!!, width.toInt(), height.toInt(), true)
        } catch (e: Exception) {
        }

        return contactImage
    }

    private fun buildNotificationDefaults(conversation: NotificationConversation, conversationIndex: Int): Int {
        if (WearableCheck.isAndroidWear(service)) {
            return Notification.DEFAULT_ALL
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

        return defaults
    }

    private fun buildMessagingStyle(conversation: NotificationConversation): NotificationCompat.MessagingStyle? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N || !Settings.historyInNotifications) {
            return null
        }

        val messagingStyle = NotificationCompat.MessagingStyle(service.getString(R.string.you))

        if (conversation.groupConversation) {
            messagingStyle.conversationTitle = conversation.title
        }

        val messages = DataSource.getMessages(service, conversation.id, 10)

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
                MimeType.isAudio(message.mimeType!!) -> Html.fromHtml("<i>" + service.getString(R.string.audio_message) + "</i>")
                MimeType.isVideo(message.mimeType!!) -> Html.fromHtml("<i>" + service.getString(R.string.video_message) + "</i>")
                MimeType.isVcard(message.mimeType!!) -> Html.fromHtml("<i>" + service.getString(R.string.contact_card) + "</i>")
                MimeType.isStaticImage(message.mimeType) -> Html.fromHtml("<i>" + service.getString(R.string.picture_message) + "</i>")
                message.mimeType == MimeType.IMAGE_GIF -> Html.fromHtml("<i>" + service.getString(R.string.gif_message) + "</i>")
                MimeType.isExpandedMedia(message.mimeType) -> Html.fromHtml("<i>" + service.getString(R.string.media) + "</i>")
                else -> message.data
            }

            messagingStyle.addMessage(messageText, message.timestamp, from)
        }

        return messagingStyle
    }

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



    //
    //
    // Extension methods on the notification builder
    //
    //



    private fun NotificationCompat.Builder.applyLightsSoundAndVibrate(conversation: NotificationConversation, conversationIndex: Int): NotificationCompat.Builder {
        if (WearableCheck.isAndroidWear(service)) {
            return this
        }

        if (conversation.ledColor != Color.WHITE) {
            this.setLights(conversation.ledColor, 1000, 500)
        }

        if (conversationIndex == 0) {
            val sound = ringtoneProvider.getRingtone(conversation.ringtoneUri)
            if (sound != null) {
                this.setSound(sound)
            }

            if (Settings.vibrate.pattern != null) {
                this.setVibrate(Settings.vibrate.pattern)
            } else if (Settings.vibrate === VibratePattern.OFF) {
                this.setVibrate(LongArray(0))
            }
        }

        return this
    }

    private fun NotificationCompat.Builder.applyStyle(conversation: NotificationConversation): NotificationCompat.Builder {
        val messagingStyle: NotificationCompat.Style? = buildMessagingStyle(conversation)
        var pictureStyle: NotificationCompat.BigPictureStyle? = null
        var inboxStyle: NotificationCompat.InboxStyle? = null

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
                this.setContentText(Html.fromHtml(content, 0))
            } else {
                this.setContentText(Html.fromHtml(content))
            }

            when {
                pictureStyle != null -> {
                    this.setStyle(pictureStyle)
                    this.setContentText(service.getString(R.string.picture_message))
                }
                messagingStyle != null -> this.setStyle(messagingStyle)
                inboxStyle != null -> this.setStyle(inboxStyle)
                else -> this.setStyle(NotificationCompat.BigTextStyle().bigText(Html.fromHtml(content)))
            }
        }

        return this
    }

    private fun NotificationCompat.Builder.addPerson(conversation: NotificationConversation): NotificationCompat.Builder {
        if (!conversation.groupConversation) {
            this.addPerson("tel:" + conversation.phoneNumbers!!)
        } else {
            for (number in conversation.phoneNumbers!!.split(", ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()) {
                this.addPerson("tel:$number")
            }
        }

        return this
    }

    internal fun getNotificationChannel(conversationId: Long): String {
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