package xyz.klinker.messenger.shared.service;

import android.app.IntentService;
import android.content.Intent;
import android.database.Cursor;
import android.support.v4.app.NotificationManagerCompat;

import java.util.List;

import xyz.klinker.messenger.api.implementation.Account;
import xyz.klinker.messenger.api.implementation.ApiUtils;
import xyz.klinker.messenger.shared.data.DataSource;
import xyz.klinker.messenger.shared.data.MimeType;
import xyz.klinker.messenger.shared.data.model.Message;
import xyz.klinker.messenger.shared.receiver.ConversationListUpdatedReceiver;
import xyz.klinker.messenger.shared.util.UnreadBadger;
import xyz.klinker.messenger.shared.widget.MessengerAppWidgetProvider;

public class NotificationDeleteService extends IntentService {

    public static final String EXTRA_CONVERSATION_ID = "conversation_id";
    public static final String EXTRA_MESSAGE_ID = "message_id";

    public NotificationDeleteService() {
        super("NotificationDeleteService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        long messageId = intent.getLongExtra(EXTRA_MESSAGE_ID, -1);
        long conversationId = intent.getLongExtra(EXTRA_CONVERSATION_ID, -1);

        DataSource source = DataSource.Companion.getInstance(this);
        source.open();
        source.deleteMessage(messageId);

        List<Message> messages = source.getMessages(conversationId, 1);
        Message latest = null;

        if (messages.size() == 1) {
            latest = messages.get(0);
        }

        if (latest == null) {
            source.deleteConversation(conversationId);
        } else if (latest.mimeType.equals(MimeType.TEXT_PLAIN)) {
            source.updateConversation(conversationId, true, latest.timestamp, latest.data, latest.mimeType, false);
        }

        // cancel the notification we just replied to or
        // if there are no more notifications, cancel the summary as well
        Cursor unseenMessages = source.getUnseenMessages();
        if (unseenMessages.getCount() <= 0) {
            NotificationManagerCompat.from(this).cancelAll();
        } else {
            NotificationManagerCompat.from(this).cancel((int) conversationId);
        }

        unseenMessages.close();
        source.close();

        new ApiUtils().dismissNotification(Account.get(this).accountId,
                Account.get(this).deviceId,
                conversationId);

        ConversationListUpdatedReceiver.sendBroadcast(this, conversationId,
                (latest != null && latest.mimeType.equals(MimeType.TEXT_PLAIN)) ? latest.data : "",
                true);

        new UnreadBadger(this).writeCountFromDatabase();
        MessengerAppWidgetProvider.refreshWidget(this);
    }
}
