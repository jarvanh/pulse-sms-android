package xyz.klinker.messenger.shared.service;

import android.app.Activity;
import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.provider.Telephony;
import android.support.annotation.VisibleForTesting;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import xyz.klinker.messenger.api.implementation.Account;
import xyz.klinker.messenger.shared.data.DataSource;
import xyz.klinker.messenger.shared.data.FeatureFlags;
import xyz.klinker.messenger.shared.data.MimeType;
import xyz.klinker.messenger.shared.data.Settings;
import xyz.klinker.messenger.shared.data.model.Message;
import xyz.klinker.messenger.shared.receiver.MessageListUpdatedReceiver;
import xyz.klinker.messenger.shared.util.PhoneNumberUtils;
import xyz.klinker.messenger.shared.util.SmsMmsUtils;
import xyz.klinker.messenger.shared.util.TimeUtils;

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
        try {
            handle(intent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handle(Intent intent) {
        if (ApiDownloadService.IS_RUNNING || (Account.INSTANCE.exists() && !Account.INSTANCE.getPrimary())) {
            return;
        }

        SharedPreferences sharedPreferences = Settings.get(this).getSharedPrefs(this);
        long lastRun = sharedPreferences.getLong("new_message_check_last_run", 0L);
        long fiveSecondsBefore = System.currentTimeMillis() - (TimeUtils.SECOND * 5);

        String appSignature;
        if (!Settings.get(this).signature.isEmpty()) {
            appSignature = "\n" + Settings.get(this).signature;

            if (!FeatureFlags.INSTANCE.getCHECK_NEW_MESSAGES_WITH_SIGNATURE()) {
                // issues with this duplicating sent messages that I hadn't worked out. Disable for now
                // TODO: fix this integration.
                return;
            }
        } else {
            appSignature = "";
        }

        // grab the latest 60 messages from Pulse's database
        // grab the latest 20 messages from the the internal SMS/MMS database
        // iterate over the internal messages and see if they are in the list from Pulse's database (search by text is fine)
        // if they are:
        //      continue, no problems here
        // if they aren't:
        //      insert them into the correct conversation and give the conversation update broadcast
        //      should I worry about updating the conversation list here?

        DataSource source = DataSource.INSTANCE;

        List<Message> pulseMessages = source.getNumberOfMessages(this, 60);
        Cursor internalMessages = SmsMmsUtils.getLatestSmsMessages(this, 20);

        List<Message> messagesToInsert = new ArrayList<>();
        List<String> addressesForMessages = new ArrayList<>();
        if (internalMessages != null && internalMessages.moveToFirst()) {
            do {
                String messageBody = internalMessages.getString(internalMessages.getColumnIndex(Telephony.Sms.BODY)).trim();
                int messageType = SmsMmsUtils.getSmsMessageType(internalMessages);
                long messageTimestamp = internalMessages.getLong(internalMessages.getColumnIndex(Telephony.Sms.DATE));

                if (messageType != Message.Companion.getTYPE_RECEIVED()) {
                    // sent message don't show a signature in the app, but they would be written to the internal database with one
                    // received messages don't need to worry about this, and shouldn't. If you send yourself a message, then it would come
                    // in with a signature, if this was applied to received messages, then that received message would get duplicated
                    messageBody = messageBody.replace(appSignature, "");
                }

                // the message timestamp should be more than the last time this service ran, but more than 5 seconds old,
                // and it shouldn't already be in the database
                if (messageTimestamp > lastRun && messageTimestamp < fiveSecondsBefore) {
                    if (!alreadyInDatabase(pulseMessages, messageBody, messageType)) {
                        Message message = new Message();

                        message.setType(messageType);
                        message.setData(messageBody);
                        message.setTimestamp(messageTimestamp);
                        message.setMimeType(MimeType.INSTANCE.getTEXT_PLAIN());
                        message.setRead(true);
                        message.setSeen(true);
                        if (messageType != Message.Companion.getTYPE_RECEIVED()) {
                            message.setSentDeviceId(Account.INSTANCE.exists() ? Long.parseLong(Account.INSTANCE.getDeviceId()) : -1L);
                        } else {
                            message.setSentDeviceId(-1L);
                        }

                        messagesToInsert.add(message);
                        addressesForMessages.add(PhoneNumberUtils.clearFormatting(
                                internalMessages.getString(internalMessages.getColumnIndex(Telephony.Sms.ADDRESS))));
                    } else {
                        Message message = messageStatusNeedsUpdatedToSent(pulseMessages, messageBody, messageType);
                        if (message !=  null) {
                            DataSource.INSTANCE.updateMessageType(this, message.getId(), Message.Companion.getTYPE_SENT());
                        }
                    }
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
            if (message.getMimeType().equals(MimeType.INSTANCE.getTEXT_PLAIN()) && (typesAreEqual(newMessageType, message.getType())) &&
                    bodyToSearch.trim().contains(message.getData().trim())) {
                return true;
            }
        }

        return false;
    }

    private Message messageStatusNeedsUpdatedToSent(List<Message> messages, String bodyToSearch, int newMessageType) {
        if (newMessageType != Message.Companion.getTYPE_SENT()) {
            return null;
        }

        for (Message message : messages) {
            if (message.getMimeType().equals(MimeType.INSTANCE.getTEXT_PLAIN()) &&  message.getType() == Message.Companion.getTYPE_SENDING() &&
                    message.getData().trim().equals(bodyToSearch.trim())) {
                return message;
            }
        }

        return null;
    }

    @VisibleForTesting
    public static boolean typesAreEqual(int newMessageType, int oldMessageType) {
        if (newMessageType == Message.Companion.getTYPE_ERROR()) {
            return oldMessageType == Message.Companion.getTYPE_ERROR();
        } else if (newMessageType == Message.Companion.getTYPE_RECEIVED()) {
            return oldMessageType == Message.Companion.getTYPE_RECEIVED();
        } else {
            return oldMessageType != Message.Companion.getTYPE_RECEIVED();
        }
    }
}
