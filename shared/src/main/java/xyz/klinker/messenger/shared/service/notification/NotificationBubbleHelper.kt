package xyz.klinker.messenger.shared.service.notification

import android.annotation.TargetApi
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Person
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import xyz.klinker.messenger.shared.MessengerActivityExtras
import xyz.klinker.messenger.shared.R
import xyz.klinker.messenger.shared.data.model.Conversation
import xyz.klinker.messenger.shared.util.*

@RequiresApi(Build.VERSION_CODES.Q)
object NotificationBubbleHelper {

    fun showBubble(context: Context, conversation: Conversation) {
        val icon = getIcon(context, conversation)
        val person = Person.Builder()
                .setName(conversation.title)
                .setIcon(icon)
                .setImportant(true)
                .build()
        val contentUri = Uri.parse("https://messenger.klinkerapps.com/${conversation.id}")

        val intent = PendingIntent.getActivity(
                context,
                conversation.id.toInt(),
                ActivityUtils.buildForComponent(ActivityUtils.BUBBLE_ACTIVITY)
                        .setAction(Intent.ACTION_VIEW)
                        .setData(contentUri),
                PendingIntent.FLAG_UPDATE_CURRENT
        )

        val open = ActivityUtils.buildForComponent(ActivityUtils.MESSENGER_ACTIVITY)
        open.putExtra(MessengerActivityExtras.EXTRA_CONVERSATION_ID, conversation.id)
        open.putExtra(MessengerActivityExtras.EXTRA_FROM_NOTIFICATION, true)
        open.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        val pendingOpen = PendingIntent.getActivity(context, conversation.id.toInt(), open, PendingIntent.FLAG_UPDATE_CURRENT)

        val builder = Notification.Builder(context, NotificationUtils.BUBBLE_CHANNEL_ID)
                .setContentIntent(pendingOpen)
                .setBubbleMetadata(
                        Notification.BubbleMetadata.Builder()
                                .setDesiredHeight(DensityUtil.toDp(context, 400))
                                .setIcon(icon)
                                .setIntent(intent)
                                .setAutoExpandBubble(true)
                                .setSuppressNotification(true)
                                .build()
                )
                .setContentTitle(conversation.title)
                .setContentText(conversation.title)
                .setSmallIcon(R.drawable.ic_stat_notify)
                .setCategory(Notification.CATEGORY_MESSAGE)
                .setShowWhen(false)
                .addPerson(person)

        val notificationManager = context.getSystemService(NotificationManager::class.java)
        notificationManager.notify(conversation.id.toInt() * 3, builder.build())
    }

    private fun getIcon(context: Context, conversation: Conversation): Icon {
        val image = ImageUtils.getBitmap(context, conversation.imageUri)
        return if (image != null) {
            createIcon(image)
        } else {
            val color = ContactImageCreator.getLetterPicture(context, conversation)
            createIcon(color)
        }
    }

    @TargetApi(Build.VERSION_CODES.O)
    private fun createIcon(bitmap: Bitmap?): Icon {
        return if (AndroidVersionUtil.isAndroidO) {
            Icon.createWithAdaptiveBitmap(bitmap)
        } else {
            val circleBitmap = ImageUtils.clipToCircle(bitmap)
            Icon.createWithBitmap(circleBitmap)
        }
    }
}