package xyz.klinker.messenger.shared.service.notification.conversation

import android.os.Build
import android.support.v4.app.NotificationCompat
import android.text.Html
import android.text.Spanned
import xyz.klinker.messenger.shared.R
import xyz.klinker.messenger.shared.data.MimeType
import xyz.klinker.messenger.shared.data.model.Message
import xyz.klinker.messenger.shared.data.pojo.NotificationConversation
import xyz.klinker.messenger.shared.service.notification.NotificationService

class NotificationWearableHelper(private val service: NotificationService, private val conversationProvider: NotificationConversationProvider) {

    fun buildExtender(conversation: NotificationConversation): NotificationCompat.WearableExtender {
        val wear = NotificationCompat.Builder(service, conversationProvider.getNotificationChannel(conversation.id))
                .setStyle(NotificationCompat.BigTextStyle()
                        .setBigContentTitle(conversation.title)
                        .bigText(getWearableSecondPageConversation(conversation)))

        return NotificationCompat.WearableExtender().addPage(wear.build())
    }

    private fun getWearableSecondPageConversation(conversation: NotificationConversation): Spanned {
        val source = service.dataSource
        val messages = source.getMessages(service, conversation.id, 10)

        val you = service.getString(R.string.you)
        val builder = StringBuilder()

        for (message in messages) {
            val messageText = when {
                MimeType.isAudio(message.mimeType!!) -> "<i>" + service.getString(R.string.audio_message) + "</i>"
                MimeType.isVideo(message.mimeType!!) -> "<i>" + service.getString(R.string.video_message) + "</i>"
                MimeType.isVcard(message.mimeType!!) -> "<i>" + service.getString(R.string.contact_card) + "</i>"
                MimeType.isStaticImage(message.mimeType) -> "<i>" + service.getString(R.string.picture_message) + "</i>"
                message.mimeType == MimeType.IMAGE_GIF -> "<i>" + service.getString(R.string.gif_message) + "</i>"
                MimeType.isExpandedMedia(message.mimeType) -> "<i>" + service.getString(R.string.media) + "</i>"
                else -> message.data
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
}