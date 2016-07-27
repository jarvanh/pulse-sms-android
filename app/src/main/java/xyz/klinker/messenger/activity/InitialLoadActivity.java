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
import android.content.res.Resources;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.ContactsContract;
import android.provider.Telephony;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.ProgressBar;

import com.klinker.android.send_message.Utils;

import java.util.ArrayList;
import java.util.List;

import xyz.klinker.messenger.R;
import xyz.klinker.messenger.data.DataSource;
import xyz.klinker.messenger.data.Settings;
import xyz.klinker.messenger.data.model.Conversation;
import xyz.klinker.messenger.util.PermissionsUtil;
import xyz.klinker.messenger.util.PhoneNumberUtil;
import xyz.klinker.messenger.util.SmsMmsUtil;
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
        if (PermissionsUtil.checkRequestMainPermissions(this)) {
            PermissionsUtil.startMainPermissionRequest(this);
        } else {
            startDatabaseSync();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        if (PermissionsUtil.processPermissionRequest(this, requestCode, permissions, grantResults)) {
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
                String myPhoneNumber = PhoneNumberUtil.format(getPhoneNumber());

                final Settings settings = Settings.get(context);
                settings.setValue(Settings.MY_NAME, myName);
                settings.setValue(Settings.MY_PHONE_NUMBER, myPhoneNumber);

                List<Conversation> conversations = SmsMmsUtil.queryConversations(context);
                if (conversations.size() == 0) {
                    conversations = getFakeConversations(getResources());
                }

                DataSource source = DataSource.getInstance(context);
                source.open();
                source.insertConversations(conversations, context, InitialLoadActivity.this);
                source.close();

                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        settings.setValue(Settings.FIRST_START, false);
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
        return PhoneNumberUtil.clearFormatting(Utils.getMyPhoneNumber(this));
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
        conversation.idMatcher = "11493";
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
        conversation.idMatcher = "80846";
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
        conversation.idMatcher = "96726";
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
        conversation.idMatcher = "18235";
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
        conversation.idMatcher = "67749";
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
        conversation.idMatcher = "08532";
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
        conversation.idMatcher = "90939";
        conversations.add(conversation);

        return conversations;
    }

    public static Cursor getFakeMessages() {
        MatrixCursor cursor = new MatrixCursor(new String[] {
                Telephony.MmsSms._ID,
                Telephony.Sms.BODY,
                Telephony.Sms.DATE,
                Telephony.Sms.READ,
                Telephony.Sms.TYPE,
                Telephony.Mms.MESSAGE_BOX,
                Telephony.Mms.MESSAGE_TYPE,
                Telephony.Sms.STATUS
        });

        cursor.addRow(new Object[] {
                1,
                "Do you want to go to summerfest this weekend?",
                System.currentTimeMillis() - (1000 * 60 * 60 * 12) - (1000 * 60 * 30),
                1,
                Telephony.Sms.MESSAGE_TYPE_SENT,
                null,
                null,
                null
        });

        cursor.addRow(new Object[] {
                2,
                "Yeah, I'll probably go on Friday.",
                System.currentTimeMillis() - (1000 * 60 * 60 * 12),
                1,
                Telephony.Sms.MESSAGE_TYPE_INBOX,
                null,
                null,
                null
        });

        cursor.addRow(new Object[] {
                3,
                "I started working on the designs for a new messaging app today... I'm thinking " +
                        "that it could be somewhere along the lines of a compliment to Evolve. " +
                        "The main app will be focused on tablet design and so Evolve could " +
                        "support hooking up to the same backend and the two could be used " +
                        "together. Or, users could just use this app on their phone as well... " +
                        "up to them which they prefer.",
                System.currentTimeMillis() - (1000 * 60 * 60 * 8) - (1000 * 60 * 6),
                1,
                Telephony.Sms.MESSAGE_TYPE_SENT,
                null,
                null,
                null
        });

        cursor.addRow(new Object[] {
                4,
                "Are you going to make this into an actual app?",
                System.currentTimeMillis() - (1000 * 60 * 60 * 8),
                1,
                Telephony.Sms.MESSAGE_TYPE_INBOX,
                null,
                null,
                null
        });

        cursor.addRow(new Object[] {
                5,
                "dunno",
                System.currentTimeMillis() - (1000 * 60 * 60 * 7) - (1000 * 60 * 55),
                1,
                Telephony.Sms.MESSAGE_TYPE_SENT,
                null,
                null,
                null
        });

        cursor.addRow(new Object[] {
                6,
                "I got to build some Legos, plus get 5 extra character packs and 3 level packs " +
                        "with the deluxe edition lol",
                System.currentTimeMillis() - (1000 * 60 * 38),
                1,
                Telephony.Sms.MESSAGE_TYPE_SENT,
                null,
                null,
                null
        });

        cursor.addRow(new Object[] {
                7,
                "woah nice one haha",
                System.currentTimeMillis() - (1000 * 60 * 37),
                1,
                Telephony.Sms.MESSAGE_TYPE_INBOX,
                null,
                null,
                null
        });

        cursor.addRow(new Object[] {
                8,
                "Already shaping up to be a better deal than battlefront!",
                System.currentTimeMillis() - (1000 * 60 * 23),
                1,
                Telephony.Sms.MESSAGE_TYPE_SENT,
                null,
                null,
                null
        });

        cursor.addRow(new Object[] {
                9,
                "is it fun?",
                System.currentTimeMillis() - (1000 * 60 * 22),
                1,
                Telephony.Sms.MESSAGE_TYPE_INBOX,
                null,
                null,
                null
        });

        cursor.addRow(new Object[] {
                10,
                "So far! Looks like a lot of content in the game too. Based on the trophies " +
                        "required at least",
                System.currentTimeMillis() - (1000 * 60 * 20),
                1,
                Telephony.Sms.MESSAGE_TYPE_SENT,
                null,
                null,
                null
        });

        cursor.addRow(new Object[] {
                11,
                "so maybe not going to be able to get platinum huh? haha",
                System.currentTimeMillis() - (1000 * 60 * 16),
                1,
                Telephony.Sms.MESSAGE_TYPE_INBOX,
                null,
                null,
                null
        });

        cursor.addRow(new Object[] {
                12,
                "Oh, I will definitely get it! Just might take 24+ hours to do it... and when " +
                        "those 24 hours are in a single week, things get to be a little tedious. " +
                        "Hopefully I don't absolutely hate the game once I finish!",
                System.currentTimeMillis() - (1000 * 60),
                1,
                Telephony.Sms.MESSAGE_TYPE_SENT,
                null,
                null,
                null
        });

        return cursor;
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
