/*
 * Copyright (C) 2016 Jacob Klinker
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package xyz.klinker.messenger.shared.service;

import android.app.Notification;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteConstraintException;
import android.graphics.Bitmap;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;

import com.klinker.android.send_message.Utils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.Date;
import java.util.Locale;
import java.util.Random;

import xyz.klinker.messenger.shared.R;
import xyz.klinker.messenger.api.implementation.ApiUtils;
import xyz.klinker.messenger.api.implementation.LoginActivity;
import xyz.klinker.messenger.api.implementation.firebase.FirebaseDownloadCallback;
import xyz.klinker.messenger.api.implementation.firebase.MessengerFirebaseMessagingService;
import xyz.klinker.messenger.api.implementation.Account;
import xyz.klinker.messenger.shared.data.DataSource;
import xyz.klinker.messenger.shared.data.FeatureFlags;
import xyz.klinker.messenger.shared.data.MimeType;
import xyz.klinker.messenger.shared.data.Settings;
import xyz.klinker.messenger.shared.data.model.Blacklist;
import xyz.klinker.messenger.shared.data.model.Contact;
import xyz.klinker.messenger.shared.data.model.Conversation;
import xyz.klinker.messenger.shared.data.model.Draft;
import xyz.klinker.messenger.shared.data.model.Message;
import xyz.klinker.messenger.shared.data.model.ScheduledMessage;
import xyz.klinker.messenger.encryption.EncryptionUtils;
import xyz.klinker.messenger.shared.receiver.ConversationListUpdatedReceiver;
import xyz.klinker.messenger.shared.receiver.MessageListUpdatedReceiver;
import xyz.klinker.messenger.shared.util.ContactUtils;
import xyz.klinker.messenger.shared.util.DualSimUtils;
import xyz.klinker.messenger.shared.util.ImageUtils;
import xyz.klinker.messenger.shared.util.PhoneNumberUtils;
import xyz.klinker.messenger.shared.util.SendUtils;
import xyz.klinker.messenger.shared.util.SetUtils;
import xyz.klinker.messenger.shared.util.StringUtils;

/**
 * Receiver responsible for processing firebase data messages and persisting to the database.
 */
public class FirebaseHandlerService extends WakefulIntentService {

    private static final String TAG = "FirebaseHandlerService";
    private static final int INFORMATION_NOTIFICATION_ID = 13;

    private EncryptionUtils encryptionUtils;

    public FirebaseHandlerService() {
        super("FirebaseHandlerService");
    }

    @Override
    protected void doWakefulWork(Intent intent) {
        if (intent != null && intent.getAction() != null && intent.getAction()
                .equals(MessengerFirebaseMessagingService.ACTION_FIREBASE_MESSAGE_RECEIVED)) {
            process(this, intent);
        }
    }

    private void process(Context context, Intent intent) {
        Account account = Account.get(context);

        // received a message without having initialized an account yet
        // could happen if their subscription ends
        if (account.key == null) {
            return;
        }

        encryptionUtils = account.getEncryptor();

        if (encryptionUtils == null && account.exists()) {
            context.startActivity(new Intent(context, LoginActivity.class));
            return;
        }

        String operation = intent.getStringExtra(MessengerFirebaseMessagingService.EXTRA_OPERATION);
        String data = intent.getStringExtra(MessengerFirebaseMessagingService.EXTRA_DATA);

        JSONObject json;
        try {
            json = new JSONObject(data);

            final DataSource source = DataSource.getInstance(context);
            source.open();
            source.setUpload(false);

            if (!source.isOpen()) {
                // this happens sometimes, for some reason... so lets close it down to get rid of
                // the current instance and open up a new one, I guess

                source.close();
                source.open();
            }

            switch (operation) {
                case "removed_account":
                    removeAccount(json, source, context);
                    break;
                case "added_message":
                    addMessage(json, source, context);
                    break;
                case "update_message_type":
                    updateMessageType(json, source, context);
                    break;
                case "updated_message":
                    updateMessage(json, source, context);
                    break;
                case "removed_message":
                    removeMessage(json, source);
                    break;
                case "cleanup_messages":
                    cleanupMessages(json, source);
                    break;
                case "added_contact":
                    addContact(json, source);
                    break;
                case "updated_contact":
                    updateContact(json, source);
                    break;
                case "removed_contact":
                    removeContact(json, source);
                    break;
                case "added_conversation":
                    addConversation(json, source, context);
                    break;
                case "update_conversation_snippet":
                    updateConversationSnippet(json, source, context);
                    break;
                case "update_conversation_title":
                    updateConversationTitle(json, source);
                    break;
                case "updated_conversation":
                    updateConversation(json, source, context);
                    break;
                case "removed_conversation":
                    removeConversation(json, source);
                    break;
                case "read_conversation":
                    readConversation(json, source, context);
                    break;
                case "seen_conversation":
                    seenConversation(json, source);
                    break;
                case "archive_conversation":
                    archiveConversation(json, source);
                    break;
                case "seen_conversations":
                    seenConversations(source);
                    break;
                case "added_draft":
                    addDraft(json, source);
                    break;
                case "removed_drafts":
                    removeDrafts(json, source);
                    break;
                case "added_blacklist":
                    addBlacklist(json, source);
                    break;
                case "removed_blacklist":
                    removeBlacklist(json, source);
                    break;
                case "added_scheduled_message":
                    addScheduledMessage(json, source);
                    break;
                case "updated_scheduled_message":
                    updatedScheduledMessage(json, source);
                    break;
                case "removed_scheduled_message":
                    removeScheduledMessage(json, source);
                    break;
                case "update_setting":
                    updateSetting(json, context);
                    break;
                case "dismissed_notification":
                    dismissNotification(json, source, context);
                    break;
                case "update_subscription":
                    updateSubscription(json, context);
                    break;
                case "update_primary_device":
                    updatePrimaryDevice(json, context);
                    break;
                case "feature_flag":
                    writeFeatureFlag(json, context);
                    break;
                case "forward_to_phone":
                    forwardToPhone(json, source, context);
                    break;
                default:
                    Log.e(TAG, "unsupported operation: " + operation);
                    break;
            }

            // sleep for a short amount of time to try to avoid uploading duplicates
            try { Thread.sleep(50); } catch (Exception e) { }
            source.setUpload(true);
            source.close();
        } catch (JSONException e) {
            Log.e(TAG, "error parsing data json", e);
        }
    }

    private void removeAccount(JSONObject json, DataSource source, Context context)
            throws JSONException {
        Account account = Account.get(context);

        if (json.getString("id").equals(account.accountId)) {
            Log.v(TAG, "clearing account");
            source.clearTables();
            account.clearAccount();
        } else {
            Log.v(TAG, "ids do not match, did not clear account");
        }
    }

    private void addMessage(JSONObject json, final DataSource source, final Context context)
            throws JSONException {
        final long id = getLong(json, "id");
        if (source.getMessage(id) == null) {
            Conversation conversation = source.getConversation(getLong(json, "conversation_id"));

            final Message message = new Message();
            message.id = id;
            message.conversationId = conversation == null ? getLong(json, "conversation_id") : conversation.id;
            message.type = json.getInt("type");
            message.timestamp = getLong(json, "timestamp");
            message.read = json.getBoolean("read");
            message.seen = json.getBoolean("seen");
            message.simPhoneNumber = conversation == null || conversation.simSubscriptionId == null ? null :
                    DualSimUtils.get(this).getPhoneNumberFromSimSubscription(conversation.simSubscriptionId);

            try {
                message.data = encryptionUtils.decrypt(json.getString("data"));
                message.mimeType = encryptionUtils.decrypt(json.getString("mime_type"));
                message.from = encryptionUtils.decrypt(json.has("from") ? json.getString("from") : null);
            } catch (Exception e) {
                Log.v(TAG, "error adding message, from decyrption.");
                message.data = context.getString(R.string.error_decrypting);
                message.mimeType = MimeType.TEXT_PLAIN;
                message.from = null;
            }

            if (json.has("color") && !json.getString("color").equals("null")) {
                message.color = json.getInt("color");
            }

            if (message.data.equals("firebase -1") &&
                    !message.mimeType.equals(MimeType.TEXT_PLAIN)) {
                Log.v(TAG, "downloading binary from firebase");

                addMessageAfterFirebaseDownload(context, message);
                return;
            }

            source.insertMessage(context, message, message.conversationId);
            Log.v(TAG, "added message");

            if (!Utils.isDefaultSmsApp(context) && message.type == Message.TYPE_SENDING) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try { Thread.sleep(500); } catch (Exception e) {}
                        DataSource source = DataSource.getInstance(context);
                        source.open();
                        source.updateMessageType(id, Message.TYPE_SENT);
                        source.close();
                    }
                }).start();
            }

            boolean isSending = message.type == Message.TYPE_SENDING;

            if (!Utils.isDefaultSmsApp(context) && isSending) {
                message.type = Message.TYPE_SENT;
            }

            if (Account.get(context).primary && isSending) {
                conversation = source.getConversation(message.conversationId);

                if (conversation != null) {
                    if (message.mimeType.equals(MimeType.TEXT_PLAIN)) {
                        new SendUtils(conversation.simSubscriptionId)
                                .send(context, message.data, conversation.phoneNumbers);
                    } else {
                        new SendUtils(conversation.simSubscriptionId)
                                .send(context, "", conversation.phoneNumbers,
                                Uri.parse(message.data), message.mimeType);
                    }
                } else {
                    Log.e(TAG, "trying to send message without the conversation, so can't find phone numbers");
                }

                Log.v(TAG, "sent message");
            }

            MessageListUpdatedReceiver.sendBroadcast(context, message);
            ConversationListUpdatedReceiver.sendBroadcast(context, message.conversationId,
                    message.mimeType.equals(MimeType.TEXT_PLAIN) ? message.data : "",
                    message.type != Message.TYPE_RECEIVED);

            if (message.type == Message.TYPE_RECEIVED) {
                context.startService(new Intent(context, NotificationService.class));
            } else if (isSending) {
                source.readConversation(context, message.conversationId);
                NotificationManagerCompat.from(context).cancel((int) message.conversationId);
            }
        } else {
            Log.v(TAG, "message already exists, not doing anything with it");
        }
    }

    private void addMessageAfterFirebaseDownload(final Context context, final Message message) {
        ApiUtils apiUtils = new ApiUtils();
        apiUtils.saveFirebaseFolderRef(Account.get(context).accountId);
        final File file = new File(context.getFilesDir(),
                message.id + MimeType.getExtension(message.mimeType));

        DataSource source = DataSource.getInstance(context);
        source.open();
        source.insertMessage(context, message, message.conversationId);
        source.close();
        Log.v(TAG, "added message");

        final boolean isSending = message.type == Message.TYPE_SENDING;

        if (!Utils.isDefaultSmsApp(context) && isSending) {
            message.type = Message.TYPE_SENT;
        }

        FirebaseDownloadCallback callback = new FirebaseDownloadCallback() {
            @Override
            public void onDownloadComplete() {
                message.data = Uri.fromFile(file).toString();
                DataSource source = DataSource.getInstance(context);
                source.open();
                source.updateMessageData(message.id, message.data);
                MessageListUpdatedReceiver.sendBroadcast(context, message.conversationId);

                if (Account.get(context).primary && isSending) {
                    Conversation conversation = source.getConversation(message.conversationId);

                    if (conversation != null) {
                        if (message.mimeType.equals(MimeType.TEXT_PLAIN)) {
                            new SendUtils(conversation.simSubscriptionId)
                                    .send(context, message.data, conversation.phoneNumbers);
                        } else {
                            new SendUtils(conversation.simSubscriptionId)
                                    .send(context, "", conversation.phoneNumbers,
                                        Uri.parse(message.data), message.mimeType);
                        }
                    } else {
                        Log.e(TAG, "trying to send message without the conversation, so can't find phone numbers");
                    }

                    Log.v(TAG, "sent message");
                }

                if (!Utils.isDefaultSmsApp(context) && message.type == Message.TYPE_SENDING) {
                    source.updateMessageType(message.id, Message.TYPE_SENT);
                }

                MessageListUpdatedReceiver.sendBroadcast(context, message);
                ConversationListUpdatedReceiver.sendBroadcast(context, message.conversationId,
                        message.mimeType.equals(MimeType.TEXT_PLAIN) ? message.data : "",
                        message.type != Message.TYPE_RECEIVED);

                if (message.type == Message.TYPE_RECEIVED) {
                    context.startService(new Intent(context, NotificationService.class));
                } else if (isSending) {
                    source.readConversation(context, message.conversationId);
                    NotificationManagerCompat.from(context).cancel((int) message.conversationId);
                }

                source.close();
            }
        };

        apiUtils.downloadFileFromFirebase(file, message.id, encryptionUtils, callback);

    }

    private void updateMessage(JSONObject json, DataSource source, Context context)
            throws JSONException {
        long id = getLong(json, "id");
        int type = json.getInt("type");
        source.updateMessageType(id, type);
        Message message = source.getMessage(id);
        if (message != null) {
            MessageListUpdatedReceiver.sendBroadcast(context, message);
        }
        Log.v(TAG, "updated message type");
    }

    private void updateMessageType(JSONObject json, DataSource source, Context context)
            throws JSONException {
        long id = getLong(json, "id");
        int type = json.getInt("message_type");
        source.updateMessageType(id, type);
        Message message = source.getMessage(id);
        if (message != null) {
            MessageListUpdatedReceiver.sendBroadcast(context, message);
        }
        Log.v(TAG, "updated message type");
    }

    private void removeMessage(JSONObject json, DataSource source) throws JSONException {
        long id = getLong(json, "id");
        source.deleteMessage(id);
        Log.v(TAG, "removed message");
    }

    private void cleanupMessages(JSONObject json, DataSource source) throws JSONException {
        long timestamp = getLong(json, "timestamp");
        source.cleanupOldMessages(timestamp);
        Log.v(TAG, "cleaned up old messages");
    }

    private void addConversation(JSONObject json, DataSource source, Context context)
            throws JSONException {
        Conversation conversation = new Conversation();
        conversation.id = getLong(json, "id");
        conversation.colors.color = json.getInt("color");
        conversation.colors.colorDark = json.getInt("color_dark");
        conversation.colors.colorLight = json.getInt("color_light");
        conversation.colors.colorAccent = json.getInt("color_accent");
        conversation.ledColor = json.getInt("led_color");
        conversation.pinned = json.getBoolean("pinned");
        conversation.read = json.getBoolean("read");
        conversation.timestamp = getLong(json, "timestamp");
        conversation.title = encryptionUtils.decrypt(json.getString("title"));
        conversation.phoneNumbers = encryptionUtils.decrypt(json.getString("phone_numbers"));
        conversation.snippet = encryptionUtils.decrypt(json.getString("snippet"));
        conversation.ringtoneUri = encryptionUtils.decrypt(json.has("ringtone") ?
                json.getString("ringtone") : null);
        conversation.imageUri = ContactUtils.findImageUri(conversation.phoneNumbers, context);
        conversation.idMatcher = encryptionUtils.decrypt(json.getString("id_matcher"));
        conversation.mute = json.getBoolean("mute");
        conversation.archive = json.getBoolean("archive");
        conversation.simSubscriptionId = -1;

        Bitmap image = ImageUtils.getContactImage(conversation.imageUri, this);
        if (conversation.imageUri != null &&
                image == null) {
            conversation.imageUri = null;
        } else if (conversation.imageUri != null) {
            conversation.imageUri += "/photo";
        }

        if (image != null) {
            image.recycle();
        }

        try {
            source.insertConversation(conversation);
        } catch (SQLiteConstraintException e) {
            // conversation already exists
        }
    }

    private void updateContact(JSONObject json, DataSource source)
            throws JSONException {
        try {
            Contact contact = new Contact();
            contact.phoneNumber = encryptionUtils.decrypt(json.getString("phone_number"));
            contact.name = encryptionUtils.decrypt(json.getString("name"));
            contact.colors.color = json.getInt("color");
            contact.colors.colorDark = json.getInt("color_dark");
            contact.colors.colorLight = json.getInt("color_light");
            contact.colors.colorAccent = json.getInt("color_accent");

            source.updateContact(contact);
            Log.v(TAG, "updated contact");
        } catch (RuntimeException e) {
            Log.e(TAG, "failed to update contact b/c of decrypting data");
        }
    }

    private void removeContact(JSONObject json, DataSource source) throws JSONException {
        String phoneNumber = json.getString("phone_number");
        source.deleteContact(phoneNumber);
        Log.v(TAG, "removed contact");
    }

    private void addContact(JSONObject json, DataSource source)
            throws JSONException {

        try {
            Contact contact = new Contact();
            contact.phoneNumber = encryptionUtils.decrypt(json.getString("phone_number"));
            contact.name = encryptionUtils.decrypt(json.getString("name"));
            contact.colors.color = json.getInt("color");
            contact.colors.colorDark = json.getInt("color_dark");
            contact.colors.colorLight = json.getInt("color_light");
            contact.colors.colorAccent = json.getInt("color_accent");

            source.insertContact(contact);
            Log.v(TAG, "added contact");
        } catch (SQLiteConstraintException e) {
            // contact already exists
            Log.e(TAG, "error adding contact", e);
        } catch (Exception e) {
            // error decrypting
        }
    }

    private void updateConversation(JSONObject json, DataSource source, Context context)
            throws JSONException {
        try {
            Conversation conversation = new Conversation();
            conversation.id = getLong(json, "id");
            conversation.title = encryptionUtils.decrypt(json.getString("title"));
            conversation.colors.color = json.getInt("color");
            conversation.colors.colorDark = json.getInt("color_dark");
            conversation.colors.colorLight = json.getInt("color_light");
            conversation.colors.colorAccent = json.getInt("color_accent");
            conversation.ledColor = json.getInt("led_color");
            conversation.pinned = json.getBoolean("pinned");
            conversation.ringtoneUri = encryptionUtils.decrypt(json.has("ringtone") ?
                    json.getString("ringtone") : null);
            conversation.mute = json.getBoolean("mute");
            conversation.read = json.getBoolean("read");
            conversation.read = json.getBoolean("read");
            conversation.archive = json.getBoolean("archive");
            conversation.privateNotifications = json.getBoolean("private_notifications");

            source.updateConversationSettings(conversation);

            if (conversation.read) {
                source.readConversation(context, conversation.id);
            }
            Log.v(TAG, "updated conversation");
        } catch (RuntimeException e) {
            Log.e(TAG, "failed to update conversation b/c of decrypting data");
        }
    }

    private void updateConversationTitle(JSONObject json, DataSource source)
            throws JSONException {
        try {
            source.updateConversationTitle(
                    getLong(json, "id"),
                    encryptionUtils.decrypt(json.getString("title"))
            );

            Log.v(TAG, "updated conversation title");
        } catch (RuntimeException e) {
            Log.e(TAG, "failed to update conversation title b/c of decrypting data");
        }
    }

    private void updateConversationSnippet(JSONObject json, DataSource source, Context context)
            throws JSONException {
        try {
            source.updateConversation(
                    getLong(json, "id"),
                    json.getBoolean("read"),
                    getLong(json, "timestamp"),
                    encryptionUtils.decrypt(json.getString("snippet")),
                    MimeType.TEXT_PLAIN,
                    json.getBoolean("archive")
            );

            Log.v(TAG, "updated conversation snippet");
        } catch (RuntimeException e) {
            Log.e(TAG, "failed to update conversation snippet b/c of decrypting data");
        }
    }

    private void removeConversation(JSONObject json, DataSource source) throws JSONException {
        long id = getLong(json, "id");
        source.deleteConversation(id);
        Log.v(TAG, "removed conversation");
    }

    private void readConversation(JSONObject json, DataSource source, Context context) throws JSONException {
        long id = getLong(json, "id");
        source.readConversation(context, id);
        Log.v(TAG, "read conversation");
    }

    private void seenConversation(JSONObject json, DataSource source) throws JSONException {
        long id = getLong(json, "id");
        source.seenConversation(id);
        Log.v(TAG, "seen conversation");
    }

    private void archiveConversation(JSONObject json, DataSource source) throws JSONException {
        long id = getLong(json, "id");
        boolean archive = json.getBoolean("archive");
        source.archiveConversation(id, archive);
        Log.v(TAG, "archive conversation: " + archive);
    }

    private void seenConversations(DataSource source) throws JSONException {
        source.seenConversations();
        Log.v(TAG, "seen all conversations");
    }

    private void addDraft(JSONObject json, DataSource source) throws JSONException {
        Draft draft = new Draft();
        draft.id = getLong(json, "id");
        draft.conversationId = getLong(json, "conversation_id");
        draft.data = encryptionUtils.decrypt(json.getString("data"));
        draft.mimeType = encryptionUtils.decrypt(json.getString("mime_type"));

        source.insertDraft(draft);
        Log.v(TAG, "added draft");
    }

    private void removeDrafts(JSONObject json, DataSource source) throws JSONException {
        long id = getLong(json, "id");
        source.deleteDrafts(id);
        Log.v(TAG, "removed drafts");
    }

    private void addBlacklist(JSONObject json, DataSource source) throws JSONException {
        long id = getLong(json, "id");
        String phoneNumber = json.getString("phone_number");
        phoneNumber = encryptionUtils.decrypt(phoneNumber);

        Blacklist blacklist = new Blacklist();
        blacklist.id = id;
        blacklist.phoneNumber = phoneNumber;
        source.insertBlacklist(blacklist);
        Log.v(TAG, "added blacklist");
    }

    private void removeBlacklist(JSONObject json, DataSource source) throws JSONException {
        long id = getLong(json, "id");
        source.deleteBlacklist(id);
        Log.v(TAG, "removed blacklist");
    }

    private void addScheduledMessage(JSONObject json, DataSource source) throws JSONException {
        ScheduledMessage message = new ScheduledMessage();
        message.id = getLong(json, "id");
        message.to = encryptionUtils.decrypt(json.getString("to"));
        message.data = encryptionUtils.decrypt(json.getString("data"));
        message.mimeType = encryptionUtils.decrypt(json.getString("mime_type"));
        message.timestamp = getLong(json, "timestamp");
        message.title = encryptionUtils.decrypt(json.getString("title"));

        source.insertScheduledMessage(message);
        startService(new Intent(this, ScheduledMessageService.class));
        Log.v(TAG, "added scheduled message");
    }

    private void updatedScheduledMessage(JSONObject json, DataSource source) throws JSONException {
        ScheduledMessage message = new ScheduledMessage();
        message.id = getLong(json, "id");
        message.to = encryptionUtils.decrypt(json.getString("to"));
        message.data = encryptionUtils.decrypt(json.getString("data"));
        message.mimeType = encryptionUtils.decrypt(json.getString("mime_type"));
        message.timestamp = getLong(json, "timestamp");
        message.title = encryptionUtils.decrypt(json.getString("title"));

        source.updateScheduledMessage(message);
        startService(new Intent(this, ScheduledMessageService.class));
        Log.v(TAG, "updated scheduled message");
    }

    private void removeScheduledMessage(JSONObject json, DataSource source) throws JSONException {
        long id = getLong(json, "id");
        source.deleteScheduledMessage(id);
        startService(new Intent(this, ScheduledMessageService.class));
        Log.v(TAG, "removed scheduled message");
    }

    private void dismissNotification(JSONObject json, DataSource source, Context context)
            throws JSONException {
        long conversationId = getLong(json, "id");
        String deviceId = json.getString("device_id");

        if (deviceId == null || !deviceId.equals(Account.get(context).deviceId)) {
            // don't want to mark as read if this device was the one that sent the dismissal fcm message
            source.readConversation(context, conversationId);

            NotificationManagerCompat.from(context).cancel((int) conversationId);
            Log.v(TAG, "dismissed notification for " + conversationId);
        }
    }

    private void updateSetting(JSONObject json, Context context)
            throws JSONException {
        String pref = json.getString("pref");
        String type = json.getString("type");

        if (pref != null && type != null && json.has("value")) {
            switch (type.toLowerCase(Locale.getDefault())) {
                case "boolean":
                    Settings.get(context).setValue(pref, json.getBoolean("value"));
                    break;
                case "long":
                    Settings.get(context).setValue(pref, getLong(json, "value"));
                    break;
                case "int":
                    Settings.get(context).setValue(pref, json.getInt("value"));
                    break;
                case "string":
                    Settings.get(context).setValue(pref, json.getString("value"));
                    break;
                case "set":
                    Settings.get(context).setValue(pref, SetUtils.createSet(json.getString("value")));
                    break;
            }
        }
    }

    private void updateSubscription(JSONObject json, Context context)
            throws JSONException {
        int type = json.has("type") ? json.getInt("type") : 0;
        long expiration = json.has("expiration") ? json.getLong("expiration") : 0L;
        boolean fromAdmin = json.has("from_admin") ? json.getBoolean("from_admin") : false;

        Account account = Account.get(context);

        if (account.primary) {
            account.updateSubscription(
                    Account.SubscriptionType.findByTypeCode(type), expiration, false
            );

            SubscriptionExpirationCheckService.scheduleNextRun(context);
            SignoutService.writeSignoutTime(context, 0);

            if (fromAdmin) {
                String content = "Enjoy the app!";

                if (!account.subscriptionType.equals(Account.SubscriptionType.LIFETIME)) {
                    content = "Expiration: " + new Date(expiration).toString();
                }

                notifyUser("Subscription Updated: " + StringUtils.titleize(account.subscriptionType.name()), content);
            }
        }
    }

    private void updatePrimaryDevice(JSONObject json, Context context)
            throws JSONException {
        String newPrimaryDeviceId = json.getString("new_primary_device_id");

        Account account = Account.get(context);
        if (newPrimaryDeviceId != null && !newPrimaryDeviceId.equals(account.deviceId)) {
            account.setPrimary(false);
        }
    }

    private void writeFeatureFlag(JSONObject json, Context context)
            throws JSONException {
        FeatureFlags flags = FeatureFlags.get(context);

        String identifier = json.getString("id");
        boolean value = json.getBoolean("value");
        int rolloutPercent = json.getInt("rollout"); // 1 - 100

        if (!value) {
            // if we are turning the flag off, we want to do it for everyone immediately
            flags.updateFlag(identifier, false);
        } else {
            Random rand = new Random();
            int random = rand.nextInt(100) + 1; // between 1 - 100

            if (random <= rolloutPercent) {
                // they made it in the staged rollout!
                flags.updateFlag(identifier, true);
            }

            // otherwise, don't do anything. We don't want to turn the flag off for those
            // that had gotten it turned on in the past.
        }
    }

    private void forwardToPhone(JSONObject json, DataSource source, Context context)
            throws JSONException {

        if (!Account.get(context).primary) {
            return;
        }

        String text = json.getString("message");
        String to = PhoneNumberUtils.clearFormatting(json.getString("to"));

        Message message = new Message();
        message.type = Message.TYPE_SENDING;
        message.data = text;
        message.timestamp = System.currentTimeMillis();
        message.mimeType = MimeType.TEXT_PLAIN;
        message.read = true;
        message.seen = true;
        message.simPhoneNumber = DualSimUtils.get(this).getDefaultPhoneNumber();

        source.setUpload(true);
        long conversationId = source.insertMessage(message, to, context);
        Conversation conversation = source.getConversation(conversationId);
        source.setUpload(false);

        new SendUtils(conversation != null ? conversation.simSubscriptionId : null)
                .send(context, message.data, to);
    }
    
    private long getLong(JSONObject json, String identifier) {
        try {
            String str = json.getString(identifier);
            return Long.parseLong(str);
        } catch (Exception e) {
            return 0L;
        }
    }

    private void notifyUser(String title, String content) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                .setContentTitle(title)
                .setContentText(content)
                .setStyle(new NotificationCompat.BigTextStyle().setBigContentTitle(title).setSummaryText(content))
                .setSmallIcon(R.drawable.ic_stat_notify_group)
                .setPriority(Notification.PRIORITY_HIGH)
                .setColor(Settings.get(this).globalColorSet.color);

        NotificationManagerCompat.from(this).notify(INFORMATION_NOTIFICATION_ID, builder.build());
    }
}
