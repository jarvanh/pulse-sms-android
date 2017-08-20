package xyz.klinker.messenger.shared.service;

import android.app.Activity;
import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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

        SharedPreferences sharedPreferences = Settings.get(this).getSharedPrefs(this);
        long lastRun = sharedPreferences.getLong("new_message_check_last_run", 0L);


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
                String messageBody = internalMessages.getString(internalMessages.getColumnIndex(Telephony.Sms.BODY)).trim();
                int messageType = SmsMmsUtils.getSmsMessageType(internalMessages);
                long messageTimestamp = internalMessages.getLong(internalMessages.getColumnIndex(Telephony.Sms.DATE));
                if (!alreadyInDatabase(pulseMessages, messageBody, messageType) && messageTimestamp > lastRun) {
                    Message message = new Message();

                    message.type = messageType;
                    message.data = messageBody;
                    message.timestamp = messageTimestamp;
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

        List<Long> conversationsToRefresh = new ArrayList<>();
        for (int i = 0; i < messagesToInsert.size(); i++) {
            Message message = messagesToInsert.get(i);
            long conversationId = source.insertMessage(message,
                    PhoneNumberUtils.clearFormatting(addressesForMessages.get(i)), this);

            if (!conversationsToRefresh.contains(conversationId)) {
                conversationsToRefresh.add(conversationId);
            }
        }

        for (Long conversationId : conversationsToRefresh) {
            MessageListUpdatedReceiver.sendBroadcast(this, conversationId);
        }

        if (conversationsToRefresh.size() > 0) {
            //sendBroadcast(new Intent(REFRESH_WHOLE_CONVERSATION_LIST));
        }

        source.close();
        NewMessagesCheckService.writeLastRun(this);
    }

    public static void writeLastRun(Context context) {
        try {
            Settings.get(context).getSharedPrefs(context)
                    .edit()
                    .putLong("new_message_check_last_run", System.currentTimeMillis())
                    .apply();
        } catch (Exception e) {
            // in robolectric, i don't want it to crash
        }
    }

    private boolean alreadyInDatabase(List<Message> messages, String bodyToSearch, int newMessageType) {
        for (Message message : messages) {
            if (message.mimeType.equals(MimeType.TEXT_PLAIN) && newMessageType == message.type &&
                    message.data.equals(bodyToSearch)) {
                return true;
            }
        }
        return false;
    }
}
