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
import android.net.Uri;
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
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.concurrent.Executor;

import xyz.klinker.messenger.R;
import xyz.klinker.messenger.api.entity.BlacklistBody;
import xyz.klinker.messenger.api.entity.ConversationBody;
import xyz.klinker.messenger.api.entity.DraftBody;
import xyz.klinker.messenger.api.entity.MessageBody;
import xyz.klinker.messenger.api.entity.ScheduledMessageBody;
import xyz.klinker.messenger.api.implementation.ApiUtils;
import xyz.klinker.messenger.data.DataSource;
import xyz.klinker.messenger.data.MimeType;
import xyz.klinker.messenger.data.Settings;
import xyz.klinker.messenger.data.model.Blacklist;
import xyz.klinker.messenger.data.model.Conversation;
import xyz.klinker.messenger.data.model.Draft;
import xyz.klinker.messenger.data.model.Message;
import xyz.klinker.messenger.data.model.ScheduledMessage;
import xyz.klinker.messenger.encryption.EncryptionUtils;
import xyz.klinker.messenger.encryption.KeyUtils;
import xyz.klinker.messenger.util.ContactUtils;
import xyz.klinker.messenger.util.ImageUtils;
import xyz.klinker.messenger.util.listener.DirectExecutor;

public class ApiDownloadService extends Service {

    private static final String TAG = "ApiDownloadService";
    private static final int MESSAGE_DOWNLOAD_ID = 7237;
    private static final int MEDIA_DOWNLOAD_ID = 7238;
    private static final long MAX_SIZE = 1024 * 1024 * 2;
    public static final String ACTION_DOWNLOAD_FINISHED =
            "xyz.klinker.messenger.API_DOWNLOAD_FINISHED";

    private Settings settings;
    private ApiUtils apiUtils;
    private EncryptionUtils encryptionUtils;
    private DataSource source;
    private boolean firebaseDownloadFinished = false;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        downloadData();
        return super.onStartCommand(intent, flags, startId);
    }

    private void downloadData() {
        Notification notification = new NotificationCompat.Builder(this)
                .setContentTitle(getString(R.string.downloading_and_decrypting))
                .setSmallIcon(R.drawable.ic_download)
                .setProgress(0, 0, true)
                .setLocalOnly(true)
                .setColor(getResources().getColor(R.color.colorPrimary))
                .setOngoing(true)
                .build();
        NotificationManagerCompat.from(this).notify(MESSAGE_DOWNLOAD_ID, notification);

        new Thread(new Runnable() {
            @Override
            public void run() {
                settings = Settings.get(getApplicationContext());
                apiUtils = new ApiUtils();
                encryptionUtils = new EncryptionUtils(new KeyUtils().createKey(settings.passhash,
                        settings.accountId, settings.salt));
                source = DataSource.getInstance(getApplicationContext());
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
                Log.v(TAG, "time to download: " + (System.currentTimeMillis() - startTime) + " ms");

                sendBroadcast(new Intent(ACTION_DOWNLOAD_FINISHED));
                NotificationManagerCompat.from(getApplicationContext()).cancel(MESSAGE_DOWNLOAD_ID);
                source.setTransactionSuccessful();
                source.setUpload(true);
                source.endTransaction();
                downloadMedia();
            }
        }).start();
    }

    private void wipeDatabase() {
        source.clearTables();
    }

    private void downloadMessages() {
        long startTime = System.currentTimeMillis();
        MessageBody[] messages = apiUtils.getApi().message()
                .list(settings.accountId, null, null, null);

        if (messages != null) {
            for (MessageBody body : messages) {
                Message message = new Message(body);
                message.decrypt(encryptionUtils);
                source.insertMessage(message, message.conversationId);
            }

            Log.v(TAG, "messages inserted in " + (System.currentTimeMillis() - startTime) + " ms");
        } else {
            Log.v(TAG, "messages failed to insert");
        }
    }

    private void downloadConversations() {
        long startTime = System.currentTimeMillis();
        ConversationBody[] conversations = apiUtils.getApi().conversation()
                .list(settings.accountId);

        if (conversations != null) {
            for (ConversationBody body : conversations) {
                Conversation conversation = new Conversation(body);
                conversation.decrypt(encryptionUtils);
                conversation.imageUri = ContactUtils.findImageUri(conversation.phoneNumbers, this);

                if (conversation.imageUri != null &&
                        ImageUtils.getContactImage(conversation.imageUri, this) == null) {
                    conversation.imageUri = null;
                } else if (conversation.imageUri != null) {
                    conversation.imageUri += "/photo";
                }

                source.insertConversation(conversation);
            }

            Log.v(TAG, "conversations inserted in " + (System.currentTimeMillis() - startTime) + " ms");
        } else {
            Log.v(TAG, "conversations failed to insert");
        }
    }

    private void downloadBlacklists() {
        long startTime = System.currentTimeMillis();
        BlacklistBody[] blacklists = apiUtils.getApi().blacklist().list(settings.accountId);

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
        ScheduledMessageBody[] messages = apiUtils.getApi().scheduled().list(settings.accountId);

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
        DraftBody[] drafts = apiUtils.getApi().draft().list(settings.accountId);

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

    private void downloadMedia() {
        final NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                .setContentTitle(getString(R.string.decrypting_and_downloading_media))
                .setSmallIcon(R.drawable.ic_download)
                .setProgress(0, 0, true)
                .setLocalOnly(true)
                .setColor(getResources().getColor(R.color.colorPrimary))
                .setOngoing(true);
        final NotificationManagerCompat manager = NotificationManagerCompat.from(this);
        manager.notify(MEDIA_DOWNLOAD_ID, builder.build());

        FirebaseAuth auth = FirebaseAuth.getInstance();
        Executor executor = new DirectExecutor();
        auth.signInAnonymously()
                .addOnSuccessListener(executor, new OnSuccessListener<AuthResult>() {
                    @Override
                    public void onSuccess(AuthResult authResult) {
                        processMediaDownload(manager, builder);
                    }
                })
                .addOnFailureListener(executor, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "failed to sign in to firebase", e);
                        finishMediaDownload(manager);
                    }
                });
    }

    private void processMediaDownload(NotificationManagerCompat manager,
                                      NotificationCompat.Builder builder) {
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageRef = storage
                .getReferenceFromUrl(ApiUploadService.FIREBASE_STORAGE_URL);
        StorageReference folderRef = storageRef.child(Settings.get(this).accountId);

        Cursor media = source.getFirebaseMediaMessages();
        if (media.moveToFirst()) {
            do {
                Message message = new Message();
                message.fillFromCursor(media);

                // each firebase message is formatted as "firebase [num]" and we want to get the
                // num and process the message only if that num is actually stored on firebase
                int number = Integer.parseInt(message.data.split(" ")[1]) + 1;
                if (number < media.getCount() - ApiUploadService.NUM_MEDIA_TO_UPLOAD &&
                        number != -1) {
                    continue;
                }

                StorageReference fileRef = folderRef.child(message.id + "");
                System.out.println(fileRef.getPath());

                final File file = new File(getFilesDir(),
                        message.id + MimeType.getExtension(message.mimeType));

                fileRef.getBytes(MAX_SIZE)
                        .addOnSuccessListener(new OnSuccessListener<byte[]>() {
                            @Override
                            public void onSuccess(byte[] bytes) {
                                bytes = encryptionUtils.decryptData(new String(bytes));

                                try {
                                    BufferedOutputStream bos =
                                            new BufferedOutputStream(new FileOutputStream(file));
                                    bos.write(bytes);
                                    bos.flush();
                                    bos.close();
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }

                                firebaseDownloadFinished = true;
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                firebaseDownloadFinished = true;
                            }
                        });

                while (!firebaseDownloadFinished) ;

                source.updateMessageData(message.id, Uri.fromFile(file).toString());
                builder.setProgress(media.getCount(), media.getPosition(), false);
                manager.notify(MEDIA_DOWNLOAD_ID, builder.build());
            } while (media.moveToNext());
        }

        media.close();
        finishMediaDownload(manager);
    }

    private void finishMediaDownload(NotificationManagerCompat manager) {
        manager.cancel(MEDIA_DOWNLOAD_ID);
        source.close();
        stopSelf();
    }

}
