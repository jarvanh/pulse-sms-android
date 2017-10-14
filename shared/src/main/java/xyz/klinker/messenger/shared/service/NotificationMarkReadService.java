package xyz.klinker.messenger.shared.service;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.support.v4.app.NotificationManagerCompat;

import xyz.klinker.messenger.api.implementation.Account;
import xyz.klinker.messenger.api.implementation.ApiUtils;
import xyz.klinker.messenger.shared.data.DataSource;
import xyz.klinker.messenger.shared.data.model.Conversation;
import xyz.klinker.messenger.shared.receiver.ConversationListUpdatedReceiver;
import xyz.klinker.messenger.shared.util.CursorUtil;
import xyz.klinker.messenger.shared.util.UnreadBadger;
import xyz.klinker.messenger.shared.widget.MessengerAppWidgetProvider;

public class NotificationMarkReadService extends IntentService {

    public static final String EXTRA_CONVERSATION_ID = "conversation_id";

    public NotificationMarkReadService() {
        super("NotificationMarkReadService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        handle(intent, this);
    }

    public static void handle(Intent intent, Context context) {
        long conversationId = intent.getLongExtra(EXTRA_CONVERSATION_ID, -1);

        DataSource source = DataSource.INSTANCE;
        source.readConversation(context, conversationId);
        Conversation conversation = source.getConversation(context, conversationId);

        // cancel the notification we just replied to or
        // if there are no more notifications, cancel the summary as well
        Cursor unseenMessages = source.getUnseenMessages(context);
        if (unseenMessages.getCount() <= 0) {
            NotificationManagerCompat.from(context).cancelAll();
        } else {
            NotificationManagerCompat.from(context).cancel((int) conversationId);
        }

        CursorUtil.closeSilent(unseenMessages);

        ApiUtils.INSTANCE.dismissNotification(Account.INSTANCE.getAccountId(),
                Account.INSTANCE.getDeviceId(),
                conversationId);

        ConversationListUpdatedReceiver.sendBroadcast(context, conversationId, conversation == null ? "" : conversation.getSnippet(), true);

        new UnreadBadger(context).writeCountFromDatabase();
        MessengerAppWidgetProvider.Companion.refreshWidget(context);
    }
}
