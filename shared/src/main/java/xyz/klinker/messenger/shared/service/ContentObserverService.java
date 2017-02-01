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

import com.klinker.android.send_message.Utils;

import xyz.klinker.messenger.api.implementation.Account;
import xyz.klinker.messenger.shared.data.DataSource;
import xyz.klinker.messenger.shared.data.MimeType;
import xyz.klinker.messenger.shared.data.Settings;
import xyz.klinker.messenger.shared.data.model.Conversation;
import xyz.klinker.messenger.shared.data.model.Message;
import xyz.klinker.messenger.shared.receiver.MessageListUpdatedReceiver;
import xyz.klinker.messenger.shared.util.PhoneNumberUtils;
import xyz.klinker.messenger.shared.util.SmsMmsUtils;

/**
 * A service that looks for changes to the SMS internal database and notifies my app if it finds
 * one. This is important because some third party services (pushbullet, google now, etc.) allow
 * the user to send SMS messages and those messages are inserted directly into the internal
 * content provider, skipping the phase where Messenger saves them. If that happens, we should
 * find the message and insert it into our own database.
 */
public class ContentObserverService extends Service {

    public static boolean IS_RUNNING = false;

    private SmsContentObserver observer;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Log.v("ContentObserverService", "starting content observer service");
        if (observer == null && Account.get(this).primary) {
            observer = new SmsContentObserver(this, new Handler());
            getContentResolver().registerContentObserver(Telephony.Sms.CONTENT_URI, true, observer);
        }

        IS_RUNNING = true;
        ContentObserverRunCheckService.scheduleNextRun(this);

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        IS_RUNNING = false;

        if (observer != null) {
            getContentResolver().unregisterContentObserver(observer);
            observer = null;
        }
    }

    private class SmsContentObserver extends ContentObserver {

        private Context context;

        public SmsContentObserver(Context context, Handler handler) {
            super(handler);
            this.context = context;
        }

        @Override
        public void onChange(boolean selfChange) {
            new Thread(() -> {
                try {
                    Thread.sleep(10000);
                } catch (Exception e) {

                }

                processLastMessage();
            }).start();
        }

        private void processLastMessage() {
            Cursor cursor = SmsMmsUtils.getLastSmsMessage(context);
            if (cursor != null && cursor.moveToFirst()) {
                int type = cursor.getInt(cursor.getColumnIndex(Telephony.Sms.TYPE));
                String body = cursor.getString(cursor.getColumnIndex(Telephony.Sms.BODY));
                String address = cursor.getString(cursor.getColumnIndex(Telephony.Sms.ADDRESS));
                cursor.close();

                Settings settings = Settings.get(ContentObserverService.this);
                if (settings.signature != null && !settings.signature.isEmpty()) {
                    body = body.replace("\n" + settings.signature, "");
                }

                if (address == null || address.isEmpty()) {
                    return;
                }

                DataSource source = DataSource.getInstance(context);
                source.open();

                Cursor search = source.searchMessages(body);
                if (search != null && search.moveToFirst()) {
                    Message message = new Message();
                    message.fillFromCursor(search);
                    Conversation conversation = source.getConversation(message.conversationId);
                    
                    search.close();

                    if (type == Telephony.Sms.MESSAGE_TYPE_INBOX && !Utils.isDefaultSmsApp(context)) {
                        // if a message with the same body was received from the same person in the
                        // last minute and is the last message in that conversation, don't insert
                        // this one into the database because we already have it. Otherwise, do.
                        if (conversation != null && !(PhoneNumberUtils.checkEquality(conversation.phoneNumbers, address) &&
                                message.data.equals(body) && message.type == Message.TYPE_RECEIVED)) {
                            insertReceivedMessage(source, body, address);
                        }
                    } else if (type != Telephony.Sms.MESSAGE_TYPE_INBOX) {
                        // if a message from the same person with the exact same body exists and the
                        // type is not received, don't save the message. Otherwise, do.
                        //
                        // NOTE: we are just going to insert the message as sent here instead of
                        //       sending like it should be... this is because we won't get a callback
                        //       like we normally would for when the message has sent. This could be
                        //       handled in this content observer however, so we'll save that for
                        //       another time.
                        if (conversation != null && !(PhoneNumberUtils.checkEquality(conversation.phoneNumbers, address) &&
                                message.data.equals(body) && message.type != Message.TYPE_RECEIVED)) {
                            insertSentMessage(source, body, address);
                        }
                    }
                } else {
                    if (type == Telephony.Sms.MESSAGE_TYPE_INBOX && !Utils.isDefaultSmsApp(context)) {
                        insertReceivedMessage(source, body, address);
                    } else if (type != Telephony.Sms.MESSAGE_TYPE_INBOX) {
                        insertSentMessage(source, body, address);
                    }
                }
                
                try {
                    search.close();
                } catch (Exception e) { }

                source.close();
            }
            
            try {
                cursor.close();
            } catch (Exception e) {
                
            }
        }

        private void insertReceivedMessage(DataSource source, String body, String address) {
            insertMessage(source, body, address, Message.TYPE_RECEIVED);
        }

        private void insertSentMessage(DataSource source, String body, String address) {
            insertMessage(source, body, address, Message.TYPE_SENT);
        }

        private void insertMessage(DataSource source, String body, String address, int type) {
            Message insert = new Message();
            insert.data = body;
            insert.mimeType = MimeType.TEXT_PLAIN;
            insert.timestamp = System.currentTimeMillis();
            insert.type = type;
            insert.read = true;
            insert.seen = true;

            source.insertMessage(insert, address, context);
            MessageListUpdatedReceiver.sendBroadcast(context, insert);
        }

    }

}
