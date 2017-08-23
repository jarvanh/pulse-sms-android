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
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

import xyz.klinker.messenger.api.implementation.firebase.FirebaseDownloadCallback;
import xyz.klinker.messenger.shared.R;
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
import xyz.klinker.messenger.shared.util.ContactUtils;
import xyz.klinker.messenger.shared.util.ImageUtils;
import xyz.klinker.messenger.shared.util.NotificationUtils;
import xyz.klinker.messenger.shared.util.listener.DirectExecutor;

public class ApiDownloadService extends Service {

    public static void start(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(new Intent(context, ApiDownloadService.class));
        } else {
            context.startService(new Intent(context, ApiDownloadService.class));
        }
    }

    private static final String TAG = "ApiDownloadService";
    private static final int MESSAGE_DOWNLOAD_ID = 7237;
    public static final String ACTION_DOWNLOAD_FINISHED =
            "xyz.klinker.messenger.API_DOWNLOAD_FINISHED";

    public static final int MESSAGE_DOWNLOAD_PAGE_SIZE = 300;
    public static final int MAX_MEDIA_DOWNLOADS = 75;
    public static final String ARG_SHOW_NOTIFICATION = "show_notification";

    public static boolean IS_RUNNING = false;

    private Account account;
    private ApiUtils apiUtils;
    private EncryptionUtils encryptionUtils;
    private DataSource source;

    private boolean showNotification = true;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            showNotification = intent.getBooleanExtra(ARG_SHOW_NOTIFICATION, true);
        } else {
            showNotification = true;
        }

        downloadData();
        return super.onStartCommand(intent, flags, startId);
    }

    private void downloadData() {
        Notification notification = new NotificationCompat.Builder(this,
                    NotificationUtils.STATUS_NOTIFICATIONS_CHANNEL_ID)
                .setContentTitle(getString(R.string.downloading_and_decrypting))
                .setSmallIcon(R.drawable.ic_download)
                .setProgress(0, 0, true)
                .setLocalOnly(true)
                .setColor(ColorSet.DEFAULT(this).color)
                .setOngoing(true)
                .build();

        if (showNotification) {
            startForeground(MESSAGE_DOWNLOAD_ID, notification);
        }

        new Thread(() -> {
            IS_RUNNING = true;

            account = Account.get(getApplicationContext());

            apiUtils = new ApiUtils();
            encryptionUtils = account.getEncryptor();
            source = DataSource.Companion.getInstance(getApplicationContext());
            source.open();
            source.setUpload(false);
            source.beginTransaction();

            long startTime = System.currentTimeMillis();
            wipeDatabase();
            downloadMessages();
            downloadConversations();
            downloadBlacklists();
            downloadScheduledMessages();
            downloadDrafts();
            downloadContacts();
            Log.v(TAG, "time to download: " + (System.currentTimeMillis() - startTime) + " ms");

            sendBroadcast(new Intent(ACTION_DOWNLOAD_FINISHED));
            NotificationManagerCompat.from(getApplicationContext()).cancel(MESSAGE_DOWNLOAD_ID);
            source.setTransactionSuccessful();
            source.setUpload(true);
            source.endTransaction();
            downloadMedia();

            IS_RUNNING = false;
        }).start();
    }

    private void wipeDatabase() {
        source.clearTables();
    }

    private void downloadMessages() {
        long startTime = System.currentTimeMillis();
        List<Message> messageList = new ArrayList<>();

        int pageNumber = 1;
        int nullCount = 0;
        boolean noMessages = false;

        do {
            MessageBody[] messages;

            try {
                messages = apiUtils.getApi().message()
                        .list(account.accountId, null, MESSAGE_DOWNLOAD_PAGE_SIZE, messageList.size())
                        .execute().body();
            } catch (IOException e) {
                messages = new MessageBody[0];
            }

            if (messages != null) {
                if (messages.length == 0) {
                    noMessages = true;
                }

                for (MessageBody body : messages) {
                    Message message = new Message(body);

                    try {
                        message.decrypt(encryptionUtils);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    messageList.add(message);
                }
            } else {
                nullCount++;
            }

            Log.v(TAG,  messageList.size() + " messages downloaded. " + pageNumber + " pages so far.");
            pageNumber++;
        } while (messageList.size() % MESSAGE_DOWNLOAD_PAGE_SIZE == 0 && !noMessages && nullCount < 5);

        if (messageList.size() > 0) {
            source.insertMessages(this, messageList);
            Log.v(TAG, messageList.size() + " messages inserted in " + (System.currentTimeMillis() - startTime) + " ms with " + pageNumber + " pages");

            messageList.clear();
        } else {
            Log.v(TAG, "messages failed to insert");
        }
    }

    private void downloadConversations() {
        long startTime = System.currentTimeMillis();
        ConversationBody[] conversations;

        try {
            conversations = apiUtils.getApi().conversation()
                    .list(account.accountId).execute().body();
        } catch (IOException e) {
            conversations = new ConversationBody[0];
        }

        if (conversations != null) {
            for (ConversationBody body : conversations) {
                Conversation conversation = new Conversation(body);

                try {
                    conversation.decrypt(encryptionUtils);
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.v(TAG, "decryption error while downloading conversations. Retrying now.");

                    retryConversationDownloadFromBadDecryption();
                    return;
                }

                conversation.imageUri = ContactUtils.findImageUri(conversation.phoneNumbers, this);

                Bitmap image = ImageUtils.getContactImage(conversation.imageUri, this);
                if (conversation.imageUri != null &&
                         image == null) {
                    conversation.imageUri = null;
                } else if (conversation.imageUri != null) {
                    conversation.imageUri += "/photo";
                }

                if (image != null) {
                    image.recycle();
                }

                source.insertConversation(conversation);
            }

            Log.v(TAG, "conversations inserted in " + (System.currentTimeMillis() - startTime) + " ms");
        } else {
            Log.v(TAG, "conversations failed to insert");
        }
    }

    // a bit probably got misplaced? Lets retry. If it doesn't work still, just skip inserting
    // that conversation
    private void retryConversationDownloadFromBadDecryption() {
        long startTime = System.currentTimeMillis();
        ConversationBody[] conversations;

        try {
            conversations = apiUtils.getApi().conversation()
                    .list(account.accountId).execute().body();
        } catch (IOException e) {
            conversations = new ConversationBody[0];
        }

        if (conversations != null) {
            for (ConversationBody body : conversations) {
                Conversation conversation = new Conversation(body);

                try {
                    conversation.decrypt(encryptionUtils);
                    conversation.imageUri = ContactUtils.findImageUri(conversation.phoneNumbers, this);

                    if (conversation.imageUri != null &&
                            ImageUtils.getContactImage(conversation.imageUri, this) == null) {
                        conversation.imageUri = null;
                    } else if (conversation.imageUri != null) {
                        conversation.imageUri += "/photo";
                    }

                    source.insertConversation(conversation);
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.v(TAG, "error inserting conversation due to encryption. conversation_id: " + conversation.id);
                }
            }

            Log.v(TAG, "conversations inserted in " + (System.currentTimeMillis() - startTime) + " ms");
        } else {
            Log.v(TAG, "conversations failed to insert");
        }
    }

    private void downloadBlacklists() {
        long startTime = System.currentTimeMillis();
        BlacklistBody[] blacklists;

        try {
            blacklists = apiUtils.getApi().blacklist().list(account.accountId).execute().body();
        } catch (Exception e) {
            blacklists = new BlacklistBody[0];
        }

        if (blacklists != null) {
            for (BlacklistBody body : blacklists) {
                Blacklist blacklist = new Blacklist(body);
                blacklist.decrypt(encryptionUtils);
                source.insertBlacklist(blacklist);
            }

            Log.v(TAG, "blacklists inserted in " + (System.currentTimeMillis() - startTime) + " ms");
        } else {
            Log.v(TAG, "blacklists failed to insert");
        }
    }

    private void downloadScheduledMessages() {
        long startTime = System.currentTimeMillis();
        ScheduledMessageBody[] messages;

        try {
            messages = apiUtils.getApi().scheduled().list(account.accountId).execute().body();
        } catch (IOException e) {
            messages = new ScheduledMessageBody[0];
        }

        if (messages != null) {
            for (ScheduledMessageBody body : messages) {
                ScheduledMessage message = new ScheduledMessage(body);
                message.decrypt(encryptionUtils);
                source.insertScheduledMessage(message);
            }

            Log.v(TAG, "scheduled messages inserted in " + (System.currentTimeMillis() - startTime) + " ms");
        } else {
            Log.v(TAG, "scheduled messages failed to insert");
        }
    }

    private void downloadDrafts() {
        long startTime = System.currentTimeMillis();
        DraftBody[] drafts;

        try {
            drafts = apiUtils.getApi().draft().list(account.accountId).execute().body();
        } catch (IOException e) {
            drafts = new DraftBody[0];
        }

        if (drafts != null) {
            for (DraftBody body : drafts) {
                Draft draft = new Draft(body);
                draft.decrypt(encryptionUtils);
                source.insertDraft(draft);
            }

            Log.v(TAG, "drafts inserted in " + (System.currentTimeMillis() - startTime) + " ms");
        } else {
            Log.v(TAG, "drafts failed to insert");
        }
    }

    private void downloadContacts() {
        long startTime = System.currentTimeMillis();
        ContactBody[] contacts;

        try {
            contacts = apiUtils.getApi().contact().list(account.accountId).execute().body();
        } catch (IOException e) {
            contacts = new ContactBody[0];
        }

        if (contacts != null) {
            List<Contact> contactList = new ArrayList<>();

            for (ContactBody body : contacts) {
                Contact contact = new Contact(body);
                contact.decrypt(encryptionUtils);
                contactList.add(contact);
            }

            source.insertContacts(contactList, null);

            Log.v(TAG, "contacts inserted in " + (System.currentTimeMillis() - startTime) + " ms");
        } else {
            Log.v(TAG, "contacts failed to insert");
        }
    }

    private void downloadMedia() {
        final NotificationCompat.Builder builder = new NotificationCompat.Builder(this,
                    NotificationUtils.STATUS_NOTIFICATIONS_CHANNEL_ID)
                .setContentTitle(getString(R.string.decrypting_and_downloading_media))
                .setSmallIcon(R.drawable.ic_download)
                .setProgress(0, 0, true)
                .setLocalOnly(true)
                .setColor(ColorSet.DEFAULT(this).color)
                .setOngoing(true);
        final NotificationManagerCompat manager = NotificationManagerCompat.from(this);

        if (showNotification) {
            startForeground(MESSAGE_DOWNLOAD_ID, builder.build());
        }

        FirebaseAuth auth = FirebaseAuth.getInstance();
        Executor executor = new DirectExecutor();
        auth.signInAnonymously()
                .addOnSuccessListener(executor, authResult -> {
                    processMediaDownload(manager, builder);
                })
                .addOnFailureListener(executor, e -> {
                    Log.e(TAG, "failed to sign in to firebase", e);
                    finishMediaDownload(manager);
                });
    }

    private int completedMediaDownloads = 0;
    private void processMediaDownload(NotificationManagerCompat manager,
                                      NotificationCompat.Builder builder) {
        apiUtils.saveFirebaseFolderRef(account.accountId);

        new Thread(() -> {
            try { Thread.sleep(1000 * 60 * 5); } catch (InterruptedException e) { }
            finishMediaDownload(manager);
        }).start();


        Cursor media = source.getFirebaseMediaMessages();
        if (media.moveToFirst()) {
            final int mediaCount = media.getCount() > MAX_MEDIA_DOWNLOADS ?
                    MAX_MEDIA_DOWNLOADS : media.getCount();
            int processing = 0;
            do {
                final Message message = new Message();
                message.fillFromCursor(media);
                processing++;

                final File file = new File(getFilesDir(),
                        message.id + MimeType.getExtension(message.mimeType));

                Log.v(TAG, "started downloading " + message.id);

                apiUtils.downloadFileFromFirebase(account.accountId, file, message.id, encryptionUtils, () -> {
                    completedMediaDownloads++;

                    source.updateMessageData(message.id, Uri.fromFile(file).toString());
                    builder.setProgress(mediaCount, completedMediaDownloads, false);

                    if (completedMediaDownloads >= mediaCount) {
                        finishMediaDownload(manager);
                    } else if (showNotification) {
                        startForeground(MESSAGE_DOWNLOAD_ID, builder.build());
                    }
                }, 0);
            } while (media.moveToNext() && processing < MAX_MEDIA_DOWNLOADS);
        }

        media.close();
    }

    private void finishMediaDownload(NotificationManagerCompat manager) {
        stopForeground(true);
        source.close();
        stopSelf();
    }

}
