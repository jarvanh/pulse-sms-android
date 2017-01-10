package xyz.klinker.messenger.service;

import android.app.IntentService;
import android.app.Notification;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import xyz.klinker.messenger.R;
import xyz.klinker.messenger.api.entity.AddMessagesRequest;
import xyz.klinker.messenger.api.entity.MessageBody;
import xyz.klinker.messenger.api.implementation.Account;
import xyz.klinker.messenger.api.implementation.ApiUtils;
import xyz.klinker.messenger.data.DataSource;
import xyz.klinker.messenger.data.FeatureFlags;
import xyz.klinker.messenger.data.MimeType;
import xyz.klinker.messenger.data.Settings;
import xyz.klinker.messenger.data.model.Conversation;
import xyz.klinker.messenger.data.model.Message;
import xyz.klinker.messenger.encryption.EncryptionUtils;
import xyz.klinker.messenger.receiver.ConversationListUpdatedReceiver;
import xyz.klinker.messenger.util.PaginationUtils;
import xyz.klinker.messenger.util.SmsMmsUtils;

/**
 * Check whether or not there are messages in the internal database, that are not in Pulse's
 * database. This is useful for if a user goes away from Pulse for awhile, then wants to return to
 * it.
 */
public class NewMessagesCheckService extends IntentService {

    private static final String TAG = "NewMessageCheck";
    private static final long TIMESTAMP_BUFFER = 30000;

    public static final String REFRESH_WHOLE_CONVERSATION_LIST = "xyz.klinker.messenger.REFRESH_WHOLE_CONVERSATION_LIST";
    public static final int MESSAGE_CHECKING_ID = 6435;

    public NewMessagesCheckService() {
        super("NewMessageCheckService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (ApiDownloadService.IS_RUNNING || (Account.get(this).exists() && !Account.get(this).primary)) {
            return;
        }

        DataSource source = DataSource.getInstance(this);
        source.open();

        Message lastMessage = source.getLatestMessage();
        SharedPreferences sharedPrefs = Settings.get(this).getSharedPrefs();
        long lastTimestamp = sharedPrefs.getLong("last_new_message_check", -1L);

        if (lastTimestamp != -1L) {
            if (lastMessage != null && lastMessage.timestamp > lastTimestamp) {
                lastTimestamp = lastMessage.timestamp + TIMESTAMP_BUFFER;
            }

            int insertedMessages = 0;
            List<Conversation> conversationsWithNewMessages =
                    SmsMmsUtils.queryNewConversations(this, lastTimestamp);

            if (conversationsWithNewMessages.size() > 0) {
                NotificationCompat.Builder builder = showNotification();

                int progress = 1;

                for (Conversation conversation : conversationsWithNewMessages) {
                    if (conversation.phoneNumbers != null && !conversation.phoneNumbers.isEmpty() &&
                            !conversation.title.contains("UNKNOWN_SENDER") &&
                            !conversation.title.contains("insert-address-token") &&
                            conversation.snippet != null && !conversation.snippet.isEmpty()) {
                        insertedMessages = source.insertNewMessages(conversation, lastTimestamp,
                                SmsMmsUtils.queryConversation(conversation.id, this));
                        builder.setProgress(conversationsWithNewMessages.size() + 1, progress, false);
                        NotificationManagerCompat.from(this).notify(MESSAGE_CHECKING_ID, builder.build());
                    }

                    progress++;
                }

                if (insertedMessages > 0) {
                    sendBroadcast(new Intent(REFRESH_WHOLE_CONVERSATION_LIST));
                }
            }

            NotificationManagerCompat.from(this).cancel(MESSAGE_CHECKING_ID);
        }

        source.close();
        sharedPrefs.edit().putLong("last_new_message_check", System.currentTimeMillis()).apply();

        if (lastTimestamp != -1L && Account.get(this).exists()) {
            // do this after closing the database, becuase it will reopen it
            // conversations will have been uploaded already, but we need to upload any messages
            // newer than that timestamp.
            uploadMessages(lastTimestamp);
        }
    }

    private NotificationCompat.Builder showNotification() {
        NotificationCompat.Builder notification = new NotificationCompat.Builder(this)
                .setContentTitle(getString(R.string.updating_database))
                .setSmallIcon(R.drawable.ic_stat_notify_group)
                .setProgress(0, 0, true)
                .setLocalOnly(true)
                .setColor(Settings.get(this).globalColorSet.color)
                .setOngoing(true);
        NotificationManagerCompat.from(this).notify(MESSAGE_CHECKING_ID, notification.build());

        return notification;
    }

    /**
     * This method is very similar to the one in the {@link ApiUploadService}.
     *
     * @param latestTimestamp the timestamp that used to be the latest in our database.
     */
    private void uploadMessages(long latestTimestamp) {
        final DataSource source = DataSource.getInstance(this);
        final Account account = Account.get(this);
        final EncryptionUtils encryptionUtils = account.getEncryptor();
        final ApiUtils apiUtils = new ApiUtils();

        long startTime = System.currentTimeMillis();
        Cursor cursor = source.getNewerMessages(latestTimestamp);

        if (cursor != null && cursor.moveToFirst()) {
            List<MessageBody> messages = new ArrayList<>();
            int firebaseNumber = 0;

            do {
                Message m = new Message();
                m.fillFromCursor(cursor);

                // instead of sending the URI, we'll upload these images to firebase and retrieve
                // them on another device based on account id and message id.
                if (!m.mimeType.equals(MimeType.TEXT_PLAIN)) {
                    m.data = "firebase " + firebaseNumber;
                    firebaseNumber++;
                }

                m.encrypt(encryptionUtils);
                MessageBody message = new MessageBody(m.id, m.conversationId, m.type, m.data,
                        m.timestamp, m.mimeType, m.read, m.seen, m.from, m.color);
                messages.add(message);
            } while (cursor.moveToNext());

            List<Object> results = new ArrayList<>();
            List<List<MessageBody>> pages = PaginationUtils.getPages(messages, ApiUploadService.MESSAGE_UPLOAD_PAGE_SIZE);

            for (List<MessageBody> page : pages) {
                AddMessagesRequest request = new AddMessagesRequest(account.accountId, page.toArray(new MessageBody[0]));
                results.add(apiUtils.getApi().message().add(request));

                Log.v(TAG, "uploaded " + page.size() + " messages for page " + results.size());
            }

            if (results.size() != pages.size() || !ApiUploadService.noNull(results)) {
                Log.v(TAG, "failed to upload messages in " +
                        (System.currentTimeMillis() - startTime) + " ms");
            } else {
                Log.v(TAG, "messages upload successful in " +
                        (System.currentTimeMillis() - startTime) + " ms");
            }
        }

        try {
            cursor.close();
        } catch (Exception e) { }
    }
}
