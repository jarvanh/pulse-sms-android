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

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import java.util.List;

import xyz.klinker.messenger.R;
import xyz.klinker.messenger.data.DataSource;
import xyz.klinker.messenger.data.Settings;
import xyz.klinker.messenger.data.model.Conversation;
import xyz.klinker.messenger.util.SmsMmsUtil;

/**
 * Activity for onboarding and initial database load.
 */
public class InitialLoadActivity extends AppCompatActivity {

    private static final int REQUEST_PERMISSIONS = 1;

    private Handler handler;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        handler = new Handler();
        requestPermissions();
    }

    private void requestPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS)
                != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS)
                    != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_CONTACTS, Manifest.permission.READ_SMS},
                    REQUEST_PERMISSIONS);
        } else {
            startDatabaseSync();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[],
                                           int[] grantResults) {
        if (requestCode == REQUEST_PERMISSIONS) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED
                    && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                startDatabaseSync();
            } else {
                new AlertDialog.Builder(this)
                        .setMessage(R.string.permissions_needed)
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                requestPermissions();
                            }
                        })
                        .show();
            }
        }
    }

    private void startDatabaseSync() {
        long startTime = System.currentTimeMillis();

        List<Conversation> conversations = SmsMmsUtil.queryConversations(this);

        DataSource source = DataSource.getInstance(this);
        source.open();
        source.beginTransaction();

        source.writeConversations(conversations);

        source.setTransactionSuccessful();
        source.endTransaction();
        source.close();

        Settings.getPrefs(this).edit().putBoolean(Settings.FIRST_START, false).apply();
        startActivity(new Intent(this, MessengerActivity.class));
        finish();

        Log.v("initial_load", "load took " + (System.currentTimeMillis() - startTime) + " ms");
    }

}
