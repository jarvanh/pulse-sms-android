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
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.concurrent.atomic.AtomicBoolean;

import xyz.klinker.messenger.api.Api;
import xyz.klinker.messenger.api.entity.AddBlacklistRequest;
import xyz.klinker.messenger.api.entity.AddConversationRequest;
import xyz.klinker.messenger.api.entity.AddDeviceRequest;
import xyz.klinker.messenger.api.entity.AddDeviceResponse;
import xyz.klinker.messenger.api.entity.AddDraftRequest;
import xyz.klinker.messenger.api.entity.AddMessagesRequest;
import xyz.klinker.messenger.api.entity.AddScheduledMessageRequest;
import xyz.klinker.messenger.api.entity.BlacklistBody;
import xyz.klinker.messenger.api.entity.ConversationBody;
import xyz.klinker.messenger.api.entity.DeviceBody;
import xyz.klinker.messenger.api.entity.DraftBody;
import xyz.klinker.messenger.api.entity.LoginRequest;
import xyz.klinker.messenger.api.entity.LoginResponse;
import xyz.klinker.messenger.api.entity.MessageBody;
import xyz.klinker.messenger.api.entity.ScheduledMessageBody;
import xyz.klinker.messenger.api.entity.SignupRequest;
import xyz.klinker.messenger.api.entity.SignupResponse;
import xyz.klinker.messenger.api.entity.UpdateConversationRequest;
import xyz.klinker.messenger.api.entity.UpdateMessageRequest;
import xyz.klinker.messenger.encryption.EncryptionUtils;

/**
 * Utility for easing access to APIs.
 */
public class ApiUtils {

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

    /**
     * Logs into the server.
     */
    public LoginResponse login(String email, String password) {
        LoginRequest request = new LoginRequest(email, password);
        return api.account().login(request);
    }

    /**
     * Signs up for the service.
     */
    public SignupResponse signup(String email, String password, String name, String phoneNumber) {
        SignupRequest request = new SignupRequest(email, name, password, phoneNumber);
        return api.account().signup(request);
    }

    /**
     * Removes the account from the server.
     */
    public void deleteAccount(final String accountId) {
        Object response = api.account().remove(accountId);
        if (response == null) {
            Log.e(TAG, "error removing account");
        } else {
            Log.v(TAG, "successfully removed account");
        }
    }

    /**
     * Registers your device as a new device on the server.
     */
    public Integer registerDevice(String accountId, String info, String name,
                                  boolean primary, String fcmToken) {
        DeviceBody deviceBody = new DeviceBody(info, name, primary, fcmToken);
        AddDeviceRequest request = new AddDeviceRequest(accountId, deviceBody);
        AddDeviceResponse response = api.device().add(request);

        if (response != null) {
            return response.id;
        } else {
            return null;
        }
    }

    /**
     * Removes a device from the server.
     */
    public void removeDevice(String accountId, int deviceId) {
        api.device().remove(deviceId, accountId);
    }

    /**
     * Gets a list of all devices on the server.
     */
    public DeviceBody[] getDevices(String accountId) {
        return api.device().list(accountId);
    }

    /**
     * Updates device info on the server.
     */
    public void updateDevice(String accountId, int deviceId, String name, String fcmToken) {
        api.device().update(deviceId, accountId, name, fcmToken);
    }

    /**
     * Adds a new conversation.
     */
    public void addConversation(final String accountId, final long deviceId, final int color,
                                final int colorDark, final int colorLight, final int colorAccent,
                                final boolean pinned, final boolean read, final long timestamp,
                                final String title, final String phoneNumbers, final String snippet,
                                final String ringtone, final String idMatcher, final boolean mute,
                                final EncryptionUtils encryptionUtils) {
        if (!active || accountId == null || encryptionUtils == null) {
            return;
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                ConversationBody body = new ConversationBody(
                        deviceId, color, colorDark, colorLight, colorAccent, pinned, read,
                        timestamp, encryptionUtils.encrypt(title),
                        encryptionUtils.encrypt(phoneNumbers), encryptionUtils.encrypt(snippet),
                        encryptionUtils.encrypt(ringtone), null,
                        encryptionUtils.encrypt(idMatcher), mute);
                AddConversationRequest request = new AddConversationRequest(accountId, body);

                Object response = api.conversation().add(request);
                if (response == null) {
                    Log.e(TAG, "error adding conversation");
                } else {
                    Log.v(TAG, "successfully added conversation");
                }
            }
        }).start();
    }

    /**
     * Deletes a conversation and all of its messages.
     */
    public void deleteConversation(final String accountId, final long deviceId) {
        if (!active || accountId == null) {
            return;
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                Object response = api.conversation().remove(deviceId, accountId);
                if (response == null) {
                    Log.e(TAG, "error deleting conversation");
                } else {
                    Log.v(TAG, "successfully deleted conversation");
                }
            }
        }).start();
    }

    /**
     * Updates a conversation with new settings or info.
     */
    public void updateConversation(final String accountId, final long deviceId, final Integer color,
                                   final Integer colorDark, final Integer colorLight,
                                   final Integer colorAccent, final Boolean pinned,
                                   final Boolean read, final Long timestamp, final String title,
                                   final String snippet, final String ringtone, final Boolean mute,
                                   final EncryptionUtils encryptionUtils) {
        if (!active || accountId == null || encryptionUtils == null) {
            return;
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                UpdateConversationRequest request = new UpdateConversationRequest(color,
                        colorDark, colorLight, colorAccent, pinned, read, timestamp,
                        encryptionUtils.encrypt(title), encryptionUtils.encrypt(snippet),
                        encryptionUtils.encrypt(ringtone), mute);

                Object response = api.conversation().update(deviceId, accountId, request);
                if (response == null) {
                    Log.e(TAG, "error updating conversation");
                } else {
                    Log.v(TAG, "successfully updated conversation");
                }
            }
        }).start();
    }

    /**
     * Marks all messages in conversation as read.
     */
    public void readConversation(final String accountId, final long deviceId) {
        if (!active || accountId == null) {
            return;
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                Object response = api.conversation().read(deviceId, accountId);
                if (response == null) {
                    Log.e(TAG, "error reading conversation");
                } else {
                    Log.v(TAG, "successfully read conversation");
                }
            }
        }).start();
    }

    /**
     * Marks all messages in conversation as seen.
     */
    public void seenConversation(final String accountId, final long deviceId) {
        if (!active || accountId == null) {
            return;
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                Object response = api.conversation().seen(deviceId, accountId);
                if (response == null) {
                    Log.e(TAG, "error seeing conversation");
                } else {
                    Log.v(TAG, "successfully seen conversation");
                }
            }
        }).start();
    }

    /**
     * Marks all messages as seen.
     */
    public void seenConversations(final String accountId) {
        if (!active || accountId == null) {
            return;
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                Object response = api.conversation().seen(accountId);
                if (response == null) {
                    Log.e(TAG, "error seeing all conversations");
                } else {
                    Log.v(TAG, "successfully seen all conversations");
                }
            }
        }).start();
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

        new Thread(new Runnable() {
            @Override
            public void run() {
                String messageData;
                int type;

                if (mimeType.equals("text/plain")) {
                    messageData = data;
                    type = messageType;
                } else {
                    messageData = "firebase -1";
                    // if type is received, then received else sent. don't save sending status here
                    type = messageType/* == 0 ? 0 : 1*/;

                    saveFirebaseFolderRef(accountId);
                    byte[] bytes = BinaryUtils.getMediaBytes(context, data, mimeType);
                    uploadBytesToFirebase(bytes, deviceId, encryptionUtils);
                }

                MessageBody body = new MessageBody(deviceId,
                        deviceConversationId,
                        type,
                        encryptionUtils.encrypt(messageData),
                        timestamp,
                        encryptionUtils.encrypt(mimeType),
                        read,
                        seen,
                        encryptionUtils.encrypt(messageFrom),
                        color);
                AddMessagesRequest request = new AddMessagesRequest(accountId, body);

                Object response = api.message().add(request);
                if (response == null) {
                    Log.e(TAG, "error adding message");
                } else {
                    Log.v(TAG, "successfully added message");
                }
            }
        }).start();
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

        new Thread(new Runnable() {
            @Override
            public void run() {
                int attempt = 1;
                Object response = api.message().update(deviceId, accountId, request);

                // retry up to three times. A failure can occur when the image has not yet finished
                // uploading to firebase but has been sent and marked as so on the device.
                while (response == null && attempt <= 6) {
                    attempt++;
                    try { Thread.sleep(5000); } catch (Exception e) { }
                    response = api.message().update(deviceId, accountId, request);
                }

                if (response == null) {
                    Log.e(TAG, "error updating message");
                } else {
                    Log.v(TAG, "successfully updated");
                }
            }
        }).start();
    }

    /**
     * Deletes the given message.
     */
    public void deleteMessage(final String accountId, final long deviceId) {
        if (!active || accountId == null) {
            return;
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                Object response = api.message().remove(deviceId, accountId);
                if (response == null) {
                    Log.e(TAG, "error deleting message");
                } else {
                    Log.v(TAG, "successfully deleted message");
                }
            }
        }).start();
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

        new Thread(new Runnable() {
            @Override
            public void run() {
                DraftBody body = new DraftBody(deviceId, deviceConversationId,
                        encryptionUtils.encrypt(data), encryptionUtils.encrypt(mimeType));
                AddDraftRequest request = new AddDraftRequest(accountId, body);

                Object response = api.draft().add(request);
                if (response == null) {
                    Log.e(TAG, "error adding draft");
                } else {
                    Log.v(TAG, "successfully added draft");
                }
            }
        }).start();
    }

    /**
     * Deletes the given drafts.
     */
    public void deleteDrafts(final String accountId, final long deviceConversationId) {
        if (!active || accountId == null) {
            return;
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                Object response = api.draft().remove(deviceConversationId, accountId);
                if (response == null) {
                    Log.e(TAG, "error deleting draft");
                } else {
                    Log.v(TAG, "successfully deleted drafts");
                }
            }
        }).start();
    }

    /**
     * Adds a blacklist.
     */
    public void addBlacklist(final String accountId, final long deviceId, final String phoneNumber,
                             final EncryptionUtils encryptionUtils) {
        if (!active || accountId == null || encryptionUtils == null) {
            return;
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                BlacklistBody body = new BlacklistBody(deviceId,
                        encryptionUtils.encrypt(phoneNumber));
                AddBlacklistRequest request = new AddBlacklistRequest(accountId, body);

                Object response = api.blacklist().add(request);
                if (response == null) {
                    Log.e(TAG, "error adding blacklist");
                } else {
                    Log.v(TAG, "successfully added blacklist");
                }
            }
        }).start();
    }

    /**
     * Deletes the given blacklist.
     */
    public void deleteBlacklist(final String accountId, final long deviceId) {
        if (!active || accountId == null) {
            return;
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                Object response = api.blacklist().remove(deviceId, accountId);
                if (response == null) {
                    Log.e(TAG, "error deleting blacklist");
                } else {
                    Log.v(TAG, "successfully deleted blacklist");
                }
            }
        }).start();
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

        new Thread(new Runnable() {
            @Override
            public void run() {
                ScheduledMessageBody body = new ScheduledMessageBody(
                        deviceId,
                        encryptionUtils.encrypt(to),
                        encryptionUtils.encrypt(data),
                        encryptionUtils.encrypt(mimeType),
                        timestamp,
                        encryptionUtils.encrypt(title));

                AddScheduledMessageRequest request =
                        new AddScheduledMessageRequest(accountId, body);
                Object response = api.scheduled().add(request);

                if (response == null) {
                    Log.e(TAG, "error adding scheduled message");
                } else {
                    Log.v(TAG, "successfully scheduled message");
                }
            }
        }).start();
    }

    /**
     * Deletes the given scheduled message.
     */
    public void deleteScheduledMessage(final String accountId, final long deviceId) {
        if (!active || accountId == null) {
            return;
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                Object response = api.scheduled().remove(deviceId, accountId);
                if (response == null) {
                    Log.e(TAG, "error deleting scheduled message");
                } else {
                    Log.v(TAG, "successfully deleted scheduled message");
                }
            }
        }).start();
    }

    /**
     * Uploads a byte array of encrypted data to firebase.
     *
     * @param bytes the byte array to upload.
     * @param messageId the message id that the data belongs to.
     * @param encryptionUtils the utils to encrypt the byte array with.
     */
    public void uploadBytesToFirebase(byte[] bytes, final long messageId,
                                      EncryptionUtils encryptionUtils) {
        if (!active) {
            return;
        }

        if (folderRef == null) {
            throw new RuntimeException("need to initialize folder ref first with saveFolderRef()");
        }

        final AtomicBoolean firebaseFinished = new AtomicBoolean(false);
        StorageReference fileRef = folderRef.child(messageId + "");

        fileRef.putBytes(encryptionUtils.encrypt(bytes).getBytes()).
                addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        Log.v(TAG, "finished uploading " + messageId);
                        firebaseFinished.set(true);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "failed to upload file", e);
                        firebaseFinished.set(true);
                    }
                });

        // wait for the upload to finish. Firebase only support async requests, which
        // I do not want here.
        while (!firebaseFinished.get()) {
            Log.v(TAG, "waiting for upload to finish for " + messageId);
            try { Thread.sleep(1000); } catch (Exception e) { }
        }

        Log.v(TAG, "finished uploading and exiting for " + messageId);
    }

    /**
     * Downloads and decrypts a file from firebase.
     *
     * @param file the location on your device to save to.
     * @param messageId the id of the message to grab so we can create a firebase storage ref.
     * @param encryptionUtils the utils to use to decrypt the message.
     */
    public void downloadFileFromFirebase(final File file, final long messageId,
                                         final EncryptionUtils encryptionUtils) {
        if (folderRef == null) {
            throw new RuntimeException("need to initialize folder ref first with saveFolderRef()");
        }

        final AtomicBoolean firebaseFinished = new AtomicBoolean(false);
        StorageReference fileRef = folderRef.child(messageId + "");

        fileRef.getBytes(MAX_SIZE)
                .addOnSuccessListener(new OnSuccessListener<byte[]>() {
                    @Override
                    public void onSuccess(byte[] bytes) {
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

                        firebaseFinished.set(true);
                        Log.v(TAG, "finished downloading " + messageId);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        firebaseFinished.set(true);
                        Log.v(TAG, "failed to download file", e);
                    }
                });

        while (!firebaseFinished.get()) {
            Log.v(TAG, "waiting for download " + messageId);
            try { Thread.sleep(500); } catch (Exception e) { }
        }
    }

    /**
     * Creates a ref to a folder where all media will be stored for this user.
     */
    public void saveFirebaseFolderRef(String accountId) {
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageRef = storage
                .getReferenceFromUrl(FIREBASE_STORAGE_URL);
        folderRef = storageRef.child(accountId);
    }

    /**
     * Update the snooze time setting.
     */
    public void updateSnooze(final String accountId, final long snoozeTil) {
        if (!active || accountId == null) {
            return;
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                Object response = api.account().updateSnooze(accountId, snoozeTil);
                if (response == null) {
                    Log.e(TAG, "error updating snooze til");
                } else {
                    Log.v(TAG, "successfully updated snooze til");
                }
            }
        }).start();
    }



    /**
     * Update the dark theme setting.
     */
    public void updateDarkTheme(final String accountId, final boolean darkTheme) {
        if (!active || accountId == null) {
            return;
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                Object response = api.account().updateDarkTheme(accountId, darkTheme);
                if (response == null) {
                    Log.e(TAG, "error updating dark theme");
                } else {
                    Log.v(TAG, "successfully updated dark theme");
                }
            }
        }).start();
    }



    /**
     * Update the vibrate setting.
     */
    public void updateVibrate(final String accountId, final boolean vibrate) {
        if (!active || accountId == null) {
            return;
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                Object response = api.account().updateVibrate(accountId, vibrate);
                if (response == null) {
                    Log.e(TAG, "error updating vibrate");
                } else {
                    Log.v(TAG, "successfully updated vibrate");
                }
            }
        }).start();
    }



    /**
     * Dismiss a notification across all devices.
     */
    public void dismissNotification(final String accountId, final long id) {
        if (!active || accountId == null) {
            return;
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                Object response = api.account().dismissedNotification(accountId, (int) id);
                if (response == null) {
                    Log.e(TAG, "error dismissing notification");
                } else {
                    Log.v(TAG, "successfully dismissed notification");
                }
            }
        }).start();
    }

    /**
     * Gets direct access to the apis for more advanced options.
     */
    public Api getApi() {
        return api;
    }

}
