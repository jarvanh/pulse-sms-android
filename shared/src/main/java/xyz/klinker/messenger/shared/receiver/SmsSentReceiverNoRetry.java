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

package xyz.klinker.messenger.shared.receiver;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.Telephony;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.telephony.SmsManager;

import com.klinker.android.send_message.SentReceiver;
import com.klinker.android.send_message.StripAccents;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import xyz.klinker.messenger.shared.MessengerActivityExtras;
import xyz.klinker.messenger.shared.R;
import xyz.klinker.messenger.shared.data.ColorSet;
import xyz.klinker.messenger.shared.data.DataSource;
import xyz.klinker.messenger.shared.data.Settings;
import xyz.klinker.messenger.shared.data.model.Message;
import xyz.klinker.messenger.shared.service.ResendFailedMessage;
import xyz.klinker.messenger.shared.util.ActivityUtils;
import xyz.klinker.messenger.shared.util.NotificationUtils;
import xyz.klinker.messenger.shared.util.SmsMmsUtils;

/**
 * Receiver for getting notifications of when an SMS has finished sending. By default it's super
 * class will mark the internal message as sent, we need to also mark our database as sent.
 */
public class SmsSentReceiverNoRetry extends SmsSentReceiver {

    protected boolean retryFailedMessages() {
        return false;
    }
}
