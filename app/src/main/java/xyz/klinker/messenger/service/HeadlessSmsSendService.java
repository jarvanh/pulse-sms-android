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
import android.content.Intent;
import android.net.Uri;
import android.os.IBinder;
import android.support.annotation.Nullable;

import xyz.klinker.messenger.data.DataSource;
import xyz.klinker.messenger.data.MimeType;
import xyz.klinker.messenger.data.model.Conversation;
import xyz.klinker.messenger.util.PhoneNumberUtils;
import xyz.klinker.messenger.util.SendUtils;

/**
 * Service for sending messages to a conversation without a UI present. These messages could come
 * from something like Phone.
 */
public class HeadlessSmsSendService extends Service {

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            return START_NOT_STICKY;
        }

        String[] addresses = PhoneNumberUtils.parseAddress(Uri.decode(intent.getDataString()));
        String text = getText(intent);

        StringBuilder phoneNumbers = new StringBuilder();
        for (int i = 0; i < addresses.length; i++) {
            phoneNumbers.append(addresses[i]);
            if (i != addresses.length - 1) {
                phoneNumbers.append(", ");
            }
        }

        DataSource source = DataSource.getInstance(this);
        source.open();
        long conversationId = source.insertSentMessage(phoneNumbers.toString(), text, MimeType.TEXT_PLAIN, this);
        Conversation conversation = source.getConversation(conversationId);
        source.close();

        new SendUtils(conversation.subscriptionIdForSim)
                .send(this, text, addresses);

        return super.onStartCommand(intent, flags, startId);
    }

    private String getText(Intent intent) {
        CharSequence text = intent.getCharSequenceExtra(Intent.EXTRA_TEXT);
        if (text == null) {
            return intent.getStringExtra(Intent.EXTRA_TEXT);
        } else {
            return text.toString();
        }
    }

}
