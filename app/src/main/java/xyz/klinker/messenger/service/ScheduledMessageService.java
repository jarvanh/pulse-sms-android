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

import android.app.AlarmManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.text.Html;
import android.util.Log;

import xyz.klinker.messenger.R;
import xyz.klinker.messenger.activity.MessengerActivity;
import xyz.klinker.messenger.api.implementation.Account;
import xyz.klinker.messenger.data.DataSource;
import xyz.klinker.messenger.data.model.Conversation;
import xyz.klinker.messenger.data.model.ScheduledMessage;
import xyz.klinker.messenger.util.SendUtils;

/**
 * Service responsible for sending scheduled message that are coming up, removing that message
 * from the database and then scheduling the next one.
 */
public class ScheduledMessageService extends Service {

    private static final int SCHEDULED_ALARM_REQUEST_CODE = 5424;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        DataSource source = DataSource.getInstance(this);
        source.open();

        Cursor messages = source.getScheduledMessages();

        if (messages != null && messages.moveToFirst()) {
            do {
                long timestamp = messages.getLong(
                        messages.getColumnIndex(ScheduledMessage.COLUMN_TIMESTAMP));

                // if message is newer than 5 minutes ago and older than 5 minutes in the future
                if (timestamp > System.currentTimeMillis() - (1000 * 60 * 5) &&
                        timestamp < System.currentTimeMillis() + (1000 * 60 * 5)) {
                    ScheduledMessage message = new ScheduledMessage();
                    message.fillFromCursor(messages);

                    // delete, insert and send
                    source.deleteScheduledMessage(message.id);
                    long conversationId = source.insertSentMessage(message.to, message.data, message.mimeType, this);
                    Conversation conversation = source.getConversation(conversationId);

                    new SendUtils(conversation != null ? conversation.simSubscriptionId : null)
                            .send(this, message.data, message.to);

                    // display a notification
                    String body = "<b>" + message.title + ": </b>" + message.data;
                    Intent open = new Intent(this, MessengerActivity.class);
                    open.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    PendingIntent pendingOpen = PendingIntent.getActivity(this, 0, open,
                            PendingIntent.FLAG_UPDATE_CURRENT);

                    Notification notification = new NotificationCompat.Builder(this)
                            .setSmallIcon(R.drawable.ic_stat_notify)
                            .setContentTitle(getString(R.string.scheduled_message_sent))
                            .setContentText(Html.fromHtml(body))
                            .setColor(getResources().getColor(R.color.colorPrimary))
                            .setAutoCancel(true)
                            .setContentIntent(pendingOpen)
                            .build();
                    NotificationManagerCompat.from(this)
                            .notify(5555 + (int) message.id, notification);

                    Log.v("scheduled message", "message was sent and notification given");
                }
            } while (messages.moveToNext());
        }

        try {
            messages.close();
        } catch (Exception e) { }

        // get scheduled messages again so that we have an accurate time for the next message
        messages = source.getScheduledMessages();
        if (messages != null && messages.moveToFirst()) {
            scheduleNext(messages);
            messages.close();
        } else {
            Log.v("scheduled message", "no more messages to schedule");
        }

        source.close();
        stopSelf();
        return super.onStartCommand(intent, flags, startId);
    }

    private void scheduleNext(Cursor messages) {
        Account account = Account.get(this);
        if (account.exists() && !account.primary) {
            // if they have an online account, we only want scheduled messages to go through the phone
            return;
        }

        Intent intent = new Intent(this, ScheduledMessageService.class);
        PendingIntent pIntent = PendingIntent.getService(this, SCHEDULED_ALARM_REQUEST_CODE,
                intent, PendingIntent.FLAG_UPDATE_CURRENT);

        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

        // cancel the current request if it exists, we'll just make a completely new one
        alarmManager.cancel(pIntent);
        alarmManager.setExact(AlarmManager.RTC_WAKEUP,
                messages.getLong(messages.getColumnIndex(ScheduledMessage.COLUMN_TIMESTAMP)),
                pIntent);

        Log.v("scheduled messsage", "new message scheduled");
    }

}
