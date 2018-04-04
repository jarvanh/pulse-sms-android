package xyz.klinker.messenger.shared.util

import android.annotation.SuppressLint
import android.app.NotificationManager
import android.app.UiModeManager
import android.content.Context
import android.content.Context.NOTIFICATION_SERVICE
import android.content.res.Configuration
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.util.Log

import xyz.klinker.messenger.shared.data.DataSource
import xyz.klinker.messenger.shared.data.Settings
import xyz.klinker.messenger.shared.service.notification.NotificationService

import android.content.Context.UI_MODE_SERVICE
import android.support.v4.app.NotificationManagerCompat
import xyz.klinker.messenger.shared.service.notification.NotificationRingtoneProvider

class AudioWrapper {

    private var mediaPlayer: MediaPlayer? = null
    private var soundEffects: Boolean = false

    constructor(context: Context, conversationId: Long) {
        soundEffects = Settings.soundEffects
        if (!soundEffects || !shouldPlaySound(context)) {
            return
        }

        try {
            val source = DataSource
            val conversation = source.getConversation(context, conversationId)

            val tone: Uri?
            tone = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                val channel = manager.getNotificationChannel(conversationId.toString() + "")

                if (channel != null) {
                    channel.sound
                } else {
                    NotificationRingtoneProvider(context).getRingtone(conversation!!.ringtoneUri)
                }
            } else {
                NotificationRingtoneProvider(context).getRingtone(conversation!!.ringtoneUri)
            }

            if (tone != null) {
                mediaPlayer = MediaPlayer.create(context, tone, null, AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_UNKNOWN)
                        .setLegacyStreamType(AudioManager.STREAM_NOTIFICATION)
                        .build(), 1)
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    constructor(context: Context, resourceId: Int) {
        soundEffects = Settings.soundEffects
        if (!soundEffects || !shouldPlaySound(context)) {
            return
        }

       try {
           mediaPlayer = MediaPlayer.create(context, resourceId, AudioAttributes.Builder()
                   .setContentType(AudioAttributes.CONTENT_TYPE_UNKNOWN)
                   .setLegacyStreamType(AudioManager.STREAM_NOTIFICATION)
                   .build(), 1)
       } catch (ex: Exception) {
       }
    }

    fun play() {
        if (!soundEffects || mediaPlayer == null) {
            return
        }

        mediaPlayer!!.setOnCompletionListener { mp ->
            Log.v(TAG, "completed sound effect")
            mp.release()
        }

        mediaPlayer!!.start()
    }

    companion object {
        private val TAG = "AudioWrapper"

        @SuppressLint("NewApi")
        fun shouldPlaySound(context: Context, androidVersion: Int = Build.VERSION.SDK_INT): Boolean {
            // we don't want to play a sound in do not disturb mode
            val isDoNotDisturb = if (false) { //androidVersion >= Build.VERSION_CODES.M) {
                val manager = context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
                val dndMode = manager.currentInterruptionFilter
                dndMode == NotificationManager.INTERRUPTION_FILTER_ALARMS || dndMode == NotificationManager.INTERRUPTION_FILTER_ALL
            } else {
                false
            }

            // we don't really want to play sounds on the TV or on a watch
            val uiModeManager = context.getSystemService(UI_MODE_SERVICE) as UiModeManager
            val isWatch = uiModeManager.currentModeType == Configuration.UI_MODE_TYPE_WATCH
            val isTv = uiModeManager.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION

            return !isWatch && !isTv && !isDoNotDisturb
        }
    }

}