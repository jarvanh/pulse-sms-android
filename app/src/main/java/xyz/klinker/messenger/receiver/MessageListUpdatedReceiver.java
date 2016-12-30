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
import android.content.IntentFilter;
import android.os.Handler;

import xyz.klinker.messenger.R;
import xyz.klinker.messenger.data.MimeType;
import xyz.klinker.messenger.data.model.Message;
import xyz.klinker.messenger.fragment.MessageListFragment;
import xyz.klinker.messenger.service.NotificationService;
import xyz.klinker.messenger.util.AudioWrapper;

/**
 * Receiver that handles updating the message list when a new message is received for the
 * conversation being displayed or the sent/delivered status is updated.
 */
public class MessageListUpdatedReceiver extends BroadcastReceiver {

    private static final String ACTION_UPDATED = "xyz.klinker.messenger.MESSAGE_UPDATED";
    private static final String ARG_CONVERSATION_ID = "conversation_id";
    private static final String ARG_NEW_MESSAGE_TEXT = "new_message_text";
    private static final String ARG_MESSAGE_TYPE = "message_type";

    private MessageListFragment fragment;

    public MessageListUpdatedReceiver(MessageListFragment fragment) {
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
            fragment.createDrafts();
            fragment.loadMessages();
            fragment.setDismissOnStartup();

            if (messageType == Message.TYPE_RECEIVED &&
                    NotificationService.CONVERSATION_ID_OPEN == conversationId) {
                new AudioWrapper(context, conversationId).play();
            }

            if (newMessageText != null) {
                if (messageType == Message.TYPE_SENDING || messageType == Message.TYPE_SENT) {
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
        sendBroadcast(context, conversationId, null, Message.TYPE_SENT);
    }

    /**
     * Sends a broadcast to anywhere that has registered this receiver to let it know to update.
     */
    public static void sendBroadcast(Context context, Message message) {
        if (message.mimeType.equals(MimeType.TEXT_PLAIN)) {
            sendBroadcast(context, message.conversationId, message.data, message.type);
        } else {
            sendBroadcast(context, message.conversationId);
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
