package xyz.klinker.messenger.shared.util

import android.annotation.TargetApi
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import androidx.core.app.Person
import androidx.core.content.LocusIdCompat
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat

import java.util.HashSet

import xyz.klinker.messenger.shared.data.model.Conversation

class DynamicShortcutUtils(private val context: Context) {

    fun buildDynamicShortcuts(conversations: List<Conversation>) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            val infos = conversations.take(10).map { conversation ->
                val messenger = ActivityUtils.buildForComponent(ActivityUtils.MESSENGER_ACTIVITY)
                messenger.action = Intent.ACTION_VIEW
                messenger.data = Uri.parse("https://messenger.klinkerapps.com/" + conversation.id)

                val category = HashSet<String>()
                category.add("android.shortcut.conversation")

                val icon = getIcon(conversation)
                val info = ShortcutInfoCompat.Builder(context, conversation.id.toString())
                        .setLocusId(LocusIdCompat(conversation.id.toString()))
                        .setIntent(messenger)
                        .setShortLabel(conversation.title ?: "No title")
                        .setCategories(category)
                        .setLongLived(false)
                        .setIcon(icon)
                        .setPerson(Person.Builder()
                                .setName(conversation.title)
                                .setUri(messenger.dataString)
                                .setIcon(icon)
                                .build()
                        ).build()

                info
            }

            ShortcutManagerCompat.removeAllDynamicShortcuts(context)
            ShortcutManagerCompat.addDynamicShortcuts(context, infos)
        }
    }

    private fun getIcon(conversation: Conversation): IconCompat? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val image = ImageUtils.getBitmap(context, conversation.imageUri)

            if (image != null) {
                createIcon(image)
            } else {
                val color = ContactImageCreator.getLetterPicture(context, conversation)
                createIcon(color)
            }
        } else {
            null
        }
    }

    @TargetApi(Build.VERSION_CODES.O)
    private fun createIcon(bitmap: Bitmap?): IconCompat {
        return if (AndroidVersionUtil.isAndroidO) {
            IconCompat.createWithAdaptiveBitmap(bitmap)
        } else {
            val circleBitmap = ImageUtils.clipToCircle(bitmap)
            IconCompat.createWithBitmap(circleBitmap)
        }
    }
}
