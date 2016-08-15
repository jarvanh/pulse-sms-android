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

package xyz.klinker.messenger.api.implementation;

import android.content.Intent;
import android.util.Log;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

public class MessengerFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "FCMService";
    public static final String ACTION_FIREBASE_MESSAGE_RECEIVED =
            "xyz.klinker.messenger.api.implementation.MESSAGE_RECEIVED";
    public static final String EXTRA_OPERATION = "operation";
    public static final String EXTRA_DATA = "data";

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        Log.v(TAG, "received FCM message");

        // Check if message contains a data payload.
        if (remoteMessage.getData().size() > 0) {
            Map<String, String> payload = remoteMessage.getData();
            String operation = payload.get("operation");
            String data = payload.get("contents");

            Log.v(TAG, "operation: " + operation + ", contents: " + data);
            Intent intent = new Intent(ACTION_FIREBASE_MESSAGE_RECEIVED);
            intent.putExtra(EXTRA_OPERATION, operation);
            intent.putExtra(EXTRA_DATA, data);
            sendBroadcast(intent);
        }
    }

}
