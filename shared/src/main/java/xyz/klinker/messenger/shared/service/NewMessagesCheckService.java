package xyz.klinker.messenger.shared.service;

import android.app.Activity;
import android.app.IntentService;
import android.content.Intent;
import android.database.Cursor;
import android.provider.Telephony;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import xyz.klinker.messenger.shared.R;
import xyz.klinker.messenger.api.entity.AddMessagesRequest;
import xyz.klinker.messenger.api.entity.MessageBody;
import xyz.klinker.messenger.api.implementation.Account;
import xyz.klinker.messenger.api.implementation.ApiUtils;
import xyz.klinker.messenger.shared.data.ColorSet;
import xyz.klinker.messenger.shared.data.DataSource;
import xyz.klinker.messenger.shared.data.MimeType;
import xyz.klinker.messenger.shared.data.Settings;
import xyz.klinker.messenger.shared.data.model.Message;
import xyz.klinker.messenger.encryption.EncryptionUtils;
import xyz.klinker.messenger.shared.receiver.MessageListUpdatedReceiver;
import xyz.klinker.messenger.shared.util.PaginationUtils;
import xyz.klinker.messenger.shared.util.PhoneNumberUtils;
import xyz.klinker.messenger.shared.util.SmsMmsUtils;

/**
 * Check whether or not there are messages in the internal database, that are not in Pulse's
 * database. This is useful for if a user goes away from Pulse for awhile, then wants to return to
 * it.
 */
public class NewMessagesCheckService extends IntentService {

    public static void startService(Activity activity) {
        // only safe to start from the UI because it doesn't provide a foreground notification
        // for Android O.
        activity.startService(new Intent(activity, NewMessagesCheckService.class));
    }

    public static final String REFRESH_WHOLE_CONVERSATION_LIST = "xyz.klinker.messenger.REFRESH_WHOLE_CONVERSATION_LIST";

    public NewMessagesCheckService() {
        super("NewMessageCheckService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (ApiDownloadService.IS_RUNNING || (Account.get(this).exists() && !Account.get(this).primary)) {
            return;
        }

        // grab the latest 60 messages from Pulse's database
        // grab the latest 20 messages from the the internal SMS/MMS database
        // iterate over the internal messages and see if they are in the list from Pulse's database (search by text is fine)
        // if they are:
        //      continue, no problems here
        // if they aren't:
        //      insert them into the correct conversation and give the conversation update broadcast
        //      should I worry about updating the conversation list here?

        DataSource source = DataSource.getInstance(this);
        source.open();

        List<Message> pulseMessages = source.getNumberOfMessages(60);
        Cursor internalMessages = SmsMmsUtils.getLatestSmsMessages(this, 20);

        List<Message> messagesToInsert = new ArrayList<>();
        List<String> addressesForMessages = new ArrayList<>();
        if (internalMessages != null && internalMessages.moveToFirst()) {
            do {
                String body = internalMessages.getString(internalMessages.getColumnIndex(Telephony.Sms.BODY));
                if (!alreadyInDatabase(pulseMessages, body.trim())) {
                    Message message = new Message();

                    message.type = SmsMmsUtils.getSmsMessageType(internalMessages);
                    message.data = body.trim();
                    message.timestamp = internalMessages.getLong(internalMessages.getColumnIndex(Telephony.Sms.DATE));
                    message.mimeType = MimeType.TEXT_PLAIN;
                    message.read = true;
                    message.seen = true;

                    messagesToInsert.add(message);
                    addressesForMessages.add(PhoneNumberUtils.clearFormatting(
                            internalMessages.getString(internalMessages.getColumnIndex(Telephony.Sms.ADDRESS))));
                }
            } while (internalMessages.moveToNext());

            try {
                internalMessages.close();
            } catch (Exception e) { }
        }

        for (int i = 0; i < messagesToInsert.size(); i++) {
            Message message = messagesToInsert.get(i);
            long conversationId = source.insertMessage(message,
                    PhoneNumberUtils.clearFormatting(addressesForMessages.get(i)), this);
            MessageListUpdatedReceiver.sendBroadcast(this, conversationId);
        }

        if (messagesToInsert.size() > 0) {
            sendBroadcast(new Intent(REFRESH_WHOLE_CONVERSATION_LIST));
        }

        source.close();
    }

    private boolean alreadyInDatabase(List<Message> messages, String bodyToSearch) {
        for (Message message : messages) {
            if (message.mimeType.equals(MimeType.TEXT_PLAIN) && message.data.equals(bodyToSearch)) {
                return true;
            }
        }
        return false;
    }
}
