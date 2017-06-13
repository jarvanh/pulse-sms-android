package xyz.klinker.messenger.shared.service;

import android.app.IntentService;
import android.app.Notification;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;

import xyz.klinker.messenger.shared.R;
import xyz.klinker.messenger.shared.data.DataSource;
import xyz.klinker.messenger.shared.data.model.Message;
import xyz.klinker.messenger.shared.receiver.MessageListUpdatedReceiver;
import xyz.klinker.messenger.shared.util.AndroidVersionUtil;
import xyz.klinker.messenger.shared.util.NotificationUtils;
import xyz.klinker.messenger.shared.util.media.MediaMessageParserFactory;
import xyz.klinker.messenger.shared.util.media.MediaParser;

public class MediaParserService extends IntentService {

    private static final int MEDIA_PARSE_FOREGROUND_ID = 1334;

    public static final String EXTRA_CONVERSATION_ID = "conversation_id";
    public static final String EXTRA_BODY_TEXT = "body_text";

    public MediaParserService() {
        super("MediaParserService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent == null) {
            return;
        }

        if (AndroidVersionUtil.isAndroidO()) {
            Notification notification = new NotificationCompat.Builder(this,
                    NotificationUtils.MEDIA_PARSE_CHANNEL_ID)
                    .setContentTitle(getString(R.string.media_parse_text))
                    .setSmallIcon(R.drawable.ic_stat_notify_group)
                    .setProgress(0, 0, true)
                    .setLocalOnly(true)
                    .setColor(getResources().getColor(R.color.colorPrimary))
                    .setOngoing(true)
                    .setPriority(NotificationCompat.PRIORITY_MIN)
                    .build();
            startForeground(MEDIA_PARSE_FOREGROUND_ID, notification);
        }

        long conversationId = intent.getLongExtra(EXTRA_CONVERSATION_ID, -1L);
        String text = intent.getStringExtra(EXTRA_BODY_TEXT);

        if (conversationId == -1L || text == null) {
            return;
        }

        MediaParser parser = createParser(this, text);
        if (parser == null) {
            return;
        }

        Message message = parser.parse(conversationId);
        if (message != null) {
            DataSource source = DataSource.getInstance(this);
            source.open();
            source.insertMessage(this, message, conversationId, true);
            source.close();

            MessageListUpdatedReceiver.sendBroadcast(this, conversationId, message.data, message.type);
        }

        if (AndroidVersionUtil.isAndroidO()) {
            stopForeground(true);
        }
    }

    public static MediaParser createParser(Context context, String text) {
        return new MediaMessageParserFactory().getInstance(context, text);
    }
}
