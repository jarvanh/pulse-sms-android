package xyz.klinker.messenger.shared.util;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.UiModeManager;
import android.content.Context;
import android.content.res.Configuration;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.VisibleForTesting;
import android.util.Log;

import xyz.klinker.messenger.shared.data.DataSource;
import xyz.klinker.messenger.shared.data.Settings;
import xyz.klinker.messenger.shared.data.model.Conversation;
import xyz.klinker.messenger.shared.service.NotificationService;

import static android.content.Context.UI_MODE_SERVICE;

public class AudioWrapper {

    private static final String TAG = "AudioWrapper";

    private MediaPlayer mediaPlayer;
    private boolean soundEffects;

    public AudioWrapper(Context context, long conversationId) {
        soundEffects = Settings.get(context).soundEffects;
        if (!soundEffects || !shouldPlaySound(context)) {
            return;
        }

        try {
            DataSource source = DataSource.INSTANCE;
            Conversation conversation = source.getConversation(context, conversationId);

            Uri tone;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                NotificationChannel channel = manager.getNotificationChannel(conversationId + "");

                if (channel != null) {
                    tone = channel.getSound();
                } else {
                    tone = NotificationService.getRingtone(context, conversation.ringtoneUri);
                }
            } else {
                tone = NotificationService.getRingtone(context, conversation.ringtoneUri);
            }

            if (tone != null) {
                mediaPlayer = MediaPlayer.create(context, tone, null, new AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_UNKNOWN)
                        .setLegacyStreamType(AudioManager.STREAM_NOTIFICATION)
                        .build(), 1);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public AudioWrapper(Context context, int resourceId) {
        soundEffects = Settings.get(context).soundEffects;
        if (!soundEffects || !shouldPlaySound(context)) {
            return;
        }

        mediaPlayer = MediaPlayer.create(context, resourceId, new AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_UNKNOWN)
                        .setLegacyStreamType(AudioManager.STREAM_NOTIFICATION)
                        .build(), 1);
    }

    public void play() {
        if (!soundEffects || mediaPlayer == null) {
            return;
        }

        mediaPlayer.setOnCompletionListener(mp -> {
            Log.v(TAG, "completed sound effect");
            mp.release();
        });

        mediaPlayer.start();
    }

    @VisibleForTesting
    protected static boolean shouldPlaySound(Context context) {
        // we don't really want to play sounds on the TV or on a watch

        UiModeManager uiModeManager = (UiModeManager) context.getSystemService(UI_MODE_SERVICE);
        if (uiModeManager.getCurrentModeType() == Configuration.UI_MODE_TYPE_TELEVISION ||
                uiModeManager.getCurrentModeType() == Configuration.UI_MODE_TYPE_WATCH) {
            return false;
        } else {
            return true;
        }
    }

}