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

import android.app.IntentService;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.app.RemoteInput;
import android.util.Log;

import xyz.klinker.messenger.shared.R;
import xyz.klinker.messenger.api.implementation.Account;
import xyz.klinker.messenger.api.implementation.ApiUtils;
import xyz.klinker.messenger.shared.data.DataSource;
import xyz.klinker.messenger.shared.data.MimeType;
import xyz.klinker.messenger.shared.data.model.Conversation;
import xyz.klinker.messenger.shared.data.model.Message;
import xyz.klinker.messenger.shared.receiver.ConversationListUpdatedReceiver;
import xyz.klinker.messenger.shared.receiver.MessageListUpdatedReceiver;
import xyz.klinker.messenger.shared.service.jobs.MarkAsReadJob;
import xyz.klinker.messenger.shared.util.DualSimUtils;
import xyz.klinker.messenger.shared.util.SendUtils;
import xyz.klinker.messenger.shared.widget.MessengerAppWidgetProvider;

/**
 * Service for getting back voice replies from Android Wear and sending them out.
 */
public class ReplyService extends IntentService {

    private static final String TAG = "ReplyService";
    public static final String EXTRA_REPLY = "reply_text";
    public static final String EXTRA_CONVERSATION_ID = "conversation_id";

    public ReplyService() {
        super("Reply Service");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Bundle remoteInput = RemoteInput.getResultsFromIntent(intent);
        String reply = null;
        if (remoteInput != null) {
            reply = remoteInput.getCharSequence(EXTRA_REPLY).toString();
        }

        if (reply == null) {
            Log.e(TAG, "could not find attached reply");
            return;
        }

        long conversationId = intent.getLongExtra(EXTRA_CONVERSATION_ID, -1);

        if (conversationId == -1) {
            Log.e(TAG, "could not find attached conversation id");
            return;
        }

        DataSource source = DataSource.getInstance(this);
        source.open();

        Conversation conversation = source.getConversation(conversationId);
        if (conversation == null) {
            source.close();
            return;
        }

        Message m = new Message();
        m.conversationId = conversationId;
        m.type = Message.TYPE_SENDING;
        m.data = reply;
        m.timestamp = System.currentTimeMillis();
        m.mimeType = MimeType.TEXT_PLAIN;
        m.read = true;
        m.seen = true;
        m.from = null;
        m.color = null;
        m.simPhoneNumber = conversation.simSubscriptionId != null ? DualSimUtils.get(this)
                .getPhoneNumberFromSimSubscription(conversation.simSubscriptionId) : null;

        long messageId = source.insertMessage(this, m, conversationId, true);
        source.readConversation(this, conversationId);

        Log.v(TAG, "sending message \"" + reply + "\" to \"" + conversation.phoneNumbers + "\"");

        new SendUtils(conversation.simSubscriptionId)
                .send(this, reply, conversation.phoneNumbers);
        MarkAsReadJob.Companion.scheduleNextRun(this, messageId);

        // cancel the notification we just replied to or
        // if there are no more notifications, cancel the summary as well
        Cursor unseenMessages = source.getUnseenMessages();
        if (unseenMessages.getCount() <= 0) {
            NotificationManagerCompat.from(this).cancelAll();
        } else {
            NotificationManagerCompat.from(this).cancel((int) conversationId);
        }

        new ApiUtils().dismissNotification(Account.get(this).accountId,
                Account.get(this).deviceId,
                conversationId);

        unseenMessages.close();
        source.close();

        ConversationListUpdatedReceiver.sendBroadcast(this, conversationId, getString(R.string.you) + ": " + reply, true);
        MessageListUpdatedReceiver.sendBroadcast(this, conversationId);
        MessengerAppWidgetProvider.refreshWidget(this);
    }

}
