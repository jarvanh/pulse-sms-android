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
    public void addConversation(String accountId, long deviceId, int color, int colorDark,
                                int colorLight, int colorAccent, boolean pinned, boolean read,
                                long timestamp, String title, String phoneNumbers, String snippet,
                                String ringtone, String idMatcher, boolean mute) {
        if (!active || accountId == null) {
            return;
        }

        ConversationBody body = new ConversationBody(deviceId, color, colorDark, colorLight,
                colorAccent, pinned, read, timestamp, title, phoneNumbers, snippet, ringtone,
                null, idMatcher, mute);
        final AddConversationRequest request = new AddConversationRequest(accountId, body);

        new Thread(new Runnable() {
            @Override
            public void run() {
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
    public void updateConversation(final String accountId, final long deviceId, Integer color,
                                   Integer colorDark, Integer colorLight, Integer colorAccent,
                                   Boolean pinned, Boolean read, Long timestamp, String title,
                                   String snippet, String ringtone, Boolean mute) {
        if (!active || accountId == null) {
            return;
        }

        final UpdateConversationRequest request = new UpdateConversationRequest(color,
                colorDark, colorLight, colorAccent, pinned, read, timestamp, title, snippet,
                ringtone, mute);

        new Thread(new Runnable() {
            @Override
            public void run() {
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
    public void addMessage(String accountId, long deviceId, long deviceConversationId,
                           int messageType, String data, long timestamp, String mimeType,
                           boolean read, boolean seen, String messageFrom, int color) {
        if (!active || accountId == null) {
            return;
        }

        MessageBody body = new MessageBody(deviceId, deviceConversationId, messageType, data,
                timestamp, mimeType, read, seen, messageFrom, color);
        final AddMessagesRequest request = new AddMessagesRequest(accountId, body);

        new Thread(new Runnable() {
            @Override
            public void run() {
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
    public void addDraft(String accountId, long deviceId, long deviceConversationId, String data,
                         String mimeType) {
        if (!active || accountId == null) {
            return;
        }

        DraftBody body = new DraftBody(deviceId, deviceConversationId, data, mimeType);
        final AddDraftRequest request = new AddDraftRequest(accountId, body);

        new Thread(new Runnable() {
            @Override
            public void run() {
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
    public void addBlacklist(String accountId, long deviceId, String phoneNumber) {
        if (!active || accountId == null) {
            return;
        }

        BlacklistBody body = new BlacklistBody(deviceId, phoneNumber);
        final AddBlacklistRequest request = new AddBlacklistRequest(accountId, body);

        new Thread(new Runnable() {
            @Override
            public void run() {
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
    public void addScheduledMessage(String accountId, long deviceId, String title, String to,
                                    String data, String mimeType, long timestamp) {
        if (!active || accountId == null) {
            return;
        }

        ScheduledMessageBody body = new ScheduledMessageBody(deviceId, to, data, mimeType,
                timestamp, title);
        final AddScheduledMessageRequest request = new AddScheduledMessageRequest(accountId, body);

        new Thread(new Runnable() {
            @Override
            public void run() {
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
