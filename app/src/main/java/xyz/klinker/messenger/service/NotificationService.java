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

package xyz.klinker.messenger.service;

import android.app.IntentService;
import android.content.Intent;
import android.database.Cursor;
import android.support.annotation.VisibleForTesting;
import android.util.LongSparseArray;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import xyz.klinker.messenger.data.DataSource;
import xyz.klinker.messenger.data.model.Message;

/**
 * Service for displaying notifications to the user based on which conversations have not been
 * seen yet.
 */
public class NotificationService extends IntentService {

    public NotificationService() {
        super("NotificationService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        LongSparseArray<NotificationConversation> conversations = getUnseenConversations();
    }

    @VisibleForTesting
    LongSparseArray<NotificationConversation> getUnseenConversations() {
        DataSource source = getDataSource();
        source.open();

        Cursor unseenMessages = source.getUnseenMessages();
        LongSparseArray<NotificationConversation> conversations = new LongSparseArray<>();

        if (unseenMessages != null && unseenMessages.moveToFirst()) {
            do {
                long conversationId = unseenMessages
                        .getLong(unseenMessages.getColumnIndex(Message.COLUMN_CONVERSATION_ID));
                String data = unseenMessages
                        .getString(unseenMessages.getColumnIndex(Message.COLUMN_DATA));
                String mimeType = unseenMessages
                        .getString(unseenMessages.getColumnIndex(Message.COLUMN_MIME_TYPE));
                long timestamp = unseenMessages
                        .getLong(unseenMessages.getColumnIndex(Message.COLUMN_TIMESTAMP));

                NotificationConversation conversation = conversations.get(conversationId);

                if (conversation == null) {
                    conversation = new NotificationConversation();
                    conversation.title = source.getConversation(conversationId).title;
                    conversations.put(conversationId, conversation);
                }

                conversation.messages.add(new NotificationMessage(data, mimeType, timestamp));
            } while (unseenMessages.moveToNext());

            unseenMessages.close();
        }

        source.close();
        return conversations;
    }

    @VisibleForTesting
    DataSource getDataSource() {
        return DataSource.getInstance(this);
    }

    @VisibleForTesting
    class NotificationConversation {
        public String title;
        public List<NotificationMessage> messages;

        private NotificationConversation() {
            messages = new ArrayList<>();
        }
    }

    @VisibleForTesting
    class NotificationMessage {
        public String data;
        public String mimeType;
        public long timestamp;

        private NotificationMessage(String data, String mimeType, long timestamp) {
            this.data = data;
            this.mimeType = mimeType;
            this.timestamp = timestamp;
        }
    }

}
