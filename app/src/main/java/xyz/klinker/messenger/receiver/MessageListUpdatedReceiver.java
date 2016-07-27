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

import xyz.klinker.messenger.fragment.MessageListFragment;

/**
 * Receiver that handles updating the message list when a new message is received for the
 * conversation being displayed or the sent/delivered status is updated.
 */
public class MessageListUpdatedReceiver extends BroadcastReceiver {

    private static final String ACTION_UPDATED = "xyz.klinker.messenger.MESSAGE_UPDATED";
    private static final String ARG_CONVERSATION_ID = "conversation_id";

    private MessageListFragment fragment;

    public MessageListUpdatedReceiver(MessageListFragment fragment) {
        this.fragment = fragment;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        long conversationId = intent.getLongExtra(ARG_CONVERSATION_ID, -1);

        if (conversationId == -1) {
            return;
        }

        if (conversationId == fragment.getConversationId()) {
            fragment.loadMessages();
        }
    }

    /**
     * Sends a broadcast to anywhere that has registered this receiver to let it know to update.
     */
    public static void sendBroadcast(Context context, long conversationId) {
        Intent intent = new Intent(ACTION_UPDATED);
        intent.putExtra(ARG_CONVERSATION_ID, conversationId);
        context.sendBroadcast(intent);
    }

    /**
     * Gets an intent filter that will pick up these broadcasts.
     */
    public static IntentFilter getIntentFilter() {
        return new IntentFilter(ACTION_UPDATED);
    }

}
