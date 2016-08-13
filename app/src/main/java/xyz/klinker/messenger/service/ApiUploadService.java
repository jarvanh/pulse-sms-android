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

import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.database.Cursor;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;

import xyz.klinker.messenger.R;
import xyz.klinker.messenger.api.entity.AddBlacklistRequest;
import xyz.klinker.messenger.api.entity.AddConversationRequest;
import xyz.klinker.messenger.api.entity.AddDraftRequest;
import xyz.klinker.messenger.api.entity.AddMessagesRequest;
import xyz.klinker.messenger.api.entity.AddScheduledMessageRequest;
import xyz.klinker.messenger.api.entity.BlacklistBody;
import xyz.klinker.messenger.api.entity.ConversationBody;
import xyz.klinker.messenger.api.entity.DraftBody;
import xyz.klinker.messenger.api.entity.MessageBody;
import xyz.klinker.messenger.api.entity.ScheduledMessageBody;
import xyz.klinker.messenger.api.implementation.ApiUtils;
import xyz.klinker.messenger.data.DataSource;
import xyz.klinker.messenger.data.Settings;
import xyz.klinker.messenger.data.model.Blacklist;
import xyz.klinker.messenger.data.model.Conversation;
import xyz.klinker.messenger.data.model.Draft;
import xyz.klinker.messenger.data.model.Message;
import xyz.klinker.messenger.data.model.ScheduledMessage;
import xyz.klinker.messenger.encryption.EncryptionUtils;
import xyz.klinker.messenger.encryption.KeyUtils;

public class ApiUploadService extends Service {

    private static final String TAG = "ApiUploadService";
    private static final int NOTIFICATION_ID = 7235;

    private Settings settings;
    private ApiUtils apiUtils;
    private EncryptionUtils encryptionUtils;
    private DataSource source;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Notification notification = new NotificationCompat.Builder(this)
                .setContentTitle(getString(R.string.encrypting_and_uploading))
                .setSmallIcon(R.drawable.ic_upload)
                .setProgress(0, 0, true)
                .setLocalOnly(true)
                .setColor(getResources().getColor(R.color.colorPrimary))
                .build();
        NotificationManagerCompat.from(this).notify(NOTIFICATION_ID, notification);

        new Thread(new Runnable() {
            @Override
            public void run() {
                settings = Settings.get(getApplicationContext());
                apiUtils = new ApiUtils();
                encryptionUtils = new EncryptionUtils(new KeyUtils().createKey(settings.passhash,
                        settings.accountId, settings.salt));
                source = DataSource.getInstance(getApplicationContext());
                source.open();

                long startTime = System.currentTimeMillis();
                uploadMessages();
                uploadConversations();
                uploadBlacklists();
                uploadScheduledMessages();
                uploadDrafts();
                Log.v(TAG, "time to upload: " + (System.currentTimeMillis() - startTime) + " ms");

                NotificationManagerCompat.from(getApplicationContext()).cancel(NOTIFICATION_ID);
                source.close();
                stopSelf();
            }
        }).start();
        return super.onStartCommand(intent, flags, startId);
    }

    private void uploadMessages() {
        long startTime = System.currentTimeMillis();
        Cursor cursor = source.getMessages();

        if (cursor != null && cursor.moveToFirst()) {
            MessageBody[] messages = new MessageBody[cursor.getCount()];

            do {
                Message m = new Message();
                m.fillFromCursor(cursor);
                m.encrypt(encryptionUtils);
                MessageBody message = new MessageBody(m.id, m.conversationId, m.type, m.data,
                        m.timestamp, m.mimeType, m.read, m.seen, m.from, m.color);
                messages[cursor.getPosition()] = message;
            } while (cursor.moveToNext());

            AddMessagesRequest request = new AddMessagesRequest(settings.accountId, messages);
            Object result = apiUtils.getApi().message().add(request);

            if (result == null) {
                Log.v(TAG, "failed to upload messages in " +
                        (System.currentTimeMillis() - startTime) + " ms");
            } else {
                Log.v(TAG, "messages upload successful in " +
                        (System.currentTimeMillis() - startTime) + " ms");
            }
        }
    }

    private void uploadConversations() {
        long startTime = System.currentTimeMillis();
        Cursor cursor = source.getConversations();

        if (cursor != null && cursor.moveToFirst()) {
            ConversationBody[] conversations = new ConversationBody[cursor.getCount()];

            do {
                Conversation c = new Conversation();
                c.fillFromCursor(cursor);
                c.encrypt(encryptionUtils);
                ConversationBody conversation = new ConversationBody(c.id, c.colors.color,
                        c.colors.colorDark, c.colors.colorLight, c.colors.colorAccent, c.pinned,
                        c.read, c.timestamp, c.title, c.phoneNumbers, c.snippet, c.ringtoneUri,
                        /*c.imageUri*/null, c.idMatcher, c.mute);

                conversations[cursor.getPosition()] = conversation;
            } while (cursor.moveToNext());

            AddConversationRequest request =
                    new AddConversationRequest(settings.accountId, conversations);
            Object result = apiUtils.getApi().conversation().add(request);

            if (result == null) {
                Log.v(TAG, "failed to upload conversations in " +
                        (System.currentTimeMillis() - startTime) + " ms");
            } else {
                Log.v(TAG, "conversations upload successful in " +
                        (System.currentTimeMillis() - startTime) + " ms");
            }
        }
    }

    private void uploadBlacklists() {
        long startTime = System.currentTimeMillis();
        Cursor cursor = source.getBlacklists();

        if (cursor != null && cursor.moveToFirst()) {
            BlacklistBody[] blacklists = new BlacklistBody[cursor.getCount()];

            do {
                Blacklist b = new Blacklist();
                b.fillFromCursor(cursor);
                b.encrypt(encryptionUtils);
                BlacklistBody blacklist = new BlacklistBody(b.id, b.phoneNumber);

                blacklists[cursor.getPosition()] = blacklist;
            } while (cursor.moveToNext());

            AddBlacklistRequest request =
                    new AddBlacklistRequest(settings.accountId, blacklists);
            Object result = apiUtils.getApi().blacklist().add(request);

            if (result == null) {
                Log.v(TAG, "failed to upload blacklists in " +
                        (System.currentTimeMillis() - startTime) + " ms");
            } else {
                Log.v(TAG, "blacklists upload successful in " +
                        (System.currentTimeMillis() - startTime) + " ms");
            }
        }
    }

    private void uploadScheduledMessages() {
        long startTime = System.currentTimeMillis();
        Cursor cursor = source.getScheduledMessages();

        if (cursor != null && cursor.moveToFirst()) {
            ScheduledMessageBody[] messages = new ScheduledMessageBody[cursor.getCount()];

            do {
                ScheduledMessage m = new ScheduledMessage();
                m.fillFromCursor(cursor);
                m.encrypt(encryptionUtils);
                ScheduledMessageBody message = new ScheduledMessageBody(m.id, m.to, m.data,
                        m.mimeType, m.timestamp, m.title);

                messages[cursor.getPosition()] = message;
            } while (cursor.moveToNext());

            AddScheduledMessageRequest request =
                    new AddScheduledMessageRequest(settings.accountId, messages);
            Object result = apiUtils.getApi().scheduled().add(request);

            if (result == null) {
                Log.v(TAG, "failed to upload scheduled messages in " +
                        (System.currentTimeMillis() - startTime) + " ms");
            } else {
                Log.v(TAG, "scheduled messages upload successful in " +
                        (System.currentTimeMillis() - startTime) + " ms");
            }
        }
    }

    private void uploadDrafts() {
        long startTime = System.currentTimeMillis();
        Cursor cursor = source.getDrafts();

        if (cursor != null && cursor.moveToFirst()) {
            DraftBody[] drafts = new DraftBody[cursor.getCount()];

            do {
                Draft d = new Draft();
                d.fillFromCursor(cursor);
                d.encrypt(encryptionUtils);
                DraftBody draft = new DraftBody(d.id, d.conversationId, d.data, d.mimeType);

                drafts[cursor.getPosition()] = draft;
            } while (cursor.moveToNext());

            AddDraftRequest request = new AddDraftRequest(settings.accountId, drafts);
            Object result = apiUtils.getApi().draft().add(request);

            if (result == null) {
                Log.v(TAG, "failed to upload drafts in " +
                        (System.currentTimeMillis() - startTime) + " ms");
            } else {
                Log.v(TAG, "drafts upload successful in " +
                        (System.currentTimeMillis() - startTime) + " ms");
            }
        }
    }

}
