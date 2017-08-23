package xyz.klinker.messenger.shared.service;

import android.app.IntentService;
import android.content.Intent;
import android.database.Cursor;
import android.support.v4.app.NotificationManagerCompat;

import xyz.klinker.messenger.api.implementation.Account;
import xyz.klinker.messenger.api.implementation.ApiUtils;
import xyz.klinker.messenger.shared.data.DataSource;
import xyz.klinker.messenger.shared.data.model.Conversation;
import xyz.klinker.messenger.shared.receiver.ConversationListUpdatedReceiver;
import xyz.klinker.messenger.shared.util.UnreadBadger;
import xyz.klinker.messenger.shared.widget.MessengerAppWidgetProvider;

public class NotificationMarkReadService extends IntentService {

    public static final String EXTRA_CONVERSATION_ID = "conversation_id";

    public NotificationMarkReadService() {
        super("NotificationMarkReadService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        long conversationId = intent.getLongExtra(EXTRA_CONVERSATION_ID, -1);

        DataSource source = DataSource.Companion.getInstance(this);
        source.open();
        source.readConversation(this, conversationId);
        Conversation conversation = source.getConversation(conversationId);

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

        ConversationListUpdatedReceiver.sendBroadcast(this, conversationId, conversation == null ? "" : conversation.snippet, true);

        new UnreadBadger(this).writeCountFromDatabase();
        MessengerAppWidgetProvider.refreshWidget(this);
    }
}
