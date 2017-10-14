/*
 * Copyright (C) 2017 Luke Klinker
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
import xyz.klinker.messenger.shared.service.jobs.MarkAsSentJob;
import xyz.klinker.messenger.shared.service.jobs.ScheduledMessageJob;
import xyz.klinker.messenger.shared.service.jobs.SignoutJob;
import xyz.klinker.messenger.shared.service.jobs.SubscriptionExpirationCheckJob;
import xyz.klinker.messenger.shared.util.ContactUtils;
import xyz.klinker.messenger.shared.util.DualSimUtils;
import xyz.klinker.messenger.shared.util.ImageUtils;
import xyz.klinker.messenger.shared.util.NotificationUtils;
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

    public FirebaseHandlerService() {
        super("FirebaseHandlerService");
    }

    @Override
    protected void doWakefulWork(Intent intent) {
        if (intent != null && intent.getAction() != null && intent.getAction()
                .equals(MessengerFirebaseMessagingService.ACTION_FIREBASE_MESSAGE_RECEIVED)) {
            String operation = intent.getStringExtra(MessengerFirebaseMessagingService.EXTRA_OPERATION);
            String data = intent.getStringExtra(MessengerFirebaseMessagingService.EXTRA_DATA);

            process(this, operation, data);
        }
    }

    public static void process(Context context, String operation, String data) {
        Account account = Account.INSTANCE;

        // received a message without having initialized an account yet
        // could happen if their subscription ends
        if (account.getKey() == null) {
            return;
        }

        EncryptionUtils encryptionUtils = account.getEncryptor();

        if (encryptionUtils == null && account.exists()) {
            context.startActivity(new Intent(context, LoginActivity.class));
            return;
        }


        Log.v(TAG, "operation: " + operation + ", contents: " + data);

        JSONObject json;
        try {
            json = new JSONObject(data);

            final DataSource source = DataSource.INSTANCE;

            switch (operation) {
                case "removed_account":
                    removeAccount(json, source, context);
                    break;
                case "updated_account":
                    updatedAccount(json, context);
                    break;
                case "cleaned_account":
                    cleanAccount(json, source, context);
                    break;
                case "added_message":
                    addMessage(json, source, context, encryptionUtils);
                    break;
                case "update_message_type":
                    updateMessageType(json, source, context);
                    break;
                case "updated_message":
                    updateMessage(json, source, context);
                    break;
                case "removed_message":
                    removeMessage(json, source, context);
                    break;
                case "cleanup_messages":
                    cleanupMessages(json, source, context);
                    break;
                case "added_contact":
                    addContact(json, source, context, encryptionUtils);
                    break;
                case "updated_contact":
                    updateContact(json, source, context, encryptionUtils);
                    break;
                case "removed_contact":
                    removeContact(json, source, context);
                    break;
                case "removed_contact_by_id":
                    removeContactById(json, source, context);
                    break;
                case "added_conversation":
                    addConversation(json, source, context, encryptionUtils);
                    break;
                case "update_conversation_snippet":
                    updateConversationSnippet(json, source, context, encryptionUtils);
                    break;
                case "update_conversation_title":
                    updateConversationTitle(json, source, context, encryptionUtils);
                    break;
                case "updated_conversation":
                    updateConversation(json, source, context, encryptionUtils);
                    break;
                case "removed_conversation":
                    removeConversation(json, source, context);
                    break;
                case "read_conversation":
                    readConversation(json, source, context);
                    break;
                case "seen_conversation":
                    seenConversation(json, source, context);
                    break;
                case "archive_conversation":
                    archiveConversation(json, source, context);
                    break;
                case "seen_conversations":
                    seenConversations(source, context);
                    break;
                case "added_draft":
                    addDraft(json, source, context, encryptionUtils);
                    break;
                case "removed_drafts":
                    removeDrafts(json, source, context);
                    break;
                case "added_blacklist":
                    addBlacklist(json, source, context, encryptionUtils);
                    break;
                case "removed_blacklist":
                    removeBlacklist(json, source, context);
                    break;
                case "added_scheduled_message":
                    addScheduledMessage(json, source, context, encryptionUtils);
                    break;
                case "updated_scheduled_message":
                    updatedScheduledMessage(json, source, context, encryptionUtils);
                    break;
                case "removed_scheduled_message":
                    removeScheduledMessage(json, source, context);
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
        } catch (JSONException e) {
            Log.e(TAG, "error parsing data json", e);
        }
    }

    private static void removeAccount(JSONObject json, DataSource source, Context context)
            throws JSONException {
        Account account = Account.INSTANCE;

        if (json.getString("id").equals(account.getAccountId())) {
            Log.v(TAG, "clearing account");
            source.clearTables(context);
            account.clearAccount(context);
        } else {
            Log.v(TAG, "ids do not match, did not clear account");
        }
    }

    private static void updatedAccount(JSONObject json, Context context)
            throws JSONException {
        Account account = Account.INSTANCE;
        String name = json.getString("real_name");
        String number = json.getString("phone_number");

        if (json.getString("id").equals(account.getAccountId())) {
            account.setName(context, name);
            account.setPhoneNumber(context, number);
            Log.v(TAG, "updated account name and number");
        } else {
            Log.v(TAG, "ids do not match, did not clear account");
        }
    }

    private static void cleanAccount(JSONObject json, DataSource source, Context context)
            throws JSONException {
        Account account = Account.INSTANCE;

        if (json.getString("id").equals(account.getAccountId())) {
            Log.v(TAG, "clearing account");
            source.clearTables(context);
        } else {
            Log.v(TAG, "ids do not match, did not clear account");
        }
    }

    private static void addMessage(JSONObject json, final DataSource source, final Context context, final EncryptionUtils encryptionUtils)
            throws JSONException {
        final long id = getLong(json, "id");
        if (source.getMessage(context, id) == null) {
            Conversation conversation = source.getConversation(context, getLong(json, "conversation_id"));

            final Message message = new Message();
            message.setId(id);
            message.setConversationId(conversation == null ? getLong(json, "conversation_id") : conversation.getId());
            message.setType(json.getInt("type"));
            message.setTimestamp(getLong(json, "timestamp"));
            message.setRead(json.getBoolean("read"));
            message.setSeen(json.getBoolean("seen"));
            message.setSimPhoneNumber(conversation == null || conversation.getSimSubscriptionId() == null ? null :
                    DualSimUtils.INSTANCE.getPhoneNumberFromSimSubscription(conversation.getSimSubscriptionId()));

            if (json.has("sent_device")) {
                try {
                    message.setSentDeviceId(json.getLong("sent_device"));
                } catch (Exception e) {
                    message.setSentDeviceId(-1L);
                }
            } else {
                message.setSentDeviceId(-1L);
            }

            try {
                message.setData(encryptionUtils.decrypt(json.getString("data")));
                message.setMimeType(encryptionUtils.decrypt(json.getString("mime_type")));
                message.setFrom(encryptionUtils.decrypt(json.has("from") ? json.getString("from") : null));
            } catch (Exception e) {
                Log.v(TAG, "error adding message, from decyrption.");
                message.setData(context.getString(R.string.error_decrypting));
                message.setMimeType(MimeType.INSTANCE.getTEXT_PLAIN());
                message.setFrom(null);
            }

            if (json.has("color") && !json.getString("color").equals("null")) {
                message.setColor(json.getInt("color"));
            }

            if (message.getData().equals("firebase -1") &&
                    !message.getMimeType().equals(MimeType.INSTANCE.getTEXT_PLAIN())) {
                Log.v(TAG, "downloading binary from firebase");

                addMessageAfterFirebaseDownload(context, encryptionUtils, message);
                return;
            }

            long messageId = source.insertMessage(context, message, message.getConversationId(), true, false);
            Log.v(TAG, "added message");

            if (!Utils.isDefaultSmsApp(context) && message.getType() == Message.Companion.getTYPE_SENDING()) {
                new Thread(() -> {
                    try { Thread.sleep(500); } catch (Exception e) {}
                    DataSource.INSTANCE.updateMessageType(context, id, Message.Companion.getTYPE_SENT(), true);
                }).start();
            }

            boolean isSending = message.getType() == Message.Companion.getTYPE_SENDING();

            if (!Utils.isDefaultSmsApp(context) && isSending) {
                message.setType(Message.Companion.getTYPE_SENT());
            }

            if (Account.INSTANCE.getPrimary() && isSending) {
                conversation = source.getConversation(context, message.getConversationId());

                if (conversation != null) {
                    if (message.getMimeType().equals(MimeType.INSTANCE.getTEXT_PLAIN())) {
                        new SendUtils(conversation.getSimSubscriptionId())
                                .send(context, message.getData(), conversation.getPhoneNumbers());
                        MarkAsSentJob.Companion.scheduleNextRun(context, messageId);
                    } else {
                        new SendUtils(conversation.getSimSubscriptionId())
                                .send(context, "", conversation.getPhoneNumbers(),
                                Uri.parse(message.getData()), message.getMimeType());
                        MarkAsSentJob.Companion.scheduleNextRun(context, messageId);
                    }
                } else {
                    Log.e(TAG, "trying to send message without the conversation, so can't find phone numbers");
                }

                Log.v(TAG, "sent message");
            }

            MessageListUpdatedReceiver.Companion.sendBroadcast(context, message);
            ConversationListUpdatedReceiver.Companion.sendBroadcast(context, message.getConversationId(),
                    message.getMimeType().equals(MimeType.INSTANCE.getTEXT_PLAIN()) ? message.getData() : "",
                    message.getType() != Message.Companion.getTYPE_RECEIVED());

            if (message.getType() == Message.Companion.getTYPE_RECEIVED()) {
                context.startService(new Intent(context, NotificationService.class));
            } else if (isSending) {
                source.readConversation(context, message.getConversationId(), false);
                NotificationManagerCompat.from(context).cancel((int) message.getConversationId());
            }
        } else {
            Log.v(TAG, "message already exists, not doing anything with it");
        }
    }

    private static void addMessageAfterFirebaseDownload(final Context context, final EncryptionUtils encryptionUtils, final Message message) {
        ApiUtils apiUtils = ApiUtils.INSTANCE;
        apiUtils.saveFirebaseFolderRef(Account.INSTANCE.getAccountId());
        final File file = new File(context.getFilesDir(),
                message.getId() + MimeType.INSTANCE.getExtension(message.getMimeType()));

        final DataSource source = DataSource.INSTANCE;

        source.insertMessage(context, message, message.getConversationId(), false, false);
        Log.v(TAG, "added message");

        final boolean isSending = message.getType() == Message.Companion.getTYPE_SENDING();

        if (!Utils.isDefaultSmsApp(context) && isSending) {
            message.setType(Message.Companion.getTYPE_SENT());
        }

        FirebaseDownloadCallback callback = () -> {
            message.setData(Uri.fromFile(file).toString());
            source.updateMessageData(context, message.getId(), message.getData());
            MessageListUpdatedReceiver.Companion.sendBroadcast(context, message.getConversationId());

            if (Account.INSTANCE.getPrimary() && isSending) {
                Conversation conversation = source.getConversation(context, message.getConversationId());

                if (conversation != null) {
                    if (message.getMimeType().equals(MimeType.INSTANCE.getTEXT_PLAIN())) {
                        new SendUtils(conversation.getSimSubscriptionId())
                                .send(context, message.getData(), conversation.getPhoneNumbers());
                    } else {
                        new SendUtils(conversation.getSimSubscriptionId())
                                .send(context, "", conversation.getPhoneNumbers(),
                                    Uri.parse(message.getData()), message.getMimeType());
                    }
                } else {
                    Log.e(TAG, "trying to send message without the conversation, so can't find phone numbers");
                }

                Log.v(TAG, "sent message");
            }

            if (!Utils.isDefaultSmsApp(context) && message.getType() == Message.Companion.getTYPE_SENDING()) {
                source.updateMessageType(context, message.getId(), Message.Companion.getTYPE_SENT(), false);
            }

            MessageListUpdatedReceiver.Companion.sendBroadcast(context, message);
            ConversationListUpdatedReceiver.Companion.sendBroadcast(context, message.getConversationId(),
                    message.getMimeType().equals(MimeType.INSTANCE.getTEXT_PLAIN()) ? message.getData() : "",
                    message.getType() != Message.Companion.getTYPE_RECEIVED());

            if (message.getType() == Message.Companion.getTYPE_RECEIVED()) {
                context.startService(new Intent(context, NotificationService.class));
            } else if (isSending) {
                source.readConversation(context, message.getConversationId(), false);
                NotificationManagerCompat.from(context).cancel((int) message.getConversationId());
            }
        };

        apiUtils.downloadFileFromFirebase(Account.INSTANCE.getAccountId(), file, message.getId(), encryptionUtils, callback, 0);

    }

    private static void updateMessage(JSONObject json, DataSource source, Context context)
            throws JSONException {
        long id = getLong(json, "id");
        int type = json.getInt("type");
        source.updateMessageType(context, id, type, false);

        Message message = source.getMessage(context, id);
        if (message != null) {
            MessageListUpdatedReceiver.Companion.sendBroadcast(context, message);
        }

        Log.v(TAG, "updated message type");
    }

    private static void updateMessageType(JSONObject json, DataSource source, Context context)
            throws JSONException {
        long id = getLong(json, "id");
        int type = json.getInt("message_type");
        source.updateMessageType(context, id, type, false);

        Message message = source.getMessage(context, id);
        if (message != null) {
            MessageListUpdatedReceiver.Companion.sendBroadcast(context, message);
        }

        Log.v(TAG, "updated message type");
    }

    private static void removeMessage(JSONObject json, DataSource source, Context context) throws JSONException {
        long id = getLong(json, "id");
        source.deleteMessage(context, id, false);
        Log.v(TAG, "removed message");
    }

    private static void cleanupMessages(JSONObject json, DataSource source, Context context) throws JSONException {
        long timestamp = getLong(json, "timestamp");
        source.cleanupOldMessages(context, timestamp, false);
        Log.v(TAG, "cleaned up old messages");
    }

    private static void addConversation(JSONObject json, DataSource source, Context context, EncryptionUtils encryptionUtils)
            throws JSONException {
        Conversation conversation = new Conversation();
        conversation.setId(getLong(json, "id"));
        conversation.getColors().setColor(json.getInt("color"));
        conversation.getColors().setColorDark(json.getInt("color_dark"));
        conversation.getColors().setColorLight(json.getInt("color_light"));
        conversation.getColors().setColorAccent(json.getInt("color_accent"));
        conversation.setLedColor(json.getInt("led_color"));
        conversation.setPinned(json.getBoolean("pinned"));
        conversation.setRead(json.getBoolean("read"));
        conversation.setTimestamp(getLong(json, "timestamp"));
        conversation.setTitle(encryptionUtils.decrypt(json.getString("title")));
        conversation.setPhoneNumbers(encryptionUtils.decrypt(json.getString("phone_numbers")));
        conversation.setSnippet(encryptionUtils.decrypt(json.getString("snippet")));
        conversation.setRingtoneUri(encryptionUtils.decrypt(json.has("ringtone") ?
                json.getString("ringtone") : null));
        conversation.setImageUri(ContactUtils.INSTANCE.findImageUri(conversation.getPhoneNumbers(), context));
        conversation.setIdMatcher(encryptionUtils.decrypt(json.getString("id_matcher")));
        conversation.setMute(json.getBoolean("mute"));
        conversation.setArchive(json.getBoolean("archive"));
        conversation.setSimSubscriptionId(-1);

        Bitmap image = ImageUtils.INSTANCE.getContactImage(conversation.getImageUri(), context);
        if (conversation.getImageUri() != null &&
                image == null) {
            conversation.setImageUri(null);
        } else if (conversation.getImageUri() != null) {
            conversation.setImageUri(conversation.getImageUri() + "/photo");
        }

        if (image != null) {
            image.recycle();
        }

        try {
            source.insertConversation(context, conversation, false);
        } catch (SQLiteConstraintException e) {
            // conversation already exists
        }
    }

    private static void updateContact(JSONObject json, DataSource source, Context context, EncryptionUtils encryptionUtils)
            throws JSONException {
        try {
            Contact contact = new Contact();
            contact.setPhoneNumber(encryptionUtils.decrypt(json.getString("phone_number")));
            contact.setName(encryptionUtils.decrypt(json.getString("name")));
            contact.getColors().setColor(json.getInt("color"));
            contact.getColors().setColorDark(json.getInt("color_dark"));
            contact.getColors().setColorLight(json.getInt("color_light"));
            contact.getColors().setColorAccent(json.getInt("color_accent"));

            source.updateContact(context, contact, false);
            Log.v(TAG, "updated contact");
        } catch (RuntimeException e) {
            Log.e(TAG, "failed to update contact b/c of decrypting data");
        }
    }

    private static void removeContact(JSONObject json, DataSource source, Context context) throws JSONException {
        String phoneNumber = json.getString("phone_number");
        source.deleteContact(context, phoneNumber, false);
        Log.v(TAG, "removed contact");
    }

    private static void removeContactById(JSONObject json, DataSource source, Context context) throws JSONException {
        String[] ids = json.getString("id").split(",");
        source.deleteContacts(context, ids, false);
        Log.v(TAG, "removed contacts by id");
    }

    private static void addContact(JSONObject json, DataSource source, Context context, EncryptionUtils encryptionUtils)
            throws JSONException {

        try {
            Contact contact = new Contact();
            contact.setPhoneNumber(encryptionUtils.decrypt(json.getString("phone_number")));
            contact.setName(encryptionUtils.decrypt(json.getString("name")));
            contact.getColors().setColor(json.getInt("color"));
            contact.getColors().setColorDark(json.getInt("color_dark"));
            contact.getColors().setColorLight(json.getInt("color_light"));
            contact.getColors().setColorAccent(json.getInt("color_accent"));

            source.insertContact(context, contact, false);
            Log.v(TAG, "added contact");
        } catch (SQLiteConstraintException e) {
            // contact already exists
            Log.e(TAG, "error adding contact", e);
        } catch (Exception e) {
            // error decrypting
        }
    }

    private static void updateConversation(JSONObject json, DataSource source, Context context, EncryptionUtils encryptionUtils)
            throws JSONException {
        try {
            Conversation conversation = new Conversation();
            conversation.setId(getLong(json, "id"));
            conversation.setTitle(encryptionUtils.decrypt(json.getString("title")));
            conversation.getColors().setColor(json.getInt("color"));
            conversation.getColors().setColorDark(json.getInt("color_dark"));
            conversation.getColors().setColorLight(json.getInt("color_light"));
            conversation.getColors().setColorAccent(json.getInt("color_accent"));
            conversation.setLedColor(json.getInt("led_color"));
            conversation.setPinned(json.getBoolean("pinned"));
            conversation.setRingtoneUri(encryptionUtils.decrypt(json.has("ringtone") ?
                    json.getString("ringtone") : null));
            conversation.setMute(json.getBoolean("mute"));
            conversation.setRead(json.getBoolean("read"));
            conversation.setRead(json.getBoolean("read"));
            conversation.setArchive(json.getBoolean("archive"));
            conversation.setPrivateNotifications(json.getBoolean("private_notifications"));

            source.updateConversationSettings(context, conversation, false);

            if (conversation.getRead()) {
                source.readConversation(context, conversation.getId(), false);
            }
            Log.v(TAG, "updated conversation");
        } catch (RuntimeException e) {
            Log.e(TAG, "failed to update conversation b/c of decrypting data");
        }
    }

    private static void updateConversationTitle(JSONObject json, DataSource source, Context context, EncryptionUtils encryptionUtils)
            throws JSONException {
        try {
            source.updateConversationTitle(context, getLong(json, "id"),
                    encryptionUtils.decrypt(json.getString("title")), false
            );

            Log.v(TAG, "updated conversation title");
        } catch (RuntimeException e) {
            Log.e(TAG, "failed to update conversation title b/c of decrypting data");
        }
    }

    private static void updateConversationSnippet(JSONObject json, DataSource source, Context context, EncryptionUtils encryptionUtils)
            throws JSONException {
        try {
            source.updateConversation(context,
                    getLong(json, "id"),
                    json.getBoolean("read"),
                    getLong(json, "timestamp"),
                    encryptionUtils.decrypt(json.getString("snippet")),
                    MimeType.INSTANCE.getTEXT_PLAIN(),
                    json.getBoolean("archive"),
                    false
            );

            Log.v(TAG, "updated conversation snippet");
        } catch (RuntimeException e) {
            Log.e(TAG, "failed to update conversation snippet b/c of decrypting data");
        }
    }

    private static void removeConversation(JSONObject json, DataSource source, Context context) throws JSONException {
        long id = getLong(json, "id");
        source.deleteConversation(context, id, false);
        Log.v(TAG, "removed conversation");
    }

    private static void readConversation(JSONObject json, DataSource source, Context context) throws JSONException {
        long id = getLong(json, "id");
        String deviceId = json.getString("android_device");

        if (deviceId == null || !deviceId.equals(Account.INSTANCE.getDeviceId())) {
            Conversation conversation = source.getConversation(context, id);
            source.readConversation(context, id, false);

            if (conversation != null && !conversation.getRead()) {
                ConversationListUpdatedReceiver.Companion.sendBroadcast(context, id, conversation.getSnippet(), true);
            }

            Log.v(TAG, "read conversation");
        }
    }

    private static void seenConversation(JSONObject json, DataSource source, Context context) throws JSONException {
        long id = getLong(json, "id");
        source.seenConversation(context, id, false);
        Log.v(TAG, "seen conversation");
    }

    private static void archiveConversation(JSONObject json, DataSource source, Context context) throws JSONException {
        long id = getLong(json, "id");
        boolean archive = json.getBoolean("archive");
        source.archiveConversation(context, id, archive, false);
        Log.v(TAG, "archive conversation: " + archive);
    }

    private static void seenConversations(DataSource source, Context context) throws JSONException {
        source.seenConversations(context, false);
        Log.v(TAG, "seen all conversations");
    }

    private static void addDraft(JSONObject json, DataSource source, Context context, EncryptionUtils encryptionUtils) throws JSONException {
        Draft draft = new Draft();
        draft.setId(getLong(json, "id"));
        draft.setConversationId(getLong(json, "conversation_id"));
        draft.setData(encryptionUtils.decrypt(json.getString("data")));
        draft.setMimeType(encryptionUtils.decrypt(json.getString("mime_type")));

        source.insertDraft(context, draft, false);
        Log.v(TAG, "added draft");
    }

    private static void removeDrafts(JSONObject json, DataSource source, Context context) throws JSONException {
        long id = getLong(json, "id");
        String deviceId = json.getString("android_device");

        if (deviceId == null || !deviceId.equals(Account.INSTANCE.getDeviceId())) {
            source.deleteDrafts(context, id, false);
            Log.v(TAG, "removed drafts");
        }
    }

    private static void addBlacklist(JSONObject json, DataSource source, Context context, EncryptionUtils encryptionUtils)
            throws JSONException {
        long id = getLong(json, "id");
        String phoneNumber = json.getString("phone_number");
        phoneNumber = encryptionUtils.decrypt(phoneNumber);

        Blacklist blacklist = new Blacklist();
        blacklist.setId(id);
        blacklist.setPhoneNumber(phoneNumber);
        source.insertBlacklist(context, blacklist, false);
        Log.v(TAG, "added blacklist");
    }

    private static void removeBlacklist(JSONObject json, DataSource source, Context context) throws JSONException {
        long id = getLong(json, "id");
        source.deleteBlacklist(context, id, false);
        Log.v(TAG, "removed blacklist");
    }

    private static void addScheduledMessage(JSONObject json, DataSource source, Context context, EncryptionUtils encryptionUtils)
            throws JSONException {
        ScheduledMessage message = new ScheduledMessage();
        message.setId(getLong(json, "id"));
        message.setTo(encryptionUtils.decrypt(json.getString("to")));
        message.setData(encryptionUtils.decrypt(json.getString("data")));
        message.setMimeType(encryptionUtils.decrypt(json.getString("mime_type")));
        message.setTimestamp(getLong(json, "timestamp"));
        message.setTitle(encryptionUtils.decrypt(json.getString("title")));

        source.insertScheduledMessage(context, message, false);
        ScheduledMessageJob.scheduleNextRun(context, source);
        Log.v(TAG, "added scheduled message");
    }

    private static void updatedScheduledMessage(JSONObject json, DataSource source, Context context, EncryptionUtils encryptionUtils)
            throws JSONException {
        ScheduledMessage message = new ScheduledMessage();
        message.setId(getLong(json, "id"));
        message.setTo(encryptionUtils.decrypt(json.getString("to")));
        message.setData(encryptionUtils.decrypt(json.getString("data")));
        message.setMimeType(encryptionUtils.decrypt(json.getString("mime_type")));
        message.setTimestamp(getLong(json, "timestamp"));
        message.setTitle(encryptionUtils.decrypt(json.getString("title")));

        source.updateScheduledMessage(context, message, false);
        ScheduledMessageJob.scheduleNextRun(context, source);
        Log.v(TAG, "updated scheduled message");
    }

    private static void removeScheduledMessage(JSONObject json, DataSource source, Context context) throws JSONException {
        long id = getLong(json, "id");
        source.deleteScheduledMessage(context, id, false);
        ScheduledMessageJob.scheduleNextRun(context, source);
        Log.v(TAG, "removed scheduled message");
    }

    private static void dismissNotification(JSONObject json, DataSource source, Context context)
            throws JSONException {
        long conversationId = getLong(json, "id");
        String deviceId = json.getString("device_id");

        if (deviceId == null || !deviceId.equals(Account.INSTANCE.getDeviceId())) {
            Conversation conversation = source.getConversation(context, conversationId);

            // don't want to mark as read if this device was the one that sent the dismissal fcm message
            source.readConversation(context, conversationId, false);
            if (conversation != null && !conversation.getRead()) {
                ConversationListUpdatedReceiver.Companion.sendBroadcast(context, conversationId, conversation.getSnippet(), true);
            }

            NotificationManagerCompat.from(context).cancel((int) conversationId);
            Log.v(TAG, "dismissed notification for " + conversationId);
        }
    }

    private static void updateSetting(JSONObject json, Context context)
            throws JSONException {
        String pref = json.getString("pref");
        String type = json.getString("type");

        if (pref != null && type != null && json.has("value")) {
            Settings settings = Settings.INSTANCE;
            switch (type.toLowerCase(Locale.getDefault())) {
                case "boolean":
                    settings.setValue(context, pref, json.getBoolean("value"));
                    break;
                case "long":
                    settings.setValue(context, pref, getLong(json, "value"));
                    break;
                case "int":
                    settings.setValue(context, pref, json.getInt("value"));
                    break;
                case "string":
                    settings.setValue(context, pref, json.getString("value"));
                    break;
                case "set":
                    settings.setValue(context, pref, SetUtils.INSTANCE.createSet(json.getString("value")));
                    break;
            }
        }
    }

    private static void updateSubscription(JSONObject json, Context context)
            throws JSONException {
        int type = json.has("type") ? json.getInt("type") : 0;
        long expiration = json.has("expiration") ? json.getLong("expiration") : 0L;
        boolean fromAdmin = json.has("from_admin") ? json.getBoolean("from_admin") : false;

        Account account = Account.INSTANCE;

        if (account.getPrimary()) {
            account.updateSubscription(context,
                    Account.SubscriptionType.Companion.findByTypeCode(type), expiration, false
            );

            SubscriptionExpirationCheckJob.scheduleNextRun(context);
            SignoutJob.writeSignoutTime(context, 0);

            if (fromAdmin) {
                String content = "Enjoy the app!";

                if (!account.getSubscriptionType().equals(Account.SubscriptionType.LIFETIME)) {
                    content = "Expiration: " + new Date(expiration).toString();
                }

                notifyUser(context, "Subscription Updated: " + StringUtils.INSTANCE.titleize(account.getSubscriptionType().name()), content);
            }
        }
    }

    private static void updatePrimaryDevice(JSONObject json, Context context)
            throws JSONException {
        String newPrimaryDeviceId = json.getString("new_primary_device_id");

        Account account = Account.INSTANCE;
        if (newPrimaryDeviceId != null && !newPrimaryDeviceId.equals(account.getDeviceId())) {
            account.setPrimary(context, false);
        }
    }

    private static void writeFeatureFlag(JSONObject json, Context context)
            throws JSONException {

        String identifier = json.getString("id");
        boolean value = json.getBoolean("value");
        int rolloutPercent = json.getInt("rollout"); // 1 - 100

        if (!value) {
            // if we are turning the flag off, we want to do it for everyone immediately
            FeatureFlags.INSTANCE.updateFlag(context, identifier, false);
        } else {
            Random rand = new Random();
            int random = rand.nextInt(100) + 1; // between 1 - 100

            if (random <= rolloutPercent) {
                // they made it in the staged rollout!
                FeatureFlags.INSTANCE.updateFlag(context, identifier, true);
            }

            // otherwise, don't do anything. We don't want to turn the flag off for those
            // that had gotten it turned on in the past.
        }
    }

    private static void forwardToPhone(JSONObject json, DataSource source, Context context)
            throws JSONException {

        if (!Account.INSTANCE.getPrimary()) {
            return;
        }

        String text = json.getString("message");
        String toFromWeb = json.getString("to");
        String[] split = toFromWeb.split(",");
        
        String to = "";
        for (int i = 0; i < split.length; i++) {
            if (i != 0) {
                to += ", ";
            }
            
            to += PhoneNumberUtils.INSTANCE.clearFormatting(split[i]);
        }

        Message message = new Message();
        message.setType(Message.Companion.getTYPE_SENDING());
        message.setData(text);
        message.setTimestamp(System.currentTimeMillis());
        message.setMimeType(MimeType.INSTANCE.getTEXT_PLAIN());
        message.setRead(true);
        message.setSeen(true);
        message.setSimPhoneNumber(DualSimUtils.INSTANCE.getDefaultPhoneNumber());

        if (json.has("sent_device")) {
            message.setSentDeviceId(json.getLong("sent_device"));
        } else {
            message.setSentDeviceId(0);
        }

        long conversationId = source.insertMessage(message, to, context, true);
        Conversation conversation = source.getConversation(context, conversationId);

        new SendUtils(conversation != null ? conversation.getSimSubscriptionId() : null)
                .send(context, message.getData(), to);
    }
    
    private static long getLong(JSONObject json, String identifier) {
        try {
            String str = json.getString(identifier);
            return Long.parseLong(str);
        } catch (Exception e) {
            return 0L;
        }
    }

    private static void notifyUser(Context context, String title, String content) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, NotificationUtils.INSTANCE.getGENERAL_CHANNEL_ID())
                .setContentTitle(title)
                .setContentText(content)
                .setStyle(new NotificationCompat.BigTextStyle().setBigContentTitle(title).setSummaryText(content))
                .setSmallIcon(R.drawable.ic_stat_notify_group)
                .setPriority(Notification.PRIORITY_HIGH)
                .setColor(Settings.INSTANCE.getMainColorSet().getColor());

        NotificationManagerCompat.from(context).notify(INFORMATION_NOTIFICATION_ID, builder.build());
    }
}
