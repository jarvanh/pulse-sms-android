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
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import xyz.klinker.messenger.R;
import xyz.klinker.messenger.adapter.ImageViewerAdapter;
import xyz.klinker.messenger.data.DataSource;
import xyz.klinker.messenger.data.MimeType;
import xyz.klinker.messenger.data.model.Message;
import xyz.klinker.messenger.util.FileUtils;
import xyz.klinker.messenger.util.ImageUtils;

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

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                saveMessage(message);
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                            PERMISSION_STORAGE_REQUEST);
                } else {
                    saveMessage(message);
                }
            }
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
        if (requestCode == PERMISSION_STORAGE_REQUEST && grantResults[0] ==
                PackageManager.PERMISSION_GRANTED) {
            saveMessage(messages.get(viewPager.getCurrentItem()));
        } else {
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    PERMISSION_STORAGE_REQUEST);
        }
    }

    private void saveMessage(Message message) {
        String extension = MimeType.getExtension(message.mimeType);
        File dst = new File(Environment.getExternalStorageDirectory() + "/Download",
                SimpleDateFormat.getDateTimeInstance().format(new Date(message.timestamp)) + extension);

        if (!dst.exists()) {
            try {
                dst.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (MimeType.isStaticImage(message.mimeType)) {
            try {
                Bitmap bmp = ImageUtils.getBitmap(this, message.data);
                FileOutputStream stream = new FileOutputStream(dst);
                bmp.compress(Bitmap.CompressFormat.JPEG, 95, stream);
                stream.close();

                ContentValues values = new ContentValues(3);

                values.put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis());
                values.put(MediaStore.Images.Media.MIME_TYPE, message.mimeType);
                values.put(MediaStore.MediaColumns.DATA, dst.getPath());

                getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

                Toast.makeText(this, R.string.saved, Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, R.string.failed_to_save, Toast.LENGTH_SHORT).show();
            }
        } else {
            try {
                InputStream in = getContentResolver().openInputStream(Uri.parse(message.data));
                FileUtils.copy(in, dst);
                Toast.makeText(this, R.string.saved, Toast.LENGTH_SHORT).show();
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, R.string.failed_to_save, Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void shareMessage(Message message) {
        Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND);
        shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.parse(message.data));
        shareIntent.setType(message.mimeType);
        startActivity(Intent.createChooser(shareIntent,
                getResources().getText(R.string.share_content)));
    }

}
