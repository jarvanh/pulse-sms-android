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

package xyz.klinker.messenger.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import xyz.klinker.messenger.service.ContactSyncService;
import xyz.klinker.messenger.service.ContentObserverRunCheckService;
import xyz.klinker.messenger.service.ContentObserverService;
import xyz.klinker.messenger.service.ScheduledMessageService;

/**
 * Receiver for when boot has completed. This will be responsible for starting up the content
 * observer service and scheduling any messages that are to be sent in the future.
 */
public class BootCompletedReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
            context.startService(new Intent(context, ContentObserverService.class));
            context.startService(new Intent(context, ScheduledMessageService.class));

            ContentObserverRunCheckService.scheduleNextRun(context);
            ContactSyncService.scheduleNextRun(context);
        }
    }

}
