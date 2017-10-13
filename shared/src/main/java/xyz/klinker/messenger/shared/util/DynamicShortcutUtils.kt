package xyz.klinker.messenger.shared.util

import android.annotation.TargetApi
import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.Bitmap
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Build

import java.util.ArrayList
import java.util.HashSet

import xyz.klinker.messenger.shared.data.model.Conversation

class DynamicShortcutUtils(private val context: Context) {
    val manager: ShortcutManager by lazy {
        context.getSystemService(Context.SHORTCUT_SERVICE) as ShortcutManager
    }

    fun buildDynamicShortcuts(conversations: List<Conversation>) {
        var conversations = conversations
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            if (conversations.size > 3) {
                conversations = conversations.subList(0, 3)
            }

            val infos = ArrayList<ShortcutInfo>()

            for (conversation in conversations) {
                val messenger = ActivityUtils.buildForComponent(ActivityUtils.MESSENGER_ACTIVITY)
                messenger.action = Intent.ACTION_VIEW
                messenger.data = Uri.parse("https://messenger.klinkerapps.com/" + conversation.id)

                val category = HashSet<String>()
                category.add("android.shortcut.conversation")

                val id = if (conversation.title == null || conversation.title!!.isEmpty())
                    conversation.id.toString() + ""
                else conversation.title

                val info = ShortcutInfo.Builder(context, id)
                        .setIntent(messenger)
                        .setRank(infos.size)
                        .setShortLabel(if (conversation.title?.isEmpty() == true) "No title" else conversation.title)
                        .setCategories(category)
                        .setIcon(getIcon(conversation))
                        .build()

                infos.add(info)
            }

            manager.dynamicShortcuts = infos
        }
    }

    private fun getIcon(conversation: Conversation): Icon? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val image = ImageUtils.getBitmap(context, conversation.imageUri)

            if (image != null) {
                return createIcon(image)
            } else {
                val color = ContactImageCreator.getLetterPicture(context, conversation)
                return createIcon(color)
            }
        } else {
            return null
        }
    }

    @TargetApi(Build.VERSION_CODES.O)
    private fun createIcon(bitmap: Bitmap): Icon {
        if (AndroidVersionUtil.isAndroidO) {
            return Icon.createWithAdaptiveBitmap(bitmap)
        } else {
            val circleBitmap = ImageUtils.clipToCircle(bitmap)
            return Icon.createWithBitmap(circleBitmap)
        }
    }
}
