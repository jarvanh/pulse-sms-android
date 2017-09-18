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

package xyz.klinker.messenger.shared.service;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import xyz.klinker.messenger.api.implementation.Account;
import xyz.klinker.messenger.api.implementation.ApiUtils;
import xyz.klinker.messenger.shared.data.DataSource;

/**
 * A service to get run when a notification is dismissed.
 */
public class NotificationDismissedService extends IntentService {

    public static final String EXTRA_CONVERSATION_ID = "conversation_id";

    public NotificationDismissedService() {
        super("NotificationDismissedService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        handle(intent, this);
    }

    public static void handle(Intent intent, Context context) {
        NotificationService.cancelRepeats(context);

        long conversationId = intent.getLongExtra(EXTRA_CONVERSATION_ID, 0);

        DataSource source = DataSource.INSTANCE;

        if (conversationId == 0) {
            source.seenAllMessages(context);
        } else {
            source.seenConversation(context, conversationId);
        }

        Log.v("dismissed_notification", "id: " + conversationId);

        ApiUtils.INSTANCE.dismissNotification(Account.INSTANCE.getAccountId(), Account.INSTANCE.getDeviceId(), conversationId);
    }
}
