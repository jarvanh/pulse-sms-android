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

package xyz.klinker.messenger.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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
import xyz.klinker.messenger.api.implementation.ActivateActivity;
import xyz.klinker.messenger.api.implementation.LoginActivity;
import xyz.klinker.messenger.api.implementation.Account;
import xyz.klinker.messenger.shared.data.DataSource;
import xyz.klinker.messenger.shared.data.Settings;
import xyz.klinker.messenger.shared.data.model.Contact;
import xyz.klinker.messenger.shared.data.model.Conversation;
import xyz.klinker.messenger.shared.service.ApiDownloadService;
import xyz.klinker.messenger.shared.service.ApiUploadService;
import xyz.klinker.messenger.api.implementation.firebase.AnalyticsHelper;
import xyz.klinker.messenger.shared.util.AndroidVersionUtil;
import xyz.klinker.messenger.shared.util.ContactUtils;
import xyz.klinker.messenger.shared.util.PermissionsUtils;
import xyz.klinker.messenger.shared.util.PhoneNumberUtils;
import xyz.klinker.messenger.shared.util.SmsMmsUtils;
import xyz.klinker.messenger.shared.util.TvUtils;
import xyz.klinker.messenger.shared.util.listener.ProgressUpdateListener;

/**
 * Activity for onboarding and initial database load.
 */
public class InitialLoadActivity extends AppCompatActivity implements ProgressUpdateListener {

    public static final String UPLOAD_AFTER_SYNC = "upload_after_sync";
    private static final int SETUP_REQUEST = 54321;

    private Handler handler;
    private ProgressBar progress;
    private boolean startUploadAfterSync = false;
    private BroadcastReceiver downloadReceiver;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_initial_load);

        if (getIntent().getBooleanExtra(UPLOAD_AFTER_SYNC, false)) {
            startUploadAfterSync = true;
        }


        handler = new Handler();
        requestPermissions();

        progress = (ProgressBar) findViewById(R.id.loading_progress);
    }

    private void requestPermissions() {
        if (PermissionsUtils.INSTANCE.checkRequestMainPermissions(this)) {
            PermissionsUtils.INSTANCE.startMainPermissionRequest(this);
        } else {
            startLogin();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        try {
            if (PermissionsUtils.INSTANCE.processPermissionRequest(this, requestCode, permissions, grantResults)) {
                startLogin();
            } else {
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int responseCode, Intent data) {
        if (requestCode == SETUP_REQUEST) {
            Settings.INSTANCE.forceUpdate(this);

            if (responseCode == RESULT_CANCELED) {
                if (!startUploadAfterSync) {
                    Account account = Account.INSTANCE;
                    account.setDeviceId(this, null);
                    account.setPrimary(this,true);
                }

                startDatabaseSync();
            } else if (responseCode == LoginActivity.RESULT_START_DEVICE_SYNC) {
                startDatabaseSync();
                startUploadAfterSync = true;
            } else if (responseCode == LoginActivity.RESULT_START_NETWORK_SYNC) {
                ApiDownloadService.start(this);
                downloadReceiver = new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        close();
                    }
                };

                registerReceiver(downloadReceiver,
                        new IntentFilter(ApiDownloadService.ACTION_DOWNLOAD_FINISHED));
            } else if (responseCode == ActivateActivity.RESULT_FAILED) {
                finish();
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (downloadReceiver != null) {
            unregisterReceiver(downloadReceiver);
            downloadReceiver = null;
        }
    }

    private void startLogin() {
        // we want to pass the extras from the last intent to this one, since they will tell us if
        // we should automatically skip the login and just go into the data load.
        Intent login = new Intent(this, LoginActivity.class);
        login.putExtras(getIntent());

        startActivityForResult(login, SETUP_REQUEST);
    }

    private void startDatabaseSync() {
        new Thread(() -> {
            final Context context = getApplicationContext();
            long startTime = System.currentTimeMillis();

            String myName = getName();
            String myPhoneNumber = PhoneNumberUtils.INSTANCE.format(getPhoneNumber());

            final Account account = Account.INSTANCE;
            account.setName(this, myName);
            account.setPhoneNumber(this, myPhoneNumber);

            DataSource source = DataSource.INSTANCE;

            List<Conversation> conversations = SmsMmsUtils.queryConversations(context);
            try {
                source.insertConversations(conversations, context, InitialLoadActivity.this);
            } catch (Exception e) {
                source.ensureActionable(this);
                source.insertConversations(conversations, context, InitialLoadActivity.this);
            }

            handler.post(() -> progress.setIndeterminate(true));

            List<Contact> contacts = ContactUtils.queryContacts(context, source);
            source.insertContacts(this, contacts, null);

            long importTime = (System.currentTimeMillis() - startTime);
            AnalyticsHelper.importFinished(this, importTime);
            Log.v("initial_load", "load took " + importTime + " ms");

            handler.postDelayed(this::close, 5000);
        }).start();
    }

    private void close() {
        Settings.INSTANCE.setValue(this, getString(R.string.pref_first_start), false);

        if (TvUtils.INSTANCE.hasTouchscreen(this)) {
            startActivity(new Intent(this, MessengerActivity.class));
        } else {
            startActivity(new Intent(this, MessengerTvActivity.class));
        }

        if (startUploadAfterSync) {
            ApiUploadService.start(this);
        }

        finish();
    }

    private String getName() {
        try {
            Cursor cursor = getContentResolver()
                    .query(ContactsContract.Profile.CONTENT_URI, null, null, null, null);

            if (cursor != null && cursor.moveToFirst()) {
                cursor.moveToFirst();
                String name = cursor.getString(cursor.getColumnIndex("display_name"));
                cursor.close();
                return name;
            } else {
                try {
                    cursor.close();
                } catch (Exception e) { }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return "";
    }

    private String getPhoneNumber() {
        try {
            return PhoneNumberUtils.INSTANCE.clearFormatting(Utils.getMyPhoneNumber(this));
        } catch (Exception e) {
            return "";
        }
    }

    @Override
    public void onProgressUpdate(final int current, final int max) {
        handler.post(() -> {
            progress.setIndeterminate(false);
            progress.setMax(max);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                progress.setProgress(current, true);
            } else {
                progress.setProgress(current);
            }
        });
    }

    @Override
    public void onBackPressed() {
        // don't let them back out of this
    }

}
