package xyz.klinker.messenger.shared.service;

import android.app.IntentService;
import android.content.Intent;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;

import xyz.klinker.messenger.api.implementation.Account;
import xyz.klinker.messenger.shared.data.DataSource;
import xyz.klinker.messenger.shared.data.model.Conversation;
import xyz.klinker.messenger.shared.data.model.Message;
import xyz.klinker.messenger.shared.service.jobs.MarkAsSentJob;
import xyz.klinker.messenger.shared.util.DualSimUtils;
import xyz.klinker.messenger.shared.util.SendUtils;

public class ResendFailedMessage extends IntentService {

    public static final String EXTRA_MESSAGE_ID = "arg_message_id";

    public ResendFailedMessage() {
        super("ResendFailedMessage");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.v("ResendFailed", "attempting to resend");

        long messageId = intent.getLongExtra(EXTRA_MESSAGE_ID, -1);
        if (messageId == -1) {
            return;
        }

        NotificationManagerCompat.from(this).cancel(6666 + (int) messageId);

        DataSource source = DataSource.INSTANCE;

        Message original = source.getMessage(this, messageId);
        if (original == null) {
            return;
        }

        Conversation conversation = source.getConversation(this, original.getConversationId());

        Message m = new Message();
        m.setConversationId(original.getConversationId());
        m.setType(Message.Companion.getTYPE_SENDING());
        m.setData(original.getData());
        m.setTimestamp(original.getTimestamp());
        m.setMimeType(original.getMimeType());
        m.setRead(true);
        m.setSeen(true);
        m.setFrom(null);
        m.setColor(null);
        m.setSimPhoneNumber(conversation.getSimSubscriptionId() != null ? DualSimUtils.get(this)
                .getPhoneNumberFromSimSubscription(conversation.getSimSubscriptionId()) : null);
        m.setSentDeviceId(Account.INSTANCE.exists() ? Long.parseLong(Account.INSTANCE.getDeviceId()) : -1L);

        source.deleteMessage(this, messageId);
        messageId = source.insertMessage(this, m, m.getConversationId(), true);

        new SendUtils(conversation.getSimSubscriptionId()).setForceSplitMessage(true)
                .setRetryFailedMessages(false)
                .send(this, m.getData(), conversation.getPhoneNumbers());
        MarkAsSentJob.Companion.scheduleNextRun(this, messageId);
    }
}
