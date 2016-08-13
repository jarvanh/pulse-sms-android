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
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;

import xyz.klinker.messenger.R;
import xyz.klinker.messenger.api.entity.BlacklistBody;
import xyz.klinker.messenger.api.entity.ConversationBody;
import xyz.klinker.messenger.api.entity.DraftBody;
import xyz.klinker.messenger.api.entity.MessageBody;
import xyz.klinker.messenger.api.entity.ScheduledMessageBody;
import xyz.klinker.messenger.api.implementation.ApiUtils;
import xyz.klinker.messenger.data.DataSource;
import xyz.klinker.messenger.data.Settings;
import xyz.klinker.messenger.encryption.EncryptionUtils;
import xyz.klinker.messenger.encryption.KeyUtils;

public class ApiDownloadService extends Service {

    private static final String TAG = "ApiDownloadService";
    private static final int NOTIFICATION_ID = 7236;

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
                .setContentTitle(getString(R.string.downloading_and_decrypting))
                .setSmallIcon(R.drawable.ic_download)
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
                source.beginTransaction();

                long startTime = System.currentTimeMillis();
                wipeDatabase();
                downloadMessages();
                downloadConversations();
                downloadBlacklists();
                downloadScheduledMessages();
                downloadDrafts();
                Log.v(TAG, "time to download: " + (System.currentTimeMillis() - startTime) + " ms");

                NotificationManagerCompat.from(getApplicationContext()).cancel(NOTIFICATION_ID);
                source.setTransactionSuccessful();
                source.endTransaction();
                source.close();
                stopSelf();
            }
        }).start();
        return super.onStartCommand(intent, flags, startId);
    }

    private void wipeDatabase() {
        source.clearTables();
    }

    private void downloadMessages() {
        MessageBody[] messages = apiUtils.getApi().message()
                .list(settings.accountId, null, null, null);
    }

    private void downloadConversations() {
        ConversationBody[] conversations = apiUtils.getApi().conversation()
                .list(settings.accountId);
    }

    private void downloadBlacklists() {
        BlacklistBody[] blacklists = apiUtils.getApi().blacklist().list(settings.accountId);
    }

    private void downloadScheduledMessages() {
        ScheduledMessageBody[] messages = apiUtils.getApi().scheduled().list(settings.accountId);
    }

    private void downloadDrafts() {
        DraftBody[] drafts = apiUtils.getApi().draft().list(settings.accountId);
    }

}
