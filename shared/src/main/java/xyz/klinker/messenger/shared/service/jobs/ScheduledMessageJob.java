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

package xyz.klinker.messenger.shared.service.jobs;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.text.Html;
import android.util.Log;

import java.util.List;

import xyz.klinker.messenger.shared.R;
import xyz.klinker.messenger.api.implementation.Account;
import xyz.klinker.messenger.shared.data.DataSource;
import xyz.klinker.messenger.shared.data.model.Conversation;
import xyz.klinker.messenger.shared.data.model.ScheduledMessage;
import xyz.klinker.messenger.shared.util.ActivityUtils;
import xyz.klinker.messenger.shared.util.SendUtils;

/**
 * Service responsible for sending scheduled message that are coming up, removing that message
 * from the database and then scheduling the next one.
 */
public class ScheduledMessageJob extends IntentService {

    private static final int SCHEDULED_ALARM_REQUEST_CODE = 5424;

    public ScheduledMessageJob() {
        super("ScheduledMessageJob");
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    protected void onHandleIntent(Intent intent) {
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
                    Intent open = ActivityUtils.buildForComponent(ActivityUtils.MESSENGER_ACTIVITY);
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
            source.close();
        } catch (Exception e) { }

        scheduleNextRun(this);
    }

    public static void scheduleNextRun(Context context) {
        DataSource source = DataSource.getInstance(context);
        source.open();

        scheduleNextRun(context, source);

        source.close();
    }

    public static void scheduleNextRun(Context context, DataSource source) {
        Account account = Account.get(context);
        if (account.exists() && !account.primary) {
            // if they have an online account, we only want scheduled messages to go through the phone
            return;
        }

        List<ScheduledMessage> messages = source.getScheduledMessagesAsList();

        if (messages.size() > 0) {
            Intent intent = new Intent(context, ScheduledMessageJob.class);
            PendingIntent pIntent = PendingIntent.getService(context, SCHEDULED_ALARM_REQUEST_CODE,
                    intent, PendingIntent.FLAG_UPDATE_CURRENT);

            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

            // cancel the current request if it exists, we'll just make a completely new one
            alarmManager.cancel(pIntent);
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, messages.get(0).timestamp, pIntent);

            Log.v("scheduled messsage", "new message scheduled");
        } else {
            Log.v("scheduled messsage", "no more scheduled messages");
        }
    }
}
