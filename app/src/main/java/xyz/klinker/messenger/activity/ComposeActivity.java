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

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.MultiAutoCompleteTextView;
import android.widget.TextView;

import com.android.ex.chips.BaseRecipientAdapter;
import com.android.ex.chips.RecipientEditTextView;
import com.android.ex.chips.RecipientEntry;
import com.android.ex.chips.recipientchip.DrawableRecipientChip;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import xyz.klinker.messenger.R;
import xyz.klinker.messenger.adapter.ContactAdapter;
import xyz.klinker.messenger.data.DataSource;
import xyz.klinker.messenger.data.MimeType;
import xyz.klinker.messenger.data.model.Conversation;
import xyz.klinker.messenger.data.model.Message;
import xyz.klinker.messenger.util.ContactUtils;
import xyz.klinker.messenger.util.PhoneNumberUtils;
import xyz.klinker.messenger.util.SendUtils;
import xyz.klinker.messenger.util.listener.ContactClickedListener;

/**
 * Activity to display UI for creating a new conversation.
 */
public class ComposeActivity extends AppCompatActivity implements ContactClickedListener {

    private FloatingActionButton fab;
    private RecipientEditTextView contactEntry;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_compose);

        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        setTitle(" ");

        contactEntry = (RecipientEditTextView) findViewById(R.id.contact_entry);
        contactEntry.setTokenizer(new MultiAutoCompleteTextView.CommaTokenizer());
        contactEntry.setAdapter(new BaseRecipientAdapter(BaseRecipientAdapter.QUERY_TYPE_PHONE, this));

        fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (contactEntry.getRecipients().length > 0) {
                    showConversation();
                }
            }
        });

        contactEntry.setOnEditorActionListener(new TextView.OnEditorActionListener() {

            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if ((event != null && (event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) ||
                        (actionId == EditorInfo.IME_ACTION_DONE)) {
                    fab.performClick();
                }

                return false;
            }
        });

        handleIntent();
        displayRecents();
    }

    private void displayRecents() {
        final Handler handler = new Handler();
        new Thread(new Runnable() {
            @Override
            public void run() {
                DataSource source = DataSource.getInstance(getApplicationContext());
                source.open();
                Cursor cursor = source.getConversations();

                if (cursor != null && cursor.moveToFirst()) {
                    final List<Conversation> conversations = new ArrayList<>();

                    do {
                        Conversation conversation = new Conversation();
                        conversation.fillFromCursor(cursor);
                        conversations.add(conversation);
                    } while (cursor.moveToNext());
                    cursor.close();

                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            ContactAdapter adapter = new ContactAdapter(conversations,
                                    ComposeActivity.this);
                            RecyclerView recyclerView = (RecyclerView)
                                    findViewById(R.id.recent_contacts);

                            recyclerView.setLayoutManager(
                                    new LinearLayoutManager(getApplicationContext()));
                            recyclerView.setAdapter(adapter);
                        }
                    });
                }
            }
        }).start();
    }

    private void showConversation() {
        String phoneNumbers = getPhoneNumberFromContactEntry();
        showConversation(phoneNumbers.toString());
    }

    private String getPhoneNumberFromContactEntry() {
        DrawableRecipientChip[] chips = contactEntry.getRecipients();
        StringBuilder phoneNumbers = new StringBuilder();

        for (int i = 0; i < chips.length; i++) {
            phoneNumbers.append(PhoneNumberUtils
                    .clearFormatting(chips[i].getEntry().getDestination()));
            if (i != chips.length - 1) {
                phoneNumbers.append(", ");
            }
        }

        return phoneNumbers.toString();
    }

    private void showConversation(String phoneNumbers) {
        DataSource source = DataSource.getInstance(this);
        source.open();
        Long conversationId = source.findConversationId(phoneNumbers);

        if (conversationId == null) {
            Message message = new Message();
            message.type = Message.TYPE_INFO;
            message.data = getString(R.string.no_messages_with_contact);
            message.timestamp = System.currentTimeMillis();
            message.mimeType = MimeType.TEXT_PLAIN;
            message.read = true;
            message.seen = true;

            conversationId = source.insertMessage(message, phoneNumbers, this);
        }

        source.close();

        Intent open = new Intent(this, MessengerActivity.class);
        open.putExtra(MessengerActivity.EXTRA_CONVERSATION_ID, conversationId);
        open.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(open);

        finish();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
        }

        return false;
    }

    private void handleIntent() {
        if (getIntent().getAction() == null) {
            return;
        }

        if (getIntent().getAction().equals(Intent.ACTION_SENDTO)) {
            String[] phoneNumbers = PhoneNumberUtils
                    .parseAddress(Uri.decode(getIntent().getDataString()));

            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < phoneNumbers.length; i++) {
                builder.append(phoneNumbers[i]);
                if (i != phoneNumbers.length - 1) {
                    builder.append(", ");
                }
            }

            showConversation(builder.toString());
        } else if (getIntent().getAction().equals(Intent.ACTION_SEND)) {
            final String mimeType = getIntent().getType();
            final String data;

            if (mimeType.equals(MimeType.TEXT_PLAIN)) {
                data = getIntent().getStringExtra(Intent.EXTRA_TEXT);
            } else {
                data = getIntent().getParcelableExtra(Intent.EXTRA_STREAM).toString();
            }

            fab.setImageResource(R.drawable.ic_send);
            fab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (contactEntry.getRecipients().length > 0) {
                        sendMessage(mimeType, data);
                    }
                }
            });
        }
    }

    private void sendMessage(String mimeType, String data) {
        String phoneNumbers = getPhoneNumberFromContactEntry();

        DataSource source = DataSource.getInstance(this);
        source.open();
        source.insertSentMessage(phoneNumbers, data, mimeType, this);

        if (mimeType.equals(MimeType.TEXT_PLAIN)) {
            SendUtils.send(this, data, phoneNumbers);
        } else {
            Uri imageUri = SendUtils.send(this, "", phoneNumbers, Uri.parse(data), mimeType);
            Cursor cursor = source.searchMessages(data);
            if (cursor != null && cursor.moveToFirst()) {
                source.updateMessageData(cursor.getLong(0), imageUri.toString());
                cursor.close();
            }
        }

        source.close();
        finish();
    }

    @Override
    public void onClicked(String title, String phoneNumber, String imageUri) {
        String[] names = title.split(", ");

        if (names.length == 1) {
            if (imageUri == null) {
                contactEntry.submitItem(title, phoneNumber);
            } else {
                contactEntry.submitItem(title, phoneNumber, Uri.parse(imageUri));
            }
        } else {
            String[] phoneNumbers = phoneNumber.split(", ");
            for (int i = 0; i < names.length; i++) {
                String name = names[i];
                String number = phoneNumbers[i];
                String image = ContactUtils.findImageUri(number, this) + "/photo";

                contactEntry.submitItem(name, number, Uri.parse(image));
            }
        }
    }

}
