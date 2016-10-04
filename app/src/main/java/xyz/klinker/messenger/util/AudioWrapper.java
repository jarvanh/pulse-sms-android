package xyz.klinker.messenger.util;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.util.Log;

import java.io.IOException;
import java.util.Date;

import xyz.klinker.messenger.data.DataSource;
import xyz.klinker.messenger.data.Settings;
import xyz.klinker.messenger.data.model.Conversation;

public class AudioWrapper {

    private static final String TAG = "AudioWrapper";

    private MediaPlayer mediaPlayer;
    private boolean soundEffects;

    public AudioWrapper(Context context, long conversationId) {
        soundEffects = Settings.get(context).soundEffects;
        if (!soundEffects) {
            return;
        }

        try {
            Settings settings = Settings.get(context);
            DataSource source = DataSource.getInstance(context);
            source.open();
            Conversation conversation = source.getConversation(conversationId);
            source.close();

            if (conversation.ringtoneUri != null && !conversation.ringtoneUri.isEmpty()) {
                mediaPlayer = MediaPlayer.create(context, Uri.parse(conversation.ringtoneUri));
            } else if (settings.ringtone != null && !settings.ringtone.isEmpty()) {
                mediaPlayer = MediaPlayer.create(context, Uri.parse(settings.ringtone));
            } else {
                mediaPlayer = MediaPlayer.create(context,
                        RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public AudioWrapper(Context context, int resourceId) {
        soundEffects = Settings.get(context).soundEffects;
        if (!soundEffects) {
            return;
        }

        mediaPlayer = MediaPlayer.create(context, resourceId,
                new AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION_COMMUNICATION_INSTANT)
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

}