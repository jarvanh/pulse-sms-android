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

package xyz.klinker.messenger.api.implementation.firebase;

import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

import xyz.klinker.messenger.api.implementation.Account;

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
            final Intent handleMessage = new Intent(ACTION_FIREBASE_MESSAGE_RECEIVED);
            handleMessage.setComponent(new ComponentName("xyz.klinker.messenger",
                    "xyz.klinker.messenger" + ".service.FirebaseHandlerService"));
            handleMessage.putExtra(EXTRA_OPERATION, operation);
            handleMessage.putExtra(EXTRA_DATA, data);
            startService(handleMessage);
        }

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        if (!prefs.contains("subscribed_to_feature_flag")) {
            prefs.edit().putBoolean("subscribed_to_feature_flag", true).commit();
            FirebaseMessaging.getInstance().subscribeToTopic("feature_flag");
        }
    }

    @Override
    public void onDeletedMessages() {
        Log.v(TAG, "deleted FCM messages");

        if (!Account.get(this).primary) {
            final Intent handleMessage = new Intent(ACTION_FIREBASE_MESSAGE_RECEIVED);
            handleMessage.setComponent(new ComponentName("xyz.klinker.messenger",
                    "xyz.klinker.messenger" + ".service.FirebaseResetService"));
            startService(handleMessage);
        }
    }

}
