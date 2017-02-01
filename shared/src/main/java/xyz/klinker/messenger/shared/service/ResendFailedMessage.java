package xyz.klinker.messenger.shared.service;

import android.app.IntentService;
import android.content.Intent;
import android.support.v4.app.NotificationManagerCompat;

import xyz.klinker.messenger.shared.data.DataSource;
import xyz.klinker.messenger.shared.data.model.Conversation;
import xyz.klinker.messenger.shared.data.model.Message;
import xyz.klinker.messenger.shared.util.DualSimUtils;
import xyz.klinker.messenger.shared.util.SendUtils;

public class ResendFailedMessage extends IntentService {

    public static final String EXTRA_MESSAGE_ID = "arg_message_id";

    public ResendFailedMessage() {
        super("ResendFailedMessage");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        long messageId = intent.getLongExtra(EXTRA_MESSAGE_ID, -1);
        if (messageId == -1) {
            return;
        }

        NotificationManagerCompat.from(this).cancel(6666 + (int) messageId);

        DataSource source = DataSource.getInstance(this);
        source.open();

        Message original = source.getMessage(messageId);
        if (original == null) {
            return;
        }

        Conversation conversation = source.getConversation(original.conversationId);

        Message m = new Message();
        m.conversationId = original.conversationId;
        m.type = Message.TYPE_SENDING;
        m.data = original.data;
        m.timestamp = original.timestamp;
        m.mimeType = original.mimeType;
        m.read = true;
        m.seen = true;
        m.from = null;
        m.color = null;
        m.simPhoneNumber = conversation.simSubscriptionId != null ? DualSimUtils.get(this)
                .getPhoneNumberFromSimSubscription(conversation.simSubscriptionId) : null;

        source.deleteMessage(messageId);
        source.insertMessage(this, m, m.conversationId);

        new SendUtils(conversation.simSubscriptionId)
                .send(this, m.data, conversation.phoneNumbers);

        source.close();
    }
}
