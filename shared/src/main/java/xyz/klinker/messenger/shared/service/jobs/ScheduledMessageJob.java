/*
 * Copyright (C) 2017 Luke Klinker
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
import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.text.Html;
import android.util.Log;

import java.util.Date;
import java.util.List;

import xyz.klinker.messenger.shared.R;
import xyz.klinker.messenger.api.implementation.Account;
import xyz.klinker.messenger.shared.data.ColorSet;
import xyz.klinker.messenger.shared.data.DataSource;
import xyz.klinker.messenger.shared.data.model.Conversation;
import xyz.klinker.messenger.shared.data.model.ScheduledMessage;
import xyz.klinker.messenger.shared.util.ActivityUtils;
import xyz.klinker.messenger.shared.util.CursorUtil;
import xyz.klinker.messenger.shared.util.SendUtils;
import xyz.klinker.messenger.shared.util.TimeUtils;

/**
 * Service responsible for sending scheduled message that are coming up, removing that message
 * from the database and then scheduling the next one.
 */
public class ScheduledMessageJob extends BackgroundJob {

    public static final String BROADCAST_SCHEDULED_SENT = "xyz.klinker.messenger.SENT_SCHEDULED_MESSAGE";
    private static final int JOB_ID = 5424;

    @Override
    protected void onRunJob(JobParameters parameters) {
        DataSource source = DataSource.INSTANCE;
        Cursor messages = source.getScheduledMessages(this);

        if (messages.moveToFirst()) {
            do {
                long timestamp = messages.getLong(
                        messages.getColumnIndex(ScheduledMessage.Companion.getCOLUMN_TIMESTAMP()));

                // if message scheduled to be sent in less than 5 mins in the future,
                // or more than 60 in the past
                if (timestamp > System.currentTimeMillis() - (TimeUtils.HOUR) &&
                        timestamp < System.currentTimeMillis() + (TimeUtils.MINUTE * 5)) {
                    ScheduledMessage message = new ScheduledMessage();
                    message.fillFromCursor(messages);

                    // delete, insert and send
                    source.deleteScheduledMessage(this, message.getId());
                    long conversationId = source.insertSentMessage(message.getTo(), message.getData(), message.getMimeType(), this);
                    Conversation conversation = source.getConversation(this, conversationId);

                    new SendUtils(conversation != null ? conversation.getSimSubscriptionId() : null)
                            .send(this, message.getData(), message.getTo());

                    // display a notification
                    String body = "<b>" + message.getTitle() + ": </b>" + message.getData();
                    Intent open = ActivityUtils.INSTANCE.buildForComponent(ActivityUtils.INSTANCE.getMESSENGER_ACTIVITY());
                    open.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    PendingIntent pendingOpen = PendingIntent.getActivity(this, 0, open,
                            PendingIntent.FLAG_UPDATE_CURRENT);

                    Notification notification = new NotificationCompat.Builder(this)
                            .setSmallIcon(R.drawable.ic_stat_notify)
                            .setContentTitle(getString(R.string.scheduled_message_sent))
                            .setContentText(Html.fromHtml(body))
                            .setColor(ColorSet.Companion.DEFAULT(this).getColor())
                            .setAutoCancel(true)
                            .setContentIntent(pendingOpen)
                            .build();
                    NotificationManagerCompat.from(this)
                            .notify(5555 + (int) message.getId(), notification);

                    Log.v("scheduled message", "message was sent and notification given");
                }
            } while (messages.moveToNext());
        }

        CursorUtil.closeSilent(messages);

        sendBroadcast(new Intent(BROADCAST_SCHEDULED_SENT));
        scheduleNextRun(this);
    }

    public static void scheduleNextRun(Context context) {
        scheduleNextRun(context, DataSource.INSTANCE);
    }

    public static void scheduleNextRun(Context context, DataSource source) {
        Account account = Account.INSTANCE;
        if (account.exists() && !account.getPrimary()) {
            // if they have an online account, we only want scheduled messages to go through the phone
            return;
        }

        List<ScheduledMessage> messages = source.getScheduledMessagesAsList(context);

        if (messages.size() > 0) {
            long timeout = messages.get(0).getTimestamp() - new Date().getTime();

            ComponentName component = new ComponentName(context, ScheduledMessageJob.class);
            JobInfo.Builder builder = new JobInfo.Builder(JOB_ID, component)
                    .setMinimumLatency(timeout)
                    .setOverrideDeadline(timeout + TimeUtils.MINUTE)
                    .setRequiresCharging(false)
                    .setRequiresDeviceIdle(false);

            JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
            jobScheduler.schedule(builder.build());

            Log.v("scheduled message", "new message scheduled");
        } else {
            Log.v("scheduled message", "no more scheduled messages");
        }
    }
}
