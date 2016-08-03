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

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.os.Handler;
import android.os.IBinder;
import android.provider.Telephony;
import android.support.annotation.Nullable;
import android.util.Log;

import xyz.klinker.messenger.data.DataSource;
import xyz.klinker.messenger.data.MimeType;
import xyz.klinker.messenger.data.model.Message;
import xyz.klinker.messenger.receiver.MessageListUpdatedReceiver;
import xyz.klinker.messenger.util.SmsMmsUtils;

/**
 * A service that looks for changes to the SMS internal database and notifies my app if it finds
 * one. This is important because some third party services (pushbullet, google now, etc.) allow
 * the user to send SMS messages and those messages are inserted directly into the internal
 * content provider, skipping the phase where Messenger saves them. If that happens, we should
 * find the message and insert it into our own database.
 */
public class ContentObserverService extends Service {

    private SmsContentObserver observer;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.v("ContentObserverService", "starting content observer service");
        if (observer == null) {
            observer = new SmsContentObserver(this, new Handler());
            getContentResolver().registerContentObserver(Telephony.Sms.CONTENT_URI, true, observer);
        }

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        getContentResolver().unregisterContentObserver(observer);
    }

    private class SmsContentObserver extends ContentObserver {

        private Context context;

        public SmsContentObserver(Context context, Handler handler) {
            super(handler);
            this.context = context;
        }

        @Override
        public void onChange(boolean selfChange) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    processLastMessage();
                }
            }).start();
        }

        private void processLastMessage() {
            Cursor cursor = SmsMmsUtils.getLastSmsMessage(context);
            if (cursor != null && cursor.moveToFirst()) {
                int type = cursor.getInt(cursor.getColumnIndex(Telephony.Sms.TYPE));
                String body = cursor.getString(cursor.getColumnIndex(Telephony.Sms.BODY));
                String address = cursor.getString(cursor.getColumnIndex(Telephony.Sms.ADDRESS));

                if (type == Telephony.Sms.MESSAGE_TYPE_INBOX) {
                    cursor.close();
                    return;
                }

                DataSource source = DataSource.getInstance(context);
                source.open();

                Cursor search = source.searchMessages(body);
                if (search != null && search.moveToFirst()) {
                    Message message = new Message();
                    message.fillFromCursor(search);

                    // if a message with the same body was sent in the last minute, don't insert
                    // this one into the database because we already have it.
                    //
                    // NOTE: we are just going to insert the message as sent here instead of
                    //       sending like it should be... this is because we won't get a callback
                    //       like we normally would for when the message has sent. This could be
                    //       handled in this content observer however, so we'll save that for
                    //       another time.
                    if (!(message.data.equals(body) && message.type != Message.TYPE_RECEIVED &&
                            message.timestamp > System.currentTimeMillis() - (1000 * 60))) {
                        insertMessage(source, body, address);
                    }

                    search.close();
                } else {
                    // if we don't find the search text anywhere, insert the message since we
                    // already filtered out received messages above.
                    insertMessage(source, body, address);
                }

                source.close();
                cursor.close();
            }
        }

        private void insertMessage(DataSource source, String body, String address) {
            Message insert = new Message();
            insert.data = body;
            insert.mimeType = MimeType.TEXT_PLAIN;
            insert.timestamp = System.currentTimeMillis();
            insert.type = Message.TYPE_SENT;
            insert.read = true;
            insert.seen = true;

            source.insertMessage(insert, address, context);
            MessageListUpdatedReceiver.sendBroadcast(context, insert.conversationId);
        }

    }

}
