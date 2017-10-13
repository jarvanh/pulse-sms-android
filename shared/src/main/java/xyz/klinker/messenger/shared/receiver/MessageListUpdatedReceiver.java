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

package xyz.klinker.messenger.shared.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;

import xyz.klinker.messenger.shared.R;
import xyz.klinker.messenger.shared.data.MimeType;
import xyz.klinker.messenger.shared.data.Settings;
import xyz.klinker.messenger.shared.data.model.Message;
import xyz.klinker.messenger.shared.service.NotificationService;
import xyz.klinker.messenger.shared.shared_interfaces.IMessageListFragment;
import xyz.klinker.messenger.shared.util.AudioWrapper;

/**
 * Receiver that handles updating the message list when a new message is received for the
 * conversation being displayed or the sent/delivered status is updated.
 */
public class MessageListUpdatedReceiver extends BroadcastReceiver {

    private static final String ACTION_UPDATED = "xyz.klinker.messenger.MESSAGE_UPDATED";
    private static final String ARG_CONVERSATION_ID = "conversation_id";
    private static final String ARG_NEW_MESSAGE_TEXT = "new_message_text";
    private static final String ARG_MESSAGE_TYPE = "message_type";

    private IMessageListFragment fragment;

    public MessageListUpdatedReceiver(IMessageListFragment fragment) {
        this.fragment = fragment;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        try {
            handleReceiver(context, intent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleReceiver(Context context, Intent intent) throws Exception {
        long conversationId = intent.getLongExtra(ARG_CONVERSATION_ID, -1);
        String newMessageText = intent.getStringExtra(ARG_NEW_MESSAGE_TEXT);
        int messageType = intent.getIntExtra(ARG_MESSAGE_TYPE, -1);

        if (conversationId == -1) {
            return;
        }

        if (conversationId == fragment.getConversationId()) {
            if (messageType == Message.Companion.getTYPE_RECEIVED()) {
                fragment.setShouldPullDrafts(false);
                fragment.loadMessages(true);
            } else {
                fragment.loadMessages(false);
            }

            fragment.setDismissOnStartup();

            if (Settings.INSTANCE.getSoundEffects() && messageType == Message.Companion.getTYPE_RECEIVED() &&
                    NotificationService.CONVERSATION_ID_OPEN == conversationId) {
                new Thread(() -> {
                    try {
                        Thread.sleep(500);
                    } catch (Exception e) { }

                    final AudioWrapper wrapper = new AudioWrapper(context, conversationId);
                    wrapper.play();
                }).start();
            }

            if (newMessageText != null) {
                if (messageType == Message.Companion.getTYPE_SENDING() || messageType == Message.Companion.getTYPE_SENT()) {
                    fragment.setConversationUpdateInfo(context.getString(R.string.you) + ": " + newMessageText);
                } else {
                    fragment.setConversationUpdateInfo(newMessageText);
                }
            }
        }
    }

    /**
     * Sends a broadcast to anywhere that has registered this receiver to let it know to update.
     */
    public static void sendBroadcast(Context context, long conversationId) {
        sendBroadcast(context, conversationId, null, Message.Companion.getTYPE_SENT());
    }

    /**
     * Sends a broadcast to anywhere that has registered this receiver to let it know to update.
     */
    public static void sendBroadcast(Context context, Message message) {
        if (message.getMimeType().equals(MimeType.INSTANCE.getTEXT_PLAIN())) {
            sendBroadcast(context, message.getConversationId(), message.getData(), message.getType());
        } else {
            sendBroadcast(context, message.getConversationId());
        }
    }

    /**
     * Sends a broadcast to anywhere that has registered this receiver to let it know to update.
     */
    public static void sendBroadcast(Context context, long conversationId, String newMessageText, int messageType) {
        Intent intent = new Intent(ACTION_UPDATED);
        intent.putExtra(ARG_CONVERSATION_ID, conversationId);
        intent.putExtra(ARG_NEW_MESSAGE_TEXT, newMessageText);
        intent.putExtra(ARG_MESSAGE_TYPE, messageType);
        context.sendBroadcast(intent);
    }

    /**
     * Gets an intent filter that will pick up these broadcasts.
     */
    public static IntentFilter getIntentFilter() {
        return new IntentFilter(ACTION_UPDATED);
    }

}
