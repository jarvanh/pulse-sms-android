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

package xyz.klinker.messenger.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Base64;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.crypto.spec.SecretKeySpec;

import xyz.klinker.messenger.api.implementation.ApiUtils;
import xyz.klinker.messenger.api.implementation.MessengerFirebaseMessagingService;
import xyz.klinker.messenger.data.DataSource;
import xyz.klinker.messenger.data.MimeType;
import xyz.klinker.messenger.data.Settings;
import xyz.klinker.messenger.data.model.Blacklist;
import xyz.klinker.messenger.data.model.Conversation;
import xyz.klinker.messenger.data.model.Draft;
import xyz.klinker.messenger.data.model.Message;
import xyz.klinker.messenger.data.model.ScheduledMessage;
import xyz.klinker.messenger.encryption.EncryptionUtils;
import xyz.klinker.messenger.service.NotificationService;
import xyz.klinker.messenger.util.ContactUtils;
import xyz.klinker.messenger.util.SendUtils;

/**
 * Receiver responsible for processing firebase data messages and persisting to the database.
 */
public class FirebaseMessageReceiver extends BroadcastReceiver {

    private static final String TAG = "FirebaseMessageReceiver";

    private EncryptionUtils encryptionUtils;

    @Override
    public void onReceive(final Context context, final Intent intent) {
        if (!MessengerFirebaseMessagingService.ACTION_FIREBASE_MESSAGE_RECEIVED
                .equals(intent.getAction())) {
            return;
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                process(context, intent);
            }
        }).start();
    }

    private void process(Context context, Intent intent) {
        encryptionUtils = new EncryptionUtils(
                new SecretKeySpec(Base64.decode(Settings.get(context).key, Base64.DEFAULT), "AES"));

        String operation = intent.getStringExtra(MessengerFirebaseMessagingService.EXTRA_OPERATION);
        String data = intent.getStringExtra(MessengerFirebaseMessagingService.EXTRA_DATA);

        JSONObject json;
        try {
            json = new JSONObject(data);


            DataSource source = DataSource.getInstance(context);
            source.open();
            source.setUpload(false);

            switch (operation) {
                case "removed_account":
                    removeAccount(json, source, context);
                    break;
                case "added_message":
                    addMessage(json, source, context);
                    break;
                case "updated_message":
                    updateMessage(json, source, context);
                    break;
                case "removed_message":
                    removeMessage(json, source);
                    break;
                case "added_conversation":
                    addConversation(json, source, context);
                    break;
                case "updated_conversation":
                    updateConversation(json, source, context);
                    break;
                case "removed_conversation":
                    removeConversation(json, source);
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
                case "removed_scheduled_message":
                    removeScheduledMessage(json, source);
                    break;
                case "changed_snooze":
                    changeSnooze(json, context);
                    break;
                case "changed_dark_theme":
                    changeDarkTheme(json, context);
                    break;
                case "changed_vibrate":
                    changeVibrate(json, context);
                    break;
                case "dismissed_notification":
                    dismissNotification(json, context);
                    break;
                default:
                    Log.e(TAG, "unsupported operation: " + operation);
                    break;
            }

            source.setUpload(true);
            source.close();
        } catch (JSONException e) {
            Log.e(TAG, "error parsing data json", e);
        }
    }

    private void removeAccount(JSONObject json, DataSource source, Context context)
            throws JSONException {
        Settings settings = Settings.get(context);

        if (json.getString("id").equals(settings.accountId)) {
            Log.v(TAG, "clearing account");
            source.clearTables();
            settings.removeValue("account_id");
            settings.removeValue("device_id");
        } else {
            Log.v(TAG, "ids do not match, did not clear account");
        }
    }

    private void addMessage(JSONObject json, final DataSource source, final Context context)
            throws JSONException {
        long id = json.getLong("id");
        if (source.getMessage(id) == null) {
            final Message message = new Message();
            message.id = id;
            message.conversationId = json.getLong("conversation_id");
            message.type = json.getInt("type");
            message.data = encryptionUtils.decrypt(json.getString("data"));
            message.timestamp = json.getLong("timestamp");
            message.mimeType = encryptionUtils.decrypt(json.getString("mime_type"));
            message.read = json.getBoolean("read");
            message.seen = json.getBoolean("seen");
            message.from = encryptionUtils.decrypt(json.has("from") ? json.getString("from") : null);

            if (json.has("color") && !json.getString("color").equals("null")) {
                message.color = json.getInt("color");
            }

            final AtomicBoolean downloading = new AtomicBoolean(false);
            if (message.data.equals("firebase -1") &&
                    !message.mimeType.equals(MimeType.TEXT_PLAIN)) {
                Log.v(TAG, "downloading binary from firebase");
                downloading.set(true);

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        ApiUtils apiUtils = new ApiUtils();
                        apiUtils.saveFirebaseFolderRef(Settings.get(context).accountId);

                        final File file = new File(context.getFilesDir(),
                                message.id + MimeType.getExtension(message.mimeType));
                        apiUtils.downloadFileFromFirebase(file, message.id, encryptionUtils);
                        message.data = Uri.fromFile(file).toString();
                        source.updateMessageData(message.id, message.data);
                        MessageListUpdatedReceiver.sendBroadcast(context, message.conversationId);
                        downloading.set(false);
                    }
                }).start();
            }

            source.insertMessage(context, message, message.conversationId);
            Log.v(TAG, "added message");

            if (Settings.get(context).primary && message.type == Message.TYPE_SENDING) {
                while (downloading.get()) {
                    Log.v(TAG, "waiting for download before sending");
                    try { Thread.sleep(1000); } catch (Exception e) { }
                }

                Conversation conversation = source.getConversation(message.conversationId);

                if (message.mimeType.equals(MimeType.TEXT_PLAIN)) {
                    SendUtils.send(context, message.data, conversation.phoneNumbers);
                } else {
                    SendUtils.send(context, "", conversation.phoneNumbers,
                            Uri.parse(message.data), message.mimeType);
                }

                Log.v(TAG, "sent message");
            }

            MessageListUpdatedReceiver.sendBroadcast(context, message.conversationId);
            ConversationListUpdatedReceiver.sendBroadcast(context, message.conversationId,
                    message.mimeType.equals(MimeType.TEXT_PLAIN) ? message.data : "",
                    message.type != Message.TYPE_RECEIVED);

            if (message.type == Message.TYPE_RECEIVED) {
                context.startService(new Intent(context, NotificationService.class));
            }
        } else {
            Log.v(TAG, "message already exists, not doing anything with it");
        }
    }

    private void updateMessage(JSONObject json, DataSource source, Context context)
            throws JSONException {
        long id = json.getLong("id");
        source.updateMessageType(id, json.getInt("type"));
        Message message = source.getMessage(id);
        if (message != null) {
            MessageListUpdatedReceiver.sendBroadcast(context, message.conversationId);
        }
        Log.v(TAG, "updated message type");
    }

    private void removeMessage(JSONObject json, DataSource source) throws JSONException {
        long id = json.getLong("id");
        source.deleteMessage(id);
        Log.v(TAG, "removed message");
    }

    private void addConversation(JSONObject json, DataSource source, Context context)
            throws JSONException {
        Conversation conversation = new Conversation();
        conversation.id = json.getLong("id");
        conversation.colors.color = json.getInt("color");
        conversation.colors.colorDark = json.getInt("color_dark");
        conversation.colors.colorLight = json.getInt("color_light");
        conversation.colors.colorAccent = json.getInt("color_accent");
        conversation.pinned = json.getBoolean("pinned");
        conversation.read = json.getBoolean("read");
        conversation.timestamp = json.getLong("timestamp");
        conversation.title = encryptionUtils.decrypt(json.getString("title"));
        conversation.phoneNumbers = encryptionUtils.decrypt(json.getString("phone_numbers"));
        conversation.snippet = encryptionUtils.decrypt(json.getString("snippet"));
        conversation.ringtoneUri = encryptionUtils.decrypt(json.has("ringtone") ?
                json.getString("ringtone") : null);
        conversation.imageUri = ContactUtils.findImageUri(conversation.phoneNumbers, context);
        conversation.idMatcher = encryptionUtils.decrypt(json.getString("id_matcher"));
        conversation.mute = json.getBoolean("mute");

        source.insertConversation(conversation);
    }

    private void updateConversation(JSONObject json, DataSource source, Context context)
            throws JSONException {
        Conversation conversation = new Conversation();
        conversation.id = json.getLong("id");
        conversation.title = encryptionUtils.decrypt(json.getString("title"));
        conversation.colors.color = json.getInt("color");
        conversation.colors.colorDark = json.getInt("color_dark");
        conversation.colors.colorLight = json.getInt("color_light");
        conversation.colors.colorAccent = json.getInt("color_accent");
        conversation.pinned = json.getBoolean("pinned");
        conversation.ringtoneUri = encryptionUtils.decrypt(json.has("ringtone") ?
                json.getString("ringtone") : null);
        conversation.mute = json.getBoolean("mute");
        conversation.read = json.getBoolean("read");

        source.updateConversationSettings(conversation);

        if (conversation.read) {
            source.readConversation(context, conversation.id);
        }
        Log.v(TAG, "updated conversation");
    }

    private void removeConversation(JSONObject json, DataSource source) throws JSONException {
        long id = json.getLong("id");
        source.deleteConversation(id);
        Log.v(TAG, "removed conversation");
    }

    private void addDraft(JSONObject json, DataSource source) throws JSONException {
        Draft draft = new Draft();
        draft.id = json.getLong("id");
        draft.conversationId = json.getLong("conversation_id");
        draft.data = encryptionUtils.decrypt(json.getString("data"));
        draft.mimeType = encryptionUtils.decrypt(json.getString("mime_type"));

        source.insertDraft(draft);
        Log.v(TAG, "added draft");
    }

    private void removeDrafts(JSONObject json, DataSource source) throws JSONException {
        long id = json.getLong("id");
        source.deleteDrafts(id);
        Log.v(TAG, "removed drafts");
    }

    private void addBlacklist(JSONObject json, DataSource source) throws JSONException {
        long id = json.getLong("id");
        String phoneNumber = json.getString("phone_number");
        phoneNumber = encryptionUtils.decrypt(phoneNumber);

        Blacklist blacklist = new Blacklist();
        blacklist.id = id;
        blacklist.phoneNumber = phoneNumber;
        source.insertBlacklist(blacklist);
        Log.v(TAG, "added blacklist");
    }

    private void removeBlacklist(JSONObject json, DataSource source) throws JSONException {
        long id = json.getLong("id");
        source.deleteBlacklist(id);
        Log.v(TAG, "removed blacklist");
    }

    private void addScheduledMessage(JSONObject json, DataSource source) throws JSONException {
        ScheduledMessage message = new ScheduledMessage();
        message.id = json.getLong("id");
        message.to = encryptionUtils.decrypt(json.getString("to"));
        message.data = encryptionUtils.decrypt(json.getString("data"));
        message.mimeType = encryptionUtils.decrypt(json.getString("mime_type"));
        message.timestamp = json.getLong("timestamp");
        message.title = encryptionUtils.decrypt(json.getString("title"));

        source.insertScheduledMessage(message);
        Log.v(TAG, "added scheduled message");
    }

    private void removeScheduledMessage(JSONObject json, DataSource source) throws JSONException {
        long id = json.getLong("id");
        source.deleteScheduledMessage(id);
        Log.v(TAG, "removed scheduled message");
    }

    private void changeSnooze(JSONObject json, Context context)
            throws JSONException {
        Settings.get(context).setValue("snooze", json.getLong("value"));
    }

    private void changeDarkTheme(JSONObject json, Context context)
            throws JSONException {
        Settings.get(context).setValue("dark_theme", json.getBoolean("value"));
    }

    private void changeVibrate(JSONObject json, Context context)
            throws JSONException {
        Settings.get(context).setValue("vibrate", json.getBoolean("value"));
    }

    private void dismissNotification(JSONObject json, Context context)
            throws JSONException {
        NotificationManagerCompat.from(context).cancel(json.getInt("id"));
    }

}
