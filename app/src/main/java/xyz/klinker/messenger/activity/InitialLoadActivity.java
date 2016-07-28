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

package xyz.klinker.messenger.activity;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.ContactsContract;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.ProgressBar;

import com.klinker.android.send_message.Utils;

import java.util.List;

import xyz.klinker.messenger.R;
import xyz.klinker.messenger.data.DataSource;
import xyz.klinker.messenger.data.Settings;
import xyz.klinker.messenger.data.model.Conversation;
import xyz.klinker.messenger.util.PermissionsUtils;
import xyz.klinker.messenger.util.PhoneNumberUtils;
import xyz.klinker.messenger.util.SmsMmsUtils;
import xyz.klinker.messenger.util.listener.ProgressUpdateListener;

/**
 * Activity for onboarding and initial database load.
 */
public class InitialLoadActivity extends AppCompatActivity implements ProgressUpdateListener {

    private Handler handler;
    private ProgressBar progress;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_initial_load);

        handler = new Handler();
        requestPermissions();

        progress = (ProgressBar) findViewById(R.id.loading_progress);
    }

    private void requestPermissions() {
        if (PermissionsUtils.checkRequestMainPermissions(this)) {
            PermissionsUtils.startMainPermissionRequest(this);
        } else {
            startDatabaseSync();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        if (PermissionsUtils.processPermissionRequest(this, requestCode, permissions, grantResults)) {
            startDatabaseSync();
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    private void startDatabaseSync() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                final Context context = getApplicationContext();
                long startTime = System.currentTimeMillis();

                String myName = getName();
                String myPhoneNumber = PhoneNumberUtils.format(getPhoneNumber());

                final Settings settings = Settings.get(context);
                settings.setValue(getString(R.string.pref_my_name), myName);
                settings.setValue(getString(R.string.pref_my_phone_number), myPhoneNumber);

                List<Conversation> conversations = SmsMmsUtils.queryConversations(context);

                DataSource source = DataSource.getInstance(context);
                source.open();
                source.insertConversations(conversations, context, InitialLoadActivity.this);
                source.close();

                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        settings.setValue(getString(R.string.pref_first_start), false);
                        startActivity(new Intent(context, MessengerActivity.class));
                        finish();
                    }
                });

                Log.v("initial_load", "load took " +
                        (System.currentTimeMillis() - startTime) + " ms");
            }
        }).start();
    }

    private String getName() {
        Cursor cursor = getContentResolver()
                .query(ContactsContract.Profile.CONTENT_URI, null, null, null, null);

        if (cursor != null && cursor.moveToFirst()) {
            cursor.moveToFirst();
            String name = cursor.getString(cursor.getColumnIndex("display_name"));
            cursor.close();
            return name;
        }

        return null;
    }

    private String getPhoneNumber() {
        return PhoneNumberUtils.clearFormatting(Utils.getMyPhoneNumber(this));
    }

    @Override
    public void onProgressUpdate(final int current, final int max) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                progress.setIndeterminate(false);
                progress.setMax(max);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    progress.setProgress(current, true);
                } else {
                    progress.setProgress(current);
                }
            }
        });
    }

}
