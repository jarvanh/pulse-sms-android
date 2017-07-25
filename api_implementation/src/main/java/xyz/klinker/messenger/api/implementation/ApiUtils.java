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

package xyz.klinker.messenger.api.implementation;

import android.content.Context;
import android.util.Log;

import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import retrofit2.Call;
import retrofit2.Response;
import xyz.klinker.messenger.api.Api;
import xyz.klinker.messenger.api.entity.AddBlacklistRequest;
import xyz.klinker.messenger.api.entity.AddContactRequest;
import xyz.klinker.messenger.api.entity.AddConversationRequest;
import xyz.klinker.messenger.api.entity.AddDeviceRequest;
import xyz.klinker.messenger.api.entity.AddDeviceResponse;
import xyz.klinker.messenger.api.entity.AddDraftRequest;
import xyz.klinker.messenger.api.entity.AddMessagesRequest;
import xyz.klinker.messenger.api.entity.AddScheduledMessageRequest;
import xyz.klinker.messenger.api.entity.BlacklistBody;
import xyz.klinker.messenger.api.entity.ContactBody;
import xyz.klinker.messenger.api.entity.ConversationBody;
import xyz.klinker.messenger.api.entity.DeviceBody;
import xyz.klinker.messenger.api.entity.DraftBody;
import xyz.klinker.messenger.api.entity.LoginRequest;
import xyz.klinker.messenger.api.entity.LoginResponse;
import xyz.klinker.messenger.api.entity.MessageBody;
import xyz.klinker.messenger.api.entity.ScheduledMessageBody;
import xyz.klinker.messenger.api.entity.SignupRequest;
import xyz.klinker.messenger.api.entity.SignupResponse;
import xyz.klinker.messenger.api.entity.UpdateContactRequest;
import xyz.klinker.messenger.api.entity.UpdateConversationRequest;
import xyz.klinker.messenger.api.entity.UpdateMessageRequest;
import xyz.klinker.messenger.api.entity.UpdateScheduledMessageRequest;
import xyz.klinker.messenger.api.implementation.firebase.FirebaseDownloadCallback;
import xyz.klinker.messenger.api.implementation.firebase.FirebaseUploadCallback;
import xyz.klinker.messenger.api.implementation.retrofit.LoggingRetryableCallback;
import xyz.klinker.messenger.encryption.EncryptionUtils;

/**
 * Utility for easing access to APIs.
 */
public class ApiUtils {

    public static final int RETRY_COUNT = 3;

    private static final String TAG = "ApiUtils";
    private static final long MAX_SIZE = 1024 * 1024 * 5;
    private static final String FIREBASE_STORAGE_URL = "gs://messenger-42616.appspot.com";
    public static String environment;

    private Api api;
    private boolean active = true;
    private StorageReference folderRef;

    /**
     * Creates a new api utility that will be used to directly interface with the server apis.
     */
    public ApiUtils() {
        this.api = ApiAccessor.create(environment);
    }

    /**
     * Sets whether or not the utilities are currently active. This will be useful when we
     * receive messages on devices through FCM and we don't want to persist those messages back
     * to FCM again.
     */
    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean isActive() {
        return active;
    }

    public static boolean isCallSuccessful(Response response) {
        int code = response.code();
        return (code >= 200 && code < 400);
    }

    /**
     * Logs into the server.
     */
    public LoginResponse login(String email, String password) {
        try {
            LoginRequest request = new LoginRequest(email, password);
            return api.account().login(request).execute().body();
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Signs up for the service.
     */
    public SignupResponse signup(String email, String password, String name, String phoneNumber) {
        try {
            SignupRequest request = new SignupRequest(email, name, password, phoneNumber);
            return api.account().signup(request).execute().body();
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Removes the account from the server.
     */
    public void deleteAccount(final String accountId) {
        String message = "removed account";
        Call<Void> call = api.account().remove(accountId);

        call.enqueue(new LoggingRetryableCallback<>(call, RETRY_COUNT, message));
    }

    /**
     * Cleans all the database tables, for the account, on the server
     */
    public void cleanAccount(final String accountId) {
        String message = "cleaned account";
        Call<Void> call = api.account().clean(accountId);

        call.enqueue(new LoggingRetryableCallback<>(call, RETRY_COUNT, message));
    }

    /**
     * Registers your device as a new device on the server.
     */
    public Integer registerDevice(String accountId, String info, String name,
                                  boolean primary, String fcmToken) {
        final DeviceBody deviceBody = new DeviceBody(info, name, primary, fcmToken);
        final AddDeviceRequest request = new AddDeviceRequest(accountId, deviceBody);

        try {
            AddDeviceResponse response = api.device().add(request).execute().body();
            if (response != null) {
                return response.id;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Removes a device from the server.
     */
    public void removeDevice(String accountId, int deviceId) {
        String message = "remove device";
        Call<Void> call = api.device().remove(deviceId, accountId);

        call.enqueue(new LoggingRetryableCallback<>(call, RETRY_COUNT, message));
    }

    public void updatePrimaryDevice(final String accountId, final String newPrimaryDeviceId) {
        if (!active || accountId == null) {
            return;
        }

        String message = "update primary device";
        Call<Void> call = api.device().updatePrimary(newPrimaryDeviceId, accountId);

        call.enqueue(new LoggingRetryableCallback<>(call, RETRY_COUNT, message));
    }

    /**
     * Gets a list of all devices on the server.
     */
    public DeviceBody[] getDevices(String accountId) {
        try {
            return api.device().list(accountId).execute().body();
        } catch (IOException e) {
            return new DeviceBody[0];
        }
    }

    /**
     * Updates device info on the server.
     */
    public void updateDevice(String accountId, long deviceId, String name, String fcmToken) {
        String message = "update device";
        Call<Void> call = api.device().update(deviceId, accountId, name, fcmToken);

        call.enqueue(new LoggingRetryableCallback<>(call, RETRY_COUNT, message));
    }

    /**
     * Adds a new contact.
     */
    public void addContact(final String accountId, final String phoneNumber, final String name, final int color,
                                final int colorDark, final int colorLight, final int colorAccent,
                                final EncryptionUtils encryptionUtils) {
        if (!active || accountId == null || encryptionUtils == null) {
            return;
        }

        final ContactBody body = new ContactBody(
                encryptionUtils.encrypt(phoneNumber), encryptionUtils.encrypt(name),
                color, colorDark, colorLight, colorAccent);
        final AddContactRequest request = new AddContactRequest(accountId, body);

        addContact(request);
    }
    /**
     * Adds a new contact.
     */
    public void addContact(final AddContactRequest request) {
        String message = "add contact";
        Call<Void> call = api.contact().add(request);

        call.enqueue(new LoggingRetryableCallback<>(call, RETRY_COUNT, message));
    }

    /**
     * Deletes a conversation and all of its messages.
     */
    public void deleteContact(final String accountId, final String phoneNumber,
                              final EncryptionUtils encryptionUtils) {
        if (!active || accountId == null || encryptionUtils == null) {
            return;
        }

        String message = "delete contact";
        Call<Void> call = api.contact().remove(encryptionUtils.encrypt(phoneNumber), accountId);

        call.enqueue(new LoggingRetryableCallback<>(call, RETRY_COUNT, message));
    }

    /**
     * Updates a conversation with new settings or info.
     */
    public void updateContact(final String accountId, final String phoneNumber, final String name,
                                   final Integer color, final Integer colorDark, final Integer colorLight,
                                   final Integer colorAccent,
                                   final EncryptionUtils encryptionUtils) {
        if (!active || accountId == null || encryptionUtils == null) {
            return;
        }

        final UpdateContactRequest request = new UpdateContactRequest(
                encryptionUtils.encrypt(phoneNumber), encryptionUtils.encrypt(name),
                color, colorDark, colorLight, colorAccent);

        String message = "update contact";
        Call<Void> call = api.contact().update(encryptionUtils.encrypt(phoneNumber), accountId, request);

        call.enqueue(new LoggingRetryableCallback<>(call, RETRY_COUNT, message));
    }

    /**
     * Adds a new conversation.
     */
    public void addConversation(final String accountId, final long deviceId, final int color,
                                final int colorDark, final int colorLight, final int colorAccent,
                                final int ledColor, final boolean pinned, final boolean read, final long timestamp,
                                final String title, final String phoneNumbers, final String snippet,
                                final String ringtone, final String idMatcher, final boolean mute,
                                final boolean archive, final boolean privateNotifications,
                                final EncryptionUtils encryptionUtils) {
        if (!active || accountId == null || encryptionUtils == null) {
            return;
        }

        final ConversationBody body = new ConversationBody(
                deviceId, color, colorDark, colorLight, colorAccent, ledColor,
                pinned, read, timestamp, encryptionUtils.encrypt(title),
                encryptionUtils.encrypt(phoneNumbers), encryptionUtils.encrypt(snippet),
                encryptionUtils.encrypt(ringtone), null,
                encryptionUtils.encrypt(idMatcher), mute, archive, privateNotifications);
        final AddConversationRequest request = new AddConversationRequest(accountId, body);

        String message = "add conversation";
        Call<Void> call = api.conversation().add(request);

        call.enqueue(new LoggingRetryableCallback<>(call, RETRY_COUNT, message));
    }

    /**
     * Deletes a conversation and all of its messages.
     */
    public void deleteConversation(final String accountId, final long deviceId) {
        if (!active || accountId == null) {
            return;
        }

        String message = "delete conversation";
        Call<Void> call = api.conversation().remove(deviceId, accountId);

        call.enqueue(new LoggingRetryableCallback<>(call, RETRY_COUNT, message));
    }

    /**
     * Archives a conversation.
     */
    public void archiveConversation(final String accountId, final long deviceId) {
        if (!active || accountId == null) {
            return;
        }

        String message = "archive conversation";
        Call<Void> call = api.conversation().archive(deviceId, accountId);

        call.enqueue(new LoggingRetryableCallback<>(call, RETRY_COUNT, message));
    }

    /**
     * Moves a conversation back to the inbox.
     */
    public void unarchiveConversation(final String accountId, final long deviceId) {
        if (!active || accountId == null) {
            return;
        }

        String message = "unarchive conversation";
        Call<Void> call = api.conversation().unarchive(deviceId, accountId);

        call.enqueue(new LoggingRetryableCallback<>(call, RETRY_COUNT, message));
    }

    /**
     * Updates a conversation with new settings or info.
     */
    public void updateConversation(final String accountId, final long deviceId, final Integer color,
                                   final Integer colorDark, final Integer colorLight,
                                   final Integer colorAccent, final Integer ledColor, final Boolean pinned,
                                   final Boolean read, final Long timestamp, final String title,
                                   final String snippet, final String ringtone, final Boolean mute,
                                   final Boolean archive, final Boolean privateNotifications,
                                   final EncryptionUtils encryptionUtils) {
        if (!active || accountId == null || encryptionUtils == null) {
            return;
        }

        final UpdateConversationRequest request = new UpdateConversationRequest(color,
                colorDark, colorLight, colorAccent, ledColor, pinned, read, timestamp,
                encryptionUtils.encrypt(title), encryptionUtils.encrypt(snippet),
                encryptionUtils.encrypt(ringtone), mute, archive, privateNotifications);

        String message = "update conversation";
        Call<Void> call = api.conversation().update(deviceId, accountId, request);

        call.enqueue(new LoggingRetryableCallback<>(call, RETRY_COUNT, message));
    }

    /**
     * Updates a conversation with new snippet info
     */
    public void updateConversationSnippet(final String accountId, final long deviceId,
                                          final Boolean read, final Boolean archive,
                                          final Long timestamp, final String snippet,
                                          final EncryptionUtils encryptionUtils) {
        if (!active || accountId == null || encryptionUtils == null) {
            return;
        }

        final UpdateConversationRequest request = new UpdateConversationRequest(null, null, null,
                null, null, null, read, timestamp, null, encryptionUtils.encrypt(snippet),
                null, null, archive, null);

        String message = "update conversation snippet";
        Call<Void> call = api.conversation().updateSnippet(deviceId, accountId, request);

        call.enqueue(new LoggingRetryableCallback<>(call, RETRY_COUNT, message));
    }

    /**
     * Updates a conversation with a new title (usually when the name changes)
     */
    public void updateConversationTitle(final String accountId, final long deviceId,
                                          final String title, final EncryptionUtils encryptionUtils) {
        if (!active || accountId == null || encryptionUtils == null) {
            return;
        }

        String message = "update conversation title";
        Call<Void> call = api.conversation().updateTitle(deviceId, accountId, encryptionUtils.encrypt(title));

        call.enqueue(new LoggingRetryableCallback<>(call, RETRY_COUNT, message));
    }

    /**
     * Marks all messages in conversation as read.
     */
    public void readConversation(final String accountId, final String androidDevice, final long deviceId) {
        if (!active || accountId == null) {
            return;
        }

        String message = "read conversation";
        Call<Void> call = api.conversation().read(deviceId, androidDevice, accountId);

        call.enqueue(new LoggingRetryableCallback<>(call, RETRY_COUNT, message));
    }

    /**
     * Marks all messages in conversation as seen.
     */
    public void seenConversation(final String accountId, final long deviceId) {
        if (!active || accountId == null) {
            return;
        }

        String message = "seen conversation";
        Call<Void> call = api.conversation().seen(deviceId, accountId);

        //call.enqueue(new LoggingRetryableCallback<>(call, RETRY_COUNT, message));
    }

    /**
     * Marks all messages as seen.
     */
    public void seenConversations(final String accountId) {
        if (!active || accountId == null) {
            return;
        }

        String message = "seen all conversation";
        Call<Void> call = api.conversation().seen(accountId);

        //call.enqueue(new LoggingRetryableCallback<>(call, RETRY_COUNT, message));
    }

    /**
     * Adds a new message to the server.
     */
    public void addMessage(final Context context, final String accountId, final long deviceId,
                           final long deviceConversationId, final int messageType,
                           final String data, final long timestamp, final String mimeType,
                           final boolean read, final boolean seen, final String messageFrom,
                           final Integer color, final EncryptionUtils encryptionUtils) {
        if (!active || accountId == null || encryptionUtils == null) {
            return;
        }

        if (mimeType.equals("text/plain") || messageType == 6) {
            final MessageBody body = new MessageBody(deviceId,
                    deviceConversationId, messageType, encryptionUtils.encrypt(data),
                    timestamp, encryptionUtils.encrypt(mimeType), read, seen,
                    encryptionUtils.encrypt(messageFrom), color);
            final AddMessagesRequest request = new AddMessagesRequest(accountId, body);
            String message = "add message";
            Call<Void> call = api.message().add(request);

            call.enqueue(new LoggingRetryableCallback<>(call, RETRY_COUNT, message));
        } else {
            saveFirebaseFolderRef(accountId);
            byte[] bytes = BinaryUtils.getMediaBytes(context, data, mimeType);
            uploadBytesToFirebase(bytes, deviceId, encryptionUtils, () -> {
                final MessageBody body = new MessageBody(deviceId, deviceConversationId,
                        messageType, encryptionUtils.encrypt("firebase -1"),
                        timestamp, encryptionUtils.encrypt(mimeType), read, seen,
                        encryptionUtils.encrypt(messageFrom), color);
                final AddMessagesRequest request = new AddMessagesRequest(accountId, body);
                String message = "add media message";
                Call<Void> call = api.message().add(request);

                call.enqueue(new LoggingRetryableCallback<>(call, RETRY_COUNT, message));
            }, 0);
        }
    }

    /**
     * Updates a message with the given parameters.
     */
    public void updateMessage(final String accountId, final long deviceId, Integer type,
                              Boolean read, Boolean seen) {
        if (!active || accountId == null) {
            return;
        }

        final UpdateMessageRequest request = new UpdateMessageRequest(type, read, seen);
        String message = "update message";
        Call<Void> call = api.message().update(deviceId, accountId, request);

        call.enqueue(new LoggingRetryableCallback<>(call, 6, message));
    }

    /**
     * Updates a message with the given parameters.
     */
    public void updateMessageType(final String accountId, final long deviceId, final int type) {
        if (!active || accountId == null) {
            return;
        }

        String message = "update message type";
        Call<Void> call = api.message().updateType(deviceId, accountId, type);

        call.enqueue(new LoggingRetryableCallback<>(call, RETRY_COUNT, message));
    }

    /**
     * Deletes the given message.
     */
    public void deleteMessage(final String accountId, final long deviceId) {
        if (!active || accountId == null) {
            return;
        }

        String message = "delete message";
        Call<Void> call = api.message().remove(deviceId, accountId);

        call.enqueue(new LoggingRetryableCallback<>(call, RETRY_COUNT, message));
    }

    /**
     * Deletes messages older than the given timestamp.
     */
    public void cleanupMessages(final String accountId, final long timestamp) {
        if (!active || accountId == null) {
            return;
        }

        String message = "clean up messages";
        Call<Void> call = api.message().cleanup(accountId, timestamp);

        call.enqueue(new LoggingRetryableCallback<>(call, RETRY_COUNT, message));
    }

    /**
     * Adds a draft.
     */
    public void addDraft(final String accountId, final long deviceId,
                         final long deviceConversationId, final String data,
                         final String mimeType, final EncryptionUtils encryptionUtils) {
        if (!active || accountId == null || encryptionUtils == null) {
            return;
        }

        final DraftBody body = new DraftBody(deviceId, deviceConversationId,
                encryptionUtils.encrypt(data), encryptionUtils.encrypt(mimeType));
        final AddDraftRequest request = new AddDraftRequest(accountId, body);

        String message = "add draft";
        Call<Void> call = api.draft().add(request);

        call.enqueue(new LoggingRetryableCallback<>(call, RETRY_COUNT, message));
    }

    /**
     * Deletes the given drafts.
     */
    public void deleteDrafts(final String accountId, final String androidDeviceId, final long deviceConversationId) {
        if (!active || accountId == null) {
            return;
        }

        String message = "delete drafts";
        Call<Void> call = api.draft().remove(deviceConversationId, androidDeviceId, accountId);

        call.enqueue(new LoggingRetryableCallback<>(call, RETRY_COUNT, message));
    }

    /**
     * Adds a blacklist.
     */
    public void addBlacklist(final String accountId, final long deviceId, final String phoneNumber,
                             final EncryptionUtils encryptionUtils) {
        if (!active || accountId == null || encryptionUtils == null) {
            return;
        }

        final BlacklistBody body = new BlacklistBody(deviceId,
                encryptionUtils.encrypt(phoneNumber));
        final AddBlacklistRequest request = new AddBlacklistRequest(accountId, body);

        String message = "add blacklist";
        Call<Void> call = api.blacklist().add(request);

        call.enqueue(new LoggingRetryableCallback<>(call, RETRY_COUNT, message));
    }

    /**
     * Deletes the given blacklist.
     */
    public void deleteBlacklist(final String accountId, final long deviceId) {
        if (!active || accountId == null) {
            return;
        }

        String message = "delete blacklist";
        Call<Void> call = api.blacklist().remove(deviceId, accountId);

        call.enqueue(new LoggingRetryableCallback<>(call, RETRY_COUNT, message));
    }

    /**
     * Adds a scheduled message.
     */
    public void addScheduledMessage(final String accountId, final long deviceId, final String title,
                                    final String to, final String data, final String mimeType,
                                    final long timestamp, final EncryptionUtils encryptionUtils) {
        if (!active || accountId == null || encryptionUtils == null) {
            return;
        }

        final ScheduledMessageBody body = new ScheduledMessageBody(
                deviceId,
                encryptionUtils.encrypt(to),
                encryptionUtils.encrypt(data),
                encryptionUtils.encrypt(mimeType),
                timestamp,
                encryptionUtils.encrypt(title));

        final AddScheduledMessageRequest request =
                new AddScheduledMessageRequest(accountId, body);

        String message = "add scheduled message";
        Call<Void> call = api.scheduled().add(request);

        call.enqueue(new LoggingRetryableCallback<>(call, RETRY_COUNT, message));
    }

    public void updateScheduledMessage(final String accountId, final long deviceId, final String title,
                                       final String to, final String data, final String mimeType,
                                       final long timestamp, final EncryptionUtils encryptionUtils) {
        if (!active || accountId == null || encryptionUtils == null) {
            return;
        }

        final UpdateScheduledMessageRequest request = new UpdateScheduledMessageRequest(
                encryptionUtils.encrypt(to), encryptionUtils.encrypt(data),
                encryptionUtils.encrypt(mimeType), timestamp,
                encryptionUtils.encrypt(title));

        String message = "update scheduled message";
        Call<Void> call = api.scheduled().update(deviceId, accountId, request);

        call.enqueue(new LoggingRetryableCallback<>(call, RETRY_COUNT, message));
    }

    /**
     * Deletes the given scheduled message.
     */
    public void deleteScheduledMessage(final String accountId, final long deviceId) {
        if (!active || accountId == null) {
            return;
        }

        String message = "delete scheduled message";
        Call<Void> call = api.scheduled().remove(deviceId, accountId);

        call.enqueue(new LoggingRetryableCallback<>(call, RETRY_COUNT, message));
    }

    /**
     * Uploads a byte array of encrypted data to firebase.
     *
     * @param bytes the byte array to upload.
     * @param messageId the message id that the data belongs to.
     * @param encryptionUtils the utils to encrypt the byte array with.
     */
    public void uploadBytesToFirebase(final byte[] bytes, final long messageId, final EncryptionUtils encryptionUtils,
                                      final FirebaseUploadCallback callback, int retryCount) {
        if (!active || encryptionUtils == null || retryCount > RETRY_COUNT) {
            callback.onUploadFinished();
            return;
        }

        if (folderRef == null) {
            throw new RuntimeException("need to initialize folder ref first with saveFolderRef()");
        }

        StorageReference fileRef = folderRef.child(messageId + "");
        fileRef.putBytes(encryptionUtils.encrypt(bytes).getBytes())
                .addOnSuccessListener(taskSnapshot -> {
                    Log.v(TAG, "finished uploading and exiting for " + messageId);
                    callback.onUploadFinished();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "failed to upload file", e);
                    uploadBytesToFirebase(bytes, messageId, encryptionUtils, callback, retryCount + 1);
                });
    }

    /**
     * Downloads and decrypts a file from firebase, using a callback for when the response is done
     *
     * @param file the location on your device to save to.
     * @param messageId the id of the message to grab so we can create a firebase storage ref.
     * @param encryptionUtils the utils to use to decrypt the message.
     */
    public void downloadFileFromFirebase(final File file, final long messageId,
                                         final EncryptionUtils encryptionUtils,
                                         final FirebaseDownloadCallback callback, int retryCount) {
        if (encryptionUtils == null || retryCount > RETRY_COUNT) {
            callback.onDownloadComplete();
            return;
        }

        if (folderRef == null) {
            throw new RuntimeException("need to initialize folder ref first with saveFolderRef()");
        }

        StorageReference fileRef = folderRef.child(messageId + "");
        fileRef.getBytes(MAX_SIZE)
                .addOnSuccessListener(bytes -> {
                    bytes = encryptionUtils.decryptData(new String(bytes));

                    try {
                        BufferedOutputStream bos =
                                new BufferedOutputStream(new FileOutputStream(file));
                        bos.write(bytes);
                        bos.flush();
                        bos.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    Log.v(TAG, "finished downloading " + messageId);
                    callback.onDownloadComplete();
                })
                .addOnFailureListener(e -> {
                    Log.v(TAG, "failed to download file", e);
                    if (!e.getMessage().contains("does not exist")) {
                        downloadFileFromFirebase(file, messageId, encryptionUtils, callback, retryCount + 1);
                    } else {
                        callback.onDownloadComplete();
                    }
                });
    }

    /**
     * Creates a ref to a folder where all media will be stored for this user.
     */
    public void saveFirebaseFolderRef(String accountId) {
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageRef = storage.getReferenceFromUrl(FIREBASE_STORAGE_URL);
        try {
            folderRef = storageRef.child(accountId);
        } catch (Exception e) {
            e.printStackTrace();
            try {
                folderRef = storageRef.child(accountId);
            } catch (Exception ex) {
                folderRef = null;
            }
        }
    }

    /**
     * Dismiss a notification across all devices.
     */
    public void dismissNotification(final String accountId, final String deviceId, final long conversationId) {
        if (!active || accountId == null) {
            return;
        }

        String message = "dismiss notification";
        Call<Void> call = api.account().dismissedNotification(accountId, deviceId, conversationId);

        call.enqueue(new LoggingRetryableCallback<>(call, RETRY_COUNT, message));
    }

    /**
     * Update the subscription status on the server.
     */
    public void updateSubscription(final String accountId, final Integer subscriptionType, final Long expirationDate) {
        if (!active || accountId == null) {
            return;
        }

        String message = "update subscription";
        Call<Void> call = api.account().updateSubscription(accountId, subscriptionType, expirationDate);

        call.enqueue(new LoggingRetryableCallback<>(call, RETRY_COUNT, message));
    }

    /**
     * Update the snooze time setting.
     */
    public void updateSnooze(final String accountId, final long snoozeTil) {
        if (active && accountId != null) {
            updateSetting(accountId, "snooze", "long", snoozeTil);
        }
    }

    /**
     * Update the vibrate setting.
     */
    public void updateVibrate(final String accountId, final String vibratePattern) {
        if (active && accountId != null) {
            updateSetting(accountId, "vibrate_pattern_identifier", "string", vibratePattern);
        }
    }

    /**
     * Update the repeat notifications setting.
     */
    public void updateRepeatNotifications(final String accountId, final String repeatString) {
        if (active && accountId != null) {
            updateSetting(accountId, "repeat_notifications_interval", "string", repeatString);
        }
    }

    /**
     * Update the wake screen setting
     */
    public void updateWakeScreen(final String accountId, final String wake) {
        if (active && accountId != null) {
            updateSetting(accountId, "wake_screen", "string", wake);
        }
    }

    /**
     * Update the wake screen setting
     */
    public void updateHeadsUp(final String accountId, final String headsUp) {
        if (active && accountId != null) {
            updateSetting(accountId, "heads_up", "string", headsUp);
        }
    }

    /**
     * Update the delivery reports setting.
     */
    public void updateDeliveryReports(final String accountId, final boolean deliveryReports) {
        if (active && accountId != null) {
            updateSetting(accountId, "delivery_reports", "boolean", deliveryReports);
        }
    }

    /**
     * Update the delivery reports setting.
     */
    public void updateGiffgaffDeliveryReports(final String accountId, final boolean deliveryReports) {
        if (active && accountId != null) {
            updateSetting(accountId, "giffgaff_delivery", "boolean", deliveryReports);
        }
    }

    /**
     * Update the strip Unicode setting.
     */
    public void updateStripUnicode(final String accountId, final boolean stripUnicode) {
        if (active && accountId != null) {
            updateSetting(accountId, "strip_unicode", "boolean", stripUnicode);
        }
    }

    /**
     * Update the strip Unicode setting.
     */
    public void updateShowHistoryInNotification(final String accountId, final boolean showHistory) {
        if (active && accountId != null) {
            updateSetting(accountId, "history_in_notifications", "boolean", showHistory);
        }
    }

    /**
     * Update the rounder bubbles setting.
     */
    public void updateRounderBubbles(final String accountId, final boolean rounderBubbles) {
        if (active && accountId != null) {
            updateSetting(accountId, "rounder_bubbles", "boolean", rounderBubbles);
        }
    }

    /**
     * Update the notification actions setting.
     */
    public void updateNotificationActions(final String accountId, final String stringified) {
        if (active && accountId != null) {
            updateSetting(accountId, "notification_actions", "set", stringified);
        }
    }

    /**
     * Update the swipe to delete setting
     */
    public void updateSwipeToDelete(final String accountId, final boolean swipeDelete) {
        if (active && accountId != null) {
            updateSetting(accountId, "swipe_delete", "boolean", swipeDelete);
        }
    }

    /**
     * Update the convert to MMS setting, for long messages
     */
    public void updateConvertToMMS(final String accountId, final String convert) {
        if (active && accountId != null) {
            updateSetting(accountId, "sms_to_mms_message_conversion_count", "string", convert);
        }
    }

    /**
     * Update the MMS size limit setting.
     */
    public void updateMmsSize(final String accountId, final String mmsSize) {
        if (active && accountId != null) {
            updateSetting(accountId, "mms_size_limit", "string", mmsSize);
        }
    }

    /**
     * Update the group MMS setting.
     */
    public void updateGroupMMS(final String accountId, final boolean groupMMS) {
        if (active && accountId != null) {
            updateSetting(accountId, "group_mms", "boolean", groupMMS);
        }
    }

    /**
     * Update the auto save media setting.
     */
    public void updateAutoSaveMedia(final String accountId, final boolean save) {
        if (active && accountId != null) {
            updateSetting(accountId, "auto_save_media", "boolean", save);
        }
    }

    /**
     * Update the override system apn setting.
     */
    public void updateOverrideSystemApn(final String accountId, final boolean override) {
        if (active && accountId != null) {
            updateSetting(accountId, "mms_override", "boolean", override);
        }
    }

    /**
     * Update the mmsc url for MMS.
     */
    public void updateMmscUrl(final String accountId, final String mmsc) {
        if (active && accountId != null) {
            updateSetting(accountId, "mmsc_url", "string", mmsc);
        }
    }

    /**
     * Update the MMS proxy setting.
     */
    public void updateMmsProxy(final String accountId, final String proxy) {
        if (active && accountId != null) {
            updateSetting(accountId, "mms_proxy", "string", proxy);
        }
    }

    /**
     * Update the MMS port setting.
     */
    public void updateMmsPort(final String accountId, final String port) {
        if (active && accountId != null) {
            updateSetting(accountId, "mms_port", "string", port);
        }
    }

    /**
     * Update the user agent setting.
     */
    public void updateUserAgent(final String accountId, final String userAgent) {
        if (active && accountId != null) {
            updateSetting(accountId, "user_agent", "string", userAgent);
        }
    }

    /**
     * Update the user agent profile url setting.
     */
    public void updateUserAgentProfileUrl(final String accountId, final String userAgentProfileUrl) {
        if (active && accountId != null) {
            updateSetting(accountId, "user_agent_profile_url", "string", userAgentProfileUrl);
        }
    }

    /**
     * Update the user agent tag name setting.
     */
    public void updateUserAgentProfileTagName(final String accountId, final String tagName) {
        if (active && accountId != null) {
            updateSetting(accountId, "user_agent_profile_tag_name", "string", tagName);
        }
    }

    /**
     * Update the secure private conversations setting.
     */
    public void updateSecurePrivateConversations(final String accountId, final boolean secure) {
        if (active && accountId != null) {
            updateSetting(accountId, "secure_private_conversations", "boolean", secure);
        }
    }

    /**
     * Update the quick compose setting.
     */
    public void updateQuickCompose(final String accountId, final boolean quickCompose) {
        if (active && accountId != null) {
            updateSetting(accountId, "quick_compose", "boolean", quickCompose);
        }
    }

    /**
     * Update the signature setting.
     */
    public void updateSignature(final String accountId, final String signature) {
        if (active && accountId != null) {
            updateSetting(accountId, "signature", "string", signature);
        }
    }


    /**
     * Update the delayed sending setting.
     */
    public void updateDelayedSending(final String accountId, final String delayedSending) {
        if (active && accountId != null) {
            updateSetting(accountId, "delayed_sending", "string", delayedSending);
        }
    }


    /**
     * Update the cleanup old messages setting.
     */
    public void updateCleanupOldMessages(final String accountId, final String cleanup) {
        if (active && accountId != null) {
            updateSetting(accountId, "cleanup_old_messages", "string", cleanup);
        }
    }

    /**
     * Update the sound effects setting.
     */
    public void updateSoundEffects(final String accountId, final boolean effects) {
        if (active && accountId != null) {
            updateSetting(accountId, "sound_effects", "boolean", effects);
        }
    }

    /**
     * Update the mobile only setting
     */
    public void updateMobileOnly(final String accountId, final boolean mobileOnly) {
        if (active && accountId != null) {
            updateSetting(accountId, "mobile_only", "boolean", mobileOnly);
        }
    }

    /**
     * Update the font size setting
     */
    public void updateFontSize(final String accountId, final String size) {
        if (active && accountId != null) {
            updateSetting(accountId, "font_size", "string", size);
        }
    }

    /**
     * Update the keyboard layout setting
     */
    public void updateKeyboardLayout(final String accountId, final String layout) {
        if (active && accountId != null) {
            updateSetting(accountId, "keyboard_layout", "string", layout);
        }
    }

    /**
     * Update the global theme color setting
     */
    public void updateGlobalThemeColor(final String accountId, final String color) {
        if (active && accountId != null) {
            updateSetting(accountId, "global_color_theme", "string", color);
        }
    }

    /**
     * Update the base theme (day/night, always dark, always black)
     */
    public void updateBaseTheme(final String accountId, final String themeString) {
        if (active && accountId != null) {
            updateSetting(accountId, "base_theme", "string", themeString);
        }
    }

    /**
     * Dismiss a notification across all devices.
     */
    private void updateSetting(final String accountId, final String pref, final String type, final Object value) {
        String message = "update " + pref + " setting";
        Call<Void> call = api.account().updateSetting(accountId, pref, type, value);

        call.enqueue(new LoggingRetryableCallback<>(call, RETRY_COUNT, message));
    }

    /**
     * Gets direct access to the apis for more advanced options.
     */
    public Api getApi() {
        return api;
    }

}
