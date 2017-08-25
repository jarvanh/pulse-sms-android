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
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.graphics.Color;
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
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.MultiAutoCompleteTextView;

import com.android.ex.chips.BaseRecipientAdapter;
import com.android.ex.chips.RecipientEditTextView;
import com.android.ex.chips.recipientchip.DrawableRecipientChip;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;

import xyz.klinker.messenger.R;
import xyz.klinker.messenger.adapter.ContactAdapter;
import xyz.klinker.messenger.api.implementation.Account;
import xyz.klinker.messenger.api.implementation.ApiUtils;
import xyz.klinker.messenger.shared.MessengerActivityExtras;
import xyz.klinker.messenger.shared.data.DataSource;
import xyz.klinker.messenger.shared.data.MimeType;
import xyz.klinker.messenger.shared.data.Settings;
import xyz.klinker.messenger.shared.data.model.Conversation;
import xyz.klinker.messenger.shared.data.model.Message;
import xyz.klinker.messenger.shared.service.MessengerChooserTargetService;
import xyz.klinker.messenger.shared.util.ActivityUtils;
import xyz.klinker.messenger.shared.util.ColorUtils;
import xyz.klinker.messenger.shared.util.ContactUtils;
import xyz.klinker.messenger.shared.util.NonStandardUriUtils;
import xyz.klinker.messenger.shared.util.PhoneNumberUtils;
import xyz.klinker.messenger.shared.util.SendUtils;
import xyz.klinker.messenger.shared.util.listener.ContactClickedListener;

/**
 * Activity to display UI for creating a new conversation.
 */
public class ComposeActivity extends AppCompatActivity implements ContactClickedListener {

    private static final String TAG = "ComposeActivity";
    public static final String ACTION_EDIT_RECIPIENTS = "ACTION_EDIT_RECIPIENTS";
    public static final String EXTRA_EDIT_RECIPIENTS_TITLE = "extra_edit_title";
    public static final String EXTRA_EDIT_RECIPIENTS_NUMBERS = "extra_edit_numbers";

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
        fab.setOnClickListener(view -> {
            dismissKeyboard();

            new Handler().postDelayed(() -> {
                if (contactEntry.getText().length() > 0) {
                    showConversation();
                }
            }, 100);
        });

        contactEntry.setOnEditorActionListener((v, actionId, event) -> {
            if ((event != null && (event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) ||
                    (actionId == EditorInfo.IME_ACTION_DONE)) {
                fab.performClick();
            }

            return false;
        });

        handleIntent();
        displayRecents();

        Settings settings = Settings.get(this);
        findViewById(R.id.toolbar_holder).setBackgroundColor(settings.mainColorSet.color);
        toolbar.setBackgroundColor(settings.mainColorSet.color);
        ActivityUtils.setStatusBarColor(this, settings.mainColorSet.colorDark);
        fab.setBackgroundTintList(ColorStateList.valueOf(settings.mainColorSet.colorAccent));
        contactEntry.setHighlightColor(settings.mainColorSet.colorAccent);
        ColorUtils.setCursorDrawableColor(contactEntry, settings.mainColorSet.colorAccent);

        ActivityUtils.setTaskDescription(this);
        ColorUtils.checkBlackBackground(this);

        if (!ColorUtils.isColorDark(settings.mainColorSet.color)) {
            contactEntry.setTextColor(ColorStateList.valueOf(getResources().getColor(R.color.lightToolbarTextColor)));
            contactEntry.setHintTextColor(ColorStateList.valueOf(getResources().getColor(R.color.lightToolbarTextColor)));
        }

        contactEntry.requestFocus();
    }

    private void displayRecents() {
        final Handler handler = new Handler();
        new Thread(() -> {
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

                handler.post(() -> {
                    ContactAdapter adapter = new ContactAdapter(conversations,
                            ComposeActivity.this);
                    RecyclerView recyclerView = (RecyclerView)
                            findViewById(R.id.recent_contacts);

                    recyclerView.setLayoutManager(
                            new LinearLayoutManager(getApplicationContext()));
                    recyclerView.setAdapter(adapter);
                });
            }

            try {
                cursor.close();
            } catch (Exception e) { }

            source.close();
        }).start();
    }

    private void dismissKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(contactEntry.getWindowToken(), 0);
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

        if (conversationId == null && contactEntry.getRecipients().length == 1) {
            conversationId = source.findConversationIdByTitle(
                    contactEntry.getRecipients()[0].getEntry().getDisplayName());
        }

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
        open.putExtra(MessengerActivityExtras.INSTANCE.getEXTRA_CONVERSATION_ID(), conversationId);
        open.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        if (contactEntry.getRecipients().length == 1) {
            String name = contactEntry.getRecipients()[0].getEntry().getDisplayName();
            open.putExtra(MessengerActivityExtras.INSTANCE.getEXTRA_CONVERSATION_NAME(), name);
        }

        startActivity(open);
        finish();
    }

    private void showConversation(long conversationId) {
        Intent open = new Intent(this, MessengerActivity.class);
        open.putExtra(MessengerActivityExtras.INSTANCE.getEXTRA_CONVERSATION_ID(), conversationId);
        open.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

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

                settings.setValue(this, getString(R.string.pref_mobile_only), newValue);
                settings.forceUpdate(this);

                contactEntry.getAdapter().setShowMobileOnly(item.isChecked());
                contactEntry.getAdapter().notifyDataSetChanged();

                new ApiUtils().updateMobileOnly(Account.get(this).accountId, newValue);

                return true;
        }

        return false;
    }

    private void handleIntent() {
        if (getIntent().getAction() == null) {
            return;
        }

        Intent intent = getIntent();
        if (intent.getAction().equals(ACTION_EDIT_RECIPIENTS)) {
            String phoneNumbers = intent.getStringExtra(EXTRA_EDIT_RECIPIENTS_NUMBERS);
            String title = intent.getStringExtra(EXTRA_EDIT_RECIPIENTS_TITLE);
            contactEntry.post(() -> onClicked(title, phoneNumbers, null));
        } else if (intent.getAction().equals(Intent.ACTION_SENDTO)) {
            String[] phoneNumbers = PhoneNumberUtils
                    .parseAddress(Uri.decode(getIntent().getDataString()));

            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < phoneNumbers.length; i++) {
                builder.append(phoneNumbers[i]);
                if (i != phoneNumbers.length - 1) {
                    builder.append(", ");
                }
            }

            String numbers = builder.toString();
            String data = intent.getStringExtra("sms_body");
            
            if (data != null) {
                setupSend(data, MimeType.TEXT_PLAIN, false);
            }
            
            if (!numbers.isEmpty()) {
                showConversation(builder.toString());
            }
        } else if (getIntent().getAction().equals(Intent.ACTION_SEND)) {
            final String mimeType = getIntent().getType();
            String data = "";
            boolean isVcard = false;

            try {
                if (mimeType.equals(MimeType.TEXT_PLAIN)) {
                    data = getIntent().getStringExtra(Intent.EXTRA_TEXT);
                } else if (MimeType.isVcard(mimeType)) {
                    fab.setImageResource(R.drawable.ic_send);
                    isVcard = true;
                    data = getIntent().getParcelableExtra(Intent.EXTRA_STREAM).toString();
                    Log.v(TAG, "got vcard at: " + data);
                } else {
                    String tempData = getIntent().getParcelableExtra(Intent.EXTRA_STREAM).toString();
                    try {
                        File dst = new File(getFilesDir(),
                                ((int) (Math.random() * Integer.MAX_VALUE)) + "");
                        InputStream in = getContentResolver().openInputStream(Uri.parse(tempData));

                        OutputStream out = new FileOutputStream(dst);
                        byte[] buf = new byte[1024];
                        int len;
                        while((len=in.read(buf))>0){
                            out.write(buf,0,len);
                        }
                        out.close();
                        in.close();

                        data = Uri.fromFile(dst).toString();
                    } catch (Exception e) {
                        e.printStackTrace();
                        data = tempData;
                    }
                }
            } catch (Exception e) {

            }

            setupSend(data, mimeType, isVcard);

            if (getIntent().getExtras() != null && getIntent().getExtras()
                    .containsKey(MessengerChooserTargetService.EXTRA_CONVO_ID)) {
                shareWithDirectShare(data, mimeType, isVcard);
            }
        } else if (getIntent().getAction().equals(Intent.ACTION_VIEW)) {
            Bundle extras = getIntent().getExtras();
            String data = getIntent().getDataString();
            if (extras != null && extras.containsKey("sms_body")) {
                setupSend(extras.getString("sms_body"), MimeType.TEXT_PLAIN, false);
            } else if (data != null) {
                String body = NonStandardUriUtils.getQueryParams(data).get("body");
                if (data.contains("smsto:")) {
                    String to = data.replace("smsto:", "");
                    if (to.contains("?")) {
                        to = to.substring(0, to.indexOf("?"));
                    }
                    to = URLDecoder.decode(to);

                    if (body != null) {
                        applyShare(MimeType.TEXT_PLAIN, body, to);
                    } else {
                        showConversation(to);
                    }
                } else if (data.contains("sms:")) {
                    String to = data.replace("sms:", "");
                    if (to.contains("?")) {
                        to = to.substring(0, to.indexOf("?"));
                    }
                    to = URLDecoder.decode(to);

                    if (body != null) {
                        applyShare(MimeType.TEXT_PLAIN, body, to);
                    } else {
                        showConversation(to);
                    }
                }
            }
        }
    }

    private void setupSend(final String data, final String mimeType, final boolean isvCard) {
        fab.setOnClickListener(view -> {
            if (contactEntry.getRecipients().length > 0 && isvCard) {
                sendvCard(mimeType, data);
            } else if (contactEntry.getText().length() > 0) {
                applyShare(mimeType, data);
            }
        });
    }

    private void sendvCard(String mimeType, String data) {
        String phoneNumbers = getPhoneNumberFromContactEntry();
        sendvCard(mimeType, data, phoneNumbers);
    }

    private void sendvCard(String mimeType, String data, String phoneNumbers) {
        DataSource source = DataSource.getInstance(this);
        source.open();
        long conversationId = source.insertSentMessage(phoneNumbers, data, mimeType, this);
        Conversation conversation = source.getConversation(conversationId);

        Uri uri = new SendUtils(conversation != null ? conversation.simSubscriptionId : null)
                .send(this, "", phoneNumbers, Uri.parse(data), mimeType);
        Cursor cursor = source.searchMessages(data);

        if (cursor != null && cursor.moveToFirst()) {
            source.updateMessageData(cursor.getLong(0), uri.toString());
        }

        try {
            cursor.close();
        } catch (Exception e) { }

        source.close();
        finish();
    }

    private void sendvCard(String mimeType, String data, long conversationId) {
        DataSource source = DataSource.getInstance(this);
        source.open();
        Conversation conversation = source.getConversation(conversationId);

        Uri uri = new SendUtils(conversation.simSubscriptionId)
                .send(this, "", conversation.phoneNumbers, Uri.parse(data), mimeType);
        Cursor cursor = source.searchMessages(data);

        if (cursor != null && cursor.moveToFirst()) {
            source.updateMessageData(cursor.getLong(0), uri.toString());
        }

        try {
            cursor.close();
        } catch (Exception e) { }

        source.close();
        finish();
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

        long id = source.insertDraft(conversationId, data, mimeType);
        source.close();

        showConversation(conversationId);
    }

    private void shareWithDirectShare(String data, String mimeType, boolean isVcard) {
        DataSource source = DataSource.getInstance(this);
        source.open();
        Long conversationId = getIntent().getExtras().getLong(MessengerChooserTargetService.EXTRA_CONVO_ID);

        if (isVcard) {
            sendvCard(mimeType, data, conversationId);
        } else {
            source.insertDraft(conversationId, data, mimeType);
            showConversation(conversationId);
        }

        source.close();
    }

    @Override
    public void onClicked(String title, String phoneNumber, String imageUri) {
        // we have a few different cases:
        // 1.) Single recepient (with single number)
        // 2.) Group convo with custom title (1 name, multiple numbers)
        // 3.) Group convo with non custom title (x names, x numbers)

        String[] names = title.split(", ");
        String[] numbers = phoneNumber.split(", ");

        if (names.length == 1 && numbers.length == 1) {
            // Case 1
            if (imageUri == null) {
                contactEntry.submitItem(title, phoneNumber);
            } else {
                contactEntry.submitItem(title, phoneNumber, Uri.parse(imageUri));
            }
        } else {
            if (names.length == numbers.length) {
                // case 3
                for (int i = 0; i < names.length; i++) {
                    String name = names[i];
                    String number = numbers[i];
                    String image = ContactUtils.findImageUri(number, this) + "/photo";

                    contactEntry.submitItem(name, number, Uri.parse(image));
                }
            } else {
                // case 2
                for (int i = 0; i < numbers.length; i++) {
                    String number = numbers[i];
                    String name = ContactUtils.findContactNames(number, this);
                    String image = ContactUtils.findImageUri(number, this) + "/photo";

                    contactEntry.submitItem(name, number, Uri.parse(image));
                }
            }
        }
    }

}
