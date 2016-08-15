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
import android.util.Base64;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import javax.crypto.spec.SecretKeySpec;

import xyz.klinker.messenger.api.implementation.MessengerFirebaseMessagingService;
import xyz.klinker.messenger.data.DataSource;
import xyz.klinker.messenger.data.Settings;
import xyz.klinker.messenger.data.model.Blacklist;
import xyz.klinker.messenger.encryption.EncryptionUtils;

/**
 * Receiver responsible for processing firebase data messages and persisting to the database.
 */
public class FirebaseMessageReceiver extends BroadcastReceiver {

    private static final String TAG = "FirebaseMessageReceiver";

    private EncryptionUtils encryptionUtils;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!MessengerFirebaseMessagingService.ACTION_FIREBASE_MESSAGE_RECEIVED
                .equals(intent.getAction())) {
            return;
        }

        encryptionUtils = new EncryptionUtils(
                new SecretKeySpec(Base64.decode(Settings.get(context).key, Base64.DEFAULT), "AES"));

        String operation = intent.getStringExtra(MessengerFirebaseMessagingService.EXTRA_OPERATION);
        String data = intent.getStringExtra(MessengerFirebaseMessagingService.EXTRA_DATA);

        JSONObject json;
        try {
            json = new JSONObject(data);


            DataSource source = DataSource.getInstance(context);
            source.open();
            source.setUpload(false);

            switch (operation) {
                case "removed_account":
                    removeAccount(json, source, context);
                    break;
                case "added_blacklist":
                    addBlacklist(json, source);
                    break;
                case "removed_blacklist":
                    removeBlacklist(json, source);
                    break;
                case "removed_conversation":
                    removeConversation(json, source);
                    break;
                case "removed_drafts":
                    removeDrafts(json, source);
                    break;
                case "removed_message":
                    removeMessage(json, source);
                    break;
                case "removed_scheduled_message":
                    removeScheduledMessage(json, source);
                    break;
                default:
                    Log.e(TAG, "unsupported operation: " + operation);
                    break;
            }

            source.setUpload(true);
            source.close();
        } catch (JSONException e) {
            Log.e(TAG, "error parsing data json", e);
        }
    }

    private void removeAccount(JSONObject json, DataSource source, Context context)
            throws JSONException {
        Settings settings = Settings.get(context);

        if (json.getString("id").equals(settings.accountId)) {
            Log.v(TAG, "clearing account");
            source.clearTables();
            settings.removeValue("account_id");
            settings.removeValue("device_id");
        } else {
            Log.v(TAG, "ids do not match, did not clear account");
        }
    }

    private void addBlacklist(JSONObject json, DataSource source) throws JSONException {
        long id = json.getLong("id");
        String phoneNumber = json.getString("phone_number");
        phoneNumber = encryptionUtils.decrypt(phoneNumber);

        Blacklist blacklist = new Blacklist();
        blacklist.id = id;
        blacklist.phoneNumber = phoneNumber;
        source.insertBlacklist(blacklist);
        Log.v(TAG, "added blacklist");
    }

    private void removeBlacklist(JSONObject json, DataSource source) throws JSONException {
        long id = json.getLong("id");
        source.deleteBlacklist(id);
        Log.v(TAG, "removed blacklist");
    }

    private void removeConversation(JSONObject json, DataSource source) throws JSONException {
        long id = json.getLong("id");
        source.deleteConversation(id);
        Log.v(TAG, "removed conversation");
    }

    private void removeDrafts(JSONObject json, DataSource source) throws JSONException {
        long id = json.getLong("id");
        source.deleteDrafts(id);
        Log.v(TAG, "removed drafts");
    }

    private void removeMessage(JSONObject json, DataSource source) throws JSONException {
        long id = json.getLong("id");
        source.deleteMessage(id);
        Log.v(TAG, "removed message");
    }

    private void removeScheduledMessage(JSONObject json, DataSource source) throws JSONException {
        long id = json.getLong("id");
        source.deleteScheduledMessage(id);
        Log.v(TAG, "removed scheduled message");
    }

}
