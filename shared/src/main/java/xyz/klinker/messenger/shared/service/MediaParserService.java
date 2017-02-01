package xyz.klinker.messenger.shared.service;

import android.app.IntentService;
import android.content.Intent;

import xyz.klinker.messenger.shared.data.DataSource;
import xyz.klinker.messenger.shared.data.model.Message;
import xyz.klinker.messenger.shared.receiver.MessageListUpdatedReceiver;
import xyz.klinker.messenger.shared.util.media.MediaMessageParserFactory;
import xyz.klinker.messenger.shared.util.media.MediaParser;

public class MediaParserService extends IntentService {

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

        long conversationId = intent.getLongExtra(EXTRA_CONVERSATION_ID, -1L);
        String text = intent.getStringExtra(EXTRA_BODY_TEXT);

        if (conversationId == -1L || text == null) {
            return;
        }

        MediaParser parser = new MediaMessageParserFactory().getInstance(this, text);

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
    }
}
