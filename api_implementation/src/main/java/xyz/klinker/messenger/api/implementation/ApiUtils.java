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

import android.util.Log;

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
    public static String environment;

    private Api api;
    private boolean active = true;

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
                    Log.e(TAG, "error deleting conversation");
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
                    Log.e(TAG, "error deleting conversation");
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
                    Log.e(TAG, "error deleting conversation");
                }
            }
        }).start();
    }

    /**
     * Adds a new message to the server.
     */
    public void addMessage(final String accountId, final long deviceId,
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

                if (mimeType.equals("text/plain")) {
                    messageData = data;
                } else {
                    messageData = "firebase -1";
                    // TODO upload binary file to firebase storage
                }

                MessageBody body = new MessageBody(deviceId,
                        deviceConversationId,
                        messageType,
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
                Object response = api.message().update(deviceId, accountId, request);
                if (response == null) {
                    Log.e(TAG, "error updating message");
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
