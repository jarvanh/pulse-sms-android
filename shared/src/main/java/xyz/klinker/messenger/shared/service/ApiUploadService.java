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

import android.app.Notification;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

import retrofit2.Response;
import xyz.klinker.messenger.api.implementation.LoginActivity;
import xyz.klinker.messenger.api.implementation.firebase.FirebaseUploadCallback;
import xyz.klinker.messenger.shared.R;
import xyz.klinker.messenger.api.entity.AddBlacklistRequest;
import xyz.klinker.messenger.api.entity.AddContactRequest;
import xyz.klinker.messenger.api.entity.AddConversationRequest;
import xyz.klinker.messenger.api.entity.AddDraftRequest;
import xyz.klinker.messenger.api.entity.AddMessagesRequest;
import xyz.klinker.messenger.api.entity.AddScheduledMessageRequest;
import xyz.klinker.messenger.api.entity.BlacklistBody;
import xyz.klinker.messenger.api.entity.ContactBody;
import xyz.klinker.messenger.api.entity.ConversationBody;
import xyz.klinker.messenger.api.entity.DraftBody;
import xyz.klinker.messenger.api.entity.MessageBody;
import xyz.klinker.messenger.api.entity.ScheduledMessageBody;
import xyz.klinker.messenger.api.implementation.ApiUtils;
import xyz.klinker.messenger.api.implementation.Account;
import xyz.klinker.messenger.shared.data.ColorSet;
import xyz.klinker.messenger.shared.data.DataSource;
import xyz.klinker.messenger.shared.data.MimeType;
import xyz.klinker.messenger.shared.data.model.Blacklist;
import xyz.klinker.messenger.shared.data.model.Contact;
import xyz.klinker.messenger.shared.data.model.Conversation;
import xyz.klinker.messenger.shared.data.model.Draft;
import xyz.klinker.messenger.shared.data.model.Message;
import xyz.klinker.messenger.shared.data.model.ScheduledMessage;
import xyz.klinker.messenger.encryption.EncryptionUtils;
import xyz.klinker.messenger.api.implementation.BinaryUtils;
import xyz.klinker.messenger.shared.util.CursorUtil;
import xyz.klinker.messenger.shared.util.NotificationUtils;
import xyz.klinker.messenger.shared.util.PaginationUtils;
import xyz.klinker.messenger.shared.util.listener.DirectExecutor;

public class ApiUploadService extends Service {

    public static void start(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(new Intent(context, ApiUploadService.class));
        } else {
            context.startService(new Intent(context, ApiUploadService.class));
        }
    }

    private static final String TAG = "ApiUploadService";
    private static final int MESSAGE_UPLOAD_ID = 7235;
    public static final int NUM_MEDIA_TO_UPLOAD = 20;

    public static final int MESSAGE_UPLOAD_PAGE_SIZE = 300;

    private Account account;
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
        uploadData();
        return super.onStartCommand(intent, flags, startId);
    }

    private void uploadData() {
        Notification notification = new NotificationCompat.Builder(this, NotificationUtils.STATUS_NOTIFICATIONS_CHANNEL_ID)
                .setContentTitle(getString(R.string.encrypting_and_uploading))
                .setSmallIcon(R.drawable.ic_upload)
                .setProgress(0, 0, true)
                .setLocalOnly(true)
                .setColor(ColorSet.DEFAULT(this).color)
                .setOngoing(true)
                .build();
        startForeground(MESSAGE_UPLOAD_ID, notification);

        new Thread(() -> {
            account = Account.get(getApplicationContext());
            apiUtils = new ApiUtils();
            encryptionUtils = account.getEncryptor();

            if (encryptionUtils == null) {
                startActivity(new Intent(this, LoginActivity.class));
                return;
            }

            source = DataSource.INSTANCE;

            long startTime = System.currentTimeMillis();
            uploadMessages();
            uploadConversations();
            uploadContacts(this, source, encryptionUtils, account, apiUtils);
            uploadBlacklists();
            uploadScheduledMessages();
            uploadDrafts();
            Log.v(TAG, "time to upload: " + (System.currentTimeMillis() - startTime) + " ms");

            uploadMedia();
        }).start();
    }

    private void uploadMessages() {
        long startTime = System.currentTimeMillis();
        Cursor cursor = source.getMessages(this);

        if (cursor.moveToFirst()) {
            List<MessageBody> messages = new ArrayList<>();
            int firebaseNumber = 0;

            do {
                Message m = new Message();
                m.fillFromCursor(cursor);

                // instead of sending the URI, we'll upload these images to firebase and retrieve
                // them on another device based on account id and message id.
                if (!m.mimeType.equals(MimeType.TEXT_PLAIN)) {
                    m.data = "firebase " + firebaseNumber;
                    firebaseNumber++;
                }

                m.encrypt(encryptionUtils);
                MessageBody message = new MessageBody(m.id, m.conversationId, m.type, m.data,
                        m.timestamp, m.mimeType, m.read, m.seen, m.from, m.color);
                messages.add(message);
            } while (cursor.moveToNext());

            int successPages = 0;
            int expectedPages = 0;
            List<List<MessageBody>> pages = PaginationUtils.getPages(messages, MESSAGE_UPLOAD_PAGE_SIZE);

            for (List<MessageBody> page : pages) {
                AddMessagesRequest request = new AddMessagesRequest(account.accountId, page.toArray(new MessageBody[0]));
                try {
                    Response response = apiUtils.getApi().message().add(request).execute();
                    expectedPages++;
                    if (ApiUtils.isCallSuccessful(response)) {
                        successPages++;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

                Log.v(TAG, "uploaded " + page.size() + " messages for page " + expectedPages);
            }

            if (successPages != expectedPages) {
                Log.v(TAG, "failed to upload messages in " +
                        (System.currentTimeMillis() - startTime) + " ms");
            } else {
                Log.v(TAG, "messages upload successful in " +
                        (System.currentTimeMillis() - startTime) + " ms");
            }
        }

        CursorUtil.closeSilent(cursor);
    }

    private void uploadConversations() {
        long startTime = System.currentTimeMillis();
        Cursor cursor = source.getAllConversations(this);

        if (cursor.moveToFirst()) {
            ConversationBody[] conversations = new ConversationBody[cursor.getCount()];

            do {
                Conversation c = new Conversation();
                c.fillFromCursor(cursor);
                c.encrypt(encryptionUtils);
                ConversationBody conversation = new ConversationBody(c.id, c.colors.color,
                        c.colors.colorDark, c.colors.colorLight, c.colors.colorAccent, c.ledColor, c.pinned,
                        c.read, c.timestamp, c.title, c.phoneNumbers, c.snippet, c.ringtoneUri,
                        /*c.imageUri*/null, c.idMatcher, c.mute, c.archive, c.privateNotifications);

                conversations[cursor.getPosition()] = conversation;
            } while (cursor.moveToNext());

            AddConversationRequest request =
                    new AddConversationRequest(account.accountId, conversations);
            Response result;

            String errorText = null;
            try {
                result = apiUtils.getApi().conversation().add(request).execute();
            } catch (IOException e) {
                try {
                    result = apiUtils.getApi().conversation().add(request).execute();
                } catch (Exception x) {
                    errorText = e.getMessage();
                    e.printStackTrace();
                    result = null;
                }
            }

            if (result == null || !ApiUtils.isCallSuccessful(result)) {
                if (errorText == null && result != null) {
                    if (result.body() != null) {
                        errorText = result.body().toString();
                    } else {
                        errorText = result.message();
                    }
                }

//                if (errorText != null) {
//                    final NotificationCompat.Builder builder = new NotificationCompat.Builder(this, NotificationUtils.STATUS_NOTIFICATIONS_CHANNEL_ID)
//                            .setContentTitle("Error uploading " + conversations.length + " conversations")
//                            .setContentText(errorText)
//                            .setStyle(new NotificationCompat.BigTextStyle().bigText(errorText))
//                            .setSmallIcon(R.drawable.ic_upload);
//                    final NotificationManagerCompat manager = NotificationManagerCompat.from(this);
//                    manager.notify(1234354, builder.build());
//                }

                Log.v(TAG, "failed to upload conversations in " +
                        (System.currentTimeMillis() - startTime) + " ms");
            } else {
                Log.v(TAG, result.toString());
                Log.v(TAG, "conversations upload successful in " +
                        (System.currentTimeMillis() - startTime) + " ms");
            }
        }

        CursorUtil.closeSilent(cursor);
    }

    protected static void uploadContacts(Context context, DataSource source, EncryptionUtils encryptionUtils, Account account, ApiUtils apiUtils) {
        long startTime = System.currentTimeMillis();
        Cursor cursor = source.getContacts(context);

        if (cursor.moveToFirst()) {
            List<ContactBody> contacts = new ArrayList<>();

            do {
                Contact c = new Contact();
                c.fillFromCursor(cursor);
                c.encrypt(encryptionUtils);
                ContactBody contact = new ContactBody(c.phoneNumber, c.name, c.colors.color,
                        c.colors.colorDark, c.colors.colorLight, c.colors.colorAccent);
                contacts.add(contact);
            } while (cursor.moveToNext());

            int successPages = 0;
            int expectedPages = 0;
            List<List<ContactBody>> pages = PaginationUtils.getPages(contacts, MESSAGE_UPLOAD_PAGE_SIZE);

            for (List<ContactBody> page : pages) {
                AddContactRequest request = new AddContactRequest(account.accountId, page.toArray(new ContactBody[0]));
                try {
                    Response response = apiUtils.getApi().contact().add(request).execute();
                    expectedPages++;
                    if (ApiUtils.isCallSuccessful(response)) {
                        successPages++;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

                Log.v(TAG, "uploaded " + page.size() + " contacts for page " + expectedPages);
            }

            if (successPages != expectedPages) {
                Log.v(TAG, "failed to upload contacts in " +
                        (System.currentTimeMillis() - startTime) + " ms");
            } else {
                Log.v(TAG, "contacts upload successful in " +
                        (System.currentTimeMillis() - startTime) + " ms");
            }
        }

        CursorUtil.closeSilent(cursor);
    }

    private void uploadBlacklists() {
        long startTime = System.currentTimeMillis();
        Cursor cursor = source.getBlacklists(this);

        if (cursor.moveToFirst()) {
            BlacklistBody[] blacklists = new BlacklistBody[cursor.getCount()];

            do {
                Blacklist b = new Blacklist();
                b.fillFromCursor(cursor);
                b.encrypt(encryptionUtils);
                BlacklistBody blacklist = new BlacklistBody(b.id, b.phoneNumber);

                blacklists[cursor.getPosition()] = blacklist;
            } while (cursor.moveToNext());

            AddBlacklistRequest request =
                    new AddBlacklistRequest(account.accountId, blacklists);
            Response result;
            try {
                result = apiUtils.getApi().blacklist().add(request).execute();
            } catch (IOException e) {
                result = null;
            }

            if (result == null || !ApiUtils.isCallSuccessful(result)) {
                Log.v(TAG, "failed to upload blacklists in " +
                        (System.currentTimeMillis() - startTime) + " ms");
            } else {
                Log.v(TAG, "blacklists upload successful in " +
                        (System.currentTimeMillis() - startTime) + " ms");
            }
        }

        CursorUtil.closeSilent(cursor);
    }

    private void uploadScheduledMessages() {
        long startTime = System.currentTimeMillis();
        Cursor cursor = source.getScheduledMessages(this);

        if (cursor.moveToFirst()) {
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
                    new AddScheduledMessageRequest(account.accountId, messages);
            Response result;

            try {
                result = apiUtils.getApi().scheduled().add(request).execute();
            } catch (IOException e) {
                result = null;
            }

            if (result == null || !ApiUtils.isCallSuccessful(result)) {
                Log.v(TAG, "failed to upload scheduled messages in " +
                        (System.currentTimeMillis() - startTime) + " ms");
            } else {
                Log.v(TAG, "scheduled messages upload successful in " +
                        (System.currentTimeMillis() - startTime) + " ms");
            }
        }

        CursorUtil.closeSilent(cursor);
    }

    private void uploadDrafts() {
        long startTime = System.currentTimeMillis();
        Cursor cursor = source.getDrafts(this);

        if (cursor.moveToFirst()) {
            DraftBody[] drafts = new DraftBody[cursor.getCount()];

            do {
                Draft d = new Draft();
                d.fillFromCursor(cursor);
                d.encrypt(encryptionUtils);
                DraftBody draft = new DraftBody(d.id, d.conversationId, d.data, d.mimeType);

                drafts[cursor.getPosition()] = draft;
            } while (cursor.moveToNext());

            AddDraftRequest request = new AddDraftRequest(account.accountId, drafts);
            Object result;

            try {
                result = apiUtils.getApi().draft().add(request).execute().body();
            } catch (IOException e) {
                result = null;
            }

            if (result == null) {
                Log.v(TAG, "failed to upload drafts in " +
                        (System.currentTimeMillis() - startTime) + " ms");
            } else {
                Log.v(TAG, "drafts upload successful in " +
                        (System.currentTimeMillis() - startTime) + " ms");
            }
        }

        CursorUtil.closeSilent(cursor);
    }

    /**
     * Media will be uploaded after the messages finish uploading
     */
    private void uploadMedia() {
        final NotificationCompat.Builder builder = new NotificationCompat.Builder(this, NotificationUtils.STATUS_NOTIFICATIONS_CHANNEL_ID)
                .setContentTitle(getString(R.string.encrypting_and_uploading_media))
                .setSmallIcon(R.drawable.ic_upload)
                .setProgress(0, 0, true)
                .setLocalOnly(true)
                .setColor(ColorSet.DEFAULT(this).color)
                .setOngoing(true);
        final NotificationManagerCompat manager = NotificationManagerCompat.from(this);
        startForeground(MESSAGE_UPLOAD_ID, builder.build());

        FirebaseAuth auth = FirebaseAuth.getInstance();
        Executor executor = new DirectExecutor();
        auth.signInAnonymously()
                .addOnSuccessListener(executor, authResult -> processMediaUpload(manager, builder))
                .addOnFailureListener(executor, e -> {
                    Log.e(TAG, "failed to sign in to firebase", e);
                    finishMediaUpload(manager);
                });
    }

    private int completedMediaUploads = 0;
    private void processMediaUpload(NotificationManagerCompat manager,
                                    final NotificationCompat.Builder builder) {
        apiUtils.saveFirebaseFolderRef(account.accountId);

        new Thread(() -> {
            try { Thread.sleep(1000 * 60 * 2); } catch (InterruptedException e) { }
            finishMediaUpload(manager);
        }).start();

        Cursor media = source.getAllMediaMessages(this, NUM_MEDIA_TO_UPLOAD);
        if (media.moveToFirst()) {
            int mediaCount = media.getCount() < NUM_MEDIA_TO_UPLOAD ? media.getCount() : NUM_MEDIA_TO_UPLOAD;
            do {
                Message message = new Message();
                message.fillFromCursor(media);

                Log.v(TAG, "started uploading " + message.id);

                byte[] bytes = BinaryUtils.getMediaBytes(this, message.data, message.mimeType);
                apiUtils.uploadBytesToFirebase(account.accountId, bytes, message.id, encryptionUtils, () -> {
                    completedMediaUploads++;

                    builder.setProgress(mediaCount, completedMediaUploads, false);
                    builder.setContentTitle(getString(R.string.encrypting_and_uploading_count,
                            completedMediaUploads + 1, media.getCount()));

                    if (completedMediaUploads >= mediaCount) {
                        finishMediaUpload(manager);
                    } else if (!finished) {
                        startForeground(MESSAGE_UPLOAD_ID, builder.build());
                    }
                }, 0);
            } while (media.moveToNext());
        }

        CursorUtil.closeSilent(media);
    }

    private boolean finished = false;
    private void finishMediaUpload(NotificationManagerCompat manager) {
        stopForeground(true);
        stopSelf();
        finished = true;
    }

    public static boolean noNull(List list) {
        for (Object o : list) {
            if (o == null) {
                return false;
            }
        }

        return true;
    }
}
