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
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import java.util.ArrayList;
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
        if (conversations.size() == 0) {
            conversations = getFakeConversations(getResources());
        }

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

    public static List<Conversation> getFakeConversations(Resources resources) {
        List<Conversation> conversations = new ArrayList<>();

        Conversation conversation = new Conversation();
        conversation.title = "Luke Klinker";
        conversation.phoneNumbers = "(515) 991-1493";
        conversation.colors.color = resources.getColor(R.color.materialIndigo);
        conversation.colors.colorDark = resources.getColor(R.color.materialIndigoDark);
        conversation.colors.colorAccent = resources.getColor(R.color.materialGreenAccent);
        conversation.pinned = true;
        conversation.read = true;
        conversation.timestamp = System.currentTimeMillis() - (1000 * 60 * 60);
        conversation.snippet = "So maybe not going to be able to get platinum huh?";
        conversations.add(conversation);

        conversation = new Conversation();
        conversation.title = "Matt Swiontek";
        conversation.phoneNumbers = "(708) 928-0846";
        conversation.colors.color = resources.getColor(R.color.materialRed);
        conversation.colors.colorDark = resources.getColor(R.color.materialRedDark);
        conversation.colors.colorAccent = resources.getColor(R.color.materialBlueAccent);
        conversation.pinned = true;
        conversation.read = true;
        conversation.timestamp = System.currentTimeMillis() - (1000 * 60 * 60 * 12);
        conversation.snippet = "Whoops ya idk what happened but anysho drive safe";
        conversations.add(conversation);

        conversation = new Conversation();
        conversation.title = "Kris Klinker";
        conversation.phoneNumbers = "(515) 419-6726";
        conversation.colors.color = resources.getColor(R.color.materialPink);
        conversation.colors.colorDark = resources.getColor(R.color.materialPinkDark);
        conversation.colors.colorAccent = resources.getColor(R.color.materialOrangeAccent);
        conversation.pinned = false;
        conversation.read = false;
        conversation.timestamp = System.currentTimeMillis() - (1000 * 60 * 20);
        conversation.snippet = "Will probably be there from 6:30-9, just stop by when you can!";
        conversations.add(conversation);

        conversation = new Conversation();
        conversation.title = "Andrew Klinker";
        conversation.phoneNumbers = "(515) 991-8235";
        conversation.colors.color = resources.getColor(R.color.materialBlue);
        conversation.colors.colorDark = resources.getColor(R.color.materialBlueDark);
        conversation.colors.colorAccent = resources.getColor(R.color.materialRedAccent);
        conversation.pinned = false;
        conversation.read = true;
        conversation.timestamp = System.currentTimeMillis() - (1000 * 60 * 60 * 26);
        conversation.snippet = "Just finished, it was a lot of fun";
        conversations.add(conversation);

        conversation = new Conversation();
        conversation.title = "Aaron Klinker";
        conversation.phoneNumbers = "(515) 556-7749";
        conversation.colors.color = resources.getColor(R.color.materialGreen);
        conversation.colors.colorDark = resources.getColor(R.color.materialGreenDark);
        conversation.colors.colorAccent = resources.getColor(R.color.materialIndigoAccent);
        conversation.pinned = false;
        conversation.read = true;
        conversation.timestamp = System.currentTimeMillis() - (1000 * 60 * 60 * 32);
        conversation.snippet = "Yeah I'll do it when I get home";
        conversations.add(conversation);

        conversation = new Conversation();
        conversation.title = "Mike Klinker";
        conversation.phoneNumbers = "(515) 480-8532";
        conversation.colors.color = resources.getColor(R.color.materialBrown);
        conversation.colors.colorDark = resources.getColor(R.color.materialBrownDark);
        conversation.colors.colorAccent = resources.getColor(R.color.materialDeepOrangeAccent);
        conversation.pinned = false;
        conversation.read = true;
        conversation.timestamp = System.currentTimeMillis() - (1000 * 60 * 60 * 55);
        conversation.snippet = "Yeah so hiking around in some place called beaver meadows now.";
        conversations.add(conversation);

        conversation = new Conversation();
        conversation.title = "Ben Madden";
        conversation.phoneNumbers = "(847) 609-0939";
        conversation.colors.color = resources.getColor(R.color.materialPurple);
        conversation.colors.colorDark = resources.getColor(R.color.materialPurpleDark);
        conversation.colors.colorAccent = resources.getColor(R.color.materialTealAccent);
        conversation.pinned = false;
        conversation.read = true;
        conversation.timestamp = System.currentTimeMillis() - (1000 * 60 * 60 * 78);
        conversation.snippet = "Maybe they'll run into each other on the way back... idk";
        conversations.add(conversation);

        return conversations;
    }

}
