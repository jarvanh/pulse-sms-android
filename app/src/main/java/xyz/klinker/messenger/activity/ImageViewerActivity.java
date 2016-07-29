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

import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;

import java.util.List;

import xyz.klinker.messenger.R;
import xyz.klinker.messenger.adapter.ImageViewerAdapter;
import xyz.klinker.messenger.data.DataSource;
import xyz.klinker.messenger.data.model.Message;

/**
 * Activity that allows you to scroll between images in a given conversation.
 */
public class ImageViewerActivity extends AppCompatActivity {

    public static final String EXTRA_CONVERSATION_ID = "conversation_id";
    public static final String EXTRA_MESSAGE_ID = "message_id";

    private ViewPager viewPager;
    private List<Message> messages;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_image_viewer);
        loadMessages();
        initViewPager();
    }

    private void loadMessages() {
        if (getIntent().getExtras() == null ||
                !getIntent().getExtras().containsKey(EXTRA_CONVERSATION_ID) ||
                !getIntent().getExtras().containsKey(EXTRA_MESSAGE_ID)) {
            finish();
            return;
        }

        long conversationId = getIntent().getLongExtra(EXTRA_CONVERSATION_ID, -1);

        DataSource source = DataSource.getInstance(this);
        source.open();
        messages = source.getMediaMessages(conversationId);
        source.close();
    }

    private void initViewPager() {
        if (messages == null) {
            return;
        }

        long messageId = getIntent().getLongExtra(EXTRA_MESSAGE_ID, -1);

        viewPager = (ViewPager) findViewById(R.id.view_pager);
        viewPager.setAdapter(new ImageViewerAdapter(getSupportFragmentManager(), messages));

        for (int i = 0; i < messages.size(); i++) {
            if (messages.get(i).id == messageId) {
                viewPager.setCurrentItem(i, false);
                break;
            }
        }
    }

}
