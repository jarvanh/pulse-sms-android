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

import android.content.Intent;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.MultiAutoCompleteTextView;
import android.widget.TextView;

import com.android.ex.chips.BaseRecipientAdapter;
import com.android.ex.chips.RecipientEditTextView;
import com.android.ex.chips.recipientchip.DrawableRecipientChip;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

import xyz.klinker.messenger.R;
import xyz.klinker.messenger.adapter.ContactAdapter;
import xyz.klinker.messenger.api.implementation.ApiUtils;
import xyz.klinker.messenger.data.DataSource;
import xyz.klinker.messenger.data.MimeType;
import xyz.klinker.messenger.data.Settings;
import xyz.klinker.messenger.data.model.Conversation;
import xyz.klinker.messenger.data.model.Message;
import xyz.klinker.messenger.service.MessengerChooserTargetService;
import xyz.klinker.messenger.util.ColorUtils;
import xyz.klinker.messenger.util.ContactUtils;
import xyz.klinker.messenger.util.ImageUtils;
import xyz.klinker.messenger.util.PhoneNumberUtils;
import xyz.klinker.messenger.util.SendUtils;
import xyz.klinker.messenger.util.listener.ContactClickedListener;

/**
 * Activity to display UI for creating a new conversation.
 */
public class ComposeActivity extends AppCompatActivity implements ContactClickedListener {

    private static final String TAG = "ComposeActivity";

    private FloatingActionButton fab;
    private RecipientEditTextView contactEntry;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_compose);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        setTitle(" ");

        contactEntry = (RecipientEditTextView) findViewById(R.id.contact_entry);
        contactEntry.setTokenizer(new MultiAutoCompleteTextView.CommaTokenizer());
        BaseRecipientAdapter adapter =
                new BaseRecipientAdapter(BaseRecipientAdapter.QUERY_TYPE_PHONE, this);
        adapter.setShowMobileOnly(Settings.get(this).mobileOnly);
        contactEntry.setAdapter(adapter);

        fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (contactEntry.getText().length() > 0) {
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

        Settings settings = Settings.get(this);
        if (settings.useGlobalThemeColor) {
            toolbar.setBackgroundColor(settings.globalColorSet.color);
            getWindow().setStatusBarColor(settings.globalColorSet.colorDark);
            fab.setBackgroundTintList(ColorStateList.valueOf(settings.globalColorSet.colorAccent));
            contactEntry.setHighlightColor(settings.globalColorSet.colorAccent);
            ColorUtils.setCursorDrawableColor(contactEntry, settings.globalColorSet.colorAccent);
            ColorUtils.updateRecentsEntry(this);
        }

        ColorUtils.checkBlackBackground(this);
    }

    private void displayRecents() {
        final Handler handler = new Handler();
        new Thread(new Runnable() {
            @Override
            public void run() {
                DataSource source = DataSource.getInstance(getApplicationContext());
                source.open();
                Cursor cursor = source.getUnarchivedConversations();

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
        showConversation(phoneNumbers);
    }

    private String getPhoneNumberFromContactEntry() {
        DrawableRecipientChip[] chips = contactEntry.getRecipients();
        StringBuilder phoneNumbers = new StringBuilder();

        if (chips.length > 0) {
            for (int i = 0; i < chips.length; i++) {
                phoneNumbers.append(PhoneNumberUtils
                        .clearFormatting(chips[i].getEntry().getDestination()));
                if (i != chips.length - 1) {
                    phoneNumbers.append(", ");
                }
            }
        } else {
            phoneNumbers.append(contactEntry.getText().toString());
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
        } else {
            source.unarchiveConversation(conversationId);
        }

        source.close();

        Intent open = new Intent(this, MessengerActivity.class);
        open.putExtra(MessengerActivity.EXTRA_CONVERSATION_ID, conversationId);
        open.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        if (contactEntry.getRecipients().length == 1) {
            String name = contactEntry.getRecipients()[0].getEntry().getDisplayName();
            open.putExtra(MessengerActivity.EXTRA_CONVERSATION_NAME, name);
        }

        startActivity(open);

        finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_compose, menu);
        Settings settings = Settings.get(this);

        MenuItem item = menu.findItem(R.id.menu_mobile_only);
        item.setChecked(settings.mobileOnly);

        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            case R.id.menu_mobile_only:
                boolean newValue = !item.isChecked();
                Settings settings = Settings.get(this);

                item.setChecked(newValue);

                settings.setValue(getString(R.string.pref_mobile_only), newValue);
                settings.forceUpdate();

                contactEntry.getAdapter().setShowMobileOnly(item.isChecked());
                contactEntry.getAdapter().notifyDataSetChanged();

                new ApiUtils().updateMobileOnly(settings.accountId, newValue);

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
            String data;

            if (mimeType.equals(MimeType.TEXT_PLAIN)) {
                data = getIntent().getStringExtra(Intent.EXTRA_TEXT);
            } else if (MimeType.isVcard(mimeType)) {
                data = getIntent().getParcelableExtra(Intent.EXTRA_STREAM).toString();
                Log.v(TAG, "got vcard at: " + data);
            } else {
                String tempData = getIntent().getParcelableExtra(Intent.EXTRA_STREAM).toString();
                try {
                    File dst = new File(getFilesDir(),
                            ((int) (Math.random() * Integer.MAX_VALUE)) + ".jpg");
                    Bitmap bmp = ImageUtils.getBitmap(this, tempData);
                    FileOutputStream stream = new FileOutputStream(dst);
                    bmp.compress(Bitmap.CompressFormat.JPEG, 95, stream);
                    stream.close();
                    data = Uri.fromFile(dst).toString();
                } catch (Exception e) {
                    e.printStackTrace();
                    data = tempData;
                }
            }

            setupSend(data, mimeType);

            if (getIntent().getExtras()
                    .containsKey(MessengerChooserTargetService.EXTRA_PHONE_NUMBERS)) {
                String numbers = getIntent()
                        .getStringExtra(MessengerChooserTargetService.EXTRA_PHONE_NUMBERS);
                applyShare(mimeType, data, numbers);
            }
        } else if (getIntent().getAction().equals(Intent.ACTION_VIEW)) {
            Bundle extras = getIntent().getExtras();
            if (extras.containsKey("sms_body")) {
                setupSend(extras.getString("sms_body"), MimeType.TEXT_PLAIN);
            }
        }
    }

    private void setupSend(final String data, final String mimeType) {
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (contactEntry.getRecipients().length > 0) {
                    applyShare(mimeType, data);
                }
            }
        });
    }

    private void applyShare(String mimeType, String data) {
        String phoneNumbers = getPhoneNumberFromContactEntry();
        applyShare(mimeType, data, phoneNumbers);
    }

    private void applyShare(String mimeType, String data, String phoneNumbers) {
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

        source.insertDraft(conversationId, data, mimeType);
        source.close();

        showConversation();
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
