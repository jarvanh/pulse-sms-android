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
import android.annotation.TargetApi;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import xyz.klinker.messenger.BuildConfig;
import xyz.klinker.messenger.R;
import xyz.klinker.messenger.adapter.ImageViewerAdapter;
import xyz.klinker.messenger.shared.data.DataSource;
import xyz.klinker.messenger.shared.data.MimeType;
import xyz.klinker.messenger.shared.data.model.Message;
import xyz.klinker.messenger.shared.util.ActivityUtils;
import xyz.klinker.messenger.shared.util.ColorUtils;
import xyz.klinker.messenger.shared.util.FileUtils;
import xyz.klinker.messenger.shared.util.ImageUtils;
import xyz.klinker.messenger.shared.util.MediaSaver;

/**
 * Activity that allows you to scroll between images in a given conversation.
 */
public class ImageViewerActivity extends AppCompatActivity {

    public static final String EXTRA_CONVERSATION_ID = "conversation_id";
    public static final String EXTRA_MESSAGE_ID = "message_id";
    private static final int PERMISSION_STORAGE_REQUEST = 1;

    private ViewPager viewPager;
    private List<Message> messages;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_image_viewer);
        loadMessages();
        initViewPager();
        initToolbar();

        ActivityUtils.setTaskDescription(this, "", Color.BLACK);
    }

    private void loadMessages() {
        if (getIntent().getExtras() == null ||
                !getIntent().getExtras().containsKey(EXTRA_CONVERSATION_ID)) {
            finish();
            return;
        }

        long conversationId = getIntent().getLongExtra(EXTRA_CONVERSATION_ID, -1);

        messages = DataSource.INSTANCE.getMediaMessages(this, conversationId);
        if (messages.size() == 0) {
            Snackbar.make(findViewById(android.R.id.content), R.string.no_media, Snackbar.LENGTH_INDEFINITE)
                    .setAction(R.string.close, view -> finish()).show();
        }
    }

    private void initViewPager() {
        if (messages == null) {
            return;
        }

        long messageId = getIntent().getLongExtra(EXTRA_MESSAGE_ID, -1);

        viewPager = (ViewPager) findViewById(R.id.view_pager);
        viewPager.setAdapter(new ImageViewerAdapter(getSupportFragmentManager(), messages));

        if (messageId != -1) {
            for (int i = 0; i < messages.size(); i++) {
                if (messages.get(i).id == messageId) {
                    viewPager.setCurrentItem(i, false);
                    break;
                }
            }
        } else {
            viewPager.setCurrentItem(viewPager.getAdapter().getCount() - 1, false);
        }
    }

    private void initToolbar() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        setTitle("");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.image_viewer, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
        } else if (item.getItemId() == R.id.save) {
            Message message = messages.get(viewPager.getCurrentItem());
            new MediaSaver(this).saveMedia(message);
        } else if (item.getItemId() == R.id.share) {
            Message message = messages.get(viewPager.getCurrentItem());
            shareMessage(message);
        }

        return true;
    }

    @TargetApi(Build.VERSION_CODES.M)
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (grantResults.length == 0) {
            return;
        }

        if (requestCode == PERMISSION_STORAGE_REQUEST && grantResults[0] ==
                PackageManager.PERMISSION_GRANTED) {
            new MediaSaver(this).saveMedia(messages.get(viewPager.getCurrentItem()));
        } else {
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    PERMISSION_STORAGE_REQUEST);
        }
    }

    private void shareMessage(Message message) {
        Uri contentUri =
                ImageUtils.createContentUri(this, Uri.parse(message.data));

        Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND);
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
        shareIntent.setType(message.mimeType);
        startActivity(Intent.createChooser(shareIntent,
                getResources().getText(R.string.share_content)));
    }

}
