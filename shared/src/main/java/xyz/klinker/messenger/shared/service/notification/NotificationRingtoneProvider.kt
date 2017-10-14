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
                // there is no conversation specific ringtone defined

                if (globalUri == null || globalUri.isEmpty()) {
                    return null
                }
                return if (ringtoneExists(context, globalUri)) {
                    // the global ringtone is available to use
                    Uri.parse(globalUri)
                } else {
                    // there is no global ringtone defined, or it doesn't exist on the system
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                }
            } else {
                return if (conversationRingtone.isEmpty()) {
                    null
                } else if (ringtoneExists(context, conversationRingtone)) {
                    // conversation ringtone exists and can be played
                    Uri.parse(conversationRingtone)
                } else {
                    // the global ringtone is available to use
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
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