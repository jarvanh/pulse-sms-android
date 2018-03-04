package xyz.klinker.messenger.shared.service.notification

import android.content.Context
import android.media.RingtoneManager
import android.net.Uri
import xyz.klinker.messenger.shared.data.Settings

class NotificationRingtoneProvider(private val context: Context) {

    fun getRingtone(conversationRingtone: String?): Uri? {
        try {
            val globalUri = Settings.ringtone

            if (conversationRingtone == null || conversationRingtone.contains("default") ||
                    conversationRingtone == "content://settings/system/notification_sound") {

                return when {
                    globalUri == null -> null
                    globalUri.isEmpty() || globalUri == "silent" -> null
                    ringtoneExists(context, globalUri) -> Uri.parse(globalUri)
                    else -> RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                }
            } else {
                return when {
                    conversationRingtone.isEmpty() || conversationRingtone == "silent" -> null
                    ringtoneExists(context, conversationRingtone) -> Uri.parse(conversationRingtone)
                    else -> RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                }
            }
        } catch (e: Exception) {
            return RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        }
    }

    private fun ringtoneExists(context: Context, uri: String): Boolean {
        return if (uri.contains("file://")) {
            false
        } else RingtoneManager.getRingtone(context, Uri.parse(uri)) != null
    }
}