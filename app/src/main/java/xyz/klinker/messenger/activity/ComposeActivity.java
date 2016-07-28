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
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.MultiAutoCompleteTextView;
import android.widget.TextView;

import com.android.ex.chips.BaseRecipientAdapter;
import com.android.ex.chips.RecipientEditTextView;
import com.android.ex.chips.recipientchip.DrawableRecipientChip;

import xyz.klinker.messenger.R;
import xyz.klinker.messenger.data.DataSource;
import xyz.klinker.messenger.data.MimeType;
import xyz.klinker.messenger.data.model.Message;
import xyz.klinker.messenger.util.PhoneNumberUtils;

/**
 * Activity to display UI for creating a new conversation.
 */
public class ComposeActivity extends AppCompatActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_compose);

        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        final RecipientEditTextView contactEntry =
                (RecipientEditTextView) findViewById(R.id.contact_entry);
        contactEntry.setTokenizer(new MultiAutoCompleteTextView.CommaTokenizer());
        contactEntry.setAdapter(new BaseRecipientAdapter(BaseRecipientAdapter.QUERY_TYPE_PHONE, this));

        final FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showConversation(contactEntry.getRecipients());
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
    }

    private void showConversation(DrawableRecipientChip[] chips) {
        StringBuilder phoneNumbers = new StringBuilder();

        for (int i = 0; i < chips.length; i++) {
            phoneNumbers.append(PhoneNumberUtils
                    .clearFormatting(chips[i].getEntry().getDestination()));
            if (i != chips.length - 1) {
                phoneNumbers.append(", ");
            }
        }

        showConversation(phoneNumbers.toString());
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

        }
    }

}
