package xyz.klinker.messenger.util;

import android.app.UiModeManager;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.Configuration;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.support.annotation.VisibleForTesting;
import android.util.Log;

import java.io.IOException;
import java.util.Date;

import xyz.klinker.messenger.data.DataSource;
import xyz.klinker.messenger.data.Settings;
import xyz.klinker.messenger.data.model.Conversation;

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
            Settings settings = Settings.get(context);
            DataSource source = DataSource.getInstance(context);
            source.open();
            Conversation conversation = source.getConversation(conversationId);
            source.close();

            Uri tone = null;
            if (conversation.ringtoneUri != null && conversation.ringtoneUri.isEmpty()) {
                tone = null;
            } else if (conversation.ringtoneUri != null && !conversation.ringtoneUri.isEmpty()) {
                tone = Uri.parse(conversation.ringtoneUri);
            } else if (settings.ringtone != null && !settings.ringtone.isEmpty()) {
                tone = Uri.parse(settings.ringtone);
            } else if (settings.ringtone.isEmpty()) {
                tone = null;
            } else {
                tone = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            }

            if (tone != null) {
                mediaPlayer = MediaPlayer.create(context, tone, null, new AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION_COMMUNICATION_INSTANT)
                        .setLegacyStreamType(AudioManager.STREAM_RING)
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

        mediaPlayer = MediaPlayer.create(context, resourceId,
                new AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION_COMMUNICATION_INSTANT)
                        .setLegacyStreamType(AudioManager.STREAM_RING)
                        .build(),
                1
        );
    }

    public void play() {
        if (!soundEffects || mediaPlayer == null) {
            return;
        }

        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                Log.v(TAG, "completed sound effect");
                mp.release();
            }
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